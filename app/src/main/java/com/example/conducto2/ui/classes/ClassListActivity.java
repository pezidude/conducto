package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ClassListActivity extends BaseDrawerActivity implements FirebaseComm.DBResult {

    private RecyclerView classesRecyclerView;
    private ClassAdapter classAdapter;
    private EditText searchEditText;
    private ImageButton sortByNameButton;
    private FloatingActionButton addClassFab;
    private String searchQuery = "";
    private boolean isSortedByName = false;
    private ListenerRegistration classesListener;
    // private FirestoreManager firestoreManager; // Inherited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list);

        // firestoreManager = new FirestoreManager(); // Inherited
        firestoreManager.setDbResult(this);

        initViews();
        setupRecyclerView();
        setupListeners();
        updateButtonStates();

        // User Dependent logic
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            addClassFab.setOnClickListener(v -> startActivity(new Intent(ClassListActivity.this, ClassEditActivity.class)));
            setupSwipe();
        } else {
            addClassFab.setOnClickListener(v -> showJoinClassDialog());
        }
    }

    private void initViews() {
        classesRecyclerView = findViewById(R.id.classes_recycler_view);
        classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classesRecyclerView.setItemAnimator(null); // fix bug in recycle view


        sortByNameButton = findViewById(R.id.sort_by_name_button);
        searchEditText = findViewById(R.id.search_classes_edit_text);
        addClassFab = findViewById(R.id.add_class_fab);
    }
    private void setupListeners() {
        sortByNameButton.setOnClickListener(v -> {
            isSortedByName = !isSortedByName;
            updateQuery();
        });

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
        // FAB listener is now set in onCreate after user type is determined
    }

    private void updateButtonStates() {
        // Sort Button
        sortByNameButton.setImageResource(R.drawable.sort_by_alpha_24px);
        if (isSortedByName) {
            sortByNameButton.setBackgroundResource(R.drawable.bg_circle_highlight);
            sortByNameButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        } else {
            sortByNameButton.setBackgroundResource(android.R.color.transparent);
            sortByNameButton.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        }
    }

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
                        firestoreManager.joinClassWithCode(joinCode);
                    } else {
                        Toast.makeText(this, "Please enter a join code", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.create().show();
    }

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter();
        classesRecyclerView.setAdapter(classAdapter);
    }

    private void startListening() {
        if (classesListener != null) return;

        Query query = FirebaseComm.getCollectionReference("classes")
                .whereArrayContains("members", FirebaseComm.authUserEmail());

        classesListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                return;
            }

            if (value == null) return;

            List<Class> classes = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                Class aClass = doc.toObject(Class.class);
                aClass.setId(doc.getId());
                classes.add(aClass);
            }
            classAdapter.updateData(classes);
            updateQuery(); // Apply current filter/sort
        });
    }

    private void stopListening() {
        if (classesListener != null) {
            classesListener.remove();
            classesListener = null;
        }
    }

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

        swipeHelper.setLeftAction(R.drawable.ic_edit, R.color.brand_primary);
        swipeHelper.setRightAction(R.drawable.ic_delete, R.color.error);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(classesRecyclerView);
    }

    private void editClass(int position) {
        Intent intent = new Intent(ClassListActivity.this, ClassEditActivity.class);
        intent.putExtra("class_obj", classAdapter.getItem(position));
        startActivity(intent);
    }

    private void showDeleteConfirmation(int position) {
        Class aClass = classAdapter.getItem(position);
        new AlertDialog.Builder(ClassListActivity.this)
                .setTitle("Delete Class")
                .setMessage("Are you sure you want to permanently delete this class including nested lessons, students, etc?\nTHIS CAN NOT BE UNDONE!")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseComm.getCollectionReference("classes").document(aClass.getId()).delete();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateQuery() {
        updateButtonStates();
        classAdapter.filter(searchQuery, isSortedByName);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListening();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening();
    }


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
