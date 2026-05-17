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

/**
 * PeopleFragment
 * 
 * This fragment manages and displays the classroom's participant list. It serves
 * as a specialized roster view that identifies and highlights the class instructor.
 * 
 * Role:
 * - Fetches all users from the system and filters them based on class membership.
 * - Implements custom sorting logic (Instructor first, then alphabetical).
 * - Manages the transition between a loading state, empty state, and populated roster.
 */
public class PeopleFragment extends Fragment {

    /** Identifier for logging. */
    private static final String TAG = "PeopleFragment";

    /** List component for rendering the participant cards. */
    private RecyclerView rvPeople;

    /** Adapter responsible for mapping User objects to the roster UI. */
    private PeopleAdapter adapter;

    /** Visual indicator for the background user-fetch task. */
    private ProgressBar progressBar;

    /** Feedback text shown when a classroom has no students. */
    private TextView tvEmptyState;

    /** DAO for executing Firestore user queries. */
    private FirestoreManager firestoreManager;

    /** The specific classroom context for which the roster is being generated. */
    private Class currentClass;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_people, container, false);

        rvPeople = view.findViewById(R.id.rv_people);
        progressBar = view.findViewById(R.id.progress_bar);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        setupRecyclerView();
        
        firestoreManager = new FirestoreManager();
        currentClass = DataManager.getCurClass();

        // Initiation Logic: Only attempt to load if a valid class context exists.
        if (currentClass != null) {
            loadPeople();
        } else {
            Log.e(TAG, "Current class is null");
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        return view;
    }

    /**
     * Initializes the RecyclerView with a vertical layout manager and creates
     * the adapter with the instructor's email for visual highlighting.
     */
    private void setupRecyclerView() {
        rvPeople.setLayoutManager(new LinearLayoutManager(getContext()));
        String teacherEmail = "";
        if (DataManager.getCurClass() != null) {
            teacherEmail = DataManager.getCurClass().getOwnerEmail();
        }
        adapter = new PeopleAdapter(teacherEmail);
        rvPeople.setAdapter(adapter);
    }

    /**
     * Orchestrates the multi-stage user retrieval and filtering pipeline.
     * 
     * Pipeline Steps:
     * 1. Display loading UI.
     * 2. Fetch all registered users from Firestore.
     * 3. Membership Filter: Retain only users whose email is in the class's members list 
     *    or matches the owner's email.
     * 4. Priority Sorting: Move the instructor to the top of the list, then sort 
     *    students alphabetically by their full names.
     * 5. UI Dispatch: Update the adapter and toggle empty state visibility.
     */
    private void loadPeople() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        firestoreManager.getAllUsers(new ArrayList<>(), users -> {
            // Safety Check: Ensure the fragment is still attached to the UI before proceeding.
            if (!isAdded()) return;
            
            progressBar.setVisibility(View.GONE);
            if (users != null) {
                List<User> members = new ArrayList<>();
                ArrayList<String> memberEmails = currentClass.getMembers();
                String ownerEmail = currentClass.getOwnerEmail();

                // Step 1: Intersection logic between all system users and class members.
                for (User user : users) {
                    if ((memberEmails != null && memberEmails.contains(user.getEmail())) || 
                        (ownerEmail != null && ownerEmail.equals(user.getEmail()))) {
                        members.add(user);
                    }
                }

                if (members.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    // Step 2: Custom Sorting Algorithm.
                    // Prioritizes the Teacher role, then performs a case-insensitive name comparison.
                    Collections.sort(members, (u1, u2) -> {
                        boolean isT1 = u1.getEmail().equals(ownerEmail);
                        boolean isT2 = u2.getEmail().equals(ownerEmail);
                        
                        // Hierarchy Rule: Teachers always appear first.
                        if (isT1 && !isT2) return -1;
                        if (!isT1 && isT2) return 1;
                        
                        // Secondary Rule: Alphabetical sort for student-to-student comparison.
                        String name1 = (u1.getFname() != null ? u1.getFname() : "") + (u1.getLname() != null ? u1.getLname() : "");
                        String name2 = (u2.getFname() != null ? u2.getFname() : "") + (u2.getLname() != null ? u2.getLname() : "");
                        return name1.compareToIgnoreCase(name2);
                    });
                    
                    adapter.setUsers(members);
                    tvEmptyState.setVisibility(View.GONE);
                }
            } else {
                tvEmptyState.setText("Error loading members");
                tvEmptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}