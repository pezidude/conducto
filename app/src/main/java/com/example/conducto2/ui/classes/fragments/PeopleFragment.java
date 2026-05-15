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

public class PeopleFragment extends Fragment {

    private static final String TAG = "PeopleFragment";

    private RecyclerView rvPeople;
    private PeopleAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FirestoreManager firestoreManager;
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

        if (currentClass != null) {
            loadPeople();
        } else {
            Log.e(TAG, "Current class is null");
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private void setupRecyclerView() {
        rvPeople.setLayoutManager(new LinearLayoutManager(getContext()));
        String teacherEmail = "";
        if (DataManager.getCurClass() != null) {
            teacherEmail = DataManager.getCurClass().getOwnerEmail();
        }
        adapter = new PeopleAdapter(teacherEmail);
        rvPeople.setAdapter(adapter);
    }

    private void loadPeople() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);

        firestoreManager.getAllUsers(new ArrayList<>(), users -> {
            if (!isAdded()) return;
            
            progressBar.setVisibility(View.GONE);
            if (users != null) {
                List<User> members = new ArrayList<>();
                ArrayList<String> memberEmails = currentClass.getMembers();
                String ownerEmail = currentClass.getOwnerEmail();

                for (User user : users) {
                    if ((memberEmails != null && memberEmails.contains(user.getEmail())) || 
                        (ownerEmail != null && ownerEmail.equals(user.getEmail()))) {
                        members.add(user);
                    }
                }

                if (members.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    // Sort: Teacher first, then alphabetical
                    Collections.sort(members, (u1, u2) -> {
                        boolean isT1 = u1.getEmail().equals(ownerEmail);
                        boolean isT2 = u2.getEmail().equals(ownerEmail);
                        if (isT1 && !isT2) return -1;
                        if (!isT1 && isT2) return 1;
                        
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
