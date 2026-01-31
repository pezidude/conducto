package com.example.conducto2.ui.main;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.receivers.AlarmScheduler;
import com.example.conducto2.receivers.BootReceiver;
import com.example.conducto2.receivers.HeadsetReceiver;
import com.example.conducto2.ui.TestActivity;
import com.example.conducto2.ui.auth.SignInActivity;
import com.example.conducto2.ui.auth.SignUpActivity;
import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.example.conducto2.utils.NotificationHelper;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ALARM_REQUEST_CODE = 101;

    // Views - Buttons
    MaterialButton btnSignIn, btnSignUp, btnNotify, btnAlarm, btnGoToTest;

    BootReceiver br;
    HeadsetReceiver hr;
    
    private FirestoreManager firestoreManager;

    // Notification Helper
    private NotificationHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlarmScheduler.requestExactAlarmPermission(this);
        
        firestoreManager = new FirestoreManager();
        firestoreManager.migrateUsersToTeachers();

        initViews();
        notificationHelper = new NotificationHelper(this);

        br = new BootReceiver();
        hr = new HeadsetReceiver();
    }

    private void checkIfUserSignedIn() {
        if (FirestoreManager.isUserSignedIn()) {
            btnSignIn.setText("Dashboard");
            btnSignIn.setOnClickListener(v -> {
                Intent go = new Intent(MainActivity.this, DashboardActivity.class);
                startActivity(go);
            });
            btnSignUp.setText("Logout");
            btnSignUp.setOnClickListener(v -> {
                FirestoreManager.signOut();
                checkIfUserSignedIn(); // call again to update after logout, should land in the else statement
            });
        } else {
            btnSignIn.setText("Sign In");
            btnSignIn.setOnClickListener(this);
            btnSignUp.setText("Sign Up");
            btnSignUp.setOnClickListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkIfUserSignedIn();
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
        btnGoToTest = findViewById(R.id.btnGoToTest);

        btnSignUp.setOnClickListener(this);
        btnSignIn.setOnClickListener(this);
        btnNotify.setOnClickListener(this);
        btnAlarm.setOnClickListener(this);
        btnGoToTest.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnSignIn) {
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
        } else if (id == R.id.btnSignUp) {
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
        } else if (id == R.id.btnGoToTest) {
            startActivity(new Intent(MainActivity.this, TestActivity.class));
        } else if (id == R.id.btnNotify) {
            notificationHelper.makeNotification("Status", hr.getState() == 1 ? "Headset Plugged in!" : "Headset is Not Plugged in!");
        } else if (id == R.id.btnAlarm) {
            Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 10);
            long triggerTime = calendar.getTimeInMillis();
            AlarmScheduler.setAlarm(MainActivity.this, triggerTime, ALARM_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALARM_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted to receive phone state", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied to receive phone state", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
