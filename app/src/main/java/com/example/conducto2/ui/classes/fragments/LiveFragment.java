package com.example.conducto2.ui.classes.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.LiveLesson;
import com.example.conducto2.ui.classes.ClassActivity;

public class LiveFragment extends Fragment implements FirestoreManager.LiveLessonListener {

    FirestoreManager fbManager = new FirestoreManager();

    ConstraintLayout liveAvailableLayout;
    ConstraintLayout noLiveLessonLayout;

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

        liveAvailableLayout.setVisibility(View.GONE);
        noLiveLessonLayout.setVisibility(View.VISIBLE); // default to no lesson
    }

    private void setupListeners() {

        FirestoreManager firestoreManager = new FirestoreManager();
        firestoreManager.listenForLiveLesson(DataManager.getCurClass().getId(), this);

    }

    @Override
    public void onLiveLessonChanged(LiveLesson liveLesson) {
        if (liveLesson.isActive()) {
            liveAvailableLayout.setVisibility(View.VISIBLE);
            noLiveLessonLayout.setVisibility(View.GONE);
        } else {
            liveAvailableLayout.setVisibility(View.GONE);
            noLiveLessonLayout.setVisibility(View.VISIBLE);
        }
    }
}
