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


public class FirestoreManager extends FirebaseComm {

    private static final String TAG = "Firestore DB";
    //    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;
    //   private QueryResult postQueryResult;


    public interface UserFetchListener {
        void onUserFetched(User user);
    }

    public interface AllUsersFetchListener {
        void onUsersFetched(List<User> users);
    }

    public interface ClassesFetchListener {
        void onClassesFetched(List<Class> classes);
    }

    public interface LiveLessonListener {
        void onLiveLessonChanged(Lesson lesson);
    }

    public interface RecentLessonsFetchListener {
        void onRecentLessonsFetched(List<Lesson> recentLessons);
    }

    public void setDbResult(DBResult dbr) {
        this.dbResult = dbr;
    }

/*
    public interface QueryResult<T> {
        void postsReturned(ArrayList<T> arr);
        void postsChanged(Map<String,Object> map, int oldIndex, int newIndex);
        void postRemoved(int index);
        void postAdded(Map<String,Object> map, int index);
    }
*/


    public FirestoreManager() {
        FIRESTORE = getFirestore();
    }


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

    public void updateUserProfilePicture(String email, String base64Image) {
        DocumentReference ref = FIRESTORE.collection("users").document(email);
        ref.update("profilePictureBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.uploadResult(true, DbOperation.UPDATE_USER);
                    }
                });
    }
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


    public void getUser(UserFetchListener listener) {
        String email = authUserEmail();
        FirebaseFirestore.getInstance().collection("users").document(email)
                .get().addOnSuccessListener(document -> {
                    if (document.exists()) {
                        User user = document.toObject(User.class);
                        DataManager.setUser(user);
                        if (listener != null) {
                            listener.onUserFetched(user);
                        }
                    }
                });
    }
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

    public void insertClass(Class newClass) {
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

    public void updateLessonStatus(String classId, String lessonId, String status, long targetTimestamp) {
        updateLessonStatus(classId, lessonId, status, targetTimestamp, -1, -1);
    }

    public void updateLessonStatus(String classId, String lessonId, String status) {
        updateLessonStatus(classId, lessonId, status, 0, -1, -1);
    }

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
        DocumentReference ref = FIRESTORE.collection("server_time").document("sync_temp");
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
                        Lesson lesson = snapshot.getDocuments().get(0).toObject(Lesson.class);
                        listener.onLiveLessonChanged(lesson);
                    } else {
                        listener.onLiveLessonChanged(null);
                    }
                });
    }


    public void addDraftRole(String classId, String lessonId, Role role) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles")
                .add(role);
    }

    public void updateDraftRole(String classId, String lessonId, String roleDocId, Map<String, Object> updates) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles").document(roleDocId)
                .update(updates);
    }

    public void deleteDraftRole(String classId, String lessonId, String roleDocId) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles").document(roleDocId)
                .delete();
    }

    public Query getDraftRolesQuery(String classId, String lessonId) {
        return FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("draftRoles")
                .orderBy("name", Query.Direction.ASCENDING);
    }

    public void logLessonAccess(String userId, String classId, String lessonId, String title) {
        if (userId == null || lessonId == null) return;
        DocumentReference ref = FIRESTORE.collection("users").document(userId)
                .collection("recent_lessons").document(lessonId);

        Map<String, Object> log = new HashMap<>();
        log.put("classId", classId);
        log.put("lessonId", lessonId);
        log.put("title", title);
        log.put("accessedAt", FieldValue.serverTimestamp());

        ref.set(log, com.google.firebase.firestore.SetOptions.merge());
    }

    public void deleteRecentLessonLog(String userId, String lessonId) {
        if (userId == null || lessonId == null) return;
        FIRESTORE.collection("users").document(userId)
                .collection("recent_lessons").document(lessonId)
                .delete();
    }

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
                        Lesson lesson = new Lesson();
                        lesson.setId(document.getString("lessonId"));
                        lesson.setClassId(document.getString("classId"));
                        lesson.setTitle(document.getString("title"));
                        recentLessons.add(lesson);
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
                        listener.onSuccess(documentSnapshot.toObject(Lesson.class));
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting lesson", e);
                    if (listener != null) listener.onSuccess(null);
                });
    }

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
