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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

/**
 * Fragment that displays past (archived) lessons for the class.
 */
public class HistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private LessonAdapter lessonAdapter;
    private FirestoreManager firestoreManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        firestoreManager = new FirestoreManager();

        initViews(view);
        setupRecyclerView();
        setupSwipe();
        setupUI();
        setupItemClickListener();

        return view;
    }

    private void setupSwipe() {
        User user = DataManager.getUserInstance();
        if (user == null || !"teacher".equals(user.getUserType())) {
            return;
        }

        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                showRestoreConfirmation(position);
            }

            @Override
            public void onSwipeRight(int position) {
                showDeleteConfirmation(position);
            }
        });

        swipeHelper.setLeftAction(R.drawable.unarchive_24px, R.color.brand_accent);
        swipeHelper.setRightAction(R.drawable.ic_delete, R.color.error);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(historyRecyclerView);
    }

    private void showRestoreConfirmation(int position) {
        lessonAdapter.notifyDataSetChanged();
        new AlertDialog.Builder(getContext())
                .setTitle("Restore Lesson")
                .setMessage("Are you sure you want to restore this lesson to scheduled?")
                .setPositiveButton("Restore", (dialog, which) -> {
                    Lesson lesson = lessonAdapter.getItem(position);
                    firestoreManager.updateLessonArchivedStatus(DataManager.getCurClass().getId(), lesson.getId(), false);
                    lessonAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> lessonAdapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> lessonAdapter.notifyDataSetChanged())
                .show();
    }

    private void showDeleteConfirmation(int position) {
        lessonAdapter.notifyDataSetChanged();
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Lesson")
                .setMessage("Are you sure you want to permanently delete this lesson?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Lesson lesson = lessonAdapter.getItem(position);
                    firestoreManager.deleteLesson(DataManager.getCurClass().getId(), lesson.getId());
                    lessonAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> lessonAdapter.notifyDataSetChanged())
                .setOnCancelListener(dialog -> lessonAdapter.notifyDataSetChanged())
                .show();
    }

    private void initViews(View view) {
        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecyclerView.setItemAnimator(null);
    }

    private void setupRecyclerView() {
        Query query = FirebaseFirestore.getInstance().collection("classes")
                .document(DataManager.getCurClass().getId()).collection("lessons")
                .whereEqualTo("isArchived", true);

        FirestoreRecyclerOptions<Lesson> options = new FirestoreRecyclerOptions.Builder<Lesson>()
                .setQuery(query, Lesson.class)
                .build();

        lessonAdapter = new LessonAdapter(options);
        historyRecyclerView.setAdapter(lessonAdapter);
    }

    private void setupUI() {
        User user = DataManager.getUserInstance();
        // Setup UI based on user type if needed
    }

    private void setupItemClickListener() {
        lessonAdapter.setOnItemClickListener(snapshot -> {
            Lesson lesson = snapshot.toObject(Lesson.class);
            DataManager.setCurLesson(lesson);

            Intent intent = new Intent(getContext(), LessonDetailsActivity.class);
            startActivity(intent);
        });
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
