package com.example.conducto2.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseComm {
    private static final String TAG = "Firebase Comm";
    protected static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;


    // Utility functions

    public static FirebaseAuth getAuth() {
        if (AUTH == null)
            AUTH = FirebaseAuth.getInstance();
        return AUTH;
    }

    public static FirebaseFirestore getFirestore() {
        if (FIRESTORE == null)
            FIRESTORE = FirebaseFirestore.getInstance();

        return FIRESTORE;
    }

    public static CollectionReference getCollectionReference(String collection) {
        return getFirestore().collection(collection);
    }

    public static boolean isUserSignedIn() {

        return getAuth().getCurrentUser() != null;

    }

    public static String authUserEmail() {
        return getAuth().getCurrentUser().getEmail();


    }

    public static void signOut() {
        getAuth().signOut();


    }
}