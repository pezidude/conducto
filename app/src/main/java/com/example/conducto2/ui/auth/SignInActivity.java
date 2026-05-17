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

import com.example.conducto2.ui.dashboard.DashboardActivity;
import com.example.conducto2.data.firebase.FBAuth;
import com.example.conducto2.R;

/**
 * SignInActivity
 * 
 * This activity handles the user login workflow. It provides an interface for 
 * existing users to authenticate with their credentials and manage their session.
 * 
 * Key Responsibilities:
 * 1. Session Persistence Check: Automatically routes signed-in users to the Dashboard.
 * 2. Auth Bridging: Interfaces with the {@link FBAuth} manager to execute cloud sign-in.
 * 3. Error Feedback: Provides real-time visual feedback for failed authentication attempts.
 * 4. Navigation: Facilitates the transition to the registration screen.
 */
public class SignInActivity extends AppCompatActivity implements View.OnClickListener, FBAuth.SignInResult {

    /** The authentication manager responsible for cloud communication. */
    private FBAuth signIn;

    /** Input field for the user's email address. */
    private EditText etEmail;

    /** Input field for the user's password. */
    private EditText etPassword;

    /** Text display for error messages (e.g., "Invalid credentials"). */
    private TextView tvError;

    /** Clickable text to navigate to the {@link SignUpActivity}. */
    private TextView tvGoToSignUp;

    /** Button to trigger the authentication attempt. */
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // UI Initialization
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(this);

        tvError = findViewById(R.id.tvError);
        tvError.setVisibility(View.GONE);

        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);
        tvGoToSignUp.setOnClickListener(this);

        connectToDB();
    }

    /**
     * Initializes the Auth manager and checks if a valid session already exists.
     * If a user is found, it bypasses the login screen for a smoother UX.
     */
    private void connectToDB() {
        signIn = new FBAuth();
        signIn.setSignInResult(this);

        if (isUserSignedIn()) {
            // Logic: Immediate bypass to Dashboard if tokens are still valid.
            Log.d("DATA", "user is already signed in: " + authUserEmail());
            Intent go = new Intent(SignInActivity.this, DashboardActivity.class);
            startActivity(go);
            finish(); // Prevent the user from returning to login via back button.
        }

        Toast.makeText(this, "please register/sign in", Toast.LENGTH_SHORT).show();
    }

    /**
     * Extracts user inputs and initiates the authentication request.
     * Performs basic client-side validation before hitting the network.
     */
    private void signIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email or Password are blank", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Delegate authentication logic to the service layer.
        signIn.loginUser(email, password);
    }

    @Override
    public void onClick(View view) {
        if (view == btnSignIn) {
            signIn();
        } else if (view == tvGoToSignUp) {
            // Screen Transition: Move to Registration.
            Intent go = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(go);
        }
    }

    /**
     * Callback from FBAuth after a login attempt completes.
     * @param result True if authentication was successful.
     * @param message Error details or success status.
     */
    @Override
    public void loginResult(boolean result, String message) {
        if (result){
            // Transition: Move to main application dashboard.
            Intent go = new Intent(SignInActivity.this, DashboardActivity.class);
            startActivity(go);
            finish();
        } else {
            // Visual Alert: Display the server error to the user.
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void registerResult(boolean result, String message) {
        // Not used in this activity context.
    }
}