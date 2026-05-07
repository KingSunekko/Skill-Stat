package com.example.skillstat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class FriendProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvAvatar, tvPoints, tvStreak;
    private LinearLayout llSkillsList;
    private DatabaseReference mDatabase;
    private String friendUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        friendUid = getIntent().getStringExtra("friend_uid");

        if (friendUid == null) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvName = findViewById(R.id.tv_friend_name);
        tvEmail = findViewById(R.id.tv_friend_handle);
        tvAvatar = findViewById(R.id.tv_friend_avatar_emoji);
        tvPoints = findViewById(R.id.tv_friend_xp);
        tvStreak = findViewById(R.id.tv_friend_streak);
        llSkillsList = findViewById(R.id.ll_friend_skills_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_challenge_friend).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartDuelActivity.class);
            intent.putExtra("opponent_uid", friendUid);
            startActivity(intent);
        });

        loadFriendData();
    }

    private void loadFriendData() {
        mDatabase.child("users").child(friendUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvName.setText(user.getUsername());
                    tvEmail.setText("@" + user.getUsername().toLowerCase());
                    tvAvatar.setText(user.getAvatarUrl());
                    tvPoints.setText("⭐ " + user.getTotalPoints() + " XP");
                    tvStreak.setText("🔥 " + user.getStreak() + " streak");
                    
                    displaySkills(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FriendProfile", "Load failed", error.toException());
            }
        });
    }

    private void displaySkills(User user) {
        if (llSkillsList == null) return;
        llSkillsList.removeAllViews();
        
        List<String> skills = user.getSkills();
        Map<String, Integer> mastery = user.getSkillMastery();
        
        if (skills != null) {
            for (String skill : skills) {
                int progress = mastery.getOrDefault(skill, 0);
                
                View item = getLayoutInflater().inflate(R.layout.item_home_skill, llSkillsList, false);
                ((TextView) item.findViewById(R.id.tv_skill_name)).setText(skill);
                ((TextView) item.findViewById(R.id.tv_status_text)).setText(progress + "%");
                
                // Set progress bar width
                View fill = item.findViewById(R.id.v_progress_fill);
                View bg = item.findViewById(R.id.v_progress_bg);
                bg.post(() -> {
                    ViewGroup.LayoutParams params = fill.getLayoutParams();
                    params.width = (int) (bg.getWidth() * (progress / 100f));
                    fill.setLayoutParams(params);
                });
                
                llSkillsList.addView(item);
            }
        }
    }
}
