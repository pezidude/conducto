package com.example.conducto2.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.ClassListActivity;
import com.google.android.material.button.MaterialButton;

public class DashboardActivity extends BaseDrawerActivity implements com.example.conducto2.data.firebase.FirestoreManager.UserFetchListener {

    MaterialButton btnLogout;
    TextView tvWelcomeMessage, tvUserTypeStatus;
    // firestoreManager is inherited from BaseDrawerActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Toolbar toolbar = findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        // firestoreManager is initialized in BaseDrawerActivity.onCreate

        initViews();
        setupUserSpecificElements();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);
        tvWelcomeMessage = findViewById(R.id.tvWelcomeMessage);
        tvUserTypeStatus = findViewById(R.id.tvUserTypeStatus);

        btnLogout.setOnClickListener(v -> logout());
    }

    private void setupUserSpecificElements() {
        firestoreManager.getUser(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_my_classes) {
            startActivity(new Intent(this, ClassListActivity.class));
            return true;
        } else if (itemId == R.id.menu_todo) {
            Toast.makeText(this, "TODO clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.menu_recent_lessons) {
            Toast.makeText(this, "Recent Lessons clicked", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUserFetched(User user) {
        if (user != null) {
            String welcomeMsg = "Welcome, " + user.getFname() + " " + user.getLname();
            tvWelcomeMessage.setText(welcomeMsg);
            tvUserTypeStatus.setText("User Type: " + user.getUserType());
            if ("teacher".equals(user.getUserType())) {
                // Teacher specific logic here
            }
        }
    }

    private void logout() {
        FirebaseComm.signOut();
        finish();
    }
}