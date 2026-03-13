package com.example.conducto2.data.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Annotation;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.DynamicAnnotation;
import com.example.conducto2.data.model.HighlightAnnotation;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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

    public interface DBResult {
        void uploadResult(boolean success);

        void displayMessage(String message);
    }

    public interface UserFetchListener {
        void onUserFetched(User user);
    }

    public interface ClassesFetchListener {
        void onClassesFetched(List<Class> classes);
    }
    
    public interface AnnotationFetchListener {
        void onAnnotationsFetched(List<Annotation> annotations);
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
        FIRESTORE = getFisrestore();

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
                            dbResult.uploadResult(true);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null)
                            dbResult.displayMessage("upload failed " + e.getMessage());

                    }
                });
    }

    public void getUser() {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore.getInstance().collection("users").document(email)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        Log.d("DATA", document.getId() + " => " + document.getData());
                        String type = "data";
                        User user = document.toObject(User.class);
                        DataManager.setUser(user);
                        Log.d("DATA", user.toString());

                    }


                });
    }

    public void getUser(UserFetchListener listener) {
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
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
    public void getAllUsers(List<User> allUsers, Context context) {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allUsers.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            allUsers.add(document.toObject(User.class));
                        }
                    } else {
                        Toast.makeText(context, "Error getting users.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void migrateUsersToTeachers() {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Check if userType field exists
                            if (!document.contains("userType")) {
                                document.getReference().update("userType", "teacher")
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + document.getId() + " migrated to teacher."))
                                        .addOnFailureListener(e -> Log.w(TAG, "Error migrating user " + document.getId(), e));
                            }
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
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
                        dbResult.uploadResult(true);
                    }
                }).addOnFailureListener(e -> {
                    if (dbResult != null)
                        dbResult.displayMessage("lesson upload failed " + e.getMessage());
                });
    }

    public void updateLesson(String classId, Lesson lesson) {
        if (lesson.getId() == null || lesson.getId().isEmpty()) {
            if (dbResult != null) {
                dbResult.displayMessage("Lesson ID is missing, cannot update.");
            }
            return;
        }

        DocumentReference ref = FIRESTORE.collection("classes").document(classId).collection("lessons").document(lesson.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", lesson.getTitle());
        updates.put("info", lesson.getInfo());
        updates.put("date", lesson.getDate());
        updates.put("attendees", lesson.getAttendees());
        updates.put("musicXMLFiles", lesson.getMusicXMLFiles());
        updates.put("fileMapping", lesson.getFileMapping());

        ref.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "onSuccess: lesson updated successfully ");
                    if (dbResult != null) {
                        dbResult.displayMessage("lesson updated successfully");
                        dbResult.uploadResult(true);
                    }
                }).addOnFailureListener(e -> {
                    if (dbResult != null)
                        dbResult.displayMessage("lesson update failed " + e.getMessage());
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
                            dbResult.uploadResult(true);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null)
                            dbResult.displayMessage("class upload failed " + e.getMessage());

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
                            dbResult.uploadResult(true);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null)
                            dbResult.displayMessage("class upload failed " + e.getMessage());

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
                                        dbResult.uploadResult(true);
                                    })
                                    .addOnFailureListener(e -> {
                                        dbResult.displayMessage("Failed to join class: " + e.getMessage());
                                    });
                        } else {
                            dbResult.displayMessage("Invalid join code");
                        }
                    } else {
                        dbResult.displayMessage("Failed to find class: " + task.getException().getMessage());
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