package com.example.conducto2.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseComm {
    private static final String TAG = "Firebase Comm";
    protected static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;

    public enum DbOperation {
        INSERT_USER,
        UPDATE_USER,
        INSERT_LESSON,
        UPDATE_LESSON,
        DELETE_LESSON,
        INSERT_CLASS,
        UPDATE_CLASS,
        JOIN_CLASS,
        FETCH_CLASSES,
        UPDATE_LESSON_STATUS,
        UPDATE_LESSON_LIVE_STATUS,
        UPDATE_LESSON_ARCHIVED_STATUS,
        UPLOAD_MUSIC_FILE,
        RENAME_MUSIC_FILE,
        OTHER
    }

    public interface DBResult {
        void uploadResult(boolean success, DbOperation operation);

        void displayMessage(String message);
    }

    protected DBResult dbResult;

    public void setDbResult(DBResult dbr) {
        this.dbResult = dbr;
    }


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