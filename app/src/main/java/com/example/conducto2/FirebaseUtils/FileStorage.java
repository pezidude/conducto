package com.example.conducto2.FirebaseUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

public class FileStorage extends  FirebaseComm{

    private static final String TAG = "FileStorage";
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
