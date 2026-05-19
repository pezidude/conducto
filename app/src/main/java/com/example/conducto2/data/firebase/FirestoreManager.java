package com.example.conducto2.data.firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.Role;
import com.example.conducto2.data.model.User;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FirestoreManager
 * 
 * A specialized subclass of {@link FirebaseComm} that serves as the primary Data Access Object (DAO) 
 * for the Firestore NoSQL database. It handles all CRUD (Create, Read, Update, Delete) operations 
 * for Users, Classes, and Lessons.
 * 
 * This class implements complex business logic including:
 * 1. Multi-device synchronization via server-time offset calculation.
 * 2. Real-time lesson status tracking (PLAYING, PAUSED, STOPPED).
 * 3. Class membership management using join codes.
 * 4. Recent activity logging for personalized user dashboards.
 */
public class FirestoreManager extends FirebaseComm {

    /** Identifier for logging database events. */
    private static final String TAG = "Firestore DB";
    
    /** Reference to the currently authenticated Firebase user. */
    private FirebaseUser firebaseUser;


    /** Callback interface for single user retrieval. */
    public interface UserFetchListener {
        void onUserFetched(User user);
    }

    /** Callback interface for bulk user retrieval. */
    public interface AllUsersFetchListener {
        void onUsersFetched(List<User> users);
    }

    /** Callback interface for class list retrieval. */
    public interface ClassesFetchListener {
        void onClassesFetched(List<Class> classes);
    }

    /** Callback interface for reactive live lesson monitoring. */
    public interface LiveLessonListener {
        void onLiveLessonChanged(Lesson lesson);
    }

    /** Callback interface for dashboard recent history retrieval. */
    public interface RecentLessonsFetchListener {
        void onRecentLessonsFetched(List<Lesson> recentLessons);
    }

    /**
     * Assigns a listener to receive database operation results.
     * @param dbr The listener implementation.
     */
    public void setDbResult(DBResult dbr) {
        this.dbResult = dbr;
    }


    /**
     * Initializes the manager and secures a reference to the global Firestore singleton
     * inherited from the base class.
     */
    public FirestoreManager() {
        FIRESTORE = getFirestore();
    }


    /**
     * Creates a new user record in the 'users' collection using their email as the document ID.
     * @param user The user object containing profile information.
     */
    public void insertUser(User user) {
        firebaseUser = getAuth().getCurrentUser();
        DocumentReference ref = FIRESTORE.collection("users").document(user.getEmail());
        ref.set(user)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.INSERT_USER);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.INSERT_USER);
                    }
                });
    }

    /**
     * Updates an existing user's profile information in Firestore.
     * @param user The user object with updated fields.
     */
    public void updateUser(User user) {
        if (user == null || user.getEmail() == null) return;

        DocumentReference ref = FIRESTORE.collection("users").document(user.getEmail());
        ref.set(user)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_USER);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user", e);
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.UPDATE_USER);
                    }
                });
    }

    /**
     * Adds a new lesson to a specific class. Generates a unique document ID automatically.
     * @param classId The parent class ID.
     * @param lesson The lesson object to persist.
     */
    public void insertLesson(String classId, Lesson lesson) {
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document();
        lesson.setId(ref.getId());
        ref.set(lesson)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Lesson added successfully");
                        dbResult.uploadResult(true, DbOperation.INSERT_LESSON);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Failed to add lesson: " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.INSERT_LESSON);
                    }
                });
    }

    /**
     * Updates metadata for an existing lesson.
     * @param classId The parent class ID.
     * @param lesson The lesson object with updated fields.
     */
    public void updateLesson(String classId, Lesson lesson) {
        if (lesson.getId() == null) {
            if (dbResult != null) dbResult.displayMessage("Error: Lesson ID is null");
            return;
        }
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lesson.getId());
        ref.set(lesson)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Lesson updated successfully");
                        dbResult.uploadResult(true, DbOperation.UPDATE_LESSON);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Failed to update lesson: " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.UPDATE_LESSON);
                    }
                });
    }

    /**
     * Updates a user's profile picture using a Base64 encoded string.
     */
    public void updateUserProfilePicture(String email, String base64Image) {
        DocumentReference ref = FIRESTORE.collection("users").document(email);
        ref.update("profilePictureBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_USER);
                    }
                });
    }

    /**
     * Removes a lesson document from Firestore.
     */
    public void deleteLesson(String classId, String lessonId) {
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lessonId);
        ref.delete()
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.DELETE_LESSON);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.DELETE_LESSON);
                    }
                });
    }

    /**
     * Toggles the archived state of a lesson.
     */
    public void updateLessonArchivedStatus(String classId, String lessonId, boolean isArchived) {
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lessonId);
        ref.update("isArchived", isArchived)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_LESSON_ARCHIVED_STATUS);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.UPDATE_LESSON_ARCHIVED_STATUS);
                    }
                });
    }


    /**
     * Fetches a single user's profile by their email and caches it in DataManager.
     */
    public void getUser(UserFetchListener listener) {
        getUserByEmail(authUserEmail(), listener);
    }

    /**
     * Fetches a single user's profile by their email.
     */
    public void getUserByEmail(String email, UserFetchListener listener) {
        if (email == null) {
            if (listener != null) listener.onUserFetched(null);
            return;
        }
        FIRESTORE.collection("users").document(email)
                .get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        if (email.equals(authUserEmail())) {
                            DataManager.setUser(user);
                        }
                        if (listener != null) {
                            listener.onUserFetched(user);
                        }
                    } else {
                        if (listener != null) listener.onUserFetched(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user by email", e);
                    if (listener != null) listener.onUserFetched(null);
                });
    }

    /**
     * Fetches all registered users from the system.
     */
    public void getAllUsers(List<User> allUsers, AllUsersFetchListener listener) {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allUsers.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            allUsers.add(document.toObject(User.class));
                        }
                        if (listener != null) {
                            listener.onUsersFetched(allUsers);
                        }
                    } else {
                        if (dbResult != null) {
                            dbResult.displayMessage("Error getting users: " + task.getException().getMessage());
                        }
                        if (listener != null) {
                            listener.onUsersFetched(null);
                        }
                    }
                });
    }

    /**
     * Retrieves all classes where the specified user is a member.
     */
    public void getClassesForUser(String email, ClassesFetchListener listener) {
        FIRESTORE.collection("classes")
                .whereArrayContains("members", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Class> classes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        classes.add(document.toObject(Class.class));
                    }
                    if (listener != null) {
                        listener.onClassesFetched(classes);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.FETCH_CLASSES);
                    }
                });
    }

    /**
     * Creates a new class document after ensuring the join code is unique.
     * This method recursively regenerates the join code if a collision is detected.
     */
    public void insertClassWithUniqueCode(Class newClass) {
        FIRESTORE.collection("classes")
                .whereEqualTo("joinCode", newClass.getJoinCode())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Collision found! Generate a new code and try again.
                        Log.d(TAG, "Join code collision detected, regenerating code...");
                        newClass.regenerateJoinCode();
                        insertClassWithUniqueCode(newClass);
                    } else if (task.isSuccessful()) {
                        // No collision, safe to insert.
                        insertClass(newClass);
                    } else {
                        // Query failed.
                        if (dbResult != null) {
                            dbResult.displayMessage("Error checking join code uniqueness: " + task.getException().getMessage());
                            dbResult.uploadResult(false, DbOperation.INSERT_CLASS);
                        }
                    }
                });
    }

    /**
     * Creates a new class document and sets the unique ID.
     * this method is private and used by insertClassWithUniqueCode.
     */
    private void insertClass(Class newClass) {
        firebaseUser = getAuth().getCurrentUser();
        DocumentReference ref = FIRESTORE.collection("classes").document();
        newClass.setId(ref.getId());
        ref.set(newClass)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: class loaded successfully ");
                        if (dbResult != null) {
                            dbResult.displayMessage("class uploaded successfuly");
                            dbResult.uploadResult(true, DbOperation.INSERT_CLASS);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null) {
                            dbResult.displayMessage("class upload failed " + e.getMessage());
                            dbResult.uploadResult(false, DbOperation.INSERT_CLASS);
                        }
                    }
                });
    }

    /**
     * Updates an existing class document in Firestore.
     */
    public void updateClass(Class updatedClass) {
        firebaseUser = getAuth().getCurrentUser();
        DocumentReference ref = FIRESTORE.collection("classes").document(updatedClass.getId());
        ref.set(updatedClass)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: class loaded successfully ");
                        if (dbResult != null) {
                            dbResult.displayMessage("class uploaded successfuly");
                            dbResult.uploadResult(true, DbOperation.UPDATE_CLASS);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null) {
                            dbResult.displayMessage("class upload failed " + e.getMessage());
                            dbResult.uploadResult(false, DbOperation.UPDATE_CLASS);
                        }
                    }
                });
    }

    /**
     * Allows a student to join a class using a specific join code.
     */
    public void joinClassWithCode(String joinCode) {
        firebaseUser = getAuth().getCurrentUser();
        if (firebaseUser == null) {
            dbResult.displayMessage("You must be logged in to join a class");
            return;
        }

        String userEmail = firebaseUser.getEmail();

        FIRESTORE.collection("classes")
                .whereEqualTo("joinCode", joinCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                            Class foundClass = document.toObject(Class.class);

                            if (foundClass.getMembers().contains(userEmail)) {
                                dbResult.displayMessage("You are already a member of this class");
                                return;
                            }

                            document.getReference().update("members", FieldValue.arrayUnion(userEmail))
                                    .addOnSuccessListener(aVoid -> {
                                        dbResult.displayMessage("Successfully joined class");
                                        dbResult.uploadResult(true, DbOperation.JOIN_CLASS);
                                    })
                                    .addOnFailureListener(e -> {
                                        dbResult.displayMessage("Failed to join class: " + e.getMessage());
                                        dbResult.uploadResult(false, DbOperation.JOIN_CLASS);
                                    });
                        } else {
                            dbResult.displayMessage("Invalid join code");
                            dbResult.uploadResult(false, DbOperation.JOIN_CLASS);
                        }
                    } else {
                        dbResult.displayMessage("Failed to find class: " + task.getException().getMessage());
                        dbResult.uploadResult(false, DbOperation.JOIN_CLASS);
                    }
                });
    }

    /**
     * Toggles the active status of a class.
     */
    public void updateClassActivity(String classId, boolean isActive) {
        FIRESTORE.collection("classes").document(classId)
                .update("isActive", isActive)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_CLASS);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.UPDATE_CLASS);
                    }
                });
    }

    /**
     * Synchronizes playback state across all participants in a live session.
     */
    public void updateLessonStatus(String classId, String lessonId, String status, long targetTimestamp, int currentMeasure, int bpm) {
        Log.d(TAG, "updateLessonStatus: classId=" + classId + ", lessonId=" + lessonId + ", status=" + status + ", targetTimestamp=" + targetTimestamp + ", currentMeasure=" + currentMeasure + ", bpm=" + bpm);
        if (classId == null || lessonId == null) {
            Log.e(TAG, "updateLessonStatus: FAILED due to null ID(s)");
            return;
        }
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lessonId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("targetTimestamp", targetTimestamp);
        updates.put("currentMeasure", currentMeasure);
        updates.put("bpm", bpm);

        ref.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "updateLessonStatus: SUCCESS");
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_LESSON_STATUS);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "updateLessonStatus: FAILURE", e);
                    if (dbResult != null) {
                        dbResult.uploadResult(false, DbOperation.UPDATE_LESSON_STATUS);
                    }
                });
    }

    /** Overload of updateLessonStatus without measure and BPM. */
    public void updateLessonStatus(String classId, String lessonId, String status, long targetTimestamp) {
        updateLessonStatus(classId, lessonId, status, targetTimestamp, -1, -1);
    }

    /** Overload of updateLessonStatus with only status. */
    public void updateLessonStatus(String classId, String lessonId, String status) {
        updateLessonStatus(classId, lessonId, status, 0, -1, -1);
    }

    /**
     * Updates the connected status of a student in a specific lesson.
     */
    public void updateStudentPresence(String classId, String lessonId, String email, boolean isConnected) {
        if (classId == null || lessonId == null || email == null) return;

        DocumentReference lessonRef = FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId);

        if (isConnected) {
            lessonRef.update("connectedStudents", FieldValue.arrayUnion(email))
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Student connected: " + email))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating student connection status", e));
        } else {
            lessonRef.update("connectedStudents", FieldValue.arrayRemove(email))
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Student disconnected: " + email))
                    .addOnFailureListener(e -> Log.e(TAG, "Error updating student connection status", e));
        }
    }

    /**
     * Calculates the offset between the local device time and the Firebase server time.
     * This is used for synchronizing playback across multiple devices.
     */
    public void calculateServerTimeOffset(OnSuccessListener<Long> listener) {
        DocumentReference ref = FIRESTORE.collection("server_time").document(DataManager.getUserInstance().getEmail());
        Map<String, Object> data = new HashMap<>();
        data.put("timestamp", FieldValue.serverTimestamp());

        long localBefore = System.currentTimeMillis();
        ref.set(data).addOnSuccessListener(aVoid -> ref.get().addOnSuccessListener(snapshot -> {
            com.google.firebase.Timestamp serverTimestamp = snapshot.getTimestamp("timestamp");
            if (serverTimestamp != null) {
                long serverTime = serverTimestamp.toDate().getTime();
                long localAfter = System.currentTimeMillis();
                long localMid = (localBefore + localAfter) / 2;
                long offset = serverTime - localMid;
                listener.onSuccess(offset);
            }
        }));
    }

    /**
     * Starts or stops the live status of a lesson.
     */
    public void updateLessonLiveStatus(String classId, String lessonId, boolean isLive) {
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lessonId);
        
        if (isLive) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("isLive", true);
            updates.put("isArchived", false);
            ref.update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (dbResult != null) {
                            dbResult.uploadResult(true, DbOperation.UPDATE_LESSON_LIVE_STATUS);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (dbResult != null) {
                            dbResult.uploadResult(false, DbOperation.UPDATE_LESSON_LIVE_STATUS);
                        }
                    });
        } else {
            ref.update("isLive", false)
                    .addOnSuccessListener(aVoid -> {
                        if (dbResult != null) {
                            dbResult.uploadResult(true, DbOperation.UPDATE_LESSON_LIVE_STATUS);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (dbResult != null) {
                            dbResult.uploadResult(false, DbOperation.UPDATE_LESSON_LIVE_STATUS);
                        }
                    });
        }
    }

    /**
     * Real-time listener for lessons currently marked as live.
     */
    public void listenForLiveLesson(String classID, LiveLessonListener listener) {
        FIRESTORE.collection("classes").document(classID)
                .collection("lessons")
                .whereEqualTo("isLive", true)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && !snapshot.getDocuments().isEmpty()) {
                        // should never be more than one playing/paused
                        Lesson lesson = Lesson.fromSnapshot(snapshot.getDocuments().get(0));
                        listener.onLiveLessonChanged(lesson);
                    } else {
                        listener.onLiveLessonChanged(null);
                    }
                });
    }


    /** Adds a new draft role configuration to a lesson. */
    public void addDraftRole(String classId, String lessonId, Role role) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles")
                .add(role);
    }

    /** Updates an existing draft role's fields. */
    public void updateDraftRole(String classId, String lessonId, String roleDocId, Map<String, Object> updates) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles").document(roleDocId)
                .update(updates);
    }

    /** Removes a draft role from Firestore. */
    public void deleteDraftRole(String classId, String lessonId, String roleDocId) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles").document(roleDocId)
                .delete();
    }

    /** Returns a query for all draft roles sorted by name. */
    public Query getDraftRolesQuery(String classId, String lessonId) {
        return FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles")
                .orderBy("name", Query.Direction.ASCENDING);
    }

    /** Logs that a user has accessed a lesson for dashboard history. */
    public void logLessonAccess(String userId, String classId, String lessonId, String title, String genre) {
        if (userId == null || lessonId == null) return;
        DocumentReference ref = FIRESTORE.collection("users").document(userId)
                .collection("recent_lessons").document(lessonId);

        Map<String, Object> log = new HashMap<>();
        log.put("classId", classId);
        log.put("lessonId", lessonId);
        log.put("genre", genre);
        log.put("accessedAt", FieldValue.serverTimestamp());

        ref.set(log, com.google.firebase.firestore.SetOptions.merge());
    }

    /** Removes a lesson from the user's recent history. */
    public void deleteRecentLessonLog(String userId, String lessonId) {
        if (userId == null || lessonId == null) return;
        FIRESTORE.collection("users").document(userId)
                .collection("recent_lessons").document(lessonId)
                .delete();
    }

    /** Fetches the 5 most recently accessed lessons for a user. */
    public void getRecentLessons(String userId, RecentLessonsFetchListener listener) {
        if (userId == null) {
            if (listener != null) listener.onRecentLessonsFetched(new ArrayList<>());
            return;
        }
        FIRESTORE.collection("users").document(userId)
                .collection("recent_lessons")
                .orderBy("accessedAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Lesson> recentLessons = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String genre = document.getString("genre");
                        Lesson lesson = Lesson.createByGenre(genre != null ? genre : "Classical");
                        if (lesson != null) {
                            lesson.setId(document.getString("lessonId"));
                            lesson.setClassId(document.getString("classId"));
                            recentLessons.add(lesson);
                        }
                    }
                    if (listener != null) {
                        listener.onRecentLessonsFetched(recentLessons);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching recent lessons", e);
                    if (listener != null) {
                        listener.onRecentLessonsFetched(new ArrayList<>());
                    }
                });
    }

    /** Fetches a single lesson document. */
    public void getLesson(String classId, String lessonId, OnSuccessListener<Lesson> listener) {
        if (classId == null || lessonId == null) {
            if (listener != null) listener.onSuccess(null);
            return;
        }
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onSuccess(Lesson.fromSnapshot(documentSnapshot));
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting lesson", e);
                    if (listener != null) listener.onSuccess(null);
                });
    }

    /** Fetches a single class document. */
    public void getClassById(String classId, OnSuccessListener<Class> listener) {
        if (classId == null) {
            if (listener != null) listener.onSuccess(null);
            return;
        }
        FIRESTORE.collection("classes").document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onSuccess(documentSnapshot.toObject(Class.class));
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting class", e);
                    if (listener != null) listener.onSuccess(null);
                });
    }
}