package com.example.conducto2.ui.classes.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.lessons.LessonAdapter;
import com.example.conducto2.ui.lessons.LessonDetailsActivity;
import com.example.conducto2.ui.lessons.LessonEditActivity;
import com.example.conducto2.utils.SwipeHelper;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Fragment that displays the list of lessons (homework) for a specific class.
 */
public class HomeworkFragment extends Fragment {

    private RecyclerView lessonsRecyclerView;
    private LessonAdapter lessonAdapter;
    private ImageButton filterByUserButton;
    private ImageButton sortByDateButton;
    private FloatingActionButton addLessonFab;
    private boolean isFilteredByUser = false;
    private boolean isSortedByDate = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homework, container, false);

        initViews(view);
        setupRecyclerView(buildQuery());
        setupListeners();
        setupUI();
        setupItemClickListener();

        return view;
    }

    private void initViews(View view) {
        lessonsRecyclerView = view.findViewById(R.id.lessons_recycler_view);
        lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lessonsRecyclerView.setItemAnimator(null);

        sortByDateButton = view.findViewById(R.id.sort_by_date_button);
        filterByUserButton = view.findViewById(R.id.filter_by_user_button);
        addLessonFab = view.findViewById(R.id.add_lesson_fab);
    }

    private void setupUI() {
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            addLessonFab.setVisibility(View.VISIBLE);
            setupTeacherSwipe();
        } else {
            addLessonFab.setVisibility(View.GONE);
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
            Intent intent = new Intent(getContext(), LessonEditActivity.class);
            startActivity(intent);
        });
    }

    private void setupItemClickListener() {
        lessonAdapter.setOnItemClickListener(snapshot -> {
            Lesson lesson = snapshot.toObject(Lesson.class);
            DataManager.setCurLesson(lesson);

            Intent intent = new Intent(getContext(), LessonDetailsActivity.class);
            startActivity(intent);
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
                Intent intent = new Intent(getContext(), LessonEditActivity.class);
                DataManager.setCurLesson(lessonAdapter.getItem(position));
                startActivity(intent);
                lessonAdapter.notifyItemChanged(position);
            }

            @Override
            public void onSwipeRight(int position) {
                new AlertDialog.Builder(getContext())
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
    public void onResume() {
        super.onResume();
        if (lessonAdapter != null) {
            lessonAdapter.startListening();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (lessonAdapter != null) {
            lessonAdapter.stopListening();
        }
    }
}
