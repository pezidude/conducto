package com.example.conducto2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String NOTIFICATION_CHANNEL_ID = "HEADSET_CH";
    private static final int NOTIFICATION_CANCEL_CODE = 0;
    private static final int ALARM_REQUEST_CODE = 101;

    // Views - Buttons
    Button btnSignIn, btnSignUp, btnNotify, btnAlarm;

    BootReceiver br;
    HeadsetReceiver hr;

    // Notification
    NotificationManagerCompat notificationManager;
    NotificationChannel channel;
    NotificationManager manager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlarmScheduler.requestExactAlarmPermission(this);

        // TESTING: Go Straight to Intent MIDIPlayerActivity
        startActivity(new Intent(MainActivity.this, MIDIPlayerActivity.class));

        initViews();
        createNotificationChannel();

        br = new BootReceiver();
        hr = new HeadsetReceiver();

    }


    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        registerReceiver(br, filter);

        IntentFilter filter2 = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(hr, filter2);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
        unregisterReceiver(hr);
    }


    public void initViews() {
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnNotify = findViewById(R.id.btnNotify);
        btnAlarm = findViewById(R.id.btnAlarm);

        btnSignUp.setOnClickListener(this);
        btnSignIn.setOnClickListener(this);
        btnNotify.setOnClickListener(this);
        btnAlarm.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        if (view == btnSignIn) {
            Intent go = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(go);
        } else if (view == btnSignUp) {
            Intent go = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(go);
        } else if (view == btnNotify) {
            makeNotification("Status", hr.getState() == 1 ? "Headset Plugged in!" : "Headset is Not Plugged in!");
        } else if (view == btnAlarm) {
            Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 10);
            long triggerTime = calendar.getTimeInMillis();

            AlarmScheduler.setAlarm(MainActivity.this, triggerTime, ALARM_REQUEST_CODE);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(getString(R.string.app_name));
            channel.setLightColor(Color.GREEN);

            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void makeNotification(String notiTitle, String notiText) {

        System.out.println("" + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            //     createNotificationChannel();

            Notification.Builder notificationB = new Notification.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(notiTitle)
                    .setContentText(notiText).setSmallIcon(R.drawable.baseline_headphones_24)
                    .setAutoCancel(true);

            manager.notify(NOTIFICATION_CANCEL_CODE, notificationB.build());
        } else {
            notifyBeforAPI26(notiTitle, notiText);
        }
    }
    /*
    public void makeAlarm() {

        int currentHour, currentMinute;

        currentHour = Calendar.getInstance().get(Calendar.HOUR);
        currentMinute = Calendar.getInstance().get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(MainActivity.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        int mhour = i;
                        int mminute = i1;
                        int msecond = 0;
                        btnAlarm.setText("AlarmManager\n" + mhour + ":" + mminute);
                        doAlarm();
                    }
                }, currentHour, currentMinute, false);
        tpd.show();
    }
    */


    private void notifyBeforAPI26(String notiTitle, String notiText) {
        Intent intent = new Intent();
        PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this,
                0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // before API 26
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this);

        //build notification data:
        builder.setContentIntent(contentIntent);
        builder.setContentTitle(notiTitle);
        builder.setContentText(notiText);
        builder.setDefaults(Notification.DEFAULT_SOUND);
        builder.setWhen(System.currentTimeMillis());
        Notification noti = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_CANCEL_CODE, noti);

    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // this means fine location
            case ALARM_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "permission granted to receive phone state", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(this, "permission denied to receive phone state", Toast.LENGTH_SHORT).show();
                break;

        }
    }
}