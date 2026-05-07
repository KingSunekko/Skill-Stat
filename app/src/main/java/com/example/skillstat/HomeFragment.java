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
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private LinearLayout llSkillsList;
    private View btnStartPractice, btnCharts, btnRanks, btnNotifications;
    private View btnChallenge, btnFriends, cardRiskAlert;
    
    private TextView tvUserNameHeader, tvAvatarEmoji, tvHeaderStreak, tvStatStreak, tvStatXp, tvStatDays;
    private TextView tvXpTotal, tvXpNeeded, tvGoalLabel, tvGoalProgress, tvGoalFooter, tvLevel, tvRiskSkillName, tvNotifBadge;
    private View vXpProgressFill, vGoalProgressFill, vXpProgressBg, vGoalProgressBg;
    
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isFirebaseAvailable = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Safely initialize Firebase
        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            currentUid = FirebaseAuth.getInstance().getUid();
            isFirebaseAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase unavailable in HomeFragment", e);
            isFirebaseAvailable = false;
        }

        // Initialize Action Buttons
        llSkillsList = view.findViewById(R.id.ll_home_skills_list);
        btnStartPractice = view.findViewById(R.id.btn_start_practice);
        btnCharts = view.findViewById(R.id.btn_home_charts);
        btnRanks = view.findViewById(R.id.btn_home_ranks);
        btnNotifications = view.findViewById(R.id.btn_notifications);
        btnChallenge = view.findViewById(R.id.btn_home_challenge);
        btnFriends = view.findViewById(R.id.btn_home_friends);
        cardRiskAlert = view.findViewById(R.id.card_risk_alert);
        
        tvUserNameHeader = view.findViewById(R.id.tv_home_greeting);
        tvAvatarEmoji = view.findViewById(R.id.tv_home_avatar_emoji);
        tvHeaderStreak = view.findViewById(R.id.tv_home_streak);
        tvNotifBadge = view.findViewById(R.id.tv_notif_badge);
        
        tvStatStreak = view.findViewById(R.id.tv_stat_streak);
        tvStatXp = view.findViewById(R.id.tv_stat_xp);
        tvStatDays = view.findViewById(R.id.tv_stat_days);
        
        tvXpTotal = view.findViewById(R.id.tv_home_xp_total);
        tvXpNeeded = view.findViewById(R.id.tv_home_xp_needed);
        tvLevel = view.findViewById(R.id.tv_home_level);
        tvRiskSkillName = view.findViewById(R.id.tv_risk_skill_name);
        vXpProgressFill = view.findViewById(R.id.v_xp_progress_fill);
        vXpProgressBg = view.findViewById(R.id.v_xp_progress_bg);
        
        tvGoalLabel = view.findViewById(R.id.tv_home_goal_label);
        tvGoalProgress = view.findViewById(R.id.tv_home_goal_progress);
        tvGoalFooter = view.findViewById(R.id.tv_home_goal_footer);
        vGoalProgressFill = view.findViewById(R.id.v_goal_progress_fill);
        vGoalProgressBg = view.findViewById(R.id.v_goal_progress_bg);

        setupClickAnimations();
        
        if (isFirebaseAvailable) {
            loadUserData();
            listenForNotifications();
        } else {
            showOfflineWarning();
        }

        btnNotifications.setOnClickListener(v -> {
            NotificationsBottomSheet bottomSheet = new NotificationsBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "NotificationsBottomSheet");
        });

        btnCharts.setOnClickListener(v -> navigateToFragment(new StatsFragment()));
        btnRanks.setOnClickListener(v -> navigateToFragment(new RanksFragment()));
        btnFriends.setOnClickListener(v -> navigateToFragment(new FriendsFragment()));
        btnChallenge.setOnClickListener(v -> {
            if (isFirebaseAvailable) {
                startActivity(new Intent(getActivity(), StartDuelActivity.class));
            } else {
                Toast.makeText(getContext(), "Feature requires Firebase", Toast.LENGTH_SHORT).show();
            }
        });
        btnStartPractice.setOnClickListener(v -> navigateToPractice("General Practice"));
    }

    private void showOfflineWarning() {
        if (tvUserNameHeader != null) tvUserNameHeader.setText("Offline Mode");
        if (cardRiskAlert != null) {
            cardRiskAlert.setVisibility(View.VISIBLE);
            tvRiskSkillName.setText("Firebase not configured. Add google-services.json to sync data.");
        }
    }

    private void listenForNotifications() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).child("notifications").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                if (tvNotifBadge != null) {
                    tvNotifBadge.setText(String.valueOf(count));
                    tvNotifBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Map<String, Object> notif = (Map<String, Object>) ds.getValue();
                    if (notif != null && "nudge".equals(notif.get("type"))) {
                        showNudgeBanner((String) notif.get("message"));
                        return;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showNudgeBanner(String message) {
        if (cardRiskAlert != null) {
            cardRiskAlert.setVisibility(View.VISIBLE);
            cardRiskAlert.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2D241A)); // Orange tint
            tvRiskSkillName.setText(message);
            tvRiskSkillName.setTextColor(0xFFFF9F0A);
            cardRiskAlert.setOnClickListener(v -> navigateToPractice("General Practice"));
        }
    }

    private void loadUserData() {
        if (currentUid == null || mDatabase == null) return;
        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) updateUI(user);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { Log.e(TAG, "Load failed", error.toException()); }
        });
    }

    private void updateUI(User user) {
        if (tvUserNameHeader != null) tvUserNameHeader.setText("Hey, " + user.getUsername() + "!");
        if (tvAvatarEmoji != null) tvAvatarEmoji.setText(user.getAvatarUrl());
        
        if (tvHeaderStreak != null) tvHeaderStreak.setText(String.valueOf(user.getStreak()));
        if (tvStatStreak != null) tvStatStreak.setText(String.valueOf(user.getStreak()));
        if (tvStatXp != null) tvStatXp.setText(String.valueOf(user.getTotalPoints()));
        if (tvStatDays != null) tvStatDays.setText(String.valueOf(user.getTotalDays()));
        
        int xp = user.getTotalPoints();
        int level = (xp / 1000) + 1;
        int xpInLevel = xp % 1000;
        if (tvLevel != null) tvLevel.setText("Level " + level + " → " + (level + 1));
        if (tvXpTotal != null) tvXpTotal.setText("⭐ " + xp + " XP");
        if (tvXpNeeded != null) tvXpNeeded.setText((1000 - xpInLevel) + " XP to next level");
        updateProgressBar(vXpProgressBg, vXpProgressFill, (float) xpInLevel / 1000f);

        int goal = user.getDailyGoalMinutes() > 0 ? user.getDailyGoalMinutes() : 10;
        int current = user.getCurrentDailyMinutes();
        if (tvGoalLabel != null) tvGoalLabel.setText("TODAY'S GOAL — " + goal + " MIN");
        if (tvGoalProgress != null) tvGoalProgress.setText(current + " / " + goal + " min");
        updateProgressBar(vGoalProgressBg, vGoalProgressFill, (float) current / (float) goal);

        if (llSkillsList != null) {
            llSkillsList.removeAllViews();
            List<String> skills = user.getSkills();
            Map<String, Integer> masteryMap = user.getSkillMastery();
            if (skills != null) {
                for (String skill : skills) {
                    int progress = masteryMap.getOrDefault(skill, 0);
                    addSkillItem(skill, progress, progress >= 90 ? "Mastered" : "Learning", progress >= 90 ? "#00BCD4" : "#58CC02");
                }
            }
        }
    }

    private void updateProgressBar(View bg, View fill, float ratio) {
        if (bg == null || fill == null) return;
        ratio = Math.min(1.0f, Math.max(0f, ratio));
        float finalRatio = ratio;
        bg.post(() -> {
            ViewGroup.LayoutParams params = fill.getLayoutParams();
            params.width = (int) (bg.getWidth() * finalRatio);
            fill.setLayoutParams(params);
        });
    }

    private void addSkillItem(String name, int progress, String status, String statusColor) {
        View item = getLayoutInflater().inflate(R.layout.item_home_skill, llSkillsList, false);
        ((TextView) item.findViewById(R.id.tv_skill_name)).setText(name);
        TextView tvStatus = item.findViewById(R.id.tv_status_text);
        tvStatus.setText(status);
        tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor));
        updateProgressBar(item.findViewById(R.id.v_progress_bg), item.findViewById(R.id.v_progress_fill), progress / 100f);
        item.setOnClickListener(v -> navigateToPractice(name));
        applyHoverEffect(item);
        llSkillsList.addView(item);
    }

    private void navigateToFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    private void navigateToPractice(String skillName) {
        PracticeFragment practiceFragment = new PracticeFragment();
        Bundle args = new Bundle();
        args.putString("skill_name", skillName);
        practiceFragment.setArguments(args);
        navigateToFragment(practiceFragment);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyHoverEffect(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start();
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) 
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            return false;
        });
    }

    private void setupClickAnimations() {
        applyHoverEffect(btnStartPractice);
        applyHoverEffect(btnCharts);
        applyHoverEffect(btnRanks);
        applyHoverEffect(btnNotifications);
        applyHoverEffect(btnChallenge);
        applyHoverEffect(btnFriends);
    }
}
