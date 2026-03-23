package com.example.conducto2.ui.player;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.conducto2.R;
import com.google.android.material.slider.Slider;

import java.util.Locale;

/**
 * A fragment that displays playback controls, such as a play/pause button and speed control.
 * This fragment communicates with its host activity through the {@link PlaybackControlsListener}
 * interface to control playback.
 */
public class PlaybackFragment extends Fragment {

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface PlaybackControlsListener {
        void onPlayPauseClicked();

        void onResetClicked();

        void onSpeedChanged(int speedPercentage);
    }

    private PlaybackControlsListener mListener;
    private ImageButton playPauseButton;
    private boolean isPlaying = false;

    public PlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        playPauseButton = view.findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            updatePlayPauseButton();
            if (mListener != null) {
                mListener.onPlayPauseClicked();
            }
        });

        ImageButton resetButton = view.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onResetClicked();
            }
        });

        Slider speedSlider = view.findViewById(R.id.speed_slider);
        speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            int speedPercentage = (int) value;
            if (mListener != null) {
                mListener.onSpeedChanged(speedPercentage);
            }
        });

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (PlaybackControlsListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * Updates the play/pause button icon based on the current playback state.
     */
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);
        }
    }
}