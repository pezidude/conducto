package com.example.conducto2.ui.classes.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.classes.PeopleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.example.conducto2.data.firebase.FirebaseComm;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

/**
 * PeopleFragment
 * 
 * This fragment manages and displays the classroom's participant list. It leverages
 * {@link PeopleAdapter} and {@link FirestoreRecyclerAdapter} to provide a real-time
 * roster that synchronizes automatically with the database.
 */
public class PeopleFragment extends Fragment {

    /** Identifier for logging. */
    private static final String TAG = "PeopleFragment";

    /** List component for rendering the participant cards. */
    private RecyclerView rvPeople;

    /** Adapter responsible for mapping User objects to the roster UI. */
    private PeopleAdapter adapter;

    /** Feedback text shown when a classroom has no students. */
    private TextView tvEmptyState;

    /** The specific classroom context for which the roster is being generated. */
    private Class currentClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        rvPeople = view.findViewById(R.id.rv_people);
        // Progress bar removed as FirestoreRecyclerAdapter handles loading state internally or via events
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        currentClass = DataManager.getCurClass();

        // Initiation Logic: Only attempt to load if a valid class context exists.
        if (currentClass != null) {
            setupRecyclerView();
        } else {
            Log.e(TAG, "Current class is null");
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        return view;
    }

    /**
     * Initializes the RecyclerView and creates the FirestoreRecyclerAdapter
     * with a query for the class members.
     */
    private void setupRecyclerView() {
        rvPeople.setLayoutManager(new LinearLayoutManager(getContext()));
        
        String ownerEmail = currentClass.getOwnerEmail();
        ArrayList<String> memberEmails = currentClass.getMembers();
        if (memberEmails == null) memberEmails = new ArrayList<>();
        
        // Combine owner and members for the query list.
        List<String> allEmails = new ArrayList<>(memberEmails);
        if (ownerEmail != null && !allEmails.contains(ownerEmail)) {
            allEmails.add(ownerEmail);
        }

        if (allEmails.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        // Firestore 'whereIn' is limited to 30 items. 
        // For larger classes, a different architectural approach (like a subcollection) would be required.
        Query query = FirebaseComm.getCollectionReference("users")
                .whereIn("email", allEmails)
                .orderBy("fname", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        adapter = new PeopleAdapter(options, ownerEmail);
        rvPeople.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}