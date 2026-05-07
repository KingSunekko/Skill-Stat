package com.example.skillstat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private LinearLayout llEarnedList, llLockedList;
    private DatabaseReference mDatabase;
    private String currentUid;

    // Define all available badges in the system
    private static class Badge {
        String title, description, emoji, xp;
        Badge(String t, String d, String e, String x) {
            this.title = t; this.description = d; this.emoji = e; this.xp = x;
        }
    }

    private final List<Badge> allBadges = new ArrayList<Badge>() {{
        add(new Badge("First Step", "Complete your first practice session", "🌱", "+50 XP"));
        add(new Badge("On Fire", "Maintain a 3-day streak", "🔥", "+100 XP"));
        add(new Badge("XP Hunter", "Earn 1,000 total XP", "⭐", "+200 XP"));
        add(new Badge("Skill Collector", "Add 5 or more skills", "📚", "+150 XP"));
        add(new Badge("Diamond Skill", "Reach 100% mastery on any skill", "💎", "+500 XP"));
        add(new Badge("XP Legend", "Earn 5,000 total XP", "🌟", "+500 XP"));
        add(new Badge("Consistent", "Practice every day for 2 weeks", "🎯", "+400 XP"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        FrameLayout btnBack = findViewById(R.id.btn_back_achievements);
        btnBack.setOnClickListener(v -> finish());

        llEarnedList = findViewById(R.id.ll_earned_badges_list);
        llLockedList = findViewById(R.id.ll_locked_badges_list);

        loadUserAchievements();
    }

    private void loadUserAchievements() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    displayBadges(user.getBadges());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Achievements", "Load failed", error.toException());
            }
        });
    }

    private void displayBadges(List<String> earnedTitles) {
        llEarnedList.removeAllViews();
        llLockedList.removeAllViews();

        if (earnedTitles == null) earnedTitles = new ArrayList<>();

        for (Badge b : allBadges) {
            if (earnedTitles.contains(b.title)) {
                addBadgeItem(llEarnedList, b.title, b.description, b.emoji, b.xp, true);
            } else {
                addBadgeItem(llLockedList, b.title, b.description, b.emoji, b.xp, false);
            }
        }
    }

    private void addBadgeItem(ViewGroup parent, String title, String desc, String emoji, String xp, boolean isEarned) {
        View view = getLayoutInflater().inflate(R.layout.item_badge, parent, false);

        TextView tvEmoji = view.findViewById(R.id.tv_badge_emoji);
        TextView tvTitle = view.findViewById(R.id.tv_badge_title);
        TextView tvDesc = view.findViewById(R.id.tv_badge_description);
        TextView tvXp = view.findViewById(R.id.tv_badge_xp);
        View badgeBg = view.findViewById(R.id.v_badge_bg);

        tvEmoji.setText(emoji);
        tvTitle.setText(title);
        tvDesc.setText(desc);
        tvXp.setText(xp);

        if (isEarned) {
            view.setBackgroundResource(R.drawable.shape_leaderboard_you);
            tvXp.setBackgroundResource(R.drawable.shape_reward_skill_pill);
            tvXp.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
        } else {
            view.setAlpha(0.5f);
            tvXp.setBackgroundResource(R.drawable.shape_skill_card);
            tvXp.setTextColor(ContextCompat.getColor(this, R.color.splash_text_secondary));
            tvEmoji.setAlpha(0.3f);
        }

        parent.addView(view);
    }
}
