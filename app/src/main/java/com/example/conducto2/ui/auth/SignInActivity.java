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

public class SignInActivity extends AppCompatActivity implements View.OnClickListener, FBAuth.SignInResult{

    private FBAuth signIn;
    private EditText etEmail, etPassword;
    private TextView tvError, tvGoToSignUp;
    Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);


        // init views
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

    private void connectToDB() {
        signIn = new FBAuth();
        signIn.setSignInResult(this);

        if (isUserSignedIn()) {
            // move to post Activity
            Log.d("DATA", "user is already signed in: " + authUserEmail());
            Intent go = new Intent(SignInActivity.this, DashboardActivity.class);
            startActivity(go);

        }

        // this means user not signed in registered
        Toast.makeText(this, "please register/sign in", Toast.LENGTH_SHORT).show();
    }

    private void signIn() {
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email or Password are blank", Toast.LENGTH_SHORT).show();
            return;
        }
        signIn.loginUser(email, password);
    }


    @Override
    public void onClick(View view) {
        if (view == btnSignIn) {
            signIn();
        }
        if (view == tvGoToSignUp) {
            Intent go = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(go);
        }
    }

    @Override
    public void loginResult(boolean result, String message) {
        if (result){
            Intent go = new Intent(SignInActivity.this, DashboardActivity.class);
            startActivity(go);
        } else {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void registerResult(boolean result, String message) {

    }
}