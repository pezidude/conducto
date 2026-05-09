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

public class MixerAdapter extends RecyclerView.Adapter<MixerAdapter.ViewHolder> {

    private final List<String> instruments;
    private final OnMixerChangeListener listener;
    private final List<TrackState> states;

    public interface OnMixerChangeListener {
        void onTrackChanged(int index, float volume, boolean mute, boolean solo);
    }

    public static class TrackState {
        float volume = 1.0f;
        boolean mute = false;
        boolean solo = false;
    }

    public MixerAdapter(List<String> instruments, OnMixerChangeListener listener) {
        this.instruments = instruments;
        this.listener = listener;
        this.states = new ArrayList<>();
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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = instruments.get(position);
        TrackState state = states.get(position);

        holder.tvName.setText(name);
        holder.sbVolume.setProgress((int) (state.volume * 100));
        holder.btnMute.setChecked(state.mute);
        holder.btnSolo.setChecked(state.solo);

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

        holder.btnMute.setOnClickListener(v -> {
            state.mute = holder.btnMute.isChecked();
            listener.onTrackChanged(holder.getAdapterPosition(), state.volume, state.mute, state.solo);
        });

        holder.btnSolo.setOnClickListener(v -> {
            state.solo = holder.btnSolo.isChecked();
            listener.onTrackChanged(holder.getAdapterPosition(), state.volume, state.mute, state.solo);
        });
    }

    @Override
    public int getItemCount() {
        return instruments.size();
    }

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