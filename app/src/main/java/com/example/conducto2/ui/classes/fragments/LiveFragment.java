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

public class LiveFragment extends Fragment implements FirestoreManager.LiveLessonListener {

    FirestoreManager fbManager = new FirestoreManager();

    ConstraintLayout liveAvailableLayout;
    ConstraintLayout noLiveLessonLayout;
    Button btnEnterLive;
    Button btnEndLive;
    TextView tvStatusMessage;
    TextView tvLessonActive;
    Lesson currentLiveLesson;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        liveAvailableLayout = view.findViewById(R.id.live_available_layout);
        noLiveLessonLayout = view.findViewById(R.id.no_live_lesson_layout);
        btnEnterLive = view.findViewById(R.id.btn_enter_live);
        btnEndLive = view.findViewById(R.id.btn_end_live);
        tvStatusMessage = view.findViewById(R.id.tv_status_message);
        tvLessonActive = view.findViewById(R.id.tv_lesson_active);

        tvLessonActive.setTextSize(20); // Make it a little bigger

        if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.GONE);
        if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.VISIBLE); // default to no lesson
    }

    private void setupListeners() {
        FirestoreManager firestoreManager = new FirestoreManager();
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

    private void endLiveLesson() {
        com.example.conducto2.data.model.Class cls = DataManager.getCurClass();
        if (cls != null && currentLiveLesson != null) {
            fbManager.updateLessonLiveStatus(cls.getId(), currentLiveLesson.getId(), false);
            fbManager.updateLessonArchivedStatus(cls.getId(), currentLiveLesson.getId(), true);
            fbManager.updateClassActivity(cls.getId(), false);
            
            // Local update to hide layouts immediately
            currentLiveLesson = null;
            onLiveLessonChanged(null);
        }
    }

    private void joinLiveLesson(Lesson lesson) {
        User user = DataManager.getUserInstance();
        if (user == null) return;

        String fileUri = null;
        Map<String, List<String>> mapping = lesson.getFileMapping();
        String userEmail = user.getEmail();

        if (mapping != null && userEmail != null) {
            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                List<String> assignedStudents = entry.getValue();
                if (assignedStudents != null && assignedStudents.contains(userEmail)) {
                    fileUri = entry.getKey();
                    break;
                }
            }
        }

        if (fileUri != null) {
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(getContext(), SMPlayerActivity.class);
            intent.putExtra("fileUri", fileUri);
            intent.putExtra("isLive", true);
            intent.putExtra("canControlPlayback", "teacher".equals(user.getUserType()));
            startActivity(intent);
        } else if ("teacher".equals(user.getUserType()) && mapping != null && !mapping.isEmpty()) {
            // Fallback for teacher if not explicitly in mapping: pick first file
            fileUri = mapping.keySet().iterator().next();
            DataManager.setCurLesson(lesson);
            Intent intent = new Intent(getContext(), SMPlayerActivity.class);
            intent.putExtra("fileUri", fileUri);
            intent.putExtra("isLive", true);
            intent.putExtra("canControlPlayback", true);
            startActivity(intent);
        } else {
            if (tvStatusMessage != null) {
                tvStatusMessage.setText("No assigned music file found assigned to you.");
            }
        }
    }

    @Override
    public void onLiveLessonChanged(Lesson lesson) {
        currentLiveLesson = lesson;
        if (tvStatusMessage != null) tvStatusMessage.setText(""); 
        
        if (lesson != null) {
            if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.VISIBLE);
            if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.GONE);
            
            User user = DataManager.getUserInstance();
            if (user != null) {
                updateEndButtonVisibility(user);
            } else {
                // Fetch user if not available yet in DataManager
                fbManager.getUser(this::updateEndButtonVisibility);
            }
        } else {
            if (liveAvailableLayout != null) liveAvailableLayout.setVisibility(View.GONE);
            if (noLiveLessonLayout != null) noLiveLessonLayout.setVisibility(View.VISIBLE);
            if (btnEndLive != null) btnEndLive.setVisibility(View.GONE);
        }
    }

    private void updateEndButtonVisibility(User user) {
        if (user != null && "teacher".equals(user.getUserType())) {
            if (btnEndLive != null) btnEndLive.setVisibility(View.VISIBLE);
        } else {
            if (btnEndLive != null) btnEndLive.setVisibility(View.GONE);
        }
    }
}
