package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.lessons.LessonAdapter;
import com.example.conducto2.ui.lessons.LessonDetailsActivity;
import com.example.conducto2.ui.lessons.LessonEditActivity;
import com.example.conducto2.utils.SwipeHelper;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ClassActivity extends BaseDrawerActivity {

    private RecyclerView lessonsRecyclerView;
    private LessonAdapter lessonAdapter;
    private ImageButton filterByUserButton;
    private ImageButton sortByDateButton;
    private FloatingActionButton addLessonFab;
    private TextView joinCodeTextView;
    private boolean isFilteredByUser = false;
    private boolean isSortedByDate = false;
    private Intent lessonIntent;
    // firestoreManager is inherited
    // currentUser is inherited

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        if (getIntent().hasExtra("class_id")) {
            String classId = getIntent().getStringExtra("class_id");
            DataManager.setCurClassID(classId);
        }

        // firestoreManager = new FirestoreManager(); // Inherited

        initViews();
        setupRecyclerView(buildQuery());
        setupListeners();
        setupUI();
        fetchClassDetails();
        setupItemClickListener();
        setupIntent();
    }

    private void initViews() {
        lessonsRecyclerView = findViewById(R.id.lessons_recycler_view);
        lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        lessonsRecyclerView.setItemAnimator(null); // fix bug in recycle view

        sortByDateButton = findViewById(R.id.sort_by_date_button);
        filterByUserButton = findViewById(R.id.filter_by_user_button);
        addLessonFab = findViewById(R.id.add_lesson_fab);
        joinCodeTextView = findViewById(R.id.class_join_code);
    }

    private void fetchClassDetails() {
        String classId = DataManager.getCurClassID();
        if (classId == null) return;
        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        com.example.conducto2.data.model.Class currentClass = documentSnapshot.toObject(com.example.conducto2.data.model.Class.class);
                        if (currentClass != null) {
                            joinCodeTextView.setText("Code: " + currentClass.getJoinCode());
                        }
                    }
                });
    }

    private void setupUI() {
        if ("teacher".equals(DataManager.getUserInstance().getUserType())) {
            addLessonFab.setVisibility(View.VISIBLE);
            setupTeacherSwipe();
        } else {
            addLessonFab.setVisibility(View.GONE);
        }

    }

    private void setupIntent() {
        if ("teacher".equals(DataManager.getUserInstance().getUserType())) {
            lessonIntent = new Intent(ClassActivity.this, LessonEditActivity.class);
        } else {
            lessonIntent = new Intent(ClassActivity.this, LessonDetailsActivity.class);
        }
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
            startActivity(lessonIntent);
        });
    }

    private void setupItemClickListener() {
        lessonAdapter.setOnItemClickListener(snapshot -> {
            Lesson lesson = snapshot.toObject(Lesson.class);
            DataManager.setCurClassID(DataManager.getCurClassID());
            DataManager.setCurLesson(lesson);

            if (lesson != null) {
                lessonIntent.putExtra("classId", DataManager.getCurClassID());
                startActivity(lessonIntent);
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
                DataManager.setCurLesson(lessonAdapter.getItem(position));
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
                .document(DataManager.getCurClassID()).collection("lessons");
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
    protected void onResume() {
        super.onResume();
        setupIntent();
        lessonIntent = new Intent();
        if (lessonAdapter != null) {
            lessonAdapter.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (lessonAdapter != null) {
            lessonAdapter.stopListening();
        }
    }
}