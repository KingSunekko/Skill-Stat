package com.example.skillstat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class NotificationsBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "NotificationsBottomSheet";
    private LinearLayout llNotificationsContainer;
    private DatabaseReference mDatabase;
    private String currentUid;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_notifications_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        llNotificationsContainer = view.findViewById(R.id.ll_notifications_container);
        View btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);
        
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        applyHoverEffect(btnMarkAllRead);

        loadCurrentUser();
        loadNotifications();
    }

    private void loadCurrentUser() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadNotifications() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).child("notifications")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || llNotificationsContainer == null) return;
                llNotificationsContainer.removeAllViews();

                if (!snapshot.exists()) {
                    showEmptyState();
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object data = ds.getValue();
                    if (data instanceof Map) {
                        Map<String, Object> notif = (Map<String, Object>) data;
                        addNotificationItemFromMap(ds.getKey(), notif);
                    } else if (data instanceof String) {
                        addSimpleNotification(ds.getKey(), (String) data);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Load failed", error.toException());
            }
        });
    }

    private void addNotificationItemFromMap(String key, Map<String, Object> notif) {
        String type = (String) notif.get("type");
        String message = (String) notif.get("message");
        
        View item = getLayoutInflater().inflate(R.layout.item_notification, llNotificationsContainer, false);
        TextView tvIcon = item.findViewById(R.id.tv_notif_icon);
        TextView tvTitle = item.findViewById(R.id.tv_notif_title);
        TextView tvDesc = item.findViewById(R.id.tv_notif_desc);
        TextView tvTime = item.findViewById(R.id.tv_notif_time);
        TextView btnAction = item.findViewById(R.id.btn_notif_action);
        TextView btnDismiss = item.findViewById(R.id.btn_notif_dismiss);

        if ("duel_invite".equals(type)) {
            tvIcon.setText("⚔️");
            tvTitle.setText("Duel Invitation");
            tvDesc.setText(message);
            btnAction.setText("Accept");
            btnDismiss.setText("Decline");
            
            String duelId = (String) notif.get("duelId");
            btnAction.setOnClickListener(v -> acceptDuel(key, duelId));
            btnDismiss.setOnClickListener(v -> declineDuel(key, duelId));
        } else if ("nudge".equals(type)) {
            tvIcon.setText("👋");
            tvTitle.setText("Friend Nudge");
            tvDesc.setText(message);
            btnAction.setText("Practice Now");
            btnAction.setOnClickListener(v -> {
                dismissNotification(key);
                dismiss();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                startActivity(intent);
            });
            btnDismiss.setOnClickListener(v -> dismissNotification(key));
        } else if ("duel_accepted".equals(type)) {
            tvIcon.setText("🔥");
            tvTitle.setText("Duel Started!");
            tvDesc.setText(message);
            btnAction.setText("View Duel");
            String duelId = (String) notif.get("duelId");
            btnAction.setOnClickListener(v -> {
                dismissNotification(key);
                Intent intent = new Intent(getActivity(), ActiveDuelActivity.class);
                intent.putExtra("duel_id", duelId);
                startActivity(intent);
                dismiss();
            });
            btnDismiss.setOnClickListener(v -> dismissNotification(key));
        } else {
            tvIcon.setText("🔔");
            tvTitle.setText("Notification");
            tvDesc.setText(message);
            btnAction.setVisibility(View.GONE);
            btnDismiss.setOnClickListener(v -> dismissNotification(key));
        }

        tvTime.setText("just now");
        llNotificationsContainer.addView(item, 0);
    }

    private void addSimpleNotification(String key, String message) {
        View item = getLayoutInflater().inflate(R.layout.item_notification, llNotificationsContainer, false);
        ((TextView) item.findViewById(R.id.tv_notif_icon)).setText("🔔");
        ((TextView) item.findViewById(R.id.tv_notif_title)).setText("Alert");
        ((TextView) item.findViewById(R.id.tv_notif_desc)).setText(message);
        item.findViewById(R.id.btn_notif_action).setVisibility(View.GONE);
        item.findViewById(R.id.btn_notif_dismiss).setOnClickListener(v -> dismissNotification(key));
        llNotificationsContainer.addView(item, 0);
    }

    private void acceptDuel(String notifKey, String duelId) {
        mDatabase.child("duels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Duel duel = snapshot.getValue(Duel.class);
                if (duel != null && "pending".equals(duel.getStatus())) {
                    mDatabase.child("users").child(duel.getInitiatorUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot initSnap) {
                            User initiator = initSnap.getValue(User.class);
                            mDatabase.child("users").child(duel.getOpponentUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot oppSnap) {
                                    User opponent = oppSnap.getValue(User.class);
                                    if (initiator != null && opponent != null) {
                                        if (initiator.getTotalPoints() >= duel.getWagerAmount() && opponent.getTotalPoints() >= duel.getWagerAmount()) {
                                            // Capture fresh start mastery as Double
                                            double initStart = initiator.getSkillMastery().getOrDefault(duel.getSkillName(), 0.0);
                                            double oppStart = opponent.getSkillMastery().getOrDefault(duel.getSkillName(), 0.0);
                                            duel.setInitiatorStartMastery(initStart);
                                            duel.setOpponentStartMastery(oppStart);
                                            duel.setInitiatorCurrentMastery(initStart);
                                            duel.setOpponentCurrentMastery(oppStart);
                                            
                                            startDuel(notifKey, duelId, duel, opponent.getUsername());
                                        } else {
                                            Toast.makeText(getContext(), "One player no longer has enough XP!", Toast.LENGTH_SHORT).show();
                                            declineDuel(notifKey, duelId);
                                        }
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void startDuel(String notifKey, String duelId, Duel duel, String opponentName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("duels/" + duelId + "/status", "active");
        updates.put("duels/" + duelId + "/startTime", ServerValue.TIMESTAMP);
        updates.put("duels/" + duelId + "/initiatorStartMastery", duel.getInitiatorStartMastery());
        updates.put("duels/" + duelId + "/opponentStartMastery", duel.getOpponentStartMastery());
        updates.put("duels/" + duelId + "/initiatorCurrentMastery", duel.getInitiatorCurrentMastery());
        updates.put("duels/" + duelId + "/opponentCurrentMastery", duel.getOpponentCurrentMastery());
        
        int wager = duel.getWagerAmount();
        updates.put("users/" + duel.getInitiatorUid() + "/totalPoints", ServerValue.increment(-wager));
        updates.put("users/" + duel.getOpponentUid() + "/totalPoints", ServerValue.increment(-wager));
        
        updates.put("users/" + duel.getInitiatorUid() + "/activeDuels/" + duelId, true);
        updates.put("users/" + duel.getOpponentUid() + "/activeDuels/" + duelId, true);
        
        // Notify initiator
        Map<String, Object> acceptNotif = new HashMap<>();
        acceptNotif.put("type", "duel_accepted");
        acceptNotif.put("duelId", duelId);
        acceptNotif.put("message", opponentName + " accepted your " + duel.getSkillName() + " challenge! ⚔️");
        acceptNotif.put("timestamp", ServerValue.TIMESTAMP);
        String notifPath = "users/" + duel.getInitiatorUid() + "/notifications/" + mDatabase.push().getKey();
        updates.put(notifPath, acceptNotif);

        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            dismissNotification(notifKey);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Duel Started! ⚔️", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), ActiveDuelActivity.class);
                intent.putExtra("duel_id", duelId);
                startActivity(intent);
                dismiss();
            }
        });
    }

    private void declineDuel(String notifKey, String duelId) {
        mDatabase.child("duels").child(duelId).removeValue();
        dismissNotification(notifKey);
    }

    private void dismissNotification(String key) {
        mDatabase.child("users").child(currentUid).child("notifications").child(key).removeValue();
    }

    private void markAllAsRead() {
        mDatabase.child("users").child(currentUid).child("notifications").removeValue();
    }

    private void showEmptyState() {
        if (!isAdded() || getContext() == null) return;
        TextView tv = new TextView(getContext());
        tv.setText("No new notifications");
        tv.setTextColor(0x88FFFFFF);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(0, 50, 0, 50);
        llNotificationsContainer.addView(tv);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyHoverEffect(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            return false;
        });
    }

    @Override
    public int getTheme() {
        return R.style.CustomBottomSheetDialogTheme;
    }
}
