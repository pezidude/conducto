package com.example.conducto2.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * FirebaseComm
 * 
 * The foundational base class for all Firebase-related communication in the Conducto application.
 * This class implements the Singleton pattern for Firebase service instances (Auth and Firestore),
 * ensuring that the application maintains a single, consistent connection state across all data-driven components.
 * 
 * It provides centralized utility functions for authentication checks, session management,
 * and standardizes the callback interfaces used to report the success or failure of 
 * asynchronous database operations.
 */
public class FirebaseComm {

    /** Tag used for identifying log entries originating from this base class. */
    private static final String TAG = "Firebase Comm";

    /** Shared static instance of the Firestore database, accessible to all subclasses. */
    protected static FirebaseFirestore FIRESTORE;

    /** Shared static instance of the Firebase Authentication service. */
    private static FirebaseAuth AUTH;

    /**
     * Enumeration of all discrete database operations performed by the application.
     * This is used to uniquely identify the context of a callback response.
     */
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

    /**
     * Interface used to propagate the results of asynchronous Firebase operations back to the UI.
     */
    public interface DBResult {
        /**
         * Triggered upon completion of an asynchronous task.
         * @param success True if the operation succeeded.
         * @param operation The type of operation that was attempted.
         */
        void uploadResult(boolean success, DbOperation operation);

        /**
         * Provides a human-readable feedback message to be displayed to the user.
         * @param message The feedback or error message.
         */
        void displayMessage(String message);
    }

    /** The listener instance assigned to handle result events for this communication object. */
    protected DBResult dbResult;

    /**
     * Assigns a listener to receive the results of operations initiated by this object.
     * @param dbr The listener implementation.
     */
    public void setDbResult(DBResult dbr) {
        this.dbResult = dbr;
    }

    // --- Utility functions ---

    /**
     * Returns the singleton instance of FirebaseAuth.
     * Initializes the instance if it does not yet exist.
     * @return The active FirebaseAuth instance.
     */
    public static FirebaseAuth getAuth() {
        if (AUTH == null)
            AUTH = FirebaseAuth.getInstance();
        return AUTH;
    }

    /**
     * Returns the singleton instance of FirebaseFirestore.
     * Initializes the instance if it does not yet exist.
     * @return The active FirebaseFirestore instance.
     */
    public static FirebaseFirestore getFirestore() {
        if (FIRESTORE == null)
            FIRESTORE = FirebaseFirestore.getInstance();

        return FIRESTORE;
    }

    /**
     * Helper to obtain a reference to a specific top-level collection.
     * @param collection The name of the collection.
     * @return A CollectionReference for the requested path.
     */
    public static CollectionReference getCollectionReference(String collection) {
        return getFirestore().collection(collection);
    }

    /**
     * Checks if there is a currently authenticated user session.
     * @return True if a user is signed in.
     */
    public static boolean isUserSignedIn() {
        return getAuth().getCurrentUser() != null;
    }

    /**
     * Retrieves the email address of the currently authenticated user.
     * @return The user's email string, or null if not signed in.
     */
    public static String authUserEmail() {
        if (getAuth().getCurrentUser() == null) return null;
        return getAuth().getCurrentUser().getEmail();
    }

    /**
     * Terminates the current user session and clears authentication tokens.
     */
    public static void signOut() {
        getAuth().signOut();
    }
}