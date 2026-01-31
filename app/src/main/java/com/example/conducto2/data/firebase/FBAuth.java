package com.example.conducto2.data.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

public class FBAuth extends FirebaseComm {
    public interface SignInResult {
        void loginResult(boolean result, String message);

        void registerResult(boolean result, String message);

    }

    private static final String TAG = "FB Auth";// Methods for Authentication
    private SignInResult signInResult;

    public void setSignInResult(SignInResult signInResult) {
        this.signInResult = signInResult;
    }


    public void loginUser(String email, String password) {

        getAuth().signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {


                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: login success ");
                    if (signInResult != null) signInResult.loginResult(true, "login success");

                } else {
                    Log.d(TAG, "onComplete: login failed ");
                    if (signInResult != null)
                        signInResult.loginResult(false, task.getException().getMessage());

                }
            }
        });


    }

    public void createUser(String mail, String password) {
        getAuth().createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete:  register success");
                    if (signInResult != null) signInResult.registerResult(true, "register success");

                } else {
                    Log.d(TAG, "onComplete: " + task.getException());
                    if (signInResult != null)
                        signInResult.registerResult(false, task.getException().getMessage());
                }
            }
        });
    }


}
