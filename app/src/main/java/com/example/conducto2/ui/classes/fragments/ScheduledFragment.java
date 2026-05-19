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
 * ScheduledFragment
 * 
 * The default landing fragment for the ClassActivity hub. It displays a real-time list 
 * of active/upcoming lessons for the classroom.
 * 
 * Key Responsibilities:
 * 1. Data Display: Renders all lessons where `isArchived` is false.
 * 2. Administrative Hub: Provides the primary Floating Action Button (FAB) for teachers 
 *    to create new lessons.
 * 3. Lifecycle Management: Supports gesture-based actions to edit or archive existing lessons.
 */
public class ScheduledFragment extends Fragment {

    /** The list component rendering the active lesson cards. */
    private RecyclerView lessonsRecyclerView;
    
    /** The real-time Firestore adapter handling the polymorphic lesson UI. */
    private LessonAdapter lessonAdapter;
    
    /** Contextual button for initiating lesson creation (Teacher only). */
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

    /**
     * Initializes the ItemTouchHelper for swipe gestures.
     * Enforces Role-Based Access Control (RBAC): Only teachers can perform swipe actions.
     */
    private void setupSwipe() {
        User user = DataManager.getUserInstance();
        if (user == null || !"teacher".equals(user.getUserType())) {
            return;
        }

        SwipeHelper swipeHelper = new SwipeHelper(new SwipeHelper.SwipeActions() {
            @Override
            public void onSwipeLeft(int position) {
                // Swipe Left -> Archive Lesson
                showArchiveConfirmation(position);
            }

            @Override
            public void onSwipeRight(int position) {
                // Swipe Right -> Edit Lesson
                editLesson(position);
            }
        });

        // Configure visual metadata for the swipe actions.
        swipeHelper.setLeftAction(R.drawable.archive_24px, R.color.navy_400);
        swipeHelper.setRightAction(R.drawable.ic_edit, R.color.brand_primary);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHelper);
        itemTouchHelper.attachToRecyclerView(lessonsRecyclerView);
    }

    /**
     * Confirms the intent to move a lesson to the History tab.
     * Updates the `isArchived` flag in Firestore to `true`.
     */
    private void showArchiveConfirmation(int position) {
        lessonAdapter.notifyDataSetChanged(); // Reset swipe UI visually
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

    /**
     * Transitions to the LessonEditActivity with the selected lesson data.
     */
    private void editLesson(int position) {
        Lesson lesson = lessonAdapter.getItem(position);
        DataManager.setCurLesson(lesson);
        Intent intent = new Intent(getContext(), LessonEditActivity.class);
        startActivity(intent);
        lessonAdapter.notifyDataSetChanged(); // Reset swipe UI
    }

    private void initViews(View view) {
        lessonsRecyclerView = view.findViewById(R.id.lessons_recycler_view);
        lessonsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lessonsRecyclerView.setItemAnimator(null); // Prevent flickering on data sync

        addLessonFab = view.findViewById(R.id.add_lesson_fab);
    }

    /**
     * Configures the visibility of administrative controls based on the user's role.
     */
    private void setupUI() {
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            addLessonFab.setVisibility(View.VISIBLE);
        } else {
            addLessonFab.setVisibility(View.GONE);
        }
    }

    /**
     * Wires the FAB to clear the current lesson cache and open the editor for a fresh document.
     */
    private void setupListeners() {
        addLessonFab.setOnClickListener(v -> {
            DataManager.setCurLesson(null); // Null signals a new lesson creation
            Intent intent = new Intent(getContext(), LessonEditActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Defines the standard navigation behavior when a lesson card is clicked.
     * Opens the detailed view.
     */
    private void setupItemClickListener() {
        lessonAdapter.setOnItemClickListener(snapshot -> {
            Lesson lesson = Lesson.fromSnapshot(snapshot);
            DataManager.setCurLesson(lesson);

            Intent intent = new Intent(getContext(), LessonDetailsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView(Query query) {
        FirestoreRecyclerOptions<Lesson> options = new FirestoreRecyclerOptions.Builder<Lesson>()
                .setQuery(query, snapshot -> Lesson.fromSnapshot(snapshot))
                .build();

        lessonAdapter = new LessonAdapter(options);
        lessonsRecyclerView.setAdapter(lessonAdapter);
    }

    /**
     * Fragment Lifecycle Management: Optimizes network usage by only listening to 
     * Firestore changes when the fragment is actually visible to the user in the ViewPager.
     */
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

    /**
     * Builds the specific Firestore query for this tab: unarchived lessons for the current class.
     */
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