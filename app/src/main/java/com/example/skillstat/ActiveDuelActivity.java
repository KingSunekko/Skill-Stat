package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.TimeUnit;

public class ActiveDuelActivity extends AppCompatActivity {

    private static final String TAG = "ActiveDuelActivity";
    
    private TextView tvMyAvatar, tvMyName, tvMyGain, tvMyMasteryPercent;
    private TextView tvOpponentAvatar, tvOpponentName, tvOpponentGain, tvOpponentMasteryPercent;
    private TextView tvDuelInfoBadge, tvDuelStatusText, tvDuelStatusIcon;
    private View vMyMasteryFill, vOpponentMasteryFill, vMyMasteryBg, vOpponentMasteryBg;
    private TextView btnPractice;
    
    private DatabaseReference mDatabase;
    private String currentUid;
    private String duelId;
    private Duel currentDuel;
    private boolean isDuelFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_duel);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();
        duelId = getIntent().getStringExtra("duel_id");

        if (duelId == null) {
            Toast.makeText(this, "Error: Duel ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadDuelData();
    }

    private void initViews() {
        tvMyAvatar = findViewById(R.id.tv_my_avatar);
        tvMyName = findViewById(R.id.tv_my_name);
        tvMyGain = findViewById(R.id.tv_my_gain);
        tvMyMasteryPercent = findViewById(R.id.tv_my_mastery_percent);
        vMyMasteryFill = findViewById(R.id.v_my_mastery_fill);
        vMyMasteryBg = findViewById(R.id.v_my_mastery_bg);

        tvOpponentAvatar = findViewById(R.id.tv_opponent_avatar);
        tvOpponentName = findViewById(R.id.tv_opponent_name);
        tvOpponentGain = findViewById(R.id.tv_opponent_gain);
        tvOpponentMasteryPercent = findViewById(R.id.tv_opponent_mastery_percent);
        vOpponentMasteryFill = findViewById(R.id.v_opponent_mastery_fill);
        vOpponentMasteryBg = findViewById(R.id.v_opponent_mastery_bg);

        tvDuelInfoBadge = findViewById(R.id.tv_duel_info_badge);
        tvDuelStatusText = findViewById(R.id.tv_duel_status_text);
        tvDuelStatusIcon = findViewById(R.id.tv_duel_status_icon);
        btnPractice = findViewById(R.id.btn_practice_now_duel);

        findViewById(R.id.btn_back_active).setOnClickListener(v -> finish());
        findViewById(R.id.btn_back_to_friends).setOnClickListener(v -> finish());
        
        btnPractice.setOnClickListener(v -> {
            if (currentDuel != null && !isDuelFinished) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("navigate_to", "practice");
                intent.putExtra("skill_name", currentDuel.getSkillName());
                startActivity(intent);
            }
        });
    }

    private void loadDuelData() {
        mDatabase.child("duels").child(duelId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentDuel = snapshot.getValue(Duel.class);
                if (currentDuel != null) {
                    checkDuelExpiry();
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Load failed", error.toException());
            }
        });
    }

    private void checkDuelExpiry() {
        if ("completed".equals(currentDuel.getStatus())) {
            isDuelFinished = true;
            return;
        }

        long elapsed = System.currentTimeMillis() - currentDuel.getStartTime();
        long totalDuration = TimeUnit.DAYS.toMillis(currentDuel.getDurationDays());
        
        if (elapsed >= totalDuration) {
            finishDuel();
        }
    }

    private void finishDuel() {
        isDuelFinished = true;
        mDatabase.child("duels").child(duelId).child("status").setValue("completed");
        
        int myGain = currentUid.equals(currentDuel.getInitiatorUid()) ? 
            (currentDuel.getInitiatorCurrentMastery() - currentDuel.getInitiatorStartMastery()) : 
            (currentDuel.getOpponentCurrentMastery() - currentDuel.getOpponentStartMastery());
            
        int oppGain = currentUid.equals(currentDuel.getInitiatorUid()) ? 
            (currentDuel.getOpponentCurrentMastery() - currentDuel.getOpponentStartMastery()) : 
            (currentDuel.getInitiatorCurrentMastery() - currentDuel.getInitiatorStartMastery());

        if (myGain > oppGain) {
            // Reward user with bonus XP
            mDatabase.child("users").child(currentUid).child("totalPoints").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Integer currentPoints = snapshot.getValue(Integer.class);
                    if (currentPoints != null) {
                        mDatabase.child("users").child(currentUid).child("totalPoints").setValue(currentPoints + 500); // 500 XP Winner Bonus
                        Toast.makeText(ActiveDuelActivity.this, "CONGRATS! You won the duel! +500 XP 🏆", Toast.LENGTH_LONG).show();
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void updateUI() {
        boolean isInitiator = currentDuel.getInitiatorUid().equals(currentUid);
        String opponentUid = isInitiator ? currentDuel.getOpponentUid() : currentDuel.getInitiatorUid();

        fetchUserData(currentUid, true);
        fetchUserData(opponentUid, false);

        if (isDuelFinished) {
            tvDuelInfoBadge.setText(currentDuel.getSkillName() + " — DUEL FINISHED");
            btnPractice.setVisibility(View.GONE);
        } else {
            long elapsed = System.currentTimeMillis() - currentDuel.getStartTime();
            long totalDuration = TimeUnit.DAYS.toMillis(currentDuel.getDurationDays());
            long remaining = Math.max(0, totalDuration - elapsed);
            
            long daysLeft = TimeUnit.MILLISECONDS.toDays(remaining);
            long hoursLeft = TimeUnit.MILLISECONDS.toHours(remaining) % 24;
            tvDuelInfoBadge.setText(currentDuel.getSkillName() + " — " + daysLeft + "d " + hoursLeft + "h left");
        }
    }

    private void fetchUserData(String uid, boolean isMe) {
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (isMe) {
                        tvMyAvatar.setText(user.getAvatarUrl());
                        tvMyName.setText("You");
                        int currentMastery = user.getSkillMastery().getOrDefault(currentDuel.getSkillName(), 0);
                        int startMastery = currentDuel.getInitiatorUid().equals(currentUid) ? 
                                currentDuel.getInitiatorStartMastery() : currentDuel.getOpponentStartMastery();
                        int gain = currentMastery - startMastery;
                        tvMyGain.setText("+" + gain + "%");
                        tvMyMasteryPercent.setText(currentMastery + "%");
                        updateProgressBar(vMyMasteryBg, vMyMasteryFill, currentMastery);
                        updateDuelMastery(isMe, currentMastery);
                    } else {
                        tvOpponentAvatar.setText(user.getAvatarUrl());
                        tvOpponentName.setText(user.getUsername());
                        int currentMastery = user.getSkillMastery().getOrDefault(currentDuel.getSkillName(), 0);
                        int startMastery = currentDuel.getInitiatorUid().equals(uid) ? 
                                currentDuel.getInitiatorStartMastery() : currentDuel.getOpponentStartMastery();
                        int gain = currentMastery - startMastery;
                        tvOpponentGain.setText("+" + gain + "%");
                        tvOpponentMasteryPercent.setText(currentMastery + "%");
                        updateProgressBar(vOpponentMasteryBg, vOpponentMasteryFill, currentMastery);
                        updateDuelMastery(isMe, currentMastery);
                    }
                    updateStatusText();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDuelMastery(boolean isMe, int mastery) {
        if (isDuelFinished) return;
        String field = "";
        if (currentDuel.getInitiatorUid().equals(currentUid)) {
            field = isMe ? "initiatorCurrentMastery" : "opponentCurrentMastery";
        } else {
            field = isMe ? "opponentCurrentMastery" : "initiatorCurrentMastery";
        }
        mDatabase.child("duels").child(duelId).child(field).setValue(mastery);
    }

    private void updateStatusText() {
        int myGain = parseGain(tvMyGain.getText().toString());
        int opponentGain = parseGain(tvOpponentGain.getText().toString());

        if (isDuelFinished) {
            if (myGain > opponentGain) {
                tvDuelStatusText.setText("VICTORY! You won the duel!");
                tvDuelStatusIcon.setText("🏆");
            } else if (opponentGain > myGain) {
                tvDuelStatusText.setText("DEFEAT! Better luck next time.");
                tvDuelStatusIcon.setText("💀");
            } else {
                tvDuelStatusText.setText("DRAW! It was a perfect match.");
                tvDuelStatusIcon.setText("🤝");
            }
            return;
        }

        if (myGain > opponentGain) {
            tvDuelStatusText.setText("🏆 You're leading by +" + (myGain - opponentGain) + "%!");
            tvDuelStatusIcon.setText("🥇");
        } else if (opponentGain > myGain) {
            tvDuelStatusText.setText(tvOpponentName.getText() + " is leading by +" + (opponentGain - myGain) + "%!");
            tvDuelStatusIcon.setText("🥈");
        } else {
            tvDuelStatusText.setText("It's a dead heat! Keep practicing.");
            tvDuelStatusIcon.setText("⚔️");
        }
    }

    private int parseGain(String text) {
        try {
            return Integer.parseInt(text.replace("+", "").replace("%", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private void updateProgressBar(View bg, View fill, int progress) {
        if (bg == null || fill == null) return;
        bg.post(() -> {
            ViewGroup.LayoutParams params = fill.getLayoutParams();
            params.width = (int) (bg.getWidth() * (progress / 100f));
            fill.setLayoutParams(params);
        });
    }
}
