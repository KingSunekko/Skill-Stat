package com.example.skillstat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FocusService extends Service {

    private static final String CHANNEL_ID = "FocusServiceChannel";
    private static final String CHANNEL_ID_COMPLETE = "FocusCompleteChannel";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_RESUME = "RESUME";
    public static final String ACTION_FINISH = "FINISH";
    public static final String ACTION_UPDATE_SOUND = "UPDATE_SOUND";

    private final IBinder binder = new LocalBinder();

    public static MutableLiveData<Long> timeLeftLiveData = new MutableLiveData<>();
    public static MutableLiveData<Boolean> isTimerRunning = new MutableLiveData<>(false);
    public static MutableLiveData<Boolean> isStrictModeTriggered = new MutableLiveData<>(false);

    private CountDownTimer countDownTimer;
    private long totalTimeInMillis;
    private long timeLeftInMillis;
    private String currentSkillName;

    private CountDownTimer graceTimer;
    private boolean isInGracePeriod = false;
    private DatabaseReference mDatabase;
    private String uid;

    // Soundscape support
    private MediaPlayer mediaPlayer;
    private int currentSoundResId = 0;

    public class LocalBinder extends Binder {
        FocusService getService() {
            return FocusService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        uid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                String skillName = intent.getStringExtra("skillName");
                long newTotalTime = intent.getLongExtra("totalTime", 0);

                // FIX: Check if we are already running the SAME skill with the SAME goal time.
                // If the goal time is DIFFERENT (user edited it), we MUST restart the timer.
                boolean isSameSkill = currentSkillName != null && currentSkillName.equals(skillName);
                boolean isSameGoal = newTotalTime == totalTimeInMillis;

                if (Boolean.TRUE.equals(isTimerRunning.getValue()) && isSameSkill && isSameGoal) {
                    // Already running correctly, just bring foreground
                    startForeground(NOTIFICATION_ID, getNotification("Focusing..."));
                } else {
                    // Restart or Start new timer because goal changed or new skill
                    totalTimeInMillis = newTotalTime;
                    currentSkillName = skillName;
                    timeLeftInMillis = totalTimeInMillis;

                    isStrictModeTriggered.setValue(false);
                    isTimerRunning.setValue(true);

                    startForeground(NOTIFICATION_ID, getNotification("Focusing..."));
                    startTimer();
                    updatePracticingStatus(true);
                    resumeSound();
                }
            } else if (ACTION_STOP.equals(action)) {
                updatePracticingStatus(false);
                stopTimer();
                stopSound();
                isStrictModeTriggered.setValue(false);
                stopForeground(true);
                stopSelf();
            } else if (ACTION_PAUSE.equals(action)) {
                pauseTimer();
                pauseSound();
            } else if (ACTION_RESUME.equals(action)) {
                resumeTimer();
                resumeSound();
            } else if (ACTION_FINISH.equals(action)) {
                finishSessionEarly();
            } else if (ACTION_UPDATE_SOUND.equals(action)) {
                int soundResId = intent.getIntExtra("soundResId", 0);
                updateSound(soundResId);
            }
        }
        return START_NOT_STICKY;
    }

    private void finishSessionEarly() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning.postValue(false);
        updatePracticingStatus(false);
        stopSound();

        // Open App and trigger Rewards
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigate_to", "practice");
        intent.putExtra("skill_name", currentSkillName);
        intent.putExtra("trigger_finish", true);
        intent.putExtra("time_left", timeLeftInMillis); 
        startActivity(intent);
        
        stopForeground(true);
        stopSelf();
    }

    private void updatePracticingStatus(boolean practicing) {
        if (uid != null) {
            mDatabase.child("users").child(uid).child("practicing").setValue(practicing);
            if (practicing) {
                mDatabase.child("users").child(uid).child("practicing").onDisconnect().setValue(false);
            }
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();

        isTimerRunning.postValue(true);
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                timeLeftLiveData.postValue(timeLeftInMillis);
                updateNotification(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                isTimerRunning.postValue(false);
                timeLeftLiveData.postValue(0L);
                updatePracticingStatus(false);
                stopSound();

                updateNotification(0, "Session Complete! Well done.");

                Intent intent = new Intent(FocusService.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("timer_finished", true);
                intent.putExtra("time_left", 0L);
                intent.putExtra("navigate_to", "practice");
                intent.putExtra("skill_name", currentSkillName);
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("FocusService", "Failed to start activity", e);
                }
            }
        }.start();
    }

    private void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        if (graceTimer != null) graceTimer.cancel();
        isTimerRunning.postValue(false);
    }

    public void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            isTimerRunning.postValue(false);
            updateNotification(timeLeftInMillis, "Paused");
            pauseSound();
        }
    }

    public void resumeTimer() {
        if (!Boolean.TRUE.equals(isTimerRunning.getValue()) && timeLeftInMillis > 0) {
            startTimer();
            resumeSound();
        }
    }

    // Sound Management
    private void updateSound(int resId) {
        if (currentSoundResId == resId) return;

        stopSound();
        currentSoundResId = resId;

        if (resId != 0 && Boolean.TRUE.equals(isTimerRunning.getValue())) {
            playSound(resId);
        }
    }

    private void playSound(int resId) {
        try {
            mediaPlayer = MediaPlayer.create(this, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setVolume(0.5f, 0.5f);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e("FocusService", "Error playing sound", e);
        }
    }

    private void pauseSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    private void resumeSound() {
        if (currentSoundResId != 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
            if (mediaPlayer != null) {
                mediaPlayer.start();
            } else {
                playSound(currentSoundResId);
            }
        }
    }

    private void stopSound() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e("FocusService", "Error stopping sound", e);
            }
            mediaPlayer = null;
        }
    }

    public void enterBackground(boolean isStrictMode) {
        if (!isStrictMode) return;

        isInGracePeriod = true;
        if (graceTimer != null) graceTimer.cancel();

        graceTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateNotification(timeLeftInMillis, "⚠️ Return to SkillStat! " + (millisUntilFinished/1000) + "s left");
            }

            @Override
            public void onFinish() {
                if (isInGracePeriod) {
                    isStrictModeTriggered.postValue(true);
                    stopTimer();
                    stopSound();
                    updateNotification(timeLeftInMillis, "❌ Session Failed: You left the app!");
                    updatePracticingStatus(false);
                }
            }
        }.start();
    }

    public void enterForeground() {
        isInGracePeriod = false;
        if (graceTimer != null) graceTimer.cancel();
        if (timeLeftInMillis > 0 && Boolean.TRUE.equals(isTimerRunning.getValue())) {
            updateNotification(timeLeftInMillis, "Focusing...");
        }
    }

    private void updateNotification(long millisUntilFinished) {
        String content = Boolean.TRUE.equals(isTimerRunning.getValue()) ? "Focusing..." : "Paused";
        updateNotification(millisUntilFinished, content);
    }

    private void updateNotification(long millisUntilFinished, String contentText) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, getNotification(contentText));
        }
    }

    private Notification getNotification(String contentText) {
        boolean finished = timeLeftInMillis <= 0;
        String channelId = finished ? CHANNEL_ID_COMPLETE : CHANNEL_ID;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("timer_finished", finished);
        notificationIntent.putExtra("time_left", timeLeftInMillis);
        notificationIntent.putExtra("navigate_to", "practice");
        notificationIntent.putExtra("skill_name", currentSkillName);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(!finished)
                .setOngoing(!finished);

        if (finished) {
            builder.setContentTitle("Session Finished!");
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setCategory(NotificationCompat.CATEGORY_ALARM);
            builder.setFullScreenIntent(pendingIntent, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setDefaults(Notification.DEFAULT_ALL);
            }
            builder.setAutoCancel(true);
        } else {
            int minutes = (int) (timeLeftInMillis / 1000) / 60;
            int seconds = (int) (timeLeftInMillis / 1000) % 60;
            String timeStr = String.format("%02d:%02d left", minutes, seconds);
            builder.setContentTitle(timeStr);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);

            int progress = (int) ((totalTimeInMillis - timeLeftInMillis) * 100 / Math.max(1, totalTimeInMillis));
            builder.setProgress(100, progress, false);

            boolean running = Boolean.TRUE.equals(isTimerRunning.getValue());
            
            // PAUSE / RESUME Action
            Intent actionIntent = new Intent(this, FocusService.class);
            actionIntent.setAction(running ? ACTION_PAUSE : ACTION_RESUME);
            PendingIntent actionPendingIntent = PendingIntent.getService(this, 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            String actionText = running ? "Pause" : "Resume";
            int actionIcon = running ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            builder.addAction(actionIcon, actionText, actionPendingIntent);

            // FINISH Action
            Intent finishIntentEarly = new Intent(this, FocusService.class);
            finishIntentEarly.setAction(ACTION_FINISH);
            PendingIntent finishPendingIntent = PendingIntent.getService(this, 2, finishIntentEarly, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_menu_save, "Finish", finishPendingIntent);
        }

        return builder.build();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        CHANNEL_ID,
                        "Focus Progress",
                        NotificationManager.IMPORTANCE_LOW
                );
                NotificationChannel completeChannel = new NotificationChannel(
                        CHANNEL_ID_COMPLETE,
                        "Session Completion",
                        NotificationManager.IMPORTANCE_HIGH
                );
                manager.createNotificationChannel(serviceChannel);
                manager.createNotificationChannel(completeChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        updatePracticingStatus(false);
        stopSound();
        super.onDestroy();
    }
}
