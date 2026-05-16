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

public class FileStorage extends FirebaseComm {

    private static final String TAG = "FileStorage";
    private FirebaseStorage firebaseStorage;
    private StorageResult storageResult;

    public interface StorageResult {
        void fileResult(byte[] data);
    }

    public FileStorage() {
        firebaseStorage = FirebaseStorage.getInstance();
        if (FIRESTORE == null) {
            FIRESTORE = getFirestore();
        }
    }

    public void setStorageResult(StorageResult storageResult) {
        this.storageResult = storageResult;
    }

    public void uploadMusicFile(String classId, String lessonId, Uri fileUri, String title, String extension) {
        StorageReference storageRef = firebaseStorage.getReference();
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

                    if (teacherEmail != null && roleName.toLowerCase().contains("partitura")) {
                        updateLessonMapping(classId, lessonId, downloadUrl, teacherEmail);
                    }
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload role file: " + roleName, e));
    }

    private void updateLessonMapping(String classId, String lessonId, String fileUrl, String email) {
        DocumentReference lessonRef = FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId);

        FIRESTORE.runTransaction(transaction -> {
            Lesson lesson = transaction.get(lessonRef).toObject(Lesson.class);
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

    public void deleteMusicFile(String classId, String lessonId, String fileDocId, OnCompleteListener<Void> listener) {
        FIRESTORE.collection("classes").document(classId)
                .collection("lessons").document(lessonId)
                .collection("musicFiles").document(fileDocId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void saveImageToStorage(Bitmap bitmap, String entryName) {
        // set the reference as follows:
        // "folder
        // " named entryname which is the id of the post
        // unique image name in case we have more than one image in the post...future
        StorageReference storageRef = firebaseStorage.getReference();
        // at the moment add random name
        StorageReference imageRef = storageRef.child(entryName);
        // bitmap to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = imageRef.putBytes(data);
        // This is required only if we want to get the image url
        // in https:...  type -> direct url to the image
        // not via Firebase references
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
                    Log.d(TAG, "onSuccess: " + downloadUri);
                } else {
                    // Handle failures
                    Log.d(TAG, "onComplete:  failed");
                }
            }
        });
    }

    public void getFileFromStorage (String name)
    {
        StorageReference storageRef = firebaseStorage.getReference();
        // at the moment add random name
        StorageReference fileRef = storageRef.child(name);
        fileRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Use the bytes to display the image
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
        // at the moment add random name
        StorageReference imageRef = storageRef.child(name);
        imageRef.getBytes(Long.MAX_VALUE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Use the bytes to display the image
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
