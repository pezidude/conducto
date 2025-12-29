package com.example.conducto2;

import static com.example.conducto2.FirebaseUtils.FirebaseComm.authUserEmail;
import static com.example.conducto2.FirebaseUtils.FirebaseComm.isUserSignedIn;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.FirebaseUtils.FBAuth;
import com.example.conducto2.FirebaseUtils.FirebaseComm;
import com.example.conducto2.FirebaseUtils.FirestoreManager;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener,FBAuth.SignInResult, FirestoreManager.DBResult {

    FBAuth signUp;
    TextView tvError, tvGoToSignIn;
    EditText etEmail, etPassword, etConfirmPassword;
    EditText etFname, etLname, etUname;

    Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);


        // init views
        tvError = findViewById(R.id.tvError);
        tvError.setVisibility(View.GONE);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etFname = findViewById(R.id.etFname);
        etLname = findViewById(R.id.etLname);
        etUname = findViewById(R.id.etUname);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(this);
        tvGoToSignIn = findViewById(R.id.tvGoToSignIn);
        tvGoToSignIn.setOnClickListener(this);

        connectToDB();
    }

    private void connectToDB() {
        signUp = new FBAuth();
        signUp.setSignInResult(this);

        if (isUserSignedIn()) {
            // move to post Activity
            Log.d("DATA", "user is already signed in: " + authUserEmail());
            Intent go = new Intent(SignUpActivity.this, DashboardActivity.class);
            startActivity(go);

        }

        // this means user not signed in registered
        Toast.makeText(this, "please register/sign in", Toast.LENGTH_SHORT).show();
    }

    public void register() {
        // Get input and remove leading/trailing spaces
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString(); // Don't trim password, spaces might be intentional
        String uname = etUname.getText().toString().trim();
        String fname = etFname.getText().toString().trim();
        String lname = etLname.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString();

        tvError.setVisibility(View.VISIBLE);

        if (email.isEmpty() || password.isEmpty() || uname.isEmpty() || fname.isEmpty() || lname.isEmpty()) {
            // check if ANY field is empty
            tvError.setText("Please enter all required fields.");
        } else if (!password.equals(confirmPass)) {
            // check if passwords match
            tvError.setText("Passwords don't match.");
        } else {
            tvError.setVisibility(View.GONE);

            signUp.createUser(email, password);
        }
    }

    @Override
    public void loginResult(boolean result, String message) {
        return;
    }

    public void insertUserToFB(User user) {
        // the the user details to the fire store
        FirestoreManager fbManager = new FirestoreManager();
        fbManager.insertUser(user);
        fbManager.setDbResult(this);
    }

    @Override
    public void registerResult(boolean result, String message) {
        // 1st step: fbauth result function
        if (result) {
            // here user is authed
            User user = new User(etUname.getText().toString(),etFname.getText().toString(), etLname.getText().toString());
            insertUserToFB(user);
        } else {
            // here user is NOT authed
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(message);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == btnSignUp) {
            register();
        } else if (view == tvGoToSignIn) {
            Intent go = new Intent(SignUpActivity.this, SignInActivity.class);
            startActivity(go);
        }
    }

    @Override
    public void uploadResult(boolean success) {
        // firestore result function (should be success because of auth step)
        if (success) {
            Intent go = new Intent(SignUpActivity.this, DashboardActivity.class);
            startActivity(go);
        } else {
            Log.d("Auth", "[ERROR] User creation somehow failed after successful authentication.");

        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, "Register Success!", Toast.LENGTH_LONG).show();
    }
}