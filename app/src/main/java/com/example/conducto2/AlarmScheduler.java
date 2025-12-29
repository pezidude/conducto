package com.example.conducto2;


import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.Calendar;

public class AlarmScheduler {

    private static final int ALARM_REQUEST_CODE = 123; // A unique ID for this alarm
    public static void setAlarm(Context context, long triggerTime, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e("AlarmScheduler", "AlarmManager is null.");
            return;
        }

        // Create the intentthat will be fired when the alarm goes off.
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM); // Use a custom action toidentify the intent.
        intent.putExtra("EXTRA_DATA", "Some value to pass to the receiver"); // Optional: pass data

        // Create a PendingIntent that will wrap the broadcast intent.
        // The system will use this to execute the broadcast even if your app is not running.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode, // A unique request code to identify this alarm.
                intent,
                // Use FLAG_IMMUTABLE for security, and FLAG_UPDATE_CURRENT to update
                // the alarm if it's set again withthe same request code.
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Check for exact alarm permission on Android 12 (API 31) and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // The app doesn't have the permission. Inform the user or handle gracefully.
                // You might redirect them to settings: new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                Log.w("AlarmScheduler", "Cannot schedule exact alarms. App needs SCHEDULE_EXACT_ALARM permission.");
                // As a fallback, you could set an inexact alarm here.
                // alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                return;
            }
        }

        // Set the exact alarm.
        // RTC_WAKEUP: Fires the alarm at the specified time and wakes up the device if it's asleep.
        // setExactAndAllowWhileIdle: Works even in Doze mode forcritical alarms.
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );

        Log.i("AlarmScheduler", "Alarm set successfully for time: " + triggerTime + " with request code: "+ requestCode);
    }

    /**
     * Cancels a previously set alarm.
     *
     * @param context The context.
     * @param requestCode The same unique request code used to set the alarm.
     */
    public static void cancelAlarm(Context context, int requestCode) {AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM);

        PendingIntent pendingIntent= PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.i("AlarmScheduler", "Alarm with request code " + requestCode + " cancelled.");
        }
    }

    /*if the permissionis not granted and the request process has been started.
     */
    public static void requestExactAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {

                new AlertDialog.Builder(context)
                        .setTitle("Permission Required")
                        .setMessage("To ensure alarms and notifications are delivered on time, this app needs the 'Alarms & reminders' permission. Please grant this on the next screen.")
                        .setPositiveButton("Go to Settings", (dialog, which) -> {
                            // The user agreed, so redirect them to the system settings.redirectToPermissionSettings(context);
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + context.getPackageName()));
                            context.startActivity(intent);

                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            // The user cancelled. You may want to disable the feature
                            // that requires the alarm.
                            dialog.dismiss();
                        })
                        .create()
                        .show();
            }
        }



    }
    // Use a unique request code for each alarm to avoid conflicts
    private static final int SPECIFIC_TIME_ALARM_REQUEST_CODE = 456;

    /**
     * Sets a precise one-time alarm to trigger at a specific hour and minute.*
     * @param context The application or activity context.
     * @param hour    The hour of the day to trigger the alarm (0-23).
     * @param minute  The minute of the hour to trigger the alarm (0-59).
     */
    public static void setAlarmAtSpecificTime(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e("AlarmScheduler", "Could not get AlarmManager service.");
            return;
        }

        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission isnot granted. Redirect user to settings to grant it.
                Toast.makeText(context, "Permission needed to set exact alarms.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                context.startActivity(intent);return;
            }
        }

        // --- Use Calendar to set the specific time for the alarm ---
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Check if the calculated time is in the past.
        // If it is, add one day to the calendar to schedule it for the nextday.
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            Log.d("AlarmScheduler", "Alarm time is in the past. Scheduling for the next day.");
        }
        // --- Endof Calendar logic ---

        // Create the intent that will be sent to the AlarmReceiver
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(AlarmReceiver.ACTION_ALARM_SPECIFIC_TIME_TRIGGERED);

        // Create the PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,SPECIFIC_TIME_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set the exact alarm
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }


}
