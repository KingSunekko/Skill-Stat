package com.example.skillstat;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.User;
import com.example.skillstat.utils.ForgeUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvPoints, tvStreak, tvDailyGoal, tvSkillsCount, tvSkillsTracked, tvBio, tvForgeTitle;
    private LinearProgressIndicator pbForgeXp;
    private ImageView ivAvatar;
    private TextView tvRankBadge, tvStatBestStreak, tvStatTotalSessions, tvStatWinRate;
    private GridLayout glAchievements;
    private SwitchMaterial switchDailyReminder, switchDecayAlerts;
    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isUpdatingFromDatabase = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        tvUsername = view.findViewById(R.id.tv_profile_name);
        tvEmail = view.findViewById(R.id.tv_profile_handle);
        tvForgeTitle = view.findViewById(R.id.tv_forge_title);
        pbForgeXp = view.findViewById(R.id.pb_forge_xp);
        ivAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvPoints = view.findViewById(R.id.tv_total_points);
        tvStreak = view.findViewById(R.id.tv_streak_days);
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal_info);
        tvSkillsCount = view.findViewById(R.id.tv_skills_count);
        tvSkillsTracked = view.findViewById(R.id.tv_skills_tracked_info);
        tvBio = view.findViewById(R.id.tv_bio_info);

        tvRankBadge = view.findViewById(R.id.tv_profile_rank_badge);
        tvStatBestStreak = view.findViewById(R.id.tv_stat_best_streak);
        tvStatTotalSessions = view.findViewById(R.id.tv_stat_total_sessions);
        tvStatWinRate = view.findViewById(R.id.tv_stat_win_rate);

        glAchievements = view.findViewById(R.id.gl_achievements);

        switchDailyReminder = view.findViewById(R.id.switch_daily_reminder);
        switchDecayAlerts = view.findViewById(R.id.switch_decay_alerts);

        loadUserData();
        setupSwitchListeners();
        setupClickListeners(view);
    }

    private void setupClickListeners(View view) {
        View[] clickableViews = {
                view.findViewById(R.id.btn_edit_profile),
                view.findViewById(R.id.btn_daily_goal),
                view.findViewById(R.id.btn_history),
                view.findViewById(R.id.btn_ghost_library),
                view.findViewById(R.id.btn_badges),
                view.findViewById(R.id.btn_heatmap),
                view.findViewById(R.id.btn_weekly),
                view.findViewById(R.id.btn_sub_skills),
                view.findViewById(R.id.btn_categories),
                view.findViewById(R.id.btn_logout_text)
        };

        for (View v : clickableViews) {
            if (v != null) {
                v.setOnClickListener(v1 -> {
                    v1.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                        v1.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        handleNavigation(v1.getId());
                    }).start();
                });
            }
        }
    }

    private void handleNavigation(int id) {
        if (id == R.id.btn_edit_profile) navigateTo(new EditProfileFragment());
        else if (id == R.id.btn_daily_goal) navigateTo(new SettingsGoalFragment());
        else if (id == R.id.btn_history) startActivity(new Intent(getActivity(), PracticeHistoryActivity.class));
        else if (id == R.id.btn_ghost_library) showGhostLibrary();
        else if (id == R.id.btn_badges) startActivity(new Intent(getActivity(), AchievementsActivity.class));
        else if (id == R.id.btn_heatmap) navigateTo(new HeatmapFragment());
        else if (id == R.id.btn_weekly) navigateTo(new WeeklyReportFragment());
        else if (id == R.id.btn_sub_skills) navigateTo(new SubSkillsFragment());
        else if (id == R.id.btn_categories) startActivity(new Intent(getActivity(), CategoriesActivity.class));
        else if (id == R.id.btn_logout_text) logout();
    }

    private void showGhostLibrary() {
        if (getContext() == null) return;

        mDatabase.child("users").child(currentUid).child("personalBests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder sb = new StringBuilder("Your Personal Best Records:\n\n");
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String skill = ds.getKey().replace("_", ".");
                        long seconds = ds.child("seconds").getValue(Long.class);
                        double effort = ds.child("effortPoints").getValue(Double.class);
                        sb.append("👻 ").append(skill).append(": ")
                                .append(seconds / 60).append("m ").append(seconds % 60).append("s ")
                                .append("(").append(String.format("%.1f", effort)).append(" pts)\n");
                    }

                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext(), R.style.MaterialAlertDialog_Rounded)
                            .setTitle("👻 Ghost Replay Library")
                            .setMessage(sb.toString())
                            .setPositiveButton("Close", null)
                            .show();
                } else {
                    Toast.makeText(getContext(), "No Ghost Records yet. Finish a session in Zen Mode!", Toast.LENGTH_LONG).show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.step_forward_enter, R.anim.step_forward_exit, R.anim.step_backward_enter, R.anim.step_backward_exit)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null).commit();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void loadUserData() {
        if (currentUid == null) return;
        mDatabase.child("users").child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    isUpdatingFromDatabase = true;
                    updateProfileUI(user);
                    isUpdatingFromDatabase = false;
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateProfileUI(User user) {
        tvUsername.setText(user.getUsername());
        tvEmail.setText(user.getEmail());

        double maxM = 0;
        Map<String, Double> masteryMap = user.getSubSkillMastery();
        if (masteryMap != null && !masteryMap.isEmpty()) {
            for (Double m : masteryMap.values()) if (m > maxM) maxM = m;
        }

        tvForgeTitle.setText(ForgeUtils.getLevelTitle(maxM));
        tvForgeTitle.setTextColor(ForgeUtils.getTierColor(maxM));

        if (pbForgeXp != null) {
            pbForgeXp.setProgress((int) maxM, true);
        }

        if (ivAvatar != null) {
            int resId = getResources().getIdentifier(user.getAvatarUrl(), "drawable", getContext().getPackageName());
            if (resId != 0) ivAvatar.setImageResource(resId);
        }

        // Removed star prefix from Java because it's now a separate icon in layout
        animateNumber(tvPoints, user.getTotalPoints(), "", " XP");
        animateNumber(tvStreak, user.getStreak(), "🔥 ", " streak");
        tvDailyGoal.setText("⏱️ " + user.getDailyGoalMinutes() + " min");
        tvRankBadge.setText(user.getRankEmoji() + " " + user.getRankName() + " Rank");

        tvStatBestStreak.setText(user.getBestStreak() + " days");
        tvStatTotalSessions.setText(String.valueOf(user.getTotalSessionsCount()));
        tvStatWinRate.setText(String.format(Locale.getDefault(), "%.1f%%", user.getWinRate()));

        int count = user.getSkills().size();
        tvSkillsCount.setText("🧠 " + count + " skills");
        tvSkillsTracked.setText(count + " 📚");

        tvBio.setText(user.getBio() == null || user.getBio().isEmpty() ? "None" : user.getBio());

        if (glAchievements != null) {
            glAchievements.removeAllViews();
            List<String> badges = user.getBadges();
            if (badges != null) {
                for (int i = 0; i < badges.size(); i++) {
                    String b = badges.get(i);
                    addBadgeView((b.contains("Grandmaster") ? "👑 " : "🏅 ") + b, i);
                }
            }
        }
    }

    private void animateNumber(TextView tv, int target, String prefix, String suffix) {
        if (tv == null) return;
        int current = 0;
        try {
            String raw = tv.getText().toString().replace(prefix, "").replace(suffix, "").replace(",", "");
            current = Integer.parseInt(raw);
        } catch (Exception ignored) {}

        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> tv.setText(prefix + animation.getAnimatedValue() + suffix));
        animator.start();
    }

    private void addBadgeView(String text, int index) {
        if (getContext() == null) return;
        TextView badge = new TextView(getContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0; params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(12, 12, 12, 12);
        badge.setLayoutParams(params);
        badge.setPadding(0, 30, 0, 30);
        badge.setGravity(android.view.Gravity.CENTER);
        badge.setTextSize(10);
        badge.setTypeface(android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD));
        badge.setText(text);
        badge.setBackgroundResource(R.drawable.shape_achievement_badge);
        badge.setTextColor(Color.WHITE);
        glAchievements.addView(badge);
    }

    private void setupSwitchListeners() {
        switchDailyReminder.setOnCheckedChangeListener((b, checked) -> { if (!isUpdatingFromDatabase) mDatabase.child("users").child(currentUid).child("dailyRemindersEnabled").setValue(checked); });
        switchDecayAlerts.setOnCheckedChangeListener((b, checked) -> { if (!isUpdatingFromDatabase) mDatabase.child("users").child(currentUid).child("decayAlertsEnabled").setValue(checked); });
    }
}
