package com.example.conducto2.ui.classes.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
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
public class ScheduledFragment extends Fragment {

    private RecyclerView lessonsRecyclerView;
    private LessonAdapter lessonAdapter;
    private FloatingActionButton addLessonFab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scheduled, container, false);

        initViews(view);
        setupRecyclerView(buildQuery());
        setupSwipe();
        setupListeners();
        setupUI();
        setupItemClickListener();

        return view;
    }

    private void setupSwipe() {
        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                showArchiveConfirmation(position);
            }

            @Override
            public void onSwipeRight(int position) {
                editLesson(position);
            }
        });

        swipeHelper.setLeftAction(R.drawable.archive_24px, R.color.navy_400);
        swipeHelper.setRightAction(R.drawable.ic_edit, R.color.brand_primary);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(lessonsRecyclerView);
    }

    private void showArchiveConfirmation(int position) {
        lessonAdapter.notifyDataSetChanged();
        new AlertDialog.Builder(getContext())
                .setTitle("Archive Lesson")
                .setMessage("Are you sure you want to archive this lesson?")
                .setPositiveButton("Archive", (dialog, which) -> {
                    Lesson lesson = lessonAdapter.getItem(position);
                    new FirestoreManager().updateLessonArchivedStatus(DataManager.getCurClass().getId(), lesson.getId(), true);
                    lessonAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> lessonAdapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> lessonAdapter.notifyDataSetChanged())
                .show();
    }

    private void editLesson(int position) {
        Lesson lesson = lessonAdapter.getItem(position);
        DataManager.setCurLesson(lesson);
        Intent intent = new Intent(getContext(), LessonEditActivity.class);
        startActivity(intent);
        lessonAdapter.notifyDataSetChanged();
    }

    private void initViews(View view) {
        lessonsRecyclerView = view.findViewById(R.id.lessons_recycler_view);
        lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lessonsRecyclerView.setItemAnimator(null);

        addLessonFab = view.findViewById(R.id.add_lesson_fab);
    }

    private void setupUI() {
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            addLessonFab.setVisibility(View.VISIBLE);
        } else {
            addLessonFab.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        addLessonFab.setOnClickListener(v -> {
            DataManager.setCurLesson(null); // set an empty lesson to be edited
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

    @Override
    public void setMenuVisibility(boolean isVisibleToUser) {
        super.setMenuVisibility(isVisibleToUser);
        if (lessonAdapter != null) {
            if (isVisibleToUser) {
                lessonAdapter.startListening();
                lessonAdapter.notifyDataSetChanged();
            } else {
                lessonAdapter.stopListening();
            }
        }
    }

    private Query buildQuery() {
        return FirebaseFirestore.getInstance().collection("classes")
                .document(DataManager.getCurClass().getId()).collection("lessons")
                .whereEqualTo("isArchived", false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (lessonAdapter != null) {
            lessonAdapter.startListening();
            lessonAdapter.notifyDataSetChanged();
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
