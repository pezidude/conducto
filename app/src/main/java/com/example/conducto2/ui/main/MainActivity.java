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
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.ui.TestActivity;
import com.example.conducto2.ui.auth.SignInActivity;
import com.example.conducto2.ui.auth.SignUpActivity;
import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int ALARM_REQUEST_CODE = 101;

    // Views - Buttons
    MaterialButton btnSignIn, btnSignUp, btnNotify, btnAlarm, btnGoToTest;
    
    private FirestoreManager firestoreManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestoreManager = new FirestoreManager();

        initViews();
    }

    private void checkIfUserSignedIn() {
        if (FirestoreManager.isUserSignedIn()) {
            firestoreManager.getUser(user -> {
                DataManager.setUser(user);
            });
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
    }

    @Override
    protected void onPause() {
        super.onPause();

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
