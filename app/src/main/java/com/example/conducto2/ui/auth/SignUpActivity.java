package com.example.conducto2.ui.auth;

import static com.example.conducto2.data.firebase.FirebaseComm.authUserEmail;
import static com.example.conducto2.data.firebase.FirebaseComm.isUserSignedIn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.example.conducto2.data.firebase.FBAuth;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.R;
import com.example.conducto2.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * SignUpActivity
 * 
 * This activity manages the new user registration pipeline. It implements a 
 * robust, two-stage data persistence strategy:
 * 1. Cloud Authentication: Creating an identity (Email/Password) in Firebase Auth.
 * 2. Profile Persistence: Storing user metadata (Names, Type, Profile Pic) in Firestore.
 * 
 * Features:
 * - Comprehensive multi-field validation.
 * - Role selection (Teacher vs. Student) using custom Material toggle groups.
 * - Cascading callback orchestration (waiting for Auth success before triggering DB writes).
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener, FBAuth.SignInResult, FirebaseComm.DBResult {

    /** The authentication manager for account creation. */
    private FBAuth signUp;

    /** Feedback labels for error reporting and navigation. */
    private TextView tvError, tvGoToSignIn;

    /** Primary input fields for identity and profile data. */
    private EditText etEmail, etPassword, etConfirmPassword;
    private EditText etFname, etLname;

    /** Material UI component for selecting the account's system role. */
    private MaterialButtonToggleGroup rgUserType;    

    /** Button to initiate the multi-stage registration process. */
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // UI Initialization
        tvError = findViewById(R.id.tvError);
        tvError.setVisibility(View.GONE);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etFname = findViewById(R.id.etFname);
        etLname = findViewById(R.id.etLname);
        rgUserType = findViewById(R.id.rgUserType);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(this);
        tvGoToSignIn = findViewById(R.id.tvGoToSignIn);
        tvGoToSignIn.setOnClickListener(this);

        connectToDB();
    }

    /**
     * Secures a connection to the Auth service and performs an immediate session check.
     */
    private void connectToDB() {
        signUp = new FBAuth();
        signUp.setSignInResult(this);

        if (isUserSignedIn()) {
            Log.d("DATA", "user is already signed in: " + authUserEmail());
            startActivity(new Intent(SignUpActivity.this, DashboardActivity.class));
            finish();
        }

        Toast.makeText(this, "please register/sign in", Toast.LENGTH_SHORT).show();
    }

    /**
     * Performs a comprehensive set of client-side validations.
     * Checks for: Empty fields and password mismatch.
     */
    public void register() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString(); 
        String fname = etFname.getText().toString().trim();
        String lname = etLname.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString();

        tvError.setVisibility(View.VISIBLE);

        // Validation Rule 1: All metadata fields must be non-empty.
        if (email.isEmpty() || password.isEmpty() || fname.isEmpty() || lname.isEmpty()) {
            tvError.setText("Please enter all required fields.");
        } 
        // Validation Rule 2: Name validation (only letters and spaces).
        else if (!fname.matches("^[a-zA-Z\\s]+$") || !lname.matches("^[a-zA-Z\\s]+$")) {
            tvError.setText("Names can only contain letters and spaces.");
        }
        // Validation Rule 3: Password confirmation must match.
        else if (!password.equals(confirmPass)) {
            tvError.setText("Passwords don't match.");
        } 
        else {
            tvError.setVisibility(View.GONE);
            // Logic Trigger: Start Stage 1 (Cloud Account Creation).
            signUp.createUser(email, password);
        }
    }

    /**
     * Stage 2 Logic: Persists the validated user profile to the Firestore database.
     * @param user The fully populated User POJO.
     */
    public void insertUserToFB(User user) {
        FirestoreManager fbManager = new FirestoreManager();
        fbManager.setDbResult(this);
        fbManager.insertUser(user);
    }

    /**
     * Stage 1 Callback: Handles the result of the Firebase Auth account creation.
     */
    @Override
    public void registerResult(boolean result, String message) {
        if (result) {
            // Identity Created. Now resolve the system role from the UI selection.
            int selectedId = rgUserType.getCheckedButtonId();
            MaterialButton selectedButton = findViewById(selectedId);
            String userType = selectedButton != null ? selectedButton.getText().toString().toLowerCase() : "student";

            // Construct the persistent user model.
            User user = new User(etEmail.getText().toString().trim(), etFname.getText().toString().trim(), etLname.getText().toString().trim(), userType);
            
            // Initiation: Trigger Stage 2 (Profile Persistence).
            insertUserToFB(user);
            DataManager.setUser(user); // Cache for local use.
        } else {
            // Error Path: Notify user of Auth failure (e.g., email already in use).
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(message);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == btnSignUp) {
            register();
        } else if (view == tvGoToSignIn) {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
        }
    }

    /**
     * Stage 2 Callback: Handles the result of the profile write to Firestore.
     */
    @Override
    public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
        if (success) {
            // Pipeline Complete. Navigate to the main application hub.
            Intent go = new Intent(SignUpActivity.this, DashboardActivity.class);
            startActivity(go);
            finish();
        } else {
            Log.e("Auth", "[CRITICAL] Profile creation failed after successful auth.");
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, "Register Success!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void loginResult(boolean result, String message) {
        // Not used in this activity.
    }
}