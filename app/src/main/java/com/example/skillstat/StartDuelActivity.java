package com.example.skillstat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StartDuelActivity extends AppCompatActivity {

    private LinearLayout llSkillsContainer;
    private TextView btnDuration3, btnDuration7, btnDuration14;
    private TextView btnWager0, btnWager100, btnWager250, btnWager500;
    private TextView btnChallenge;
    private TextView tvOpponentName, tvOpponentHandle, tvOpponentStreak;
    private ImageView ivOpponentAvatar;

    private String selectedSkill = "";
    private int selectedDays = 7;
    private int selectedWager = 0;
    private String opponentUid;
    private DatabaseReference mDatabase;
    private User currentUserData;
    private User opponentUserData;
    private boolean isSending = false;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_duel);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        opponentUid = getIntent().getStringExtra("opponent_uid");
        if (opponentUid == null) opponentUid = getIntent().getStringExtra("friend_uid");

        if (opponentUid == null) {
            Toast.makeText(this, "No opponent selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViewById(R.id.btn_back_duel).setOnClickListener(v -> finish());

        llSkillsContainer = findViewById(R.id.ll_duel_skills_list);
        btnDuration3 = findViewById(R.id.btn_duration_3);
        btnDuration7 = findViewById(R.id.btn_duration_7);
        btnDuration14 = findViewById(R.id.btn_duration_14);

        btnWager0 = findViewById(R.id.btn_wager_0);
        btnWager100 = findViewById(R.id.btn_wager_100);
        btnWager250 = findViewById(R.id.btn_wager_250);
        btnWager500 = findViewById(R.id.btn_wager_500);

        btnChallenge = findViewById(R.id.btn_challenge_final);

        tvOpponentName = findViewById(R.id.tv_opponent_name);
        ivOpponentAvatar = findViewById(R.id.iv_opponent_avatar);
        tvOpponentHandle = findViewById(R.id.tv_opponent_handle);
        tvOpponentStreak = findViewById(R.id.tv_opponent_streak);

        setupDurationListeners();
        setupWagerListeners();
        loadMySkills();
        loadOpponentData();

        btnChallenge.setOnClickListener(v -> {
            triggerVibration(20);
            if (isSending) return;
            if (selectedSkill.isEmpty()) {
                Toast.makeText(this, "Please select a skill to compete in!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentUserData != null && currentUserData.getTotalPoints() < selectedWager) {
                Toast.makeText(this, "You don't have enough XP for this wager!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendDuelInvite();
        });
    }

    private void triggerVibration(int ms) {
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(ms);
        }
    }

    private void loadOpponentData() {
        mDatabase.child("users").child(opponentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                opponentUserData = snapshot.getValue(User.class);
                if (opponentUserData != null) {
                    tvOpponentName.setText(opponentUserData.getUsername());
                    int resId = getResources().getIdentifier(opponentUserData.getAvatarUrl(), "drawable", getPackageName());
                    if (resId != 0) ivOpponentAvatar.setImageResource(resId);

                    tvOpponentHandle.setText(String.format("@%s", opponentUserData.getUsername().toLowerCase(Locale.US)));
                    tvOpponentStreak.setText(String.format(Locale.US, "🔥 %dd streak", opponentUserData.getStreak()));
                    btnChallenge.setText(String.format("⚔️ CHALLENGE %s!", opponentUserData.getUsername().toUpperCase(Locale.US)));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMySkills() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        mDatabase.child("users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing()) return;
                currentUserData = snapshot.getValue(User.class);
                if (currentUserData != null && llSkillsContainer != null) {
                    llSkillsContainer.removeAllViews();
                    List<String> skills = currentUserData.getSkills();
                    Map<String, Double> masteryMap = currentUserData.getSkillMastery();
                    if (skills != null) {
                        for (String skillName : skills) {
                            double progress = masteryMap.getOrDefault(skillName, 0.0);
                            addSkillOption(skillName, progress);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSkillOption(String name, double mastery) {
        View item = getLayoutInflater().inflate(R.layout.layout_skill_duel_option, llSkillsContainer, false);
        ((TextView) item.findViewById(R.id.tv_duel_skill_name)).setText(name);
        ((TextView) item.findViewById(R.id.tv_duel_skill_mastery)).setText(String.format(Locale.US, "Your mastery: %.0f%%", mastery));

        View bg = item.findViewById(R.id.v_duel_progress_bg);
        View fill = item.findViewById(R.id.v_duel_progress_fill);
        bg.post(() -> {
            if (isFinishing()) return;
            int bgWidth = bg.getWidth();
            if (bgWidth > 0) {
                fill.getLayoutParams().width = (int) (bgWidth * (mastery / 100f));
                fill.requestLayout();
            }
        });

        item.setOnClickListener(v -> {
            triggerVibration(15);
            selectedSkill = name;
            for (int i = 0; i < llSkillsContainer.getChildCount(); i++) {
                View child = llSkillsContainer.getChildAt(i);
                child.setBackgroundResource(R.drawable.shape_skill_card);
                child.findViewById(R.id.v_duel_check).setVisibility(View.GONE);
            }
            item.setBackgroundResource(R.drawable.shape_leaderboard_you);
            item.findViewById(R.id.v_duel_check).setVisibility(View.VISIBLE);
        });
        llSkillsContainer.addView(item);
    }

    private void setupDurationListeners() {
        btnDuration3.setOnClickListener(v -> selectDuration(3, btnDuration3));
        btnDuration7.setOnClickListener(v -> selectDuration(7, btnDuration7));
        btnDuration14.setOnClickListener(v -> selectDuration(14, btnDuration14));
    }

    private void selectDuration(int days, TextView view) {
        triggerVibration(15);
        selectedDays = days;
        btnDuration3.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration7.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration14.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration3.setTextColor(0xFFFFFFFF); btnDuration7.setTextColor(0xFFFFFFFF); btnDuration14.setTextColor(0xFFFFFFFF);
        view.setBackgroundResource(R.drawable.shape_leaderboard_you);
        view.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
    }

    private void setupWagerListeners() {
        btnWager0.setOnClickListener(v -> selectWager(0, btnWager0));
        btnWager100.setOnClickListener(v -> selectWager(100, btnWager100));
        btnWager250.setOnClickListener(v -> selectWager(250, btnWager250));
        btnWager500.setOnClickListener(v -> selectWager(500, btnWager500));
    }

    private void selectWager(int amount, TextView view) {
        triggerVibration(15);
        selectedWager = amount;
        btnWager0.setBackgroundResource(R.drawable.shape_skill_card);
        btnWager100.setBackgroundResource(R.drawable.shape_skill_card);
        btnWager250.setBackgroundResource(R.drawable.shape_skill_card);
        btnWager500.setBackgroundResource(R.drawable.shape_skill_card);
        btnWager0.setTextColor(0xFFFFFFFF); btnWager100.setTextColor(0xFFFFFFFF); btnWager250.setTextColor(0xFFFFFFFF); btnWager500.setTextColor(0xFFFFFFFF);

        view.setBackgroundResource(R.drawable.shape_leaderboard_you);
        view.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
    }

    private void sendDuelInvite() {
        isSending = true;
        btnChallenge.setEnabled(false);
        btnChallenge.setAlpha(0.5f);

        String duelId = mDatabase.child("duels").push().getKey();
        if (duelId == null || currentUserData == null || opponentUserData == null) {
            isSending = false;
            btnChallenge.setEnabled(true);
            btnChallenge.setAlpha(1.0f);
            return;
        }

        Duel duel = new Duel(duelId, FirebaseAuth.getInstance().getUid(), opponentUid, selectedSkill, selectedDays);
        duel.setStatus("pending");
        duel.setWagerAmount(selectedWager);

        double myStart = currentUserData.getSkillMastery().getOrDefault(selectedSkill, 0.0);
        double oppStart = opponentUserData.getSkillMastery().getOrDefault(selectedSkill, 0.0);

        duel.setInitiatorStartMastery(myStart);
        duel.setOpponentStartMastery(oppStart);
        duel.setInitiatorCurrentMastery(myStart);
        duel.setOpponentCurrentMastery(oppStart);

        mDatabase.child("duels").child(duelId).setValue(duel).addOnSuccessListener(aVoid -> {
            Map<String, Object> notif = new HashMap<>();
            notif.put("type", "duel_invite");
            notif.put("duelId", duelId);
            notif.put("message", currentUserData.getUsername() + " challenged you to a " + selectedSkill + " duel for " + (selectedWager > 0 ? selectedWager + " XP" : "fun") + "! ⚔️");
            notif.put("timestamp", ServerValue.TIMESTAMP);

            mDatabase.child("users").child(opponentUid).child("notifications").push().setValue(notif);

            Toast.makeText(this, "Challenge sent to " + opponentUserData.getUsername() + "!", Toast.LENGTH_LONG).show();
            finish();
        }).addOnFailureListener(e -> {
            isSending = false;
            btnChallenge.setEnabled(true);
            btnChallenge.setAlpha(1.0f);
            Toast.makeText(this, "Failed to send challenge: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
