package com.example.skillstat;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.skillstat.models.Duel;
import com.example.skillstat.models.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ReminderWorker extends Worker {
    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return Result.success();

        String uid = auth.getUid();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        try {
            DataSnapshot userSnap = Tasks.await(rootRef.child("users").child(uid).get());
            User user = userSnap.getValue(User.class);
            if (user == null) return Result.success();

            String duelSkillName = null;
            
            // Check for active duels and settle or remind
            DataSnapshot activeDuelsRef = Tasks.await(rootRef.child("users").child(uid).child("activeDuels").get());
            if (activeDuelsRef.exists()) {
                for (DataSnapshot ds : activeDuelsRef.getChildren()) {
                    String duelId = ds.getKey();
                    if (duelId == null) continue;

                    DataSnapshot duelSnap = Tasks.await(rootRef.child("duels").child(duelId).get());
                    Duel duel = duelSnap.getValue(Duel.class);

                    if (duel != null && "active".equals(duel.getStatus())) {
                        if (System.currentTimeMillis() > duel.getEndTime()) {
                            settleDuelBackground(duelId, duel, rootRef);
                        } else {
                            if (duelSkillName == null) duelSkillName = duel.getSkillName();
                        }
                    }
                }
            }

            checkConditionsAndNotify(user, rootRef.child("users").child(uid), duelSkillName);

        } catch (Exception e) {
            Log.e(TAG, "Error in ReminderWorker", e);
            return Result.retry();
        }

        return Result.success();
    }

    private void settleDuelBackground(String duelId, Duel duel, DatabaseReference rootRef) throws Exception {
        double initEff = duel.getInitiatorEffort();
        double oppEff = duel.getOpponentEffort();
        boolean draw = initEff == oppEff;

        String winner = draw ? "draw" : (initEff > oppEff ? duel.getInitiatorUid() : duel.getOpponentUid());
        String loser = draw ? null : (initEff > oppEff ? duel.getOpponentUid() : duel.getInitiatorUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("duels/" + duelId + "/status", "completed");
        updates.put("duels/" + duelId + "/winnerUid", winner);

        if (!draw) {
            updates.put("users/" + winner + "/totalPoints", ServerValue.increment(500 + (duel.getWagerAmount() * 2)));
            updates.put("users/" + winner + "/wins", ServerValue.increment(1));
            updates.put("users/" + loser + "/losses", ServerValue.increment(1));
        } else {
            updates.put("users/" + duel.getInitiatorUid() + "/totalPoints", ServerValue.increment(250 + duel.getWagerAmount()));
            updates.put("users/" + duel.getOpponentUid() + "/totalPoints", ServerValue.increment(250 + duel.getWagerAmount()));
        }

        updates.put("users/" + duel.getInitiatorUid() + "/activeDuels/" + duelId, null);
        updates.put("users/" + duel.getOpponentUid() + "/activeDuels/" + duelId, null);
        updates.put("users/" + duel.getInitiatorUid() + "/completedDuels/" + duelId, true);
        updates.put("users/" + duel.getOpponentUid() + "/completedDuels/" + duelId, true);

        Tasks.await(rootRef.updateChildren(updates));

        NotificationHelper.showNotification(getApplicationContext(),
                "Duel Results are in! ⚔️",
                "The " + duel.getSkillName() + " duel has ended. Check the results now!",
                NotificationHelper.CHANNEL_ID_DUELS);
    }

    private void checkConditionsAndNotify(User user, DatabaseReference userRef, String duelSkillName) {
        long now = System.currentTimeMillis();
        long lastPractice = user.getLastPracticeTimestamp();
        long diffInHours = (now - lastPractice) / (1000 * 60 * 60);
        long lastNotif = user.getLastNotificationTimestamp();
        
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // SPAM SETTINGS: Reduced cooldown to 1 hour to allow more frequent notifications
        long spamCooldown = 1000 * 60 * 60 * 1; 

        // 1. Weekly Recap (Sunday morning)
        if (dayOfWeek == Calendar.SUNDAY && hour >= 9) {
            if ((now - lastNotif) > (1000 * 60 * 60 * 20)) {
                NotificationHelper.showNotification(getApplicationContext(),
                        "Weekly Recap 📊",
                        "You earned " + user.getWeeklyPoints() + " XP this week! Ready for another push?");
                userRef.child("lastNotificationTimestamp").setValue(now);
                return;
            }
        }

        // 2. Skill Decay Warning (Sensitive: 1 day inactivity)
        if (user.isDecayAlertsEnabled() && (now - lastNotif) > spamCooldown) {
            Map<String, Long> lastPracticeMap = user.getSkillLastPractice();
            if (user.getSkills() != null) {
                for (String skill : user.getSkills()) {
                    long lastSkillPractice = lastPracticeMap != null ? lastPracticeMap.getOrDefault(skill, 0L) : 0L;
                    long hoursSince = (now - lastSkillPractice) / (1000 * 60 * 60);
                    
                    if (hoursSince >= 24) { 
                        NotificationHelper.showNotification(getApplicationContext(),
                                "Skill at Risk! ⚠️",
                                "Don't let your '" + skill + "' mastery drop! Practice now to stay at the top.");
                        userRef.child("lastNotificationTimestamp").setValue(now);
                        return;
                    }
                }
            }
        }

        // 3. Social: Pending Friend Requests
        if (user.getFriendRequests() != null && !user.getFriendRequests().isEmpty()) {
            if ((now - lastNotif) > spamCooldown) {
                NotificationHelper.showNotification(getApplicationContext(),
                        "Pending Requests 👥",
                        "You have " + user.getFriendRequests().size() + " new friend requests waiting!");
                userRef.child("lastNotificationTimestamp").setValue(now);
                return;
            }
        }

        // GLOBAL SPAM PREVENTION - 1 HOUR
        if ((now - lastNotif) < spamCooldown) return;

        // 4. Achievement: Streak Milestones
        if (user.getStreak() > 0 && (user.getStreak() == 3 || user.getStreak() % 7 == 0)) {
            NotificationHelper.showNotification(getApplicationContext(),
                    "Amazing Streak! 💎",
                    "Congratulations on your " + user.getStreak() + "-day streak! Keep the fire burning! 🔥");
            userRef.child("lastNotificationTimestamp").setValue(now);
            return;
        }

        // 5. Daily Reminders (Aggressive: triggers every 4 hours of inactivity)
        if (user.isDailyRemindersEnabled()) {
            if (duelSkillName != null) {
                NotificationHelper.showNotification(getApplicationContext(), 
                        "Duel Practice! ⚔️", 
                        "Your opponent is practicing " + duelSkillName + "! Wag kang papatalo!", 
                        NotificationHelper.CHANNEL_ID_DUELS);
            } else if (diffInHours >= 4) {
                String title = getRandomTitle();
                String message = "Your " + user.getStreak() + "-day streak misses you. " + getRandomQuote();
                NotificationHelper.showNotification(getApplicationContext(), title, message, NotificationHelper.CHANNEL_ID_REMINDERS);
            }
            userRef.child("lastNotificationTimestamp").setValue(now);
        }
    }

    private String getRandomTitle() {
        String[] titles = {
            "Time to Level Up! ⭐",
            "Consistency is Key! 🔑",
            "Ready for a session? 🎒",
            "Your skills miss you! 🧠",
            "Keep the fire burning! 🔥",
            "Don't break the streak! ⚡",
            "Quick 5-minute session? ⏱️",
            "Don't be lazy! 😤",
            "Rise and grind! 🌅"
        };
        return titles[new Random().nextInt(titles.length)];
    }

    private String getRandomQuote() {
        String[] quotes = {
            "Small progress is still progress.",
            "Success is the sum of small efforts.",
            "Don't stop until you're proud.",
            "Your only limit is you.",
            "Focus on being productive, not busy.",
            "Great things never come from comfort zones.",
            "Win the day!",
            "Practice makes perfect.",
            "Stay sharp!",
            "The best time to practice is NOW."
        };
        return quotes[new Random().nextInt(quotes.length)];
    }
}
