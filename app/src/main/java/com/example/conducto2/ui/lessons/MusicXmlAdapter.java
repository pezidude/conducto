package com.example.conducto2.ui.lessons;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.MusicFile;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying MusicXML files using FirestoreRecyclerAdapter.
 * This adapter connects to a Firestore collection or query for real-time updates.
 */
public class MusicXmlAdapter extends FirestoreRecyclerAdapter<MusicFile, MusicXmlAdapter.ViewHolder> {

    private OnAssignButtonClickListener assignListener;
    private OnItemClickListener itemClickListener;
    private OnDeleteButtonClickListener deleteListener;
    private OnRenameListener renameListener;
    private OnAiInfoClickListener aiInfoListener;
    private final boolean showButtons;
    private final boolean showAiButton;
    private List<String> pendingDeletions = new ArrayList<>();

    public interface OnAssignButtonClickListener {
        void onAssignButtonClick(MusicFile musicFile);
    }

    public interface OnItemClickListener {
        void onItemClick(MusicFile musicFile);
    }

    public interface OnDeleteButtonClickListener {
        void onDeleteButtonClick(MusicFile musicFile, String documentId);
    }

    public interface OnRenameListener {
        void onRename(MusicFile musicFile, String documentId);
    }

    public interface OnAiInfoClickListener {
        void onAiInfoClick(MusicFile musicFile);
    }

    /**
     * Constructor for MusicXmlAdapter.
     *
     * @param options      FirestoreRecyclerOptions for MusicFile.
     * @param showButtons  Whether to show Assign and Delete buttons (e.g., true for teachers in edit mode).
     * @param showAiButton Whether to show the AI Info button.
     */
    public MusicXmlAdapter(@NonNull FirestoreRecyclerOptions<MusicFile> options, boolean showButtons, boolean showAiButton) {
        super(options);
        this.showButtons = showButtons;
        this.showAiButton = showAiButton;
    }

    /**
     * Constructor for MusicXmlAdapter with default showAiButton = true.
     *
     * @param options     FirestoreRecyclerOptions for MusicFile.
     * @param showButtons Whether to show Assign and Delete buttons.
     */
    public MusicXmlAdapter(@NonNull FirestoreRecyclerOptions<MusicFile> options, boolean showButtons) {
        this(options, showButtons, true);
    }

    public void setOnAssignButtonClickListener(OnAssignButtonClickListener listener) {
        this.assignListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) {
        this.deleteListener = listener;
    }

    public void setOnRenameListener(OnRenameListener listener) {
        this.renameListener = listener;
    }

    public void setOnAiInfoClickListener(OnAiInfoClickListener listener) {
        this.aiInfoListener = listener;
    }

    public void setPendingDeletions(List<String> pendingDeletions) {
        this.pendingDeletions = pendingDeletions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_music_xml, parent, false);
        return new ViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull MusicFile model) {
        holder.fileNameTextView.setText(model.getTitle());
        String docId = getSnapshots().getSnapshot(position).getId();

        boolean isPendingDelete = pendingDeletions.contains(docId);
        
        if (isPendingDelete) {
            holder.itemView.setAlpha(0.4f);
            holder.fileNameTextView.setPaintFlags(holder.fileNameTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.assignButton.setEnabled(false);
            holder.aiInfoButton.setEnabled(false);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.fileNameTextView.setPaintFlags(holder.fileNameTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.assignButton.setEnabled(true);
            holder.aiInfoButton.setEnabled(true);
        }

        // Apply theme-based styling to the icon
        com.example.conducto2.data.model.Lesson currentLesson = com.example.conducto2.data.manager.DataManager.getCurLesson();
        if (currentLesson != null) {
            com.example.conducto2.data.model.Lesson polyLesson = com.example.conducto2.data.model.Lesson.fromBase(currentLesson);
            int color = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), polyLesson.getGenreColorResId());
            
            if (holder.iconTile.getBackground() != null) {
                holder.iconTile.getBackground().mutate().setTint(color);
            }
            holder.iconView.setImageResource(polyLesson.getGenreIconResId());
            holder.iconView.setColorFilter(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        }

        holder.aiInfoButton.setVisibility(showAiButton ? View.VISIBLE : View.GONE);
        holder.aiInfoButton.setOnClickListener(v -> {
            if (aiInfoListener != null) {
                aiInfoListener.onAiInfoClick(model);
            }
        });

        if (showButtons) {
            holder.assignButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.assignButton.setOnClickListener(v -> {
                if (assignListener != null) {
                    assignListener.onAssignButtonClick(model);
                }
            });
            holder.deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDeleteButtonClick(model, docId);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (renameListener != null && !isPendingDelete) {
                    renameListener.onRename(model, docId);
                    return true;
                }
                return false;
            });
        } else {
            holder.assignButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && !isPendingDelete) {
                itemClickListener.onItemClick(model);
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        Button assignButton;
        ImageButton deleteButton;
        ImageButton aiInfoButton;
        View iconTile;
        ImageView iconView;

        ViewHolder(View view) {
            super(view);
            fileNameTextView = view.findViewById(R.id.music_xml_file_name);
            assignButton = view.findViewById(R.id.assign_button);
            deleteButton = view.findViewById(R.id.delete_button);
            aiInfoButton = view.findViewById(R.id.ai_info_button);
            iconTile = view.findViewById(R.id.music_file_icon_tile);
            iconView = view.findViewById(R.id.music_file_icon);
        }
    }
}
