package com.example.conducto2.ui.classes.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.player.SMPlayerActivity;

import java.util.List;
import java.util.Map;

/**
 * LiveFragment
 * 
 * This Fragment manages the real-time presence of "Live Lessons" within the class view. 
 * It acts as a reactive gateway that listens to Firestore for any lesson within the current 
 * class that has its 'isLive' flag set to true. 
 * 
 * The fragment dynamically toggles between a "No Live Lesson" placeholder and an active 
 * "Live Lesson Available" view. It handles the logic for mapping students to their 
 * assigned MusicXML files and launching the SMPlayerActivity with the appropriate 
 * synchronization parameters.
 */
public class LiveFragment extends Fragment implements FirestoreManager.LiveLessonListener {

    /** Manager for Firestore database operations. */
    private FirestoreManager fbManager = new FirestoreManager();

    /** Layout shown when a live lesson is active and available to join. */
    private ConstraintLayout liveAvailableLayout;

    /** Layout shown when no live lessons are currently occurring in the class. */
    private ConstraintLayout noLiveLessonLayout;

    /** Button for users to enter the active live session. */
    private Button btnEnterLive;

    /** Button available only to teachers to terminate the session for everyone. */
    private Button btnEndLive;

    /** Status text to provide feedback (e.g., if no file is assigned to the student). */
    private TextView tvStatusMessage;

    /** Display for the title or status of the active live lesson. */
    private TextView tvLessonActive;

    /** The current active lesson object being monitored by the listener. */
    private Lesson currentLiveLesson;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    /**
     * Initializes the UI components and sets the default visibility state.
     * @param view The root view of the fragment.
     */
    private void initViews(View view) {
        liveAvailableLayout = view.findViewById(R.id.live_available_layout);
        noLiveLessonLayout = view.findViewById(R.id.no_live_lesson_layout);
        btnEnterLive = view.findViewById(R.id.btn_enter_live);
        btnEndLive = view.findViewById(R.id.btn_end_live);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        tvLessonActive = view.findViewById(R.id.tv_lesson_active);

        tvLessonActive.setTextSize(20); 

        // Default UI state: assume no live lesson until the listener triggers.
        if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.GONE);
        if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Configures the Firestore snapshot listener for the current class and sets up
     * click handlers for joining and ending lessons.
     */
    private void setupListeners() {
        FirestoreManager firestoreManager = new FirestoreManager();
        // Register this fragment as a listener for real-time lesson status changes.
        firestoreManager.listenForLiveLesson(DataManager.getCurClass().getId(), this);

        if (btnEnterLive != null) {
            btnEnterLive.setOnClickListener(v -> {
                if (currentLiveLesson != null) {
                    joinLiveLesson(currentLiveLesson);
                }
            });
        }

        User user = DataManager.getUserInstance();
        if (btnEndLive != null) {
            // Role-based visibility for the "End Lesson" functionality.
            if (user != null && "teacher".equals(user.getUserType())) {
                btnEndLive.setVisibility(View.VISIBLE);
                btnEndLive.setOnClickListener(v -> showEndLiveLessonConfirmation());
            } else {
                btnEndLive.setVisibility(View.GONE);
            }
        }
    }

    private void showEndLiveLessonConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("End Live Lesson")
                .setMessage("Are you sure you want to end this live lesson for all students?")
                .setPositiveButton("End Lesson", (dialog, which) -> endLiveLesson())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Terminate the live session. This involves three Firestore updates:
     * 1. Setting isLive to false on the lesson.
     * 2. Setting isArchived to true on the lesson.
     * 3. Setting the class-wide activity flag to false.
     */
    private void endLiveLesson() {
        com.example.conducto2.data.model.Class cls = DataManager.getCurClass();
        if (cls != null && currentLiveLesson != null) {
            // Persist changes to the database.
            fbManager.updateLessonLiveStatus(cls.getId(), currentLiveLesson.getId(), false);
            fbManager.updateLessonArchivedStatus(cls.getId(), currentLiveLesson.getId(), true);
            fbManager.updateClassActivity(cls.getId(), false);
            
            // Immediately update local UI to provide responsive feedback before listener cycles.
            currentLiveLesson = null;
            onLiveLessonChanged(null);
        }
    }

    /**
     * Logic for entering the SMPlayerActivity. 
     * For students, it searches the lesson's file mapping to find the specific 
     * MusicXML file assigned to their email.
     * @param lesson The active lesson to join.
     */
    private void joinLiveLesson(Lesson lesson) {
        User user = DataManager.getUserInstance();
        if (user == null) return;

        String fileUri = null;
        Map<String, List<String>> mapping = lesson.getFileMapping();
        String userEmail = user.getEmail();

        // Sequential search through the file mapping to find the user's assigned music.
        if (mapping != null && userEmail != null) {
            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                List<String> assignedStudents = entry.getValue();
                if (assignedStudents != null && assignedStudents.contains(userEmail)) {
                    fileUri = entry.getKey();
                    break; // Found the assignment, exit loop.
                }
            }
        }

        if (fileUri != null) {
            // Case 1: Student or Teacher with an explicit assignment found.
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(getContext(), SMPlayerActivity.class);
            intent.putExtra("fileUri", fileUri);
            intent.putExtra("isLive", true);
            // Permissions: Only teachers get playback controls in a live session.
            intent.putExtra("canControlPlayback", "teacher".equals(user.getUserType()));
            startActivity(intent);
        } else if ("teacher".equals(user.getUserType()) && mapping != null && !mapping.isEmpty()) {
            // Case 2: Teacher without an explicit assignment; default to the first file in the mapping.
            fileUri = mapping.keySet().iterator().next();
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(getContext(), SMPlayerActivity.class);
            intent.putExtra("fileUri", fileUri);
            intent.putExtra("isLive", true);
            intent.putExtra("canControlPlayback", true);
            startActivity(intent);
        } else {
            // Case 3: No file assignment found for the user (typically a student not in the mapping).
            if (tvStatusMessage != null) {
                tvStatusMessage.setText("No assigned music file found assigned to you.");
            }
        }
    }

    /**
     * Callback triggered by the Firestore listener when the live status of any lesson in the class changes.
     * @param lesson The new active lesson, or null if no lesson is currently live.
     */
    @Override
    public void onLiveLessonChanged(Lesson lesson) {
        currentLiveLesson = lesson;
        if (tvStatusMessage != null) tvStatusMessage.setText(""); 
        
        if (lesson != null) {
            // Transition UI to the "Available" state.
            if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.VISIBLE);
            if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.GONE);
            
            User user = DataManager.getUserInstance();
            if (user != null) {
                updateEndButtonVisibility(user);
            } else {
                // Async fetch user if the static instance isn't populated yet.
                fbManager.getUser(this::updateEndButtonVisibility);
            }
        } else {
            // Transition UI back to the "None" state.
            if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.GONE);
            if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.VISIBLE);
            if (btnEndLive != null) btnEndLive.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the visibility of teacher-only controls.
     * @param user The current user instance.
     */
    private void updateEndButtonVisibility(User user) {
        if (user != null && "teacher".equals(user.getUserType())) {
            if (btnEndLive != null) btnEndLive.setVisibility(View.VISIBLE);
        } else {
            if (btnEndLive != null) btnEndLive.setVisibility(View.GONE);
        }
    }
}