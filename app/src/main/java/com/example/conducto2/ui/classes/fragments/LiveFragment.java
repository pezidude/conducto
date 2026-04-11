package com.example.conducto2.ui.classes.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.ui.classes.ClassActivity;

public class LiveFragment extends Fragment {

    FirestoreManager fbManager = new FirestoreManager();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live, container, false);
        return view;
    }

    public void enableJoin() {
        // exchange visibility setting of both overlays.
    }

}
