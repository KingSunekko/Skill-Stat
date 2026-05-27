package com.example.skillstat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.JointQuest;
import com.example.skillstat.models.PracticeSession;
import com.example.skillstat.models.User;
import com.example.skillstat.utils.ForgeUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RewardsFragment extends Fragment {

    private FrameLayout confettiContainer;
    private AppCompatButton btnBackDashboard;
    private TextView tvRewardXp, tvRewardGrowth, tvRewardStreak;
    private TextView tvCelebrationIcon, tvZenBonusBadge;
    private LinearLayout llRankUpOverlay;
    private TextView tvRankUpTitle, tvRankUpSub;
    private View vFlashOverlay;

    private final Random random = new Random();
    private DatabaseReference mDatabase;
    private String currentUid;
    private String challengeKey, practiceNote, subSkillName;
    private boolean isZenMode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUid = FirebaseAuth.getInstance().getUid();

        confettiContainer = view.findViewById(R.id.confetti_container);
        btnBackDashboard = view.findViewById(R.id.btn_back_dashboard);
        tvRewardXp = view.findViewById(R.id.tv_reward_xp);
        tvRewardGrowth = view.findViewById(R.id.tv_reward_growth);
        tvRewardStreak = view.findViewById(R.id.tv_reward_streak);
        tvCelebrationIcon = view.findViewById(R.id.tv_celebration_icon);
        tvZenBonusBadge = view.findViewById(R.id.tv_zen_bonus_badge);

        llRankUpOverlay = view.findViewById(R.id.ll_rank_up_overlay);
        tvRankUpTitle = view.findViewById(R.id.tv_rank_up_title);
        tvRankUpSub = view.findViewById(R.id.tv_rank_up_sub);

        vFlashOverlay = new View(getContext());
        vFlashOverlay.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        vFlashOverlay.setBackgroundColor(Color.WHITE);
        vFlashOverlay.setAlpha(0);
        ((ViewGroup)view).addView(vFlashOverlay);

        if (getArguments() != null) {
            String skillName = getArguments().getString("skill_name", "General Practice");
            int seconds = getArguments().getInt("seconds_practiced", 0);
            challengeKey = getArguments().getString("challenge_key");
            practiceNote = getArguments().getString("practice_note");
            subSkillName = getArguments().getString("sub_skill_name");
            isZenMode = getArguments().getBoolean("zen_mode", false);

            ((TextView)view.findViewById(R.id.tv_skill_tag)).setText(subSkillName != null ? subSkillName : skillName);
            if (isZenMode) tvZenBonusBadge.setVisibility(View.VISIBLE);

            ((TextView)view.findViewById(R.id.tv_reward_time)).setText(seconds >= 60 ? (seconds/60)+" min" : seconds+" sec");
            loadAndSave(skillName, seconds);
        }

        // FIX: Clear backstack and go to Home to avoid going back to Timer
        btnBackDashboard.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new HomeFragment(), false);
            }
        });

        llRankUpOverlay.setOnClickListener(v -> llRankUpOverlay.setVisibility(View.GONE));

        animateEntrance(view);
    }

    private void loadAndSave(String skillName, int seconds) {
        mDatabase.child("users").child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                User user = snapshot.getValue(User.class);
                if (user == null) return;

                int xpEarned = (seconds / 10) + (isZenMode ? (seconds/20) : 0);
                double growth = (double) seconds / 120.0 * (isZenMode ? 2.0 : 1.0);

                String skillKey = skillName.replace(".", "_");
                double oldM = user.getSkillMastery().getOrDefault(skillKey, 0.0);
                double newM = Math.min(100.0, oldM + growth);

                long now = System.currentTimeMillis();
                boolean isNewPB = false;
                PracticeSession currentSession = new PracticeSession(skillName, seconds, xpEarned, now, practiceNote, newM, isZenMode);
                currentSession.setUserId(currentUid);
                
                // IMPORTANT: Set effort points for duel activity (seconds / 10)
                currentSession.setEffortPoints((double) seconds / 10.0);

                PracticeSession pb = user.getPersonalBests().get(skillKey);
                if (pb == null || currentSession.getEffortPoints() > pb.getEffortPoints()) {
                    isNewPB = true;
                }

                // Weekly Reset Logic
                Calendar calendar = Calendar.getInstance();
                String currentWeekStr = calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.WEEK_OF_YEAR);
                int updatedWeeklyPoints = user.getWeeklyPoints();

                if (user.getLastWeeklyResetWeek() == null || !user.getLastWeeklyResetWeek().equals(currentWeekStr)) {
                    updatedWeeklyPoints = xpEarned;
                } else {
                    updatedWeeklyPoints += xpEarned;
                }

                Map<String, Object> up = new HashMap<>();
                int updatedTotalPoints = user.getTotalPoints() + xpEarned;
                up.put("totalPoints", updatedTotalPoints);
                up.put("weeklyPoints", updatedWeeklyPoints);
                up.put("lastWeeklyResetWeek", currentWeekStr);
                up.put("skillMastery/" + skillKey, newM);

                // CRITICAL FIX: Update history for the chart and heatmap
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateKey = sdf.format(new Date(now));
                up.put("history/" + dateKey + "/" + skillKey, newM);

                up.put("skillLastPractice/" + skillKey, now);

                // Gap detection for Comeback Kid
                long gapDays = 0;
                if (user.getLastPracticeTimestamp() > 0) {
                    gapDays = TimeUnit.MILLISECONDS.toDays(now - user.getLastPracticeTimestamp());
                }
                up.put("lastPracticeTimestamp", now);

                // Daily Goal and Streak Update
                int updatedDailyMinutes = user.getCurrentDailyMinutes();
                int updatedStreak = user.getStreak();
                int updatedTotalDays = user.getTotalDays();

                Calendar lastCal = Calendar.getInstance();
                if (user.getLastPracticeTimestamp() > 0) {
                    lastCal.setTimeInMillis(user.getLastPracticeTimestamp());
                }
                Calendar nowCal = Calendar.getInstance();
                nowCal.setTimeInMillis(now);

                boolean isSameDay = user.getLastPracticeTimestamp() > 0 &&
                        lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR);

                boolean isNewDay = !isSameDay;

                if (isNewDay) {
                    lastCal.add(Calendar.DAY_OF_YEAR, 1);
                    boolean isNextDay = lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                            lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR);

                    if (isNextDay || user.getLastPracticeTimestamp() == 0) {
                        updatedStreak++;
                    } else {
                        updatedStreak = 1;
                    }
                    updatedDailyMinutes = Math.max(1, seconds / 60);
                    updatedTotalDays++;

                    up.put("streak", updatedStreak);
                    if (updatedStreak > user.getBestStreak()) up.put("bestStreak", updatedStreak);
                    up.put("totalDays", updatedTotalDays);
                    if (tvRewardStreak != null) tvRewardStreak.setText("🔥 " + updatedStreak + " Day Streak!");

                    // Reset Daily Challenges for the new day
                    up.put("challengeXpToday", 0);
                    up.put("dailyChallenges", new HashMap<String, Object>());
                } else {
                    updatedDailyMinutes += Math.max(1, seconds / 60);
                    if (tvRewardStreak != null) tvRewardStreak.setText("🔥 Streak Maintained!");
                }
                up.put("currentDailyMinutes", updatedDailyMinutes);

                // Daily Challenge Completion Logic
                if (challengeKey != null) {
                    boolean alreadyCompleted = !isNewDay && user.isChallengeCompleted(challengeKey);
                    if (!alreadyCompleted) {
                        boolean qualified = false;
                        int bonus = 0;
                        switch (challengeKey) {
                            case "speed_round": if (seconds >= 300) { qualified = true; bonus = 30; } break;
                            case "focus_mode": qualified = true; bonus = 50; break;
                            case "double_session": qualified = checkDoubleSession(user, skillName); bonus = 80; break;
                            case "reflect_write": if (practiceNote != null && !practiceNote.trim().isEmpty()) { qualified = true; bonus = 60; } break;
                            case "no_excuses": if (nowCal.get(Calendar.HOUR_OF_DAY) < 9) { qualified = true; bonus = 100; } break;
                        }

                        if (qualified) {
                            xpEarned += bonus;
                            updatedTotalPoints += bonus;
                            up.put("totalPoints", updatedTotalPoints);
                            up.put("dailyChallenges/" + challengeKey, true);
                            int currentChallengeXp = isNewDay ? 0 : user.getChallengeXpToday();
                            up.put("challengeXpToday", currentChallengeXp + bonus);
                            showBadgeToast("Challenge Completed! ⚡ +" + bonus + " XP");

                            // Check if all 5 are completed to grant Streak Freeze
                            int currentCount = user.getCompletedChallengesCount();
                            if (isNewDay) currentCount = 0; // It was reset above
                            if (currentCount + 1 == 5) {
                                int currentFreezes = user.getStreakFreezes();
                                up.put("streakFreezes", currentFreezes + 1);
                                showBadgeToast("ALL CHALLENGES DONE! 🧊 +1 Streak Freeze");
                            }
                        } else if (challengeKey.equals("speed_round") && seconds < 300) {
                            Toast.makeText(getContext(), "Speed Round needs at least 5 mins!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                if (isNewPB) {
                    up.put("personalBests/" + skillKey, currentSession);
                    triggerPBAnimation();
                }

                // Award System Logic (All Badges)
                checkAchievements(user, up, updatedStreak, newM, updatedTotalPoints, gapDays);

                if (ForgeUtils.getRankLevel(newM) > ForgeUtils.getRankLevel(oldM)) {
                    triggerRankUpEffects(ForgeUtils.getLevelTitle(newM), ForgeUtils.getRankIcon(ForgeUtils.getRankLevel(newM)));
                }

                if (subSkillName != null) {
                    double oldSubM = user.getSubSkillMastery().getOrDefault(subSkillName, 0.0);
                    double newSubM = Math.min(100.0, oldSubM + growth);
                    up.put("subSkillMastery/" + subSkillName, newSubM);
                    if (ForgeUtils.getRankLevel(newSubM) > ForgeUtils.getRankLevel(oldSubM)) {
                        triggerRankUpEffects(ForgeUtils.getLevelTitle(newSubM), ForgeUtils.getRankIcon(ForgeUtils.getRankLevel(newSubM)));
                    }
                }

                // Save individual session to sessions list
                String sessionKey = mDatabase.child("users").child(currentUid).child("sessions").push().getKey();
                if (sessionKey != null) {
                    up.put("sessions/" + sessionKey, currentSession);
                }

                mDatabase.child("users").child(currentUid).updateChildren(up);

                // Update Active Duel Score and Activity Feed
                updateDuelProgress(skillName, seconds, currentSession);
                
                // NEW: Update Joint Quest progress
                updateJointQuestProgress(seconds);

                animateRewardText(tvRewardXp, xpEarned, "+");
                animateRewardText(tvRewardGrowth, growth, "+");
                if (getView() != null) {
                    TextView tvTotalXp = getView().findViewById(R.id.tv_total_xp);
                    if (tvTotalXp != null) tvTotalXp.setText("⭐ " + (updatedTotalPoints));
                }
                startConfetti(30);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateJointQuestProgress(int seconds) {
        // FIX: Use 60.0 as divisor so 60 seconds = 1.0 minute.
        // This makes quest progress match the user-defined goal in minutes.
        double minutes = (double) seconds / 60.0;
        
        mDatabase.child("users").child(currentUid).child("activeQuests").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String questId = ds.getKey();
                    if (questId == null) continue;
                    mDatabase.child("quests").child(questId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot qSnap) {
                            JointQuest quest = qSnap.getValue(JointQuest.class);
                            if (quest != null && "active".equals(quest.getStatus())) {
                                String field = currentUid.equals(quest.getCreatorUid()) ? "creatorMinutes" : "partnerMinutes";
                                mDatabase.child("quests").child(questId).child(field).setValue(ServerValue.increment(minutes));
                                
                                if (isAdded()) {
                                    String msg = String.format(Locale.getDefault(), "Quest Progress: +%.2f min", minutes);
                                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                }

                                // Check if quest is now completed
                                if (quest.getTotalMinutes() + minutes >= quest.getGoalMinutes()) {
                                    Map<String, Object> questUpdate = new HashMap<>();
                                    questUpdate.put("quests/" + questId + "/status", "completed");
                                    questUpdate.put("users/" + quest.getCreatorUid() + "/totalPoints", ServerValue.increment(500));
                                    questUpdate.put("users/" + quest.getPartnerUid() + "/totalPoints", ServerValue.increment(500));
                                    
                                    mDatabase.updateChildren(questUpdate).addOnSuccessListener(aVoid -> {
                                        if (isAdded()) showBadgeToast("QUEST COMPLETE! 🤝 Both earned +500 XP");
                                    });
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDuelProgress(String skillName, int seconds, PracticeSession session) {
        double effortGained = (double) seconds / 10.0;
        mDatabase.child("users").child(currentUid).child("activeDuels").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String duelId = ds.getKey();
                    if (duelId == null) continue;
                    mDatabase.child("duels").child(duelId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot duelSnap) {
                            Duel duel = duelSnap.getValue(Duel.class);
                            // Check if duel is active and skill matches (with flexible check for " Duel" suffix)
                            if (duel != null && "active".equals(duel.getStatus())) {
                                String dsName = duel.getSkillName();
                                boolean isMatch = skillName.equalsIgnoreCase(dsName) || 
                                                (dsName != null && dsName.toLowerCase().startsWith(skillName.toLowerCase()));

                                if (isMatch) {
                                    String field = currentUid.equals(duel.getInitiatorUid()) ? "initiatorEffort" : "opponentEffort";
                                    // 1. Update total score
                                    mDatabase.child("duels").child(duelId).child(field).setValue(ServerValue.increment(effortGained));
                                    // 2. Add to Activity Feed so it shows in Recent Battle Activity
                                    mDatabase.child("duels").child(duelId).child("sessions").push().setValue(session);
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean checkDoubleSession(User user, String currentSkill) {
        if (user.getSessions() == null) return false;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        long todayStart = cal.getTimeInMillis();
        for (PracticeSession s : user.getSessions().values()) {
            if (s.getTimestamp() >= todayStart && !s.getSkillName().equals(currentSkill)) return true;
        }
        return false;
    }

    private void checkAchievements(User user, Map<String, Object> up, int updatedStreak, double updatedMastery, int updatedTotalPoints, long gapDays) {
        List<String> currentBadges = user.getBadges();
        if (currentBadges == null) currentBadges = new ArrayList<>();

        List<String> newBadges = new ArrayList<>(currentBadges);
        boolean changed = false;
        int bonusXp = 0;

        // 1. First Step 🌱
        if (!newBadges.contains("First Step")) {
            newBadges.add("First Step");
            bonusXp += 50;
            changed = true;
            showBadgeToast("First Step 🌱 (+50 XP)");
        }

        // 2. On Fire 🔥 (3-day streak)
        if (updatedStreak >= 3 && !newBadges.contains("On Fire")) {
            newBadges.add("On Fire");
            bonusXp += 100;
            changed = true;
            showBadgeToast("On Fire 🔥 (+100 XP)");
        }

        // 3. Week Warrior ⚡ (7-day streak)
        if (updatedStreak >= 7 && !newBadges.contains("Week Warrior")) {
            newBadges.add("Week Warrior");
            bonusXp += 250;
            changed = true;
            showBadgeToast("Week Warrior ⚡ (+250 XP)");
        }

        // 4. Consistent 🎯 (14-day streak)
        if (updatedStreak >= 14 && !newBadges.contains("Consistent")) {
            newBadges.add("Consistent");
            bonusXp += 400;
            changed = true;
            showBadgeToast("Consistent 🎯 (+400 XP)");
        }

        // 5. Month Master 👑 (30-day streak)
        if (updatedStreak >= 30 && !newBadges.contains("Month Master")) {
            newBadges.add("Month Master");
            bonusXp += 1000;
            changed = true;
            showBadgeToast("Month Master 👑 (+1000 XP)");
        }

        // 6. Diamond Skill 💎 (100% mastery)
        if (updatedMastery >= 100 && !newBadges.contains("Diamond Skill")) {
            newBadges.add("Diamond Skill");
            bonusXp += 500;
            changed = true;
            showBadgeToast("Diamond Skill 💎 (+500 XP)");
        }

        // 7. XP Legend 🌟 (5,000 XP)
        if (updatedTotalPoints + bonusXp >= 5000 && !newBadges.contains("XP Legend")) {
            newBadges.add("XP Legend");
            bonusXp += 500;
            changed = true;
            showBadgeToast("XP Legend 🌟 (+500 XP)");
        }

        // 8. Skill Collector 📚 (5 skills)
        if (user.getSkills().size() >= 5 && !newBadges.contains("Skill Collector")) {
            newBadges.add("Skill Collector");
            bonusXp += 150;
            changed = true;
            showBadgeToast("Skill Collector 📚 (+150 XP)");
        }

        // 9. Deep Diver 🔬 (3 sub-skills in one skill)
        boolean hasDeepDiver = false;
        for (List<String> subList : user.getSubSkills().values()) {
            if (subList != null && subList.size() >= 3) {
                hasDeepDiver = true;
                break;
            }
        }
        if (hasDeepDiver && !newBadges.contains("Deep Diver")) {
            newBadges.add("Deep Diver");
            bonusXp += 200;
            changed = true;
            showBadgeToast("Deep Diver 🔬 (+200 XP)");
        }

        // 10. Journaler 📝 (10 notes)
        int noteCount = 0;
        if (user.getSessions() != null) {
            for (PracticeSession s : user.getSessions().values()) {
                if (s.getNote() != null && !s.getNote().trim().isEmpty()) noteCount++;
            }
        }
        // Count current note too
        if (practiceNote != null && !practiceNote.trim().isEmpty()) noteCount++;

        if (noteCount >= 10 && !newBadges.contains("Journaler")) {
            newBadges.add("Journaler");
            bonusXp += 150;
            changed = true;
            showBadgeToast("Journaler 📝 (+150 XP)");
        }

        // 11. Challenge Champ 🏆 (7 daily challenges)
        if (user.getCompletedChallengesCount() >= 7 && !newBadges.contains("Challenge Champ")) {
            newBadges.add("Challenge Champ");
            bonusXp += 350;
            changed = true;
            showBadgeToast("Challenge Champ 🏆 (+350 XP)");
        }

        // 12. Comeback Kid 🦅 (Return after 7 days)
        if (gapDays >= 7 && !newBadges.contains("Comeback Kid")) {
            newBadges.add("Comeback Kid");
            bonusXp += 300;
            changed = true;
            showBadgeToast("Comeback Kid 🦅 (+300 XP)");
        }

        // 13. Decay Slayer 🛡️ (All skills Sharp or better)
        boolean allSharp = user.getSkills().size() >= 3;
        long now = System.currentTimeMillis();
        for (String s : user.getSkills()) {
            String sk = s.replace(".", "_");
            long last = user.getSkillLastPractice().getOrDefault(sk, 0L);
            int decay = user.getSkillDecaySettings().getOrDefault(sk, 7);
            if (last == 0 || (now - last) / (1000 * 60 * 60 * 24) >= decay) {
                allSharp = false;
                break;
            }
        }
        if (allSharp && !newBadges.contains("Decay Slayer")) {
            newBadges.add("Decay Slayer");
            bonusXp += 300;
            changed = true;
            showBadgeToast("Decay Slayer 🛡️ (+300 XP)");
        }

        if (changed) {
            up.put("badges", newBadges);
            up.put("totalPoints", updatedTotalPoints + bonusXp);
        }
    }

    private void showBadgeToast(String message) {
        if (isAdded()) {
            Toast.makeText(getContext(), "🏆 UNLOCKED: " + message, Toast.LENGTH_LONG).show();
            // Show push notification for new badge
            NotificationHelper.showNotification(getContext(), "New Badge Unlocked! 🏆", message, NotificationHelper.CHANNEL_ID_ACHIEVEMENTS);
        }
    }

    private void triggerPBAnimation() {
        if (getView() == null) return;
        TextView tvPB = getView().findViewById(R.id.tv_new_pb_badge);
        if (tvPB != null) {
            tvPB.setVisibility(View.VISIBLE);
            tvPB.setScaleX(0); tvPB.setScaleY(0);
            tvPB.animate().scaleX(1.1f).scaleY(1.1f).setDuration(500).setInterpolator(new AnticipateOvershootInterpolator()).withEndAction(() -> {
                tvPB.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }).start();
        }
    }

    private void triggerRankUpEffects(String title, String icon) {
        try {
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 200);
            new android.os.Handler().postDelayed(() -> toneG.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 300), 200);
        } catch (Exception ignored) {}

        if (vFlashOverlay != null) {
            vFlashOverlay.setAlpha(0.8f);
            vFlashOverlay.animate().alpha(0).setDuration(400).start();
        }

        if (llRankUpOverlay != null) {
            llRankUpOverlay.setVisibility(View.VISIBLE);
            tvRankUpTitle.setText("RANK UP! " + icon);
            tvRankUpSub.setText("You reached " + title + " rank!");
            tvRankUpTitle.setScaleX(0.5f); tvRankUpTitle.setScaleY(0.5f);
            tvRankUpTitle.animate().scaleX(1.3f).scaleY(1.3f).setDuration(500).setInterpolator(new AnticipateOvershootInterpolator()).start();
            
            // Show push notification for rank up
            NotificationHelper.showNotification(getContext(), "Rank Up! " + icon, "Congratulations! You reached " + title + " rank!", NotificationHelper.CHANNEL_ID_ACHIEVEMENTS);
        }

        if (getView() != null) ObjectAnimator.ofFloat(getView(), "translationX", 0, 20, -20, 20, -20, 10, -10, 0).setDuration(400).start();
        startConfetti(120);
    }

    private void startConfetti(int count) {
        if (confettiContainer == null) return;
        confettiContainer.post(() -> {
            if (!isAdded()) return;
            int width = confettiContainer.getWidth() > 0 ? confettiContainer.getWidth() : 1080;
            int height = confettiContainer.getHeight() > 0 ? confettiContainer.getHeight() : 1920;

            for (int i = 0; i < count; i++) {
                View p = new View(getContext());
                int size = random.nextInt(25) + 10;
                p.setLayoutParams(new FrameLayout.LayoutParams(size, size));
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(random.nextBoolean() ? GradientDrawable.RECTANGLE : GradientDrawable.OVAL);
                gd.setColor(Color.HSVToColor(new float[]{random.nextInt(360), 0.7f, 1f}));
                p.setBackground(gd);
                p.setX(random.nextInt(width));
                p.setY(-50);
                p.setRotation(random.nextInt(360));
                confettiContainer.addView(p);
                p.animate().translationY(height + 100).translationXBy(random.nextInt(400) - 200).rotationBy(360).setDuration(random.nextInt(2000) + 1000).setInterpolator(new AccelerateInterpolator()).withEndAction(() -> confettiContainer.removeView(p)).start();
            }
        });
    }

    private void animateRewardText(TextView tv, double val, String pre) {
        if (tv == null) return;
        ValueAnimator a = ValueAnimator.ofFloat(0, (float) val);
        a.setDuration(1200);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> {
            float f = (float) anim.getAnimatedValue();
            tv.setText(pre + (val < 10 ? String.format(Locale.US, "%.1f", f) : (int)f));
        });
        a.start();
    }

    private void animateRewardText(TextView tv, int val, String pre) {
        if (tv == null) return;
        ValueAnimator a = ValueAnimator.ofInt(0, val);
        a.setDuration(1200);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(anim -> {
            int i = (int) anim.getAnimatedValue();
            tv.setText(pre + i);
        });
        a.start();
    }

    private void animateEntrance(View root) {
        View card = root.findViewById(R.id.card_rewards_main);
        if (card != null) {
            card.setAlpha(0); card.setTranslationY(80);
            card.animate().alpha(1).translationY(0).setDuration(800).setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
