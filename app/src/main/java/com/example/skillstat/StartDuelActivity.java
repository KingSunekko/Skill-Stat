package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import java.util.Map;

public class StartDuelActivity extends AppCompatActivity {

    private LinearLayout llSkillsContainer;
    private TextView btnDuration3, btnDuration7, btnDuration14;
    private TextView btnChallenge;
    private TextView tvOpponentName, tvOpponentAvatar, tvOpponentHandle, tvOpponentStreak;
    
    private String selectedSkill = "";
    private int selectedDays = 7;
    private String opponentUid;
    private DatabaseReference mDatabase;
    private User currentUserData;
    private User opponentUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_duel);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Retrieve opponent UID
        opponentUid = getIntent().getStringExtra("opponent_uid");
        if (opponentUid == null) opponentUid = getIntent().getStringExtra("friend_uid");

        if (opponentUid == null) {
            Toast.makeText(this, "No opponent selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup Back Button
        findViewById(R.id.btn_back_duel).setOnClickListener(v -> finish());

        // Initialize Views
        llSkillsContainer = findViewById(R.id.ll_duel_skills_list);
        btnDuration3 = findViewById(R.id.btn_duration_3);
        btnDuration7 = findViewById(R.id.btn_duration_7);
        btnDuration14 = findViewById(R.id.btn_duration_14);
        btnChallenge = findViewById(R.id.btn_challenge_final);

        tvOpponentName = findViewById(R.id.tv_opponent_name);
        tvOpponentAvatar = findViewById(R.id.tv_opponent_avatar);
        tvOpponentHandle = findViewById(R.id.tv_opponent_handle);
        tvOpponentStreak = findViewById(R.id.tv_opponent_streak);

        setupDurationListeners();
        loadMySkills();
        loadOpponentData();

        btnChallenge.setOnClickListener(v -> {
            if (selectedSkill.isEmpty()) {
                Toast.makeText(this, "Please select a skill to compete in!", Toast.LENGTH_SHORT).show();
                return;
            }
            sendDuelInvite();
        });
    }

    private void loadOpponentData() {
        mDatabase.child("users").child(opponentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                opponentUserData = snapshot.getValue(User.class);
                if (opponentUserData != null) {
                    tvOpponentName.setText(opponentUserData.getUsername());
                    tvOpponentAvatar.setText(opponentUserData.getAvatarUrl());
                    tvOpponentHandle.setText("@" + opponentUserData.getUsername().toLowerCase());
                    tvOpponentStreak.setText("🔥 " + opponentUserData.getStreak() + "d streak");
                    btnChallenge.setText("⚔️ CHALLENGE " + opponentUserData.getUsername().toUpperCase() + "!");
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
                currentUserData = snapshot.getValue(User.class);
                if (currentUserData != null && llSkillsContainer != null) {
                    llSkillsContainer.removeAllViews();
                    List<String> skills = currentUserData.getSkills();
                    Map<String, Integer> masteryMap = currentUserData.getSkillMastery();
                    if (skills != null) {
                        for (String skillName : skills) {
                            int progress = masteryMap.getOrDefault(skillName, 0);
                            addSkillOption(skillName, progress);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addSkillOption(String name, int mastery) {
        View item = getLayoutInflater().inflate(R.layout.layout_skill_duel_option, llSkillsContainer, false);
        ((TextView) item.findViewById(R.id.tv_duel_skill_name)).setText(name);
        ((TextView) item.findViewById(R.id.tv_duel_skill_mastery)).setText("Your mastery: " + mastery + "%");
        
        View bg = item.findViewById(R.id.v_duel_progress_bg);
        View fill = item.findViewById(R.id.v_duel_progress_fill);
        bg.post(() -> {
            fill.getLayoutParams().width = (int) (bg.getWidth() * (mastery / 100f));
            fill.requestLayout();
        });

        item.setOnClickListener(v -> {
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
        selectedDays = days;
        btnDuration3.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration7.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration14.setBackgroundResource(R.drawable.shape_skill_card);
        btnDuration3.setTextColor(0xFFFFFFFF); btnDuration7.setTextColor(0xFFFFFFFF); btnDuration14.setTextColor(0xFFFFFFFF);
        view.setBackgroundResource(R.drawable.shape_leaderboard_you);
        view.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
    }

    private void sendDuelInvite() {
        String duelId = mDatabase.child("duels").push().getKey();
        if (duelId == null || currentUserData == null || opponentUserData == null) return;

        // Create the Duel Request (Status: pending)
        Duel duel = new Duel(duelId, FirebaseAuth.getInstance().getUid(), opponentUid, selectedSkill, selectedDays);
        duel.setStatus("pending"); 
        
        int myStart = currentUserData.getSkillMastery().getOrDefault(selectedSkill, 0);
        int oppStart = opponentUserData.getSkillMastery().getOrDefault(selectedSkill, 0);
        
        duel.setInitiatorStartMastery(myStart);
        duel.setOpponentStartMastery(oppStart);
        duel.setInitiatorCurrentMastery(myStart);
        duel.setOpponentCurrentMastery(oppStart);
        
        mDatabase.child("duels").child(duelId).setValue(duel).addOnSuccessListener(aVoid -> {
            // Send structured notification Map for Accept/Decline logic
            Map<String, Object> notif = new HashMap<>();
            notif.put("type", "duel_invite");
            notif.put("duelId", duelId);
            notif.put("message", currentUserData.getUsername() + " challenged you to a " + selectedSkill + " duel! ⚔️");
            notif.put("timestamp", ServerValue.TIMESTAMP);
            
            mDatabase.child("users").child(opponentUid).child("notifications").push().setValue(notif);
            
            Toast.makeText(this, "Challenge sent to " + opponentUserData.getUsername() + "!", Toast.LENGTH_LONG).show();
            finish();
        });
    }
}
