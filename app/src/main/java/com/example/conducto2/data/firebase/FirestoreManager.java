package com.example.conducto2.data.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Annotation;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.DynamicAnnotation;
import com.example.conducto2.data.model.HighlightAnnotation;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FirestoreManager extends FirebaseComm {

    private static final String TAG = "Firestore DB";
    //    private FirebaseFirestore firebaseFirestore;
    private FirebaseUser firebaseUser;
    //   private QueryResult postQueryResult;

    private DBResult dbResult;

    public enum DbOperation {
        INSERT_USER,
        INSERT_LESSON,
        UPDATE_LESSON,
        INSERT_CLASS,
        UPDATE_CLASS,
        JOIN_CLASS,
        UPDATE_LESSON_STATUS,
        UPDATE_LESSON_LIVE_STATUS,
        UPLOAD_MUSIC_FILE,
        RENAME_MUSIC_FILE,
        OTHER
    }

    public interface DBResult {
        void uploadResult(boolean success, DbOperation operation);

        void displayMessage(String message);
    }

    public interface UserFetchListener {
        void onUserFetched(User user);
    }

    public interface AllUsersFetchListener {
        void onUsersFetched(List<User> users);
    }

    public interface ClassesFetchListener {
        void onClassesFetched(List<Class> classes);
    }
    
    public interface AnnotationFetchListener {
        void onAnnotationsFetched(List<Annotation> annotations);
    }

    public interface LiveLessonListener {
        void onLiveLessonChanged(Lesson lesson);
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
        // add the photo to the firebase storage
        // hold the reference for the storage
        DocumentReference ref = FIRESTORE.collection("users").document(user.getEmail());

        // update the storage reference in the post entry
        //Post post = new Post(title, body, path, firebaseUser.getEmail());
        // upload to storage and then to firestore
        ref.set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: user loaded successfully ");
                        if (dbResult != null) {
                            dbResult.displayMessage("post uploaded successfuly");
                            dbResult.uploadResult(true, DbOperation.INSERT_USER);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null) {
                            dbResult.displayMessage("upload failed " + e.getMessage());
                            dbResult.uploadResult(false, DbOperation.INSERT_USER);
                        }
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
                    Log.e(TAG, "Error fetching classes", e);
                    if (listener != null) {
                        listener.onClassesFetched(new ArrayList<>()); // return empty on failure
                    }
                });
    }

    public void insertLesson(String classId, Lesson lesson) {
        firebaseUser = getAuth().getCurrentUser();
        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document();
        lesson.setId(ref.getId());
        lesson.setClassId(classId);

        ref.set(lesson)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "onSuccess: lesson loaded successfully ");
                    if (dbResult != null) {
                        dbResult.displayMessage("lesson uploaded successfully");
                        dbResult.uploadResult(true, DbOperation.INSERT_LESSON);
                    }
                }).addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("lesson upload failed " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.INSERT_LESSON);
                    }
                });
    }

    public void updateLesson(String classId, Lesson lesson) {
        if (lesson.getId() == null || lesson.getId().isEmpty()) {
            if (dbResult != null) {
                dbResult.displayMessage("Lesson ID is missing, cannot update.");
                dbResult.uploadResult(false, DbOperation.UPDATE_LESSON);
            }
            return;
        }

        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lesson.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", lesson.getTitle());
        updates.put("info", lesson.getInfo());
        updates.put("date", lesson.getDate());
        updates.put("musicXMLFiles", lesson.getMusicXMLFiles());
        updates.put("fileMapping", lesson.getFileMapping());

        ref.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "onSuccess: lesson updated successfully ");
                    if (dbResult != null) {
                        dbResult.displayMessage("lesson updated successfully");
                        dbResult.uploadResult(true, DbOperation.UPDATE_LESSON);
                    }
                }).addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("lesson update failed " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.UPDATE_LESSON);
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
        ref.update("isLive", isLive)
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


    public void getAnnotationsForLesson(String classId, String lessonId, AnnotationFetchListener listener) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("annotations")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Annotation> annotations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String type = document.getString("type");
                        Annotation annotation = null;
                        if ("highlight".equals(type)) {
                            annotation = document.toObject(HighlightAnnotation.class);
                        } else if ("ghost_dynamic".equals(type)) {
                            annotation = document.toObject(DynamicAnnotation.class);
                        }

                        if (annotation != null) {
                            annotation.setAnnotationId(document.getId());
                            annotations.add(annotation);
                        }
                    }
                    if (listener != null) {
                        listener.onAnnotationsFetched(annotations);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching annotations", e);
                    if (listener != null) {
                        listener.onAnnotationsFetched(new ArrayList<>());
                    }
                });
    }

    public void uploadMusicFile(String classId, String lessonId, Uri fileUri, String title, String extension) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = "musicresource_" + java.util.UUID.randomUUID().toString() + "." + extension;
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + lessonId + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    MusicFile musicFile = new MusicFile(title, uri);
                    FIRESTORE.collection("classes").document(classId)
                            .collection("lessons").document(lessonId)
                            .collection("musicFiles")
                            .add(musicFile)
                            .addOnSuccessListener(aVoid -> {
                                if (dbResult != null) {
                                    dbResult.displayMessage("File uploaded and saved.");
                                    dbResult.uploadResult(true, DbOperation.UPLOAD_MUSIC_FILE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (dbResult != null) {
                                    dbResult.displayMessage("Failed to save file URL: " + e.getMessage());
                                    dbResult.uploadResult(false, DbOperation.UPLOAD_MUSIC_FILE);
                                }
                            });
                }))
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Upload failed: " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.UPLOAD_MUSIC_FILE);
                    }
                });
    }

    public void renameMusicFile(String classId, String lessonId, String fileDocId, String newTitle) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("musicFiles").document(fileDocId)
                .update("title", newTitle)
                .addOnSuccessListener(aVoid -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("File renamed.");
                        dbResult.uploadResult(true, DbOperation.RENAME_MUSIC_FILE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (dbResult != null) {
                        dbResult.displayMessage("Failed to rename file: " + e.getMessage());
                        dbResult.uploadResult(false, DbOperation.RENAME_MUSIC_FILE);
                    }
                });
    }

    public void addDraftRole(String classId, String lessonId, com.example.conducto2.ui.lessons.Role role) {
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

    public void uploadRoleMusicFile(String classId, String lessonId, String originalTitle, String roleName, String content) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = "role_" + roleName.replaceAll("\\s+", "_") + "_" + java.util.UUID.randomUUID().toString() + ".musicxml";
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + lessonId + "/" + fileName);

        byte[] data = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    MusicFile musicFile = new MusicFile(originalTitle + " - " + roleName, uri);
                    FIRESTORE.collection("classes").document(classId)
                            .collection("lessons").document(lessonId)
                            .collection("musicFiles")
                            .add(musicFile);
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload role file: " + roleName, e));
    }

    // DELETE ME
    public static class FileStorage extends FirebaseComm {

        private static final String LOG_TAG = "FileStorage";
        private FirebaseStorage firebaseStorage;
        private StorageResult storageResult;

        public interface StorageResult
        {
            void fileResult(byte[] data);

        }

        public FileStorage()
        {
            firebaseStorage = FirebaseStorage.getInstance();
        }

        public void setStorageResult(StorageResult storageResult) {
            this.storageResult = storageResult;
        }

        public void saveImageToStorage(Bitmap bitmap, String entryName)
        {
            StorageReference storageRef = firebaseStorage.getReference();
            StorageReference imageRef = storageRef.child(entryName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            UploadTask uploadTask = imageRef.putBytes(data);
            Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    getFileFromStorage(entryName);
                    // Continue with the task to get the download URL
                    return imageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        Log.d(LOG_TAG, "onSuccess: " + downloadUri);
                    } else {
                        // Handle failures
                        Log.d(LOG_TAG, "onComplete:  failed");
                    }
                }
            });

        }

        public void getFileFromStorage(String name)
        {
            StorageReference storageRef = firebaseStorage.getReference();
            StorageReference fileRef = storageRef.child(name);
            fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    if(storageResult!=null)
                        storageResult.fileResult(bytes);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }

        public void getImageFromStorage(ImageView ivPostPhoto, String name)
        {
            StorageReference storageRef = firebaseStorage.getReference();
            StorageReference imageRef = storageRef.child(name);
            imageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ivPostPhoto.setImageBitmap(bitmap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        }
    }
}
