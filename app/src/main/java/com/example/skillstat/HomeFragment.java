package com.example.skillstat;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
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
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private LinearLayout llSkillsList, llHeatmapGrid, llBadges;
    private View btnStartPractice, btnCharts, btnRanks, btnNotifications;
    private View btnChallenge, btnFriends, btnDailyChallenges, btnHistory, cardRiskAlert, cardNudgeAlert, llHeatmapContainer;

    private TextView tvUserNameHeader, tvHeaderStreak, tvStatStreak, tvStatXp, tvStatDays;
    private ImageView ivAvatar;
    private TextView tvXpTotal, tvXpNeeded, tvLevel, tvRiskSkillName, tvNotifBadge;
    private TextView tvNudgeMessage;
    private View btnNudgePractice, btnNudgeDismiss;
    private View vXpProgressFill, vXpProgressBg;
    private View xpCard, statsContainer;

    private DatabaseReference mDatabase;
    private String currentUid;
    private boolean isFirebaseAvailable = false;
    private boolean isShowingSkillRisk = false;
    private boolean hasAnimatedEntrance = false;

    private ValueEventListener userListener;
    private ValueEventListener notificationsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        hasAnimatedEntrance = false;

        try {
            mDatabase = FirebaseDatabase.getInstance().getReference();
            currentUid = FirebaseAuth.getInstance().getUid();
            isFirebaseAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "Firebase unavailable", e);
            isFirebaseAvailable = false;
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        // Initialize Views
        llSkillsList = view.findViewById(R.id.ll_home_skills_list);
        llHeatmapGrid = view.findViewById(R.id.ll_heatmap_grid);
        llHeatmapContainer = view.findViewById(R.id.ll_heatmap_container);
        llBadges = view.findViewById(R.id.ll_home_badges);

        btnStartPractice = view.findViewById(R.id.btn_start_practice);
        btnCharts = view.findViewById(R.id.btn_home_charts);
        btnRanks = view.findViewById(R.id.btn_home_ranks);
        btnNotifications = view.findViewById(R.id.btn_notifications);
        btnChallenge = view.findViewById(R.id.btn_home_challenge);
        btnFriends = view.findViewById(R.id.btn_home_friends);
        btnDailyChallenges = view.findViewById(R.id.btn_home_daily_challenges);
        btnHistory = view.findViewById(R.id.btn_home_history);
        cardRiskAlert = view.findViewById(R.id.card_risk_alert);
        cardNudgeAlert = view.findViewById(R.id.card_nudge_alert);

        xpCard = view.findViewById(R.id.card_xp_progress);
        statsContainer = view.findViewById(R.id.ll_stats_container);

        tvUserNameHeader = view.findViewById(R.id.tv_home_greeting);
        ivAvatar = view.findViewById(R.id.iv_home_avatar);
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

        tvNudgeMessage = view.findViewById(R.id.tv_nudge_message);
        btnNudgePractice = view.findViewById(R.id.btn_nudge_practice);
        btnNudgeDismiss = view.findViewById(R.id.btn_nudge_dismiss);

        setupClickAnimations();
        prepareEntranceAnimations();

        if (isFirebaseAvailable) {
            loadUserData();
            listenForNotifications();
        } else {
            Toast.makeText(getContext(), "Offline: Loading cached data...", Toast.LENGTH_SHORT).show();
        }

        btnNotifications.setOnClickListener(v -> {
            NotificationsBottomSheet bottomSheet = new NotificationsBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "NotificationsBottomSheet");
        });

        btnCharts.setOnClickListener(v -> navigateToFragment(new StatsFragment()));
        btnRanks.setOnClickListener(v -> navigateToFragment(new RanksFragment()));
        btnFriends.setOnClickListener(v -> navigateToFragment(new FriendsFragment()));
        btnChallenge.setOnClickListener(v -> startActivity(new Intent(getActivity(), StartDuelActivity.class)));
        btnStartPractice.setOnClickListener(v -> navigateToPractice("General Practice"));

        if (btnDailyChallenges != null) {
            btnDailyChallenges.setOnClickListener(v -> navigateToFragment(new DailyChallengesFragment()));
        }

        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> startActivity(new Intent(getActivity(), PracticeHistoryActivity.class)));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && currentUid != null) {
            if (userListener != null) {
                mDatabase.child("users").child(currentUid).removeEventListener(userListener);
            }
            if (notificationsListener != null) {
                mDatabase.child("users").child(currentUid).child("notifications").removeEventListener(notificationsListener);
            }
        }
    }

    private void prepareEntranceAnimations() {
        if (xpCard != null) { xpCard.setAlpha(0); xpCard.setTranslationY(50); }
        if (statsContainer != null) { statsContainer.setAlpha(0); statsContainer.setTranslationY(50); }
        if (btnStartPractice != null) { btnStartPractice.setAlpha(0); btnStartPractice.setTranslationY(50); }
        if (llHeatmapContainer != null) { llHeatmapContainer.setAlpha(0); llHeatmapContainer.setTranslationY(50); }
    }

    private void runEntranceAnimations() {
        if (hasAnimatedEntrance) return;
        hasAnimatedEntrance = true;

        long delay = 100;
        if (xpCard != null) animateViewIn(xpCard, delay);
        delay += 100;
        if (llHeatmapContainer != null) animateViewIn(llHeatmapContainer, delay);
        delay += 100;
        if (statsContainer != null) animateViewIn(statsContainer, delay);
        delay += 100;
        if (btnStartPractice != null) animateViewIn(btnStartPractice, delay);
    }

    private void animateViewIn(View v, long delay) {
        v.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(600)
                .setStartDelay(delay)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void listenForNotifications() {
        if (currentUid == null || mDatabase == null) return;

        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                long count = snapshot.getChildrenCount();
                if (tvNotifBadge != null) {
                    tvNotifBadge.setText(String.valueOf(count));
                    if (count > 0 && tvNotifBadge.getVisibility() != View.VISIBLE) {
                        tvNotifBadge.setVisibility(View.VISIBLE);
                        tvNotifBadge.setScaleX(0);
                        tvNotifBadge.setScaleY(0);
                        tvNotifBadge.animate().scaleX(1).scaleY(1).setDuration(300).setInterpolator(new AnticipateOvershootInterpolator()).start();
                    } else if (count == 0) {
                        tvNotifBadge.setVisibility(View.GONE);
                    }
                }

                if (count == 0 && cardNudgeAlert != null) {
                    cardNudgeAlert.setVisibility(View.GONE);
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> notif = (Map<String, Object>) ds.getValue();
                    if (notif != null) {
                        String type = (String) notif.get("type");
                        String message = (String) notif.get("message");
                        String notifId = ds.getKey();

                        if ("nudge".equals(type)) {
                            showNudgeBanner(message, notifId);
                        } else if ("friend_request".equals(type)) {
                            if (!isShowingSkillRisk && cardRiskAlert != null) {
                                showNotificationBanner(message, "#1A2D1F", "#34C759", notifId, false);
                            }
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(currentUid).child("notifications").addValueEventListener(notificationsListener);
    }

    private void showNudgeBanner(String message, String notifId) {
        if (cardNudgeAlert != null) {
            if (cardNudgeAlert.getVisibility() != View.VISIBLE) {
                cardNudgeAlert.setVisibility(View.VISIBLE);
                cardNudgeAlert.setAlpha(0);
                cardNudgeAlert.setTranslationX(100);
                cardNudgeAlert.animate().alpha(1).translationX(0).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
            }

            if (tvNudgeMessage != null) tvNudgeMessage.setText(message);

            if (btnNudgePractice != null) {
                btnNudgePractice.setOnClickListener(v -> {
                    dismissNotification(notifId);
                    navigateToPractice("General Practice");
                });
            }

            if (btnNudgeDismiss != null) {
                btnNudgeDismiss.setOnClickListener(v -> dismissNotification(notifId));
            }
        }
    }

    private void dismissNotification(String notifId) {
        if (currentUid != null && mDatabase != null && notifId != null) {
            mDatabase.child("users").child(currentUid).child("notifications").child(notifId).removeValue();
            if (cardNudgeAlert != null) {
                cardNudgeAlert.animate().alpha(0).translationX(-100).setDuration(300).withEndAction(() -> cardNudgeAlert.setVisibility(View.GONE)).start();
            }
        }
    }

    private void showNotificationBanner(String message, String bgColor, String textColor, String notifId, boolean isNudge) {
        if (cardRiskAlert != null) {
            cardRiskAlert.setVisibility(View.VISIBLE);
            cardRiskAlert.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(bgColor)));
            if (tvRiskSkillName != null) {
                tvRiskSkillName.setText(message);
                tvRiskSkillName.setTextColor(android.graphics.Color.parseColor(textColor));
            }

            cardRiskAlert.setOnClickListener(v -> {
                if (currentUid != null) {
                    mDatabase.child("users").child(currentUid).child("notifications").child(notifId).removeValue();
                }
                if (isNudge) navigateToPractice("General Practice");
                else navigateToFragment(new FriendsFragment());
            });
        }
    }

    private void loadUserData() {
        if (currentUid == null || mDatabase == null) return;

        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    checkAndResetDailyStats(user);
                    updateUI(user);
                    runEntranceAnimations();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("users").child(currentUid).addValueEventListener(userListener);
    }

    private void checkAndResetDailyStats(User user) {
        if (currentUid == null || mDatabase == null) return;

        long now = System.currentTimeMillis();
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        Map<String, Object> updates = new HashMap<>();
        boolean needsUpdate = false;

        // Daily Reset Check
        if (user.getLastPracticeTimestamp() > 0) {
            Calendar lastCal = Calendar.getInstance();
            lastCal.setTimeInMillis(user.getLastPracticeTimestamp());

            boolean isSameDay = lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                    lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR);

            if (!isSameDay) {
                if (user.getChallengeXpToday() > 0 || user.getCurrentDailyMinutes() > 0 ||
                        (user.getDailyChallenges() != null && !user.getDailyChallenges().isEmpty())) {
                    updates.put("challengeXpToday", 0);
                    updates.put("currentDailyMinutes", 0);
                    updates.put("dailyChallenges", new HashMap<String, Object>());
                    needsUpdate = true;
                }
            }
        }

        // Weekly Reset Check
        String currentWeekStr = nowCal.get(Calendar.YEAR) + "-" + nowCal.get(Calendar.WEEK_OF_YEAR);
        if (user.getLastWeeklyResetWeek() == null || !user.getLastWeeklyResetWeek().equals(currentWeekStr)) {
            if (user.getWeeklyPoints() > 0) {
                updates.put("weeklyPoints", 0);
                updates.put("lastWeeklyResetWeek", currentWeekStr);
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
            mDatabase.child("users").child(currentUid).updateChildren(updates);
        }
    }

    private void updateUI(User user) {
        if (!isAdded()) return;

        if (tvUserNameHeader != null) tvUserNameHeader.setText(String.format("Hey, %s!", user.getUsername()));

        if (ivAvatar != null && user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            String avatarName = user.getAvatarUrl();
            int resId = getResources().getIdentifier(avatarName, "drawable", requireContext().getPackageName());
            ivAvatar.setImageResource(resId != 0 ? resId : R.drawable.prof1);
        }

        if (tvHeaderStreak != null) tvHeaderStreak.setText(String.valueOf(user.getStreak()));

        checkSkillRisks(user);
        updateHeatmap(user);
        updateBadges(user);

        animateNumber(tvStatStreak, user.getStreak());
        animateNumber(tvStatXp, user.getTotalPoints());
        animateNumber(tvStatDays, user.getTotalDays());

        int level = (user.getTotalPoints() / 1000) + 1;
        int currentLevelXp = user.getTotalPoints() % 1000;
        if (tvLevel != null) tvLevel.setText(String.format(Locale.getDefault(), "Level %d → %d", level, level + 1));
        if (tvXpTotal != null) tvXpTotal.setText(String.format(Locale.getDefault(), "⭐ %d XP", user.getTotalPoints()));
        if (tvXpNeeded != null) tvXpNeeded.setText(String.format(Locale.getDefault(), "%d XP to next level", 1000 - currentLevelXp));
        animateProgressBar(vXpProgressBg, vXpProgressFill, currentLevelXp / 1000f);

        if (llSkillsList != null) {
            llSkillsList.removeAllViews();
            List<String> skills = user.getSkills();
            Map<String, Double> masteryMap = user.getSkillMastery();
            if (skills != null && masteryMap != null) {
                int index = 0;
                for (String skill : skills) {
                    String skillKey = skill.replace(".", "_");
                    double progress = masteryMap.getOrDefault(skillKey, 0.0);
                    addSkillItem(skill, (int) progress, index++, user);
                }
            }
        }
    }

    private void updateBadges(User user) {
        if (llBadges == null || !isAdded()) return;
        llBadges.removeAllViews();
        List<String> badges = user.getBadges();
        if (badges != null && !badges.isEmpty()) {
            for (int i = 0; i < badges.size(); i++) {
                addBadgeItem(badges.get(i), i);
            }
        }
    }

    private void addBadgeItem(String badgeName, int index) {
        TextView tv = new TextView(getContext());
        tv.setText(getBadgeEmoji(badgeName));
        tv.setTextSize(32);
        tv.setPadding(0, 0, (int) (16 * getResources().getDisplayMetrics().density), 0);

        tv.setAlpha(0);
        tv.setScaleX(0);
        tv.setScaleY(0);
        tv.animate()
                .alpha(1)
                .scaleX(1)
                .scaleY(1)
                .setDuration(400)
                .setStartDelay(500L + (index * 100L))
                .setInterpolator(new AnticipateOvershootInterpolator())
                .start();

        tv.setOnClickListener(v -> Toast.makeText(getContext(), "Achievement: " + badgeName, Toast.LENGTH_SHORT).show());

        llBadges.addView(tv);
    }

    private String getBadgeEmoji(String name) {
        if (name == null) return "🏅";
        switch (name.toLowerCase()) {
            case "starter": case "first step": return "🌱";
            case "consistent": case "on fire": return "🔥";
            case "master": return "👑";
            case "winner": case "podium": return "🏆";
            case "social": return "🤝";
            default: return "🏅";
        }
    }

    private void updateHeatmap(User user) {
        if (llHeatmapGrid == null || !isAdded()) return;
        llHeatmapGrid.removeAllViews();

        List<String> skills = user.getSkills();
        if (skills == null || skills.isEmpty()) {
            if (llHeatmapContainer != null) llHeatmapContainer.setVisibility(View.GONE);
            return;
        }
        if (llHeatmapContainer != null) llHeatmapContainer.setVisibility(View.VISIBLE);

        Map<String, Long> lastPracticeMap = user.getSkillLastPractice();
        Map<String, Integer> decayMap = user.getSkillDecaySettings();
        long now = System.currentTimeMillis();

        for (int i = 0; i < skills.size(); i++) {
            String skill = skills.get(i);
            String skillKey = skill.replace(".", "_");

            int decayDays = decayMap != null ? decayMap.getOrDefault(skillKey, 7) : 7;
            long lastPractice = lastPracticeMap != null ? lastPracticeMap.getOrDefault(skillKey, 0L) : 0L;

            int color;
            if (lastPractice == 0) {
                color = getResources().getColor(R.color.splash_green);
            } else {
                long diffDays = (now - lastPractice) / (1000 * 60 * 60 * 24);
                if (diffDays >= decayDays) {
                    color = getResources().getColor(R.color.critical_red);
                } else if (diffDays > 0 && diffDays >= (decayDays - 1)) {
                    color = getResources().getColor(R.color.xp_yellow);
                } else {
                    color = getResources().getColor(R.color.splash_green);
                }
            }

            View square = new View(getContext());
            int size = (int) (26 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, (int) (6 * getResources().getDisplayMetrics().density), 0);
            square.setLayoutParams(params);
            square.setBackgroundResource(R.drawable.shape_heatmap_square);
            square.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            square.setOnClickListener(v -> Toast.makeText(getContext(), skill + " status", Toast.LENGTH_SHORT).show());

            llHeatmapGrid.addView(square);
        }
    }

    private void animateNumber(TextView tv, int target) {
        if (tv == null) return;
        int current = 0;
        try { current = Integer.parseInt(tv.getText().toString().replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
        if (current == target) return;

        ValueAnimator animator = ValueAnimator.ofInt(current, target);
        animator.setDuration(1000);
        animator.addUpdateListener(animation -> tv.setText(String.valueOf(animation.getAnimatedValue())));
        animator.start();
    }

    private void checkSkillRisks(User user) {
        Map<String, Double> masteryMap = user.getSkillMastery();
        if (masteryMap == null) return;

        long now = System.currentTimeMillis();
        String atRiskSkillTemp = null;
        Map<String, Long> lastPracticeMap = user.getSkillLastPractice();
        Map<String, Integer> decayMap = user.getSkillDecaySettings();
        List<String> skills = user.getSkills();

        if (skills != null) {
            for (String skill : skills) {
                String skillKey = skill.replace(".", "_");
                int decaySetting = decayMap != null ? decayMap.getOrDefault(skillKey, 7) : 7;
                long lastPractice = lastPracticeMap != null ? lastPracticeMap.getOrDefault(skillKey, 0L) : 0L;

                if (lastPractice > 0) {
                    long diffInDays = (now - lastPractice) / (1000 * 60 * 60 * 24);
                    // Match the heatmap yellow logic: Show alert ONLY if diffInDays > 0 (meaning not today)
                    if (diffInDays > 0 && diffInDays >= (decaySetting - 1) && diffInDays < decaySetting) {
                        atRiskSkillTemp = skill;
                        break;
                    }
                }
            }
        }

        final String finalAtRiskSkill = atRiskSkillTemp;
        if (finalAtRiskSkill != null) {
            isShowingSkillRisk = true;
            if (cardRiskAlert != null) {
                cardRiskAlert.setVisibility(View.VISIBLE);
                if (tvRiskSkillName != null) tvRiskSkillName.setText(String.format("%s is at risk of decaying!", finalAtRiskSkill));
                cardRiskAlert.setOnClickListener(v -> navigateToPractice(finalAtRiskSkill));
            }
        } else {
            if (cardRiskAlert != null) cardRiskAlert.setVisibility(View.GONE);
            isShowingSkillRisk = false;
        }
    }

    private void addSkillItem(String name, int progress, int index, User user) {
        View item = getLayoutInflater().inflate(R.layout.item_home_skill, llSkillsList, false);
        ((TextView) item.findViewById(R.id.tv_skill_name)).setText(name);

        String skillKey = name.replace(".", "_");
        long now = System.currentTimeMillis();

        Map<String, Long> lastPracticeMap = user.getSkillLastPractice();
        Map<String, Integer> decayMap = user.getSkillDecaySettings();

        long lastPractice = lastPracticeMap != null ? lastPracticeMap.getOrDefault(skillKey, 0L) : 0L;
        int decayDays = decayMap != null ? decayMap.getOrDefault(skillKey, 7) : 7;

        long diffDays = lastPractice == 0 ? 0 : (now - lastPractice) / (1000 * 60 * 60 * 24);
        long daysRemaining = Math.max(0, decayDays - diffDays);
        float sharpnessRatio = lastPractice == 0 ? 1.0f : (float) daysRemaining / decayDays;

        int sharpnessColor;
        if (daysRemaining <= 0) {
            sharpnessColor = getResources().getColor(R.color.critical_red);
        } else if (diffDays == 0) {
            sharpnessColor = getResources().getColor(R.color.splash_green);
        } else if (diffDays >= (decayDays - 1)) {
            sharpnessColor = getResources().getColor(R.color.xp_yellow);
        } else {
            sharpnessColor = getResources().getColor(R.color.splash_green);
        }

        View sharpnessFill = item.findViewById(R.id.v_sharpness_fill);
        sharpnessFill.setBackgroundTintList(android.content.res.ColorStateList.valueOf(sharpnessColor));

        ((TextView) item.findViewById(R.id.tv_status_text)).setText(progress >= 90 ? "Mastered" : "Learning");
        ((TextView) item.findViewById(R.id.tv_skill_stats)).setText(String.format(Locale.getDefault(), "%d%% • %s", progress, progress >= 60 ? "Advanced" : "Beginner"));
        ((TextView) item.findViewById(R.id.tv_time_left)).setText(String.format(Locale.getDefault(), "%dd left", daysRemaining));

        animateProgressBar(item.findViewById(R.id.v_progress_bg), item.findViewById(R.id.v_progress_fill), progress / 100f);
        animateProgressBar(item.findViewById(R.id.v_sharpness_bg), item.findViewById(R.id.v_sharpness_fill), sharpnessRatio);

        item.setOnClickListener(v -> navigateToPractice(name));

        // Staggered entrance for skill items
        item.setAlpha(0);
        item.setTranslationX(-50);
        item.animate()
                .alpha(1)
                .translationX(0)
                .setDuration(400)
                .setStartDelay(400L + (index * 100L))
                .setInterpolator(new DecelerateInterpolator())
                .start();

        llSkillsList.addView(item);
    }

    private void animateProgressBar(View bg, View fill, float ratio) {
        if (bg == null || fill == null) return;
        bg.post(() -> {
            if (!isAdded()) return;
            ValueAnimator animator = ValueAnimator.ofInt(0, (int) (bg.getWidth() * Math.min(1.0f, ratio)));
            animator.setDuration(1200);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                if (!isAdded()) return;
                ViewGroup.LayoutParams params = fill.getLayoutParams();
                params.width = (int) animation.getAnimatedValue();
                fill.setLayoutParams(params);
            });
            animator.start();
        });
    }

    private void navigateToFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.gentle_fade_in, R.anim.gentle_fade_out, R.anim.gentle_fade_in, R.anim.gentle_fade_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
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
            else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            return false;
        });
    }

    private void setupClickAnimations() {
        applyHoverEffect(btnStartPractice);
        applyHoverEffect(btnCharts);
        applyHoverEffect(btnRanks);
        applyHoverEffect(btnFriends);
        applyHoverEffect(btnNotifications);
        applyHoverEffect(btnChallenge);
        applyHoverEffect(btnDailyChallenges);
        applyHoverEffect(btnHistory);
        applyHoverEffect(btnNudgePractice);
        applyHoverEffect(btnNudgeDismiss);
    }
}
