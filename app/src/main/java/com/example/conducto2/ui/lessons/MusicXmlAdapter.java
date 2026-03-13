package com.example.conducto2.ui.lessons;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.conducto2.R;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.utils.FileHelper;

import java.util.List;

public class MusicXmlAdapter extends RecyclerView.Adapter<MusicXmlAdapter.ViewHolder> {

    private final List<MusicFile> musicFiles;
    private OnAssignButtonClickListener listener;

    public interface OnAssignButtonClickListener {
        void onAssignButtonClick(MusicFile musicFile);
    }

    public MusicXmlAdapter(List<MusicFile> musicFiles, OnAssignButtonClickListener listener) {
        this.musicFiles = musicFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_xml, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MusicFile musicFile = musicFiles.get(position);
        holder.fileNameTextView.setText(musicFile.getTitle());
        holder.assignButton.setOnClickListener(v -> {
            if (listener != null) {
                // update the correct title
                musicFile.setTitle(FileHelper.getTitleFromUri(holder.itemView.getContext(), musicFile.getUri()));
                listener.onAssignButtonClick(musicFile);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicFiles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        Button assignButton;

        ViewHolder(View view) {
            super(view);
            fileNameTextView = view.findViewById(R.id.music_xml_file_name);
            assignButton = view.findViewById(R.id.assign_button);
        }
    }
}