package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.util.SwipeHelper;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ClassActivity extends AppCompatActivity {

    private RecyclerView lessonsRecyclerView;
    private LessonAdapter lessonAdapter;
    private ImageButton filterByUserButton;
    private ImageButton sortByDateButton;
    private FloatingActionButton addLessonFab;
    private boolean isFilteredByUser = false;
    private boolean isSortedByDate = false;
    private String classId;
    private FirestoreManager firestoreManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        if (getIntent().hasExtra("class_id")) {
            classId = getIntent().getStringExtra("class_id");
        }

        firestoreManager = new FirestoreManager();

        initViews();
        setupRecyclerView(buildQuery());
        setupListeners();
        updateQuery(); // update query to fit the current user and class
        fetchUserAndSetupUI();
    }

    private void initViews() {
        lessonsRecyclerView = findViewById(R.id.lessons_recycler_view);
        lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lessonsRecyclerView.setItemAnimator(null); // fix bug in recycle view

        sortByDateButton = findViewById(R.id.sort_by_date_button);
        filterByUserButton = findViewById(R.id.filter_by_user_button);
        addLessonFab = findViewById(R.id.add_lesson_fab);
    }

    private void fetchUserAndSetupUI() {
        firestoreManager.getUser(user -> {
            currentUser = user;
            if (currentUser != null) {
                if ("teacher".equals(currentUser.getUserType())) {
                    addLessonFab.setVisibility(View.VISIBLE);
                    setupTeacherSwipe();
                } else {
                    addLessonFab.setVisibility(View.GONE);
                }
                setupItemClickListener();
            }
        });
    }

    private void setupListeners() {
        sortByDateButton.setOnClickListener(v -> {
            isSortedByDate = !isSortedByDate;
            updateQuery();
        });
        filterByUserButton.setOnClickListener(v -> {
            isFilteredByUser = !isFilteredByUser;
            updateQuery();
        });

        addLessonFab.setOnClickListener(v -> {
            Intent intent = new Intent(ClassActivity.this, LessonEditActivity.class);
            intent.putExtra("classId", classId);
            startActivity(intent);
        });
    }

    private void setupItemClickListener() {
        lessonAdapter.setOnItemClickListener(snapshot -> {
            Lesson lesson = snapshot.toObject(Lesson.class);
            if (lesson != null) {
                Intent intent;
                if ("teacher".equals(currentUser.getUserType())) {
                    intent = new Intent(ClassActivity.this, LessonEditActivity.class);
                } else {
                    intent = new Intent(ClassActivity.this, LessonDetailsActivity.class);
                }
                intent.putExtra("lesson", lesson);
                intent.putExtra("classId", classId);
                startActivity(intent);
            }
        });
    }

    private void setupRecyclerView(Query query) {
        FirestoreRecyclerOptions<Lesson> options = new FirestoreRecyclerOptions.Builder<Lesson>()
                .setQuery(query, Lesson.class)
                .build();

        lessonAdapter = new LessonAdapter(options);
        lessonsRecyclerView.setAdapter(lessonAdapter);
    }

    private void setupTeacherSwipe() {
        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                // Edit
                Intent intent = new Intent(ClassActivity.this, LessonEditActivity.class);
                intent.putExtra("lesson", lessonAdapter.getItem(position));
                intent.putExtra("classId", classId);
                startActivity(intent);
                lessonAdapter.notifyItemChanged(position);
            }

            @Override
            public void onSwipeRight(int position) {
                // Delete
                new AlertDialog.Builder(ClassActivity.this)
                        .setMessage("Are you sure you want to delete this lesson?")
                        .setPositiveButton("Yes", (dialog, which) -> lessonAdapter.getSnapshots().getSnapshot(position).getReference().delete())
                        .setNegativeButton("No", (dialog, which) -> lessonAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> lessonAdapter.notifyItemChanged(position))
                        .create()
                        .show();
            }
        });
        new ItemTouchHelper(swipeHelper).attachToRecyclerView(lessonsRecyclerView);
    }

    private Query buildQuery() {
        Query query = FirebaseFirestore.getInstance().collection("classes")
                .document(classId).collection("lessons");
        if (isFilteredByUser) {
            // TODO: add filter query
        }
        if (isSortedByDate) {
            query = query.orderBy("date", Query.Direction.DESCENDING);
        }
        return query;
    }

    private void updateQuery() {
        Query query = buildQuery();
        FirestoreRecyclerOptions<Lesson> options = new FirestoreRecyclerOptions.Builder<Lesson>()
                .setQuery(query, Lesson.class)
                .build();
        lessonAdapter.updateOptions(options);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (lessonAdapter != null) {
            lessonAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (lessonAdapter != null) {
            lessonAdapter.stopListening();
        }
    }
}