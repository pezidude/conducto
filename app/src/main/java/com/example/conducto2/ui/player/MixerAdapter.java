package com.example.conducto2.ui.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * MixerAdapter
 * 
 * A specialized RecyclerView adapter used for multi-track audio control in the sheet music player.
 * It provides a UI for adjusting the volume, muting, or soloing individual instruments 
 * identified in the MusicXML score.
 * 
 * This class maintains a local collection of {@link TrackState} objects to track the
 * state of each instrument and proxies UI events to the host activity for real-time 
 * JavaScript engine manipulation.
 */
public class MixerAdapter extends RecyclerView.Adapter<MixerAdapter.ViewHolder> {

    /** List of instrument names parsed from the MusicXML score. */
    private final List<String> instruments;

    /** Callback listener used to update the OSMD audio player state. */
    private final OnMixerChangeListener listener;

    /** Internal cache tracking the volume, mute, and solo states for each instrument. */
    private final List<TrackState> states;

    /** Interface definition for audio track modification events. */
    public interface OnMixerChangeListener {
        /**
         * Triggered when any control for a track is modified.
         * @param index The instrument index in the score.
         * @param volume The new volume level (0.0 to 1.0).
         * @param mute The new mute state.
         * @param solo The new solo state.
         */
        void onTrackChanged(int index, float volume, boolean mute, boolean solo);
    }

    /** Simple data structure to hold the current audio configuration of a track. */
    public static class TrackState {
        float volume = 1.0f;
        boolean mute = false;
        boolean solo = false;
    }

    /**
     * Constructs a new MixerAdapter.
     * @param instruments The list of instrument titles to display.
     * @param listener The implementation to handle audio engine updates.
     */
    public MixerAdapter(List<String> instruments, OnMixerChangeListener listener) {
        this.instruments = instruments;
        this.listener = listener;
        this.states = new ArrayList<>();
        // Initialize one state object per instrument.
        for (int i = 0; i < instruments.size(); i++) {
            states.add(new TrackState());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mixer_track, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds instrument metadata and state to the UI.
     * Configures interactive listeners for seekbars and toggle buttons.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = instruments.get(position);
        TrackState state = states.get(position);

        holder.tvName.setText(name);
        // Map 0.0-1.0 float range to 0-100 integer range for the SeekBar.
        holder.sbVolume.setProgress((int) (state.volume * 100));
        holder.btnMute.setChecked(state.mute);
        holder.btnSolo.setChecked(state.solo);

        // Volume Logic: Update local state and trigger listener upon user interaction.
        holder.sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    state.volume = progress / 100f;
                    listener.onTrackChanged(holder.getAdapterPosition(), state.volume, state.mute, state.solo);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Mute Logic: Proxy toggle events to the audio engine.
        holder.btnMute.setOnClickListener(v -> {
            state.mute = holder.btnMute.isChecked();
            listener.onTrackChanged(holder.getAdapterPosition(), state.volume, state.mute, state.solo);
        });

        // Solo Logic: Proxy toggle events. Note: Multi-track solo management is handled in JS.
        holder.btnSolo.setOnClickListener(v -> {
            state.solo = holder.btnSolo.isChecked();
            listener.onTrackChanged(holder.getAdapterPosition(), state.volume, state.mute, state.solo);
        });
    }

    @Override
    public int getItemCount() {
        return instruments.size();
    }

    /**
     * ViewHolder for individual mixer track items.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        SeekBar sbVolume;
        ToggleButton btnMute;
        ToggleButton btnSolo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_instrument_name);
            sbVolume = itemView.findViewById(R.id.sb_volume);
            btnMute = itemView.findViewById(R.id.btn_mute);
            btnSolo = itemView.findViewById(R.id.btn_solo);
        }
    }
}