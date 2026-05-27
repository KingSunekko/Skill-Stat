package com.example.skillstat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {
    public static final String CHANNEL_ID_REMINDERS = "skillstat_reminders_v4";
    public static final String CHANNEL_ID_DUELS = "skillstat_duels_v4";
    public static final String CHANNEL_ID_SOCIAL = "skillstat_social_v1";
    public static final String CHANNEL_ID_ACHIEVEMENTS = "skillstat_achievements_v1";

    public static void showNotification(Context context, String title, String message) {
        showNotification(context, title, message, CHANNEL_ID_REMINDERS);
    }

    public static void showNotification(Context context, String title, String message, String channelId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name;
            int importance = NotificationManager.IMPORTANCE_HIGH;
            
            if (channelId.equals(CHANNEL_ID_DUELS)) name = "SkillStat Duels";
            else if (channelId.equals(CHANNEL_ID_SOCIAL)) name = "SkillStat Social";
            else if (channelId.equals(CHANNEL_ID_ACHIEVEMENTS)) name = "SkillStat Achievements";
            else name = "SkillStat Reminders";

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher) 
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH) 
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        notificationManager.notify(requestCode, builder.build());
    }
}
