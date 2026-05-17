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
 * PlaybackFragment
 * 
 * This fragment provides the standard playback interface for the Sheet Music Player. 
 * It encapsulates the visual controls for starting, pausing, and resetting music, 
 * as well as adjusting the tempo (BPM) and monitoring progress.
 * 
 * It communicates user interactions to the host {@link SMPlayerActivity} via the 
 * {@link PlaybackControlsListener} interface, allowing for a decoupled architecture 
 * between UI controls and the core rendering engine.
 */
public class PlaybackFragment extends Fragment {

    /**
     * Interface for communicating playback events back to the host Activity.
     */
    public interface PlaybackControlsListener {
        /** Triggered when the play/pause button is toggled. */
        void onPlayPauseClicked();

        /** Triggered when the reset button is clicked to return to the score start. */
        void onResetClicked();

        /** Triggered when the tempo slider is adjusted. */
        void onSpeedChanged(int speedPercentage);
    }

    /** The listener instance (typically the host Activity). */
    private PlaybackControlsListener mListener;

    /** Button for toggling between play and pause states. */
    private ImageButton playPauseButton;

    /** Slider component for adjusting the tempo (BPM). */
    private Slider speedSlider;

    /** Progress bar indicating the current playback position in the score. */
    private android.widget.ProgressBar playbackProgressBar;

    /** Local state tracking if the music is currently playing. */
    private boolean isPlaying = false;

    public PlaybackFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playback, container, false);

        playPauseButton = view.findViewById(R.id.play_pause_button);
        playPauseButton.setOnClickListener(v -> {
            // Local State Toggle: Flip the playing flag and update the icon immediately.
            isPlaying = !isPlaying;
            updatePlayPauseButton();
            // Event Delegation: Notify the Activity to execute the engine command.
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

        speedSlider = view.findViewById(R.id.speed_slider);
        speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            // Only notify if the change was initiated by the user to avoid feedback loops.
            if (fromUser && mListener != null) {
                mListener.onSpeedChanged((int) value);
            }
        });

        playbackProgressBar = view.findViewById(R.id.playback_progress_bar);

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Lifecycle Sync: Register the host Activity as the listener for control events.
        if (context instanceof PlaybackControlsListener) {
            mListener = (PlaybackControlsListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Cleanup: Nullify listener to prevent memory leaks.
        mListener = null;
    }

    /**
     * Updates the progress bar state.
     * @param current The current measure index.
     * @param total The total number of measures in the score.
     */
    public void updateProgress(int current, int total) {
        if (playbackProgressBar != null) {
            playbackProgressBar.setMax(total);
            playbackProgressBar.setProgress(current);
        }
    }

    /**
     * Enables or disables the speed control slider.
     * Used to lock tempo during live lessons for student clients.
     * @param enabled True to enable, False to lock.
     */
    public void setSpeedControlEnabled(boolean enabled) {
        if (speedSlider != null) {
            speedSlider.setEnabled(enabled);
        }
    }

    /**
     * Updates the local playing state and refreshes the UI iconography.
     * @param playing True if music is playing.
     */
    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        if (playPauseButton != null) {
            updatePlayPauseButton();
        }
    }

    /**
     * Toggles the button icon between play and pause vectors.
     */
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    /**
     * Updates the slider position to reflect a specific BPM.
     * Clamps the value to the valid range (50-200 BPM).
     * @param bpm The tempo to display.
     */
    public void updateBpmUI(int bpm) {
        if (speedSlider != null) {
            float value = Math.max(50.0f, Math.min(200.0f, (float) bpm));
            speedSlider.setValue(value);
        }
    }
}