package com.example.conducto2.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    // A unique string to identify our broadcast intent
    public static final String ACTION_ALARM = "com.example.conducto2.ACTION_ALARM";
    public static final String ACTION_ALARM_SPECIFIC_TIME_TRIGGERED= "com.example.conducto2.ACTION_SPECIFIC_ALARM";
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "got timer- on receive", Toast.LENGTH_SHORT).show();

        if (intent != null && ACTION_ALARM.equals(intent.getAction())) {
            Log.d("AlarmReceiver", "Alarm has expired! Performing task.");

        } else if ( intent != null && ACTION_ALARM_SPECIFIC_TIME_TRIGGERED.equals(intent.getAction())) {
            Log.d("AlarmReceiver", "Alarm specific timer has expired! Performing task.");

        }
    }
}