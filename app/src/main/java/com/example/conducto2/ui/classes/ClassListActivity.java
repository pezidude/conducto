package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.utils.SwipeHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;


/**
 * ClassListActivity
 * 
 * This activity provides the primary management dashboard for all classrooms associated
 * with the logged-in user. It serves as a dynamic, searchable, and sortable list.
 * 
 * Features:
 * 1. Real-time Synchronization: Leverages FirestoreRecyclerAdapter for automatic UI updates.
 * 2. Role-Based Actions: 
 *    - Teachers: Can create classrooms via FAB and manage (Edit/Delete) via swipe gestures.
 *    - Students: Can join existing classrooms via Join Codes through the same FAB.
 * 3. Search & Sort: Dynamic Firestore queries for alphabetical sorting and prefix-based search.
 * 4. Context Management: Transitions users into the specific ClassActivity hub.
 */
public class ClassListActivity extends BaseDrawerActivity implements FirebaseComm.DBResult {

    /** The primary list view for classrooms. */
    private RecyclerView classesRecyclerView;

    /** Adapter implementing real-time Firestore synchronization. */
    private ClassAdapter classAdapter;

    /** UI component for real-time text-based search. */
    private EditText searchEditText;

    /** Toggle button for alphabetical sorting. */
    private ImageButton sortByNameButton;

    /** Multifunctional button (Teacher: Create, Student: Join). */
    private FloatingActionButton addClassFab;

    /** Internal state for the current search filter. */
    private String searchQuery = "";

    /** Internal state for the current sort configuration. */
    private boolean isSortedByName = false;

    final String TAG = "ClassListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list);

        // Standard Firestore interface setup.
        firestoreManager.setDbResult(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateButtonStates();

        // RBAC Logic: Configure administrative capabilities based on the user's role.
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            // Teacher Path: Enable creation and management features.
            addClassFab.setOnClickListener(v -> {
                DataManager.setCurClass(null);
                startActivity(new Intent(ClassListActivity.this, ClassEditActivity.class));
            });
            setupSwipe();
        } else {
            // Student Path: Enable classroom enrollment feature.
            addClassFab.setOnClickListener(v -> showJoinClassDialog());
        }
    }

    /**
     * Binds UI components to their layout definitions.
     */
    private void initViews() {
        classesRecyclerView = findViewById(R.id.classes_recycler_view);
        classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Optimization: Disable default animations to prevent flickering during rapid filtering.
        classesRecyclerView.setItemAnimator(null); 


        sortByNameButton = findViewById(R.id.sort_by_name_button);
        searchEditText = findViewById(R.id.search_classes_edit_text);
        addClassFab = findViewById(R.id.add_class_fab);
    }

    /**
     * Establishes reactive UI listeners for searching and sorting.
     */
    private void setupListeners() {
        sortByNameButton.setOnClickListener(v -> {
            isSortedByName = !isSortedByName;
            updateQuery();
        });

        // Reactive Search: Triggers adapter filtering as the user types.
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                updateQuery();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Synchronizes the visual state of the sort button with the internal configuration.
     */
    private void updateButtonStates() {
        sortByNameButton.setImageResource(R.drawable.sort_by_alpha_24px);
        if (isSortedByName) {
            // Active State: Highlight button with theme color.
            sortByNameButton.setBackgroundResource(R.drawable.bg_circle_highlight);
            sortByNameButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        } else {
            // Inactive State: Transparent background.
            sortByNameButton.setBackgroundResource(android.R.color.transparent);
            sortByNameButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        }
    }

    /**
     * Displays a dialog for students to enter a 6-character Join Code.
     */
    private void showJoinClassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_join_class, null);
        final EditText joinCodeEditText = dialogView.findViewById(R.id.join_code_edit_text);

        builder.setView(dialogView)
                .setTitle("Join a Class")
                .setPositiveButton("Join", (dialog, which) -> {
                    String joinCode = joinCodeEditText.getText().toString().trim();
                    if (!joinCode.isEmpty()) {
                        // Delegate network logic to FirestoreManager.
                        firestoreManager.joinClassWithCode(joinCode);
                    } else {
                        Toast.makeText(this, "Please enter a join code", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void setupRecyclerView() {
        Query query = getBaseQuery();
        FirestoreRecyclerOptions<Class> options = new FirestoreRecyclerOptions.Builder<Class>()
                .setQuery(query, Class.class)
                .build();
        
        classAdapter = new ClassAdapter(options);
        classesRecyclerView.setAdapter(classAdapter);
    }

    /**
     * Constructs the base Firestore query for classrooms where the current user
     * is listed in the 'members' array.
     */
    private Query getBaseQuery() {
        Query query = FirebaseComm.getCollectionReference("classes")
                .whereArrayContains("members", FirebaseComm.authUserEmail());

        if (isSortedByName || !searchQuery.isEmpty()) {
            query = query.orderBy("name");
        }

        if (!searchQuery.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchQuery)
                    .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff");
        }
        
        return query;
    }

    /**
     * Configures the ItemTouchHelper for swipe-to-edit and swipe-to-delete.
     * Restricted to teachers only for administrative safety.
     */
    private void setupSwipe() {
        User user = DataManager.getUserInstance();
        if (user == null || !"teacher".equals(user.getUserType())) {
            return;
        }

        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                editClass(position);
            }

            @Override
            public void onSwipeRight(int position) {
                showDeleteConfirmation(position);
            }
        });

        // Set action visual metadata (Icons and Colors).
        swipeHelper.setLeftAction(R.drawable.ic_edit, R.color.brand_primary);
        swipeHelper.setRightAction(R.drawable.ic_delete, R.color.error);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(classesRecyclerView);
    }

    /**
     * Navigates to the ClassEditActivity with the selected classroom data.
     */
    private void editClass(int position) {
        Class selectedClass = classAdapter.getItem(position);
        DataManager.setCurClass(selectedClass);
        Intent intent = new Intent(ClassListActivity.this, ClassEditActivity.class);
        startActivity(intent);
    }

    /**
     * Displays a destructive action warning before deleting a classroom.
     */
    private void showDeleteConfirmation(int position) {
        Class aClass = classAdapter.getItem(position);
        new AlertDialog.Builder(ClassListActivity.this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to permanently delete this class including nested lessons, students, etc?\nTHIS CAN NOT BE UNDONE!")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Critical Operation: Permanent removal of the classroom document.
                    FirebaseComm.getCollectionReference("classes").document(aClass.getId()).delete();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Central UI refresh method that updates the adapter's query based on search and sort states.
     */
    private void updateQuery() {
        updateButtonStates();
        
        Query newQuery = getBaseQuery();
        FirestoreRecyclerOptions<Class> newOptions = new FirestoreRecyclerOptions.Builder<Class>()
                .setQuery(newQuery, Class.class)
                .build();
        
        classAdapter.updateOptions(newOptions);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (classAdapter != null) {
            classAdapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (classAdapter != null) {
            classAdapter.startListening();
        }
    }


    /**
     * Handles the callback for successful network operations.
     */
    @Override
    public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
        if (success && operation == FirebaseComm.DbOperation.JOIN_CLASS) {
            Toast.makeText(this, "Joined class successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}