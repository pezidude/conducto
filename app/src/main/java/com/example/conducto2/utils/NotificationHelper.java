package com.example.conducto2.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.conducto2.R;

public class NotificationHelper {

    private static final String NOTIFICATION_CHANNEL_ID = "HEADSET_CH";
    private static final int NOTIFICATION_CANCEL_CODE = 0;
    private Context context;
    private NotificationManager manager;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(context.getString(R.string.app_name));
            channel.setLightColor(Color.GREEN);

            manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void makeNotification(String notiTitle, String notiText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder notificationB = new Notification.Builder(context.getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(notiTitle)
                    .setContentText(notiText)
                    .setSmallIcon(R.drawable.baseline_headphones_24)
                    .setAutoCancel(true);

            if (manager != null) {
                manager.notify(NOTIFICATION_CANCEL_CODE, notificationB.build());
            }
        } else {
            notifyBeforeAPI26(notiTitle, notiText);
        }
    }

    private void notifyBeforeAPI26(String notiTitle, String notiText) {
        Intent intent = new Intent();
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setContentTitle(notiTitle);
        builder.setContentText(notiText);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.baseline_headphones_24); // Add small icon for pre-Oreo

        Notification notification = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_CANCEL_CODE, notification);
        }
    }
}