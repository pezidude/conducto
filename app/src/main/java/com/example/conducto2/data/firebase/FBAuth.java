package com.example.conducto2.data.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;

/**
 * FBAuth
 * 
 * Specialized subclass of {@link FirebaseComm} dedicated to user identity management.
 * It encapsulates the logic for Firebase Authentication, providing a streamlined interface
 * for user registration and login operations.
 * 
 * This class translates complex Firebase Auth callbacks into a simple {@link SignInResult} 
 * interface, allowing UI components (like login activities) to react to authentication 
 * outcomes without being coupled to the Firebase SDK.
 */
public class FBAuth extends FirebaseComm {

    /**
     * Callback interface for reporting authentication outcomes.
     */
    public interface SignInResult {
        /**
         * Triggered upon completion of a login attempt.
         * @param result True if authentication was successful.
         * @param message Detailed status or error message.
         */
        void loginResult(boolean result, String message);

        /**
         * Triggered upon completion of a registration attempt.
         * @param result True if account creation was successful.
         * @param message Detailed status or error message.
         */
        void registerResult(boolean result, String message);
    }

    /** Identifier for logging authentication events. */
    private static final String TAG = "FB Auth";

    /** The listener instance assigned to handle authentication result events. */
    private SignInResult signInResult;

    /**
     * Assigns a listener to receive the outcomes of authentication attempts.
     * @param signInResult The listener implementation.
     */
    public void setSignInResult(SignInResult signInResult) {
        this.signInResult = signInResult;
    }

    /**
     * Attempts to authenticate a user using their email and password.
     * 
     * @param email The user's registered email address.
     * @param password The user's account password.
     */
    public void loginUser(String email, String password) {
        // Leverages the inherited getAuth() method to access the shared FirebaseAuth instance.
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

    /**
     * Attempts to create a new user account in Firebase Authentication.
     * 
     * @param mail The email address for the new account.
     * @param password The password for the new account.
     */
    public void createUser(String mail, String password) {
        // Asynchronous request to Firebase cloud services.
        getAuth().createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: register success");
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