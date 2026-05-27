package com.example.skillstat;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
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
import java.util.Locale;

public class AchievementsActivity extends AppCompatActivity {

    private LinearLayout llEarnedList, llLockedList;
    private TextView tvEarnedCount, tvBadgesXp, tvRemainingCount;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean hasAnimatedEntrance = false;

    // Define all available badges in the system
    private static class Badge {
        String title, description, emoji, xp;
        int xpValue;
        Badge(String t, String d, String e, String x, int val) {
            this.title = t; this.description = d; this.emoji = e; this.xp = x; this.xpValue = val;
        }
    }

    private final List<Badge> allBadges = new ArrayList<Badge>() {{
        add(new Badge("First Step", "Complete your first practice session", "🌱", "+50 XP", 50));
        add(new Badge("On Fire", "Maintain a 3-day streak", "🔥", "+100 XP", 100));
        add(new Badge("Week Warrior", "Maintain a 7-day streak", "⚡", "+250 XP", 250));
        add(new Badge("Month Master", "Maintain a 30-day streak", "👑", "+1000 XP", 1000));
        add(new Badge("Skill Collector", "Add 5 or more skills", "📚", "+150 XP", 150));
        add(new Badge("Diamond Skill", "Reach 100% mastery on any skill", "💎", "+500 XP", 500));
        add(new Badge("XP Legend", "Earn 5,000 total XP", "🌟", "+500 XP", 500));
        add(new Badge("Comeback Kid", "Return after a 7-day break and practice", "🦅", "+300 XP", 300));
        add(new Badge("Consistent", "Practice every day for 2 weeks", "🎯", "+400 XP", 400));
        add(new Badge("Journaler", "Write 10 practice notes", "📝", "+150 XP", 150));
        add(new Badge("Challenge Champ", "Complete 7 daily challenges", "🏆", "+350 XP", 350));
        add(new Badge("Deep Diver", "Add 3 sub-skills to one skill", "🔬", "+200 XP", 200));
        add(new Badge("Podium", "Reach top 3 on the leaderboard", "🥇", "+500 XP", 500));
        add(new Badge("Decay Slayer", "Keep all skills Sharp or better for a week", "🛡️", "+300 XP", 300));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        FrameLayout btnBack = findViewById(R.id.btn_back_achievements);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        llEarnedList = findViewById(R.id.ll_earned_badges_list);
        llLockedList = findViewById(R.id.ll_locked_badges_list);
        
        tvEarnedCount = findViewById(R.id.tv_stats_earned_count);
        tvBadgesXp = findViewById(R.id.tv_stats_badges_xp);
        tvRemainingCount = findViewById(R.id.tv_stats_remaining_count);

        loadUserAchievements();
        
        // Initial setup for entrance
        View header = findViewById(R.id.rl_achievements_header);
        if (header != null) {
            header.setAlpha(0);
            header.setTranslationY(-20);
        }
        View summary = findViewById(R.id.card_achievements_summary);
        if (summary != null) {
            summary.setAlpha(0);
            summary.setScaleX(0.9f);
        }
    }

    private void runEntranceAnimations() {
        if (hasAnimatedEntrance) return;
        hasAnimatedEntrance = true;

        View header = findViewById(R.id.rl_achievements_header);
        if (header != null) {
            header.animate().alpha(1).translationY(0).setDuration(400).setInterpolator(new DecelerateInterpolator()).start();
        }

        View summary = findViewById(R.id.card_achievements_summary);
        if (summary != null) {
            summary.animate().alpha(1).scaleX(1.0f).setDuration(600).setStartDelay(100).setInterpolator(new AnticipateOvershootInterpolator()).start();
        }
    }

    private void loadUserAchievements() {
        if (currentUid == null) return;

        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    displayBadges(user.getBadges());
                    runEntranceAnimations();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Achievements", "Load failed", error.toException());
            }
        });
    }

    private void displayBadges(List<String> earnedTitles) {
        if (llEarnedList == null || llLockedList == null) return;
        
        llEarnedList.removeAllViews();
        llLockedList.removeAllViews();

        if (earnedTitles == null) earnedTitles = new ArrayList<>();

        int earnedCount = 0;
        int totalXpValue = 0;

        int index = 0;
        for (Badge b : allBadges) {
            if (earnedTitles.contains(b.title)) {
                addBadgeItem(llEarnedList, b.title, b.description, b.emoji, b.xp, true, index++);
                earnedCount++;
                totalXpValue += b.xpValue;
            }
        }
        
        for (Badge b : allBadges) {
            if (!earnedTitles.contains(b.title)) {
                addBadgeItem(llLockedList, b.title, b.description, b.emoji, b.xp, false, index++);
            }
        }

        animateNumber(tvEarnedCount, earnedCount, "/" + allBadges.size());
        animateNumber(tvBadgesXp, totalXpValue, " XP");
        animateNumber(tvRemainingCount, allBadges.size() - earnedCount, "");
    }

    private void animateNumber(TextView tv, int target, String suffix) {
        if (tv == null) return;
        int current = 0;
        try { current = Integer.parseInt(tv.getText().toString().replace(suffix, "").split("/")[0]); } catch (Exception ignored) {}
        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> tv.setText(animation.getAnimatedValue() + suffix));
        animator.start();
    }

    private void addBadgeItem(ViewGroup parent, String title, String desc, String emoji, String xp, boolean isEarned, int index) {
        View view = getLayoutInflater().inflate(R.layout.item_badge, parent, false);

        TextView tvEmoji = view.findViewById(R.id.tv_badge_emoji);
        TextView tvTitle = view.findViewById(R.id.tv_badge_title);
        TextView tvDesc = view.findViewById(R.id.tv_badge_description);
        TextView tvXp = view.findViewById(R.id.tv_badge_xp);

        if (tvEmoji != null) tvEmoji.setText(emoji);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDesc != null) tvDesc.setText(desc);
        if (tvXp != null) tvXp.setText(xp);

        if (isEarned) {
            view.setBackgroundResource(R.drawable.shape_leaderboard_you);
            if (tvXp != null) {
                tvXp.setBackgroundResource(R.drawable.shape_reward_skill_pill);
                tvXp.setTextColor(ContextCompat.getColor(this, R.color.splash_green));
            }
        } else {
            view.setAlpha(0.5f);
            if (tvXp != null) {
                tvXp.setBackgroundResource(R.drawable.shape_skill_card);
                tvXp.setTextColor(ContextCompat.getColor(this, R.color.splash_text_secondary));
            }
            if (tvEmoji != null) tvEmoji.setAlpha(0.3f);
        }

        // Entrance animation
        if (!hasAnimatedEntrance) {
            view.setAlpha(0);
            view.setTranslationX(50);
            view.animate().alpha(isEarned ? 1.0f : 0.5f).translationX(0).setDuration(400).setStartDelay(300 + (index * 50L)).start();
        }

        parent.addView(view);
        
        // Add margin between items
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.bottomMargin = 16;
        view.setLayoutParams(params);
    }
}
