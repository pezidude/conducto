package com.example.conducto2.data.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FileStorage
 * 
 * A specialized subclass of {@link FirebaseComm} dedicated to binary data management.
 * It coordinates the storage of MusicXML files and user images in Firebase Storage 
 * and maintains the corresponding pointers in Firestore.
 * 
 * Key functionalities:
 * 1. Uploading raw MusicXML files and generated Role-specific scores.
 * 2. Automating the mapping of teacher-specific parts during role generation.
 * 3. Handling binary image data for user profiles or post attachments.
 * 4. Providing callback bridges for binary data retrieval (e.g., fetching score content).
 */
public class FileStorage extends FirebaseComm {

    /** Identifier for logging storage-related events. */
    private static final String TAG = "FileStorage";
    
    /** The instance of Firebase Cloud Storage service. */
    private FirebaseStorage firebaseStorage;
    
    /** The listener instance assigned to handle binary data results. */
    private StorageResult storageResult;

    /**
     * Interface for reporting binary data results (e.g., from Storage to UI).
     */
    public interface StorageResult {
        /**
         * Triggered when binary data is successfully fetched.
         * @param data The raw byte array.
         */
        void fileResult(byte[] data);
    }

    /**
     * Initializes the storage manager and secures references to both Storage and 
     * Firestore (inherited) services.
     */
    public FileStorage() {
        firebaseStorage = FirebaseStorage.getInstance();
        if (FIRESTORE == null) {
            FIRESTORE = getFirestore();
        }
    }

    /**
     * Assigns a listener to receive binary data results.
     * @param storageResult The listener implementation.
     */
    public void setStorageResult(StorageResult storageResult) {
        this.storageResult = storageResult;
    }

    /**
     * Uploads a teacher-assigned music file to a specific lesson.
     * 
     * @param classId The ID of the class.
     * @param lessonId The ID of the lesson.
     * @param fileUri The local Uri of the file to upload.
     * @param title The display title for the file.
     * @param extension The file extension (e.g., "musicxml").
     */
    public void uploadMusicFile(String classId, String lessonId, Uri fileUri, String title, String extension) {
        StorageReference storageRef = firebaseStorage.getReference();
        String fileName = "musicresource_" + java.util.UUID.randomUUID().toString() + "." + extension;
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + lessonId + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Create a MusicFile model and persist it to the lesson's musicFiles collection in Firestore.
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

    /**
     * Uploads a generated XML score for a specific role and optionally updates the lesson mapping.
     * 
     * @param classId The parent class ID.
     * @param lessonId The parent lesson ID.
     * @param originalTitle The name of the master score.
     * @param roleName The name of the role (e.g., "Violin").
     * @param content The XML text content.
     * @param teacherEmail The email of the teacher who should be auto-assigned the 'partitura'.
     */
    public void uploadRoleMusicFile(String classId, String lessonId, String originalTitle, String roleName, String content, String teacherEmail) {
        StorageReference storageRef = firebaseStorage.getReference();
        String fileName = "role_" + roleName.replaceAll("\\s+", "_") + "_" + java.util.UUID.randomUUID().toString() + ".musicxml";
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + lessonId + "/" + fileName);

        byte[] data = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    MusicFile musicFile = new MusicFile(originalTitle + " - " + roleName, uri);
                    FIRESTORE.collection("classes").document(classId)
                            .collection("lessons").document(lessonId)
                            .collection("musicFiles")
                            .add(musicFile);

                    // Logic: Automatically assign the "Full Score" (partitura) to the teacher's lesson mapping.
                    if (teacherEmail != null && roleName.toLowerCase().contains("partitura")) {
                        updateLessonMapping(classId, lessonId, downloadUrl, teacherEmail);
                    }
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload role file: " + roleName, e));
    }

    /**
     * Renames a music file document in the lesson's musicFiles sub-collection.
     * 
     * @param classId The parent class ID.
     * @param lessonId The parent lesson ID.
     * @param fileDocId The ID of the music file document to update.
     * @param newTitle The new display title for the file.
     */
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

    /**
     * Deletes a music file document from Firestore.
     * 
     * @param classId The parent class ID.
     * @param lessonId The parent lesson ID.
     * @param fileDocId The ID of the music file document to delete.
     * @param listener Callback for operation completion.
     */
    public void deleteMusicFile(String classId, String lessonId, String fileDocId, OnCompleteListener<Void> listener) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("musicFiles").document(fileDocId)
                .delete()
                .addOnCompleteListener(listener);
    }

    /**
     * Atomically updates the lesson's file mapping inside a Firestore transaction.
     * Ensures that the music part assignment is thread-safe and consistent.
     */
    private void updateLessonMapping(String classId, String lessonId, String fileUrl, String email) {
        DocumentReference lessonRef = FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId);

        FIRESTORE.runTransaction(transaction -> {
            Lesson lesson = Lesson.fromSnapshot(transaction.get(lessonRef));
            if (lesson != null) {
                Map<String, List<String>> mapping = lesson.getFileMapping();
                if (mapping == null) mapping = new HashMap<>();

                List<String> students = mapping.get(fileUrl);
                if (students == null) students = new ArrayList<>();
                if (!students.contains(email)) {
                    students.add(email);
                }
                mapping.put(fileUrl, students);
                transaction.update(lessonRef, "fileMapping", mapping);
            }
            return null;
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to update lesson mapping for teacher", e));
    }

    /**
     * Saves an image Bitmap to Firebase Storage.
     * @param bitmap The image to save.
     * @param entryName The storage path/filename.
     */
    public void saveImageToStorage(Bitmap bitmap, String entryName) {
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference imageRef = storageRef.child(entryName);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = imageRef.putBytes(data);
        
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }
            return imageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                Log.d(TAG, "onSuccess: " + downloadUri);
            }
        });
    }

    /**
     * Retrieves binary data from Storage by name.
     * @param name The storage path.
     */
    public void getFileFromStorage(String name) {
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference fileRef = storageRef.child(name);
        fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            if(storageResult != null) storageResult.fileResult(bytes);
        });
    }

    /**
     * Retrieves an image from Storage and populates an ImageView.
     * @param ivPostPhoto The target ImageView.
     * @param name The storage path.
     */
    public void getImageFromStorage(ImageView ivPostPhoto, String name) {
        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference imageRef = storageRef.child(name);
        imageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(bytes -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ivPostPhoto.setImageBitmap(bitmap);
        });
    }
}