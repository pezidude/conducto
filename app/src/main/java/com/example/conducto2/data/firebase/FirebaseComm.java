package com.example.conducto2.data.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseComm {
    private static final String TAG = "Firebase Comm";
    protected static FirebaseFirestore FIRESTORE;
    private static FirebaseAuth AUTH;


    // Utility functions

    public static FirebaseAuth getAuth() {
        if (AUTH == null)
            AUTH = FirebaseAuth.getInstance();
        return AUTH;
    }

    public static FirebaseFirestore getFisrestore() {
        if (FIRESTORE == null)
            FIRESTORE = FirebaseFirestore.getInstance();

        return FIRESTORE;
    }

    public static CollectionReference getCollectionReference(String collection) {
        return getFisrestore().collection(collection);
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


/*
    //
    // the following methods perform firestore transactions - GENERIC
    // data can be passed to calling class by Interface
    public interface FireStoreResult
    {
        void profilesReturned(ArrayList<Profile> arr);

        void elementsReturned(ArrayList<Map<String,Object>> arr);
        void elementsChanged(Map<String,Object> map,int oldIndex,int newIndex);
        void elementRemoved(int index);
        void elementAdded(Map<String,Object> map,int index);
        void changedElement(Map<String,Object> map);
    }
    private FireStoreResult fireStoreResult;

    public void setFireStoreResult(FireStoreResult fireStoreResult)
    {
        this.fireStoreResult = fireStoreResult;
    }


 */

    // Add data to a collection

/*
    public void addToFireStoreCollection(String collectionName, Map<String, Object> map) {
        CollectionReference colRef = getCollectionReference(collectionName);

      //  FirebaseFirestore.getInstance().collection("users").add()
        colRef.add(map)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "onSuccess: insert to collection");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Failure: Failed insert to collection " + e.getMessage());

                    }
                });
    }


    // set data in a specific document or create one.
    public void setFireStoreDocument(String collectionName, String documentName, Map<String, Object> map) {
        DocumentReference docRef = getCollectionReference(collectionName).document(documentName);
         // Note there are options for update & Set with Merge Flag
         // Also for a single element
         // shown here ->  set this item whether new or replace existing
        docRef.set(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                            Log.d(TAG, "onComplete:  added to document success");
                        else
                            Log.d(TAG, "onComplete:  added to document failed");
                    }
                });
    }


    // Methods to get Data from firestore
    public void getAllDocumentsInCollection(String collectionName)
    {
        getCollectionReference(collectionName).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                     //   getDataFromListener(task);
                        if(task.isSuccessful())
                        {
                            ArrayList<Profile> arr= new ArrayList<>();
                            for (QueryDocumentSnapshot snapshot:task.getResult()
                            ) {
                                // arrayList
                                snapshot.getReference();
                                arr.add(snapshot.toObject(Profile.class));
                            }

                            fireStoreResult.profilesReturned(arr);
                        }
                    }
                });
    }
    public void getDocumentWhereEqualWithLimit(String collectionName, String field,String value,int limit)
    {
        getCollectionReference(collectionName).whereEqualTo(field,value).limit(limit).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "onComplete:  equal to with limit: size= " + task.getResult().size());
                        getDataFromListener(task);
                    }
                });
    }


    public void getDocumentsOrderedByFieldWithLimit(String collectionName, String field,int limit)
    {
        getCollectionReference(collectionName).orderBy(field).limit(limit).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        Log.d(TAG, "onComplete:  order with limit: size= " + task.getResult().size());
                        getDataFromListener(task);

                    }
                });
    }

    // private method to enter data received into ArrayList
    // of Key/Value objects - MAP
    // can be used from all Get Methods used
    private void getDataFromListener(Task<QuerySnapshot> task) {

        ArrayList<Map<String,Object>> arr = new ArrayList<>();
        if(task.isSuccessful())
        {
            for (QueryDocumentSnapshot doc:task.getResult())
            {

                arr.add(doc.getData());
            }
            Log.d(TAG, "getDataFromListener: succes, received"+  arr.size() + " " +arr.toString());
            if(fireStoreResult!=null)
                fireStoreResult.elementsReturned(arr);
        }
        else
            Log.d(TAG, "getDataFromListener:  FAILED");
    }


    //
    // below methods are listeners for changes->
    // Examples : listener on a collection and on a specific document
    // data shall be provided back to the class/activity via interface
    //

    public void listenToCollectionChanges(Activity ac, String collectionName) {
        getCollectionReference(collectionName).addSnapshotListener(ac,new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.d(TAG, "onEvent: Listen Failed");
                    return;
                }
                // Note here
                // change.getDocument().getData() -> returns the Key/Value Object
                // Map -> can return this to the activity
                for (DocumentChange change : value.getDocumentChanges()) {
                    if (change.getType() == DocumentChange.Type.MODIFIED) {
                        if(fireStoreResult!=null)
                            fireStoreResult.elementsChanged(change.getDocument().getData(),change.getOldIndex(), change.getNewIndex());
                        Log.d(TAG, "onEvent: change modified");
                    } else if (change.getType() == DocumentChange.Type.REMOVED) {
                        if(fireStoreResult!=null)
                            fireStoreResult.elementRemoved(change.getOldIndex());
                        Log.d(TAG, "onEvent:  removed ");
                    } else if (change.getType() == DocumentChange.Type.ADDED) {
                        Log.d(TAG, "onEvent listener: ADDED ");
                        if(fireStoreResult!=null)
                            fireStoreResult.elementAdded(change.getDocument().getData(),change.getNewIndex());
                    }
                }

            }
        });

    }

    //
    // listen to changes -
    // A few overloads available for .addSnapshotListener
    // since we would like to stop listening when activity stopped
    // we need to perform unregister.
    // if we used the one implemented here -> when it receives Activity
    // it becomes activityscoped listener and automatically removed during onStop
        public void listenToDocumentChanges(Activity ac,String collectionName, String documentName) {
        getCollectionReference(collectionName).document(documentName).addSnapshotListener(ac,new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error!=null)
                {
                    Log.d(TAG, "onEvent: ERROR is not null" + error.getMessage());
                    return;
                }
                if(value!=null)
                {
                    // value.getData holds the key value
                    // representd by hashmap

                    Log.d(TAG, "onEvent:received map " + value.getData().toString());
                    if(fireStoreResult!=null)
                        fireStoreResult.changedElement(value.getData());
                }

            }
        });


    }

 */
}