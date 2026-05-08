package com.example.conducto2.ui.classes.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        liveAvailableLayout.setVisibility(View.GONE);
        noLiveLessonLayout.setVisibility(View.VISIBLE); // default to no lesson
    }

    private void setupListeners() {
        FirestoreManager firestoreManager = new FirestoreManager();
        firestoreManager.listenForLiveLesson(DataManager.getCurClass().getId(), this);

        btnEnterLive.setOnClickListener(v -> {
            if (currentLiveLesson != null) {
                joinLiveLesson(currentLiveLesson);
            }
        });
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
        } else {
            Toast.makeText(getContext(), "No assigned music file found for this lesson.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLiveLessonChanged(Lesson lesson) {
        currentLiveLesson = lesson;
        if (lesson != null) {
            liveAvailableLayout.setVisibility(View.VISIBLE);
            noLiveLessonLayout.setVisibility(View.GONE);
        } else {
            liveAvailableLayout.setVisibility(View.GONE);
            noLiveLessonLayout.setVisibility(View.VISIBLE);
        }
    }
}
