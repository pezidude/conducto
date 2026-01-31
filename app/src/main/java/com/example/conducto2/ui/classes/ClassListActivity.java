package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.util.SwipeHelper;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ClassListActivity extends BaseDrawerActivity implements FirestoreManager.DBResult {

    private RecyclerView classesRecyclerView;
    private ClassAdapter classAdapter;
    private ImageButton filterByUserButton;
    private ImageButton sortByNameButton;
    private FloatingActionButton addClassFab;
    private boolean isFilteredByUser = false;
    private boolean isSortedByName = false;
    // private FirestoreManager firestoreManager; // Inherited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list);

        // firestoreManager = new FirestoreManager(); // Inherited
        firestoreManager.setDbResult(this);

        initViews();
        setupRecyclerView(buildQuery());
        setupListeners();

        firestoreManager.getUser(user -> {
            if (user != null) {
                if ("teacher".equals(user.getUserType())) {
                    addClassFab.setOnClickListener(this::showFabMenu);
                    addEditDelete();
                } else {
                    addClassFab.setOnClickListener(v -> showJoinClassDialog());
                }
            }
        });
    }

    private void initViews() {
        classesRecyclerView = findViewById(R.id.classes_recycler_view);
        classesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        classesRecyclerView.setItemAnimator(null); // fix bug in recycle view


        sortByNameButton = findViewById(R.id.sort_by_name_button);
        filterByUserButton = findViewById(R.id.filter_by_user_button);
        addClassFab = findViewById(R.id.add_class_fab);
    }
    private void setupListeners() {
        sortByNameButton.setOnClickListener(v -> {
            isSortedByName = !isSortedByName;
            updateQuery();
        });

        filterByUserButton.setOnClickListener(v -> toggleFilterByUser());
        // FAB listener is now set in onCreate after user type is determined
    }

    private void showFabMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.fab_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_create_class) {
                startActivity(new Intent(ClassListActivity.this, ClassEditActivity.class));
                return true;
            } else if (itemId == R.id.menu_join_class) {
                showJoinClassDialog();
                return true;
            }
            return false;
        });
        popupMenu.show();
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

    private void setupRecyclerView(Query query) {
        FirestoreRecyclerOptions<Class> options = new FirestoreRecyclerOptions.Builder<Class>()
                .setQuery(query, Class.class)
                .build();

        classAdapter = new ClassAdapter(options);
        classesRecyclerView.setAdapter(classAdapter);


    }

    private void addEditDelete() {
        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                // Edit
                Intent intent = new Intent(ClassListActivity.this, ClassEditActivity.class);
                intent.putExtra("class", classAdapter.getItem(position));
                startActivity(intent);
                classAdapter.notifyItemChanged(position); // To reset the item view
            }

            @Override
            public void onSwipeRight(int position) {
                // Delete
                new AlertDialog.Builder(ClassListActivity.this)
                        .setMessage("Are you sure you want to delete this class?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            classAdapter.getSnapshots().getSnapshot(position).getReference().delete();
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            classAdapter.notifyItemChanged(position);
                        })
                        .setOnCancelListener(dialog -> {
                            classAdapter.notifyItemChanged(position);
                        })
                        .create()
                        .show();
            }
        });
        new ItemTouchHelper(swipeHelper).attachToRecyclerView(classesRecyclerView);
    }

    private Query buildQuery() {
        Query query = FirebaseFirestore.getInstance().collection("classes");
        query = query.whereArrayContains("members", FirebaseComm.authUserEmail());
        if (isFilteredByUser) {
            // TODO: filter by something else
        }

        if (isSortedByName) {
            query = query.orderBy("name", Query.Direction.ASCENDING);
        }

        return query;
    }

    private void toggleFilterByUser() {
        isFilteredByUser = !isFilteredByUser;
        updateQuery();
    }

    private void updateQuery() {
        Query query = buildQuery();
        FirestoreRecyclerOptions<Class> options = new FirestoreRecyclerOptions.Builder<Class>()
                .setQuery(query, Class.class)
                .build();
        classAdapter.updateOptions(options);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (classAdapter != null) {
            classesRecyclerView.post(() -> classAdapter.startListening());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (classAdapter != null) {
            classAdapter.stopListening();
        }
    }

    @Override
    public void uploadResult(boolean success) {
        if (success) {
            Toast.makeText(this, "Joined class successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}