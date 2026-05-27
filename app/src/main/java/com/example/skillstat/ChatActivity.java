package com.example.skillstat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.skillstat.models.JointQuest;
import com.example.skillstat.models.Message;
import com.example.skillstat.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ChatActivity extends AppCompatActivity {

    private String receiverUid, receiverName, receiverAvatar;
    private String senderUid, chatRoomId;
    private TextView tvName, tvStatus;
    private ImageView ivAvatar;
    private View vOnlineIndicator;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnAttach;
    private View btnQuest, btnDuel;

    // Quest UI
    private View cardQuestProgress;
    private LinearProgressIndicator pbQuestProgress;
    private TextView tvQuestStats, tvQuestDaysLeft, tvMyContribution, tvPartnerContribution;
    private boolean isCompletingQuest = false;
    private String currentListeningQuestId = null;

    private ChatAdapter chatAdapter;
    private List<Message> messageList;
    private DatabaseReference mDatabase, currentChatRef;
    private ValueEventListener messagesListener, seenListener, onlineListener, questListener, activeQuestsListener;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) uploadImage(uri); }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        receiverUid = getIntent().getStringExtra("receiver_uid");
        receiverName = getIntent().getStringExtra("receiver_name");
        receiverAvatar = getIntent().getStringExtra("receiver_avatar");
        senderUid = FirebaseAuth.getInstance().getUid();

        if (receiverUid == null || senderUid == null) { finish(); return; }

        chatRoomId = senderUid.compareTo(receiverUid) < 0 ? senderUid + "_" + receiverUid : receiverUid + "_" + senderUid;
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentChatRef = mDatabase.child("chats").child(chatRoomId);

        initViews();
        handleInsets();

        tvName.setText(receiverName);
        
        // Load Avatar Image
        int resId = getResources().getIdentifier(receiverAvatar, "drawable", getPackageName());
        if (resId != 0) ivAvatar.setImageResource(resId);
        else ivAvatar.setImageResource(R.drawable.prof1);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, receiverAvatar);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rvMessages.setLayoutManager(lm);
        rvMessages.setAdapter(chatAdapter);

        // Scroll to bottom when keyboard appears
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && !messageList.isEmpty()) rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(messageList.size() - 1), 100);
        });

        readMessages();
        updateOnlineStatus();
        resetUnreadCount();
        seenMessage();
        listenForActiveQuest();

        btnSend.setOnClickListener(v -> {
            String msg = etMessage.getText().toString().trim();
            if (!TextUtils.isEmpty(msg)) sendMessage(msg, null);
        });
        btnAttach.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        
        btnQuest.setOnClickListener(v -> showJointQuestDialog());
        btnDuel.setOnClickListener(v -> {
            Intent intent = new Intent(this, StartDuelActivity.class);
            intent.putExtra("opponent_uid", receiverUid);
            startActivity(intent);
        });

        setupQuickReplies();
    }

    private void listenForActiveQuest() {
        activeQuestsListener = mDatabase.child("users").child(senderUid).child("activeQuests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (cardQuestProgress != null) cardQuestProgress.setVisibility(View.GONE);
                if (!snapshot.exists()) return;
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String questId = ds.getKey();
                    if (questId == null) continue;
                    
                    mDatabase.child("quests").child(questId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot qSnap) {
                            if (isFinishing() || isDestroyed()) return;
                            JointQuest quest = qSnap.getValue(JointQuest.class);
                            if (quest != null && ("active".equals(quest.getStatus()) || "pending".equals(quest.getStatus()))) {
                                if (quest.getCreatorUid().equals(receiverUid) || quest.getPartnerUid().equals(receiverUid)) {
                                    updateQuestUI(quest);
                                    setupQuestLiveListener(questId);
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupQuestLiveListener(String questId) {
        if (questListener != null && currentListeningQuestId != null) {
            mDatabase.child("quests").child(currentListeningQuestId).removeEventListener(questListener);
        }
        
        currentListeningQuestId = questId;
        questListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                JointQuest quest = snapshot.getValue(JointQuest.class);
                if (quest != null && ("active".equals(quest.getStatus()) || "pending".equals(quest.getStatus()))) {
                    updateQuestUI(quest);
                } else {
                    if (quest != null && "completed".equals(quest.getStatus()) && !isCompletingQuest) {
                        showCompletionDialog(quest);
                    }
                    if (cardQuestProgress != null) cardQuestProgress.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("quests").child(questId).addValueEventListener(questListener);
    }

    private void updateQuestUI(JointQuest quest) {
        if (quest == null || cardQuestProgress == null) return;

        long millisLeft = quest.getEndTime() - System.currentTimeMillis();
        if (millisLeft <= 0 && !"completed".equals(quest.getStatus())) {
            cardQuestProgress.setVisibility(View.GONE);
            cleanupExpiredQuest(quest.getQuestId(), quest);
            return;
        }

        cardQuestProgress.setVisibility(View.VISIBLE);
        
        double total = quest.getTotalMinutes();
        int goal = quest.getGoalMinutes();
        if (goal <= 0) goal = 1;

        pbQuestProgress.setMax(1000);
        int progress = (int) Math.min(1000, (total * 1000.0) / goal);
        pbQuestProgress.setProgress(progress, true);
        
        tvQuestStats.setText(String.format(Locale.US, "%.1f / %d minutes completed", total, goal));
        
        double myMins = quest.getCreatorUid().equals(senderUid) ? quest.getCreatorMinutes() : quest.getPartnerMinutes();
        double partnerMins = quest.getCreatorUid().equals(receiverUid) ? quest.getCreatorMinutes() : quest.getPartnerMinutes();
        
        tvMyContribution.setText(String.format(Locale.US, "You: %.1fm", myMins));
        tvPartnerContribution.setText(String.format(Locale.US, "Partner: %.1fm", partnerMins));
        
        if (millisLeft > 0) {
            long days = TimeUnit.MILLISECONDS.toDays(millisLeft);
            if (days > 0) {
                tvQuestDaysLeft.setText(days + (days == 1 ? " day left" : " days left"));
            } else {
                long hours = TimeUnit.MILLISECONDS.toHours(millisLeft);
                if (hours > 0) {
                    tvQuestDaysLeft.setText(hours + (hours == 1 ? " hour left" : " hours left"));
                } else {
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(millisLeft);
                    if (minutes > 0) {
                        tvQuestDaysLeft.setText(minutes + (minutes == 1 ? " min left" : " mins left"));
                    } else {
                        tvQuestDaysLeft.setText("Ending soon...");
                    }
                }
            }
        }

        if (total >= goal && "active".equals(quest.getStatus()) && !isCompletingQuest) {
            completeQuest(quest);
        }
    }

    private void cleanupExpiredQuest(String questId, JointQuest quest) {
        if (quest == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("quests/" + questId + "/status", "failed");
        updates.put("users/" + quest.getCreatorUid() + "/activeQuests/" + questId, null);
        updates.put("users/" + quest.getPartnerUid() + "/activeQuests/" + questId, null);
        mDatabase.updateChildren(updates);
    }

    private void completeQuest(JointQuest quest) {
        if (isCompletingQuest) return;
        isCompletingQuest = true;
        String questId = quest.getQuestId();
        
        if (cardQuestProgress != null) cardQuestProgress.setVisibility(View.GONE);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("quests/" + questId + "/status", "completed");
        updates.put("users/" + quest.getCreatorUid() + "/activeQuests/" + questId, null);
        updates.put("users/" + quest.getPartnerUid() + "/activeQuests/" + questId, null);
        updates.put("users/" + quest.getCreatorUid() + "/totalPoints", ServerValue.increment(500));
        updates.put("users/" + quest.getPartnerUid() + "/totalPoints", ServerValue.increment(500));
        
        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            sendMessage("🎉 MISSION ACCOMPLISHED! We completed our Joint Quest of " + quest.getGoalMinutes() + " mins! 🏆", null);
            showCompletionDialog(quest);
        });
    }

    private void showCompletionDialog(JointQuest quest) {
        if (!isFinishing()) {
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                    .setTitle("Quest Completed! 🏆")
                    .setMessage("Amazing teamwork! You and " + receiverName + " reached the goal of " + quest.getGoalMinutes() + " minutes. Both earned +500 XP!")
                    .setPositiveButton("Awesome!", null)
                    .show();
        }
    }

    private void showJointQuestDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_joint_quest, null);
        EditText etMinutes = dialogView.findViewById(R.id.et_quest_minutes);
        EditText etDays = dialogView.findViewById(R.id.et_quest_days);

        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setView(dialogView)
                .setPositiveButton("Start Quest", (dialog, which) -> {
                    String minsStr = etMinutes.getText().toString();
                    String daysStr = etDays.getText().toString();
                    if (!minsStr.isEmpty() && !daysStr.isEmpty()) {
                        int mins = Integer.parseInt(minsStr);
                        int days = Integer.parseInt(daysStr);
                        if (mins > 0 && days > 0) {
                            isCompletingQuest = false;
                            startQuest(mins, days);
                        } else {
                            Toast.makeText(this, "Please enter values > 0", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startQuest(int goalMins, int days) {
        String questId = mDatabase.child("quests").push().getKey();
        if (questId == null) return;

        JointQuest quest = new JointQuest(questId, senderUid, receiverUid, "General Mastery", goalMins, days);
        quest.setStatus("active");
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("quests/" + questId, quest);
        updates.put("users/" + senderUid + "/activeQuests/" + questId, true);
        updates.put("users/" + receiverUid + "/activeQuests/" + questId, true);
        
        mDatabase.updateChildren(updates);

        sendMessage("🤝 I started a Joint Quest! Target: " + goalMins + " mins in " + days + " days. Let's do this!", null);
        Toast.makeText(this, "Joint Quest Started! 🤝", Toast.LENGTH_SHORT).show();
    }

    private void setupQuickReplies() {
        View.OnClickListener ql = v -> sendMessage(((TextView)v).getText().toString(), null);
        findViewById(R.id.qr_keep_grinding).setOnClickListener(ql);
        findViewById(R.id.qr_lets_duel).setOnClickListener(ql);
        findViewById(R.id.qr_go_practice).setOnClickListener(ql);
    }

    private void initViews() {
        tvName = findViewById(R.id.tv_name); tvStatus = findViewById(R.id.tv_status);
        ivAvatar = findViewById(R.id.iv_avatar); vOnlineIndicator = findViewById(R.id.v_online_indicator);
        rvMessages = findViewById(R.id.rv_messages); etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send); btnAttach = findViewById(R.id.btn_attach);
        btnQuest = findViewById(R.id.btn_quest_header); btnDuel = findViewById(R.id.btn_duel_header);
        
        cardQuestProgress = findViewById(R.id.card_quest_progress);
        pbQuestProgress = findViewById(R.id.pb_quest_progress);
        tvQuestStats = findViewById(R.id.tv_quest_stats);
        tvQuestDaysLeft = findViewById(R.id.tv_quest_days_left);
        tvMyContribution = findViewById(R.id.tv_my_contribution);
        tvPartnerContribution = findViewById(R.id.tv_partner_contribution);
        
        if (cardQuestProgress != null) {
            cardQuestProgress.setOnLongClickListener(v -> {
                showQuestOptionsDialog();
                return true;
            });
        }
        
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void showQuestOptionsDialog() {
        if (currentListeningQuestId == null) return;
        new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Quest Options")
                .setMessage("Do you want to cancel this Joint Quest?")
                .setPositiveButton("Cancel Quest", (dialog, which) -> cancelQuest(currentListeningQuestId))
                .setNegativeButton("Back", null)
                .show();
    }

    private void cancelQuest(String questId) {
        mDatabase.child("quests").child(questId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                JointQuest quest = snapshot.getValue(JointQuest.class);
                if (quest != null) {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("quests/" + questId + "/status", "cancelled");
                    updates.put("users/" + quest.getCreatorUid() + "/activeQuests/" + questId, null);
                    updates.put("users/" + quest.getPartnerUid() + "/activeQuests/" + questId, null);
                    mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(ChatActivity.this, "Quest cancelled", Toast.LENGTH_SHORT).show();
                        if (cardQuestProgress != null) cardQuestProgress.setVisibility(View.GONE);
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleInsets() {
        View root = findViewById(R.id.main_chat_layout);
        View bottomControls = findViewById(R.id.ll_bottom_controls);
        View headerContainer = findViewById(R.id.ll_header_container);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sb = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            if (headerContainer != null) {
                int extraPadding = (int) (10 * getResources().getDisplayMetrics().density);
                headerContainer.setPadding(0, sb.top + extraPadding, 0, 0);
            }
            if (bottomControls != null) bottomControls.setPadding(0, 0, 0, Math.max(sb.bottom, ime.bottom));
            return insets;
        });
    }

    private void sendMessage(String msg, String imageUrl) {
        String msgId = currentChatRef.push().getKey();
        if (msgId == null) return;
        
        long time = System.currentTimeMillis();
        Message m = new Message(senderUid, receiverUid, msg, imageUrl, time);
        String lastText = (msg != null) ? msg : "Image sent";

        Map<String, Object> updates = new HashMap<>();
        updates.put("chats/" + chatRoomId + "/messages/" + msgId, m);
        updates.put("chats/" + chatRoomId + "/lastMessage", lastText);
        updates.put("chats/" + chatRoomId + "/lastTimestamp", time);

        // Update Recent Chats using leaf-only paths to prevent ancestor conflict crash
        String sPath = "users/" + senderUid + "/recentChats/" + receiverUid;
        updates.put(sPath + "/lastMessage", lastText);
        updates.put(sPath + "/lastMessageTimestamp", time);

        String rPath = "users/" + receiverUid + "/recentChats/" + senderUid;
        updates.put(rPath + "/lastMessage", lastText);
        updates.put(rPath + "/lastMessageTimestamp", time);
        updates.put(rPath + "/unreadCount", ServerValue.increment(1));

        mDatabase.updateChildren(updates).addOnFailureListener(e -> {
            Toast.makeText(ChatActivity.this, "Send failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });

        etMessage.setText("");
    }

    private void uploadImage(Uri uri) {
        String fileName = UUID.randomUUID().toString();
        StorageReference ref = FirebaseStorage.getInstance().getReference("chat_images/" + fileName);
        ref.putFile(uri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(url -> sendMessage(null, url.toString())));
    }

    private void readMessages() {
        currentChatRef.child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message m = ds.getValue(Message.class);
                    if (m != null) messageList.add(m);
                }
                chatAdapter.notifyDataSetChanged();
                if (!messageList.isEmpty()) rvMessages.smoothScrollToPosition(messageList.size() - 1);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateOnlineStatus() {
        DatabaseReference myStatusRef = mDatabase.child("users").child(senderUid).child("online");
        myStatusRef.setValue(true);
        myStatusRef.onDisconnect().setValue(false);

        mDatabase.child("users").child(receiverUid).child("online").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean online = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                vOnlineIndicator.setBackgroundResource(online ? R.drawable.shape_notification_green : R.drawable.shape_notification_red);
                tvStatus.setText(online ? "• Online" : "• Offline");
                tvStatus.setTextColor(ContextCompat.getColor(ChatActivity.this, online ? R.color.splash_green : android.R.color.darker_gray));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void resetUnreadCount() {
        mDatabase.child("users").child(senderUid).child("recentChats").child(receiverUid).child("unreadCount").setValue(0);
    }

    private void seenMessage() {
        currentChatRef.child("messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message m = ds.getValue(Message.class);
                    if (m != null && m.getReceiverId().equals(senderUid) && !m.isSeen()) {
                        ds.getRef().child("seen").setValue(true);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (questListener != null && currentListeningQuestId != null) {
            mDatabase.child("quests").child(currentListeningQuestId).removeEventListener(questListener);
        }
        if (activeQuestsListener != null) {
            mDatabase.child("users").child(senderUid).child("activeQuests").removeEventListener(activeQuestsListener);
        }
    }
}
