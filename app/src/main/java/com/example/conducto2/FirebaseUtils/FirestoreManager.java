package com.example.conducto2.FirebaseUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.conducto2.DataManager;
import com.example.conducto2.Lesson;
import com.example.conducto2.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;


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
/*
    // this method simulates an update of a specific field for a user
    public void updatePhoneNumber(String phone) {
        DocumentReference documentReference = firebaseFirestore.collection("users").document(firebaseUser.getUid());
        documentReference.update("phone", phone);

    }


 */

    public void insertUser(User user) {
        firebaseUser = getAuth().getCurrentUser();
        // add the photo to the firebase storage
        // hold the reference for the storage
        DocumentReference ref = FIRESTORE.collection("users").document(firebaseUser.getEmail());

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

    public void insertLesson(Lesson lesson) {
        firebaseUser = getAuth().getCurrentUser();
        // add the photo to the firebase storage
        // hold the reference for the storage
        DocumentReference ref = FIRESTORE.collection("lessons").document();

        // update the storage reference in the post entry
        //Post post = new Post(title, body, path, firebaseUser.getEmail());
        // upload to storage and then to firestore
        ref.set(lesson)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: lesson loaded successfully ");
                        if (dbResult != null) {
                            dbResult.displayMessage("lesson uploaded successfuly");
                            dbResult.uploadResult(true);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null)
                            dbResult.displayMessage("lesson upload failed " + e.getMessage());

                    }
                });
    }

    public void updateLesson(Lesson lesson) {
        firebaseUser = getAuth().getCurrentUser();
        // add the photo to the firebase storage
        // hold the reference for the storage
        DocumentReference ref = FIRESTORE.collection("lessons").document(lesson.getId());

        // update the storage reference in the post entry
        //Post post = new Post(title, body, path, firebaseUser.getEmail());
        // upload to storage and then to firestore
        ref.set(lesson)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: lesson loaded successfully ");
                        if (dbResult != null) {
                            dbResult.displayMessage("lesson uploaded successfuly");
                            dbResult.uploadResult(true);
                        }

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        if (dbResult != null)
                            dbResult.displayMessage("lesson upload failed " + e.getMessage());

                    }
                });
    }

/*
    public void setPostQueryResult(QueryResult pqr) {
        postQueryResult = pqr;

    }


    public void getDataFromListener(Task<QuerySnapshot> task) {
        ArrayList<Map<String,Object>> arr = new ArrayList<>();
        if (task.isSuccessful()) {
            for (QueryDocumentSnapshot doc : task.getResult()) {
                arr.add(doc.getData());
            }
            if (postQueryResult != null)
                postQueryResult.postsReturned(arr);
        } else {
            Log.d(TAG, "onComplete: failed");
        }

    }


    public void getPostsOrderByWithLimit(String field, int limit) {

        FIRESTORE.collection("posts").orderBy(field).limit(limit).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        getDataFromListener(task);
                    }
                });
    }

    public void getAllDocumentsInColletction( String collectionName) {

        FIRESTORE.collection(collectionName).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        getDataFromListener(task);
                    }

                });

    }

 */
    /*
    public void listenForChanges(String path)
    {

        FIRESTORE.collection(path).addSnapshotListener(new LessonListener<QuerySnapshot>() {
            @Override
            public void onLesson(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error!=null)
                {
                    Log.d(TAG, "onLesson: FAILED UPDATE");
                }
                for (DocumentChange change:value.getDocumentChanges()) {
                    if(change.getType() == DocumentChange.Type.MODIFIED)
                    {

                        if (postQueryResult != null)
                            postQueryResult.postsChanged(change.getDocument().getData(),change.getOldIndex(),change.getNewIndex());

                    }
                    else if(change.getType() == DocumentChange.Type.REMOVED)
                    {
                        //  Post p = change.getDocument().toObject(Post.class);

                        Log.d(TAG, "onLesson:  removed" + change.getOldIndex());
                        if (postQueryResult != null)
                            postQueryResult.postRemoved(change.getOldIndex());
                    }

                    else if(change.getType() == DocumentChange.Type.ADDED)
                    {
                        if(change.getOldIndex() != change.getNewIndex()) {
                            //    T t=null;
                            //  Post p = change.getDocument().toObject(t.getClass().getSimpleName());
                            //   Log.d(TAG, "onLesson:  added" + p.getTitle());
                            if (postQueryResult != null)
                                // Map<String,Object>
                                postQueryResult.postAdded(change.getDocument().getData(), change.getNewIndex());
                        }
                    }

                }

            }
        });
    }

    public void removeListenForPostChanges()
    {
        //firebaseFirestore.collection("posts").

    }
    */

    public static class FileStorage extends FirebaseComm {

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

        public void getFileFromStorage(String name)
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
}