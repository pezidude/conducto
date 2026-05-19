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
 * MusicXmlAdapter
 * 
 * A configurable RecyclerView adapter for listing sheet music resources. 
 * It manages diverse UI states depending on the user's context (e.g., student vs. teacher).
 * 
 * Key Features:
 * 1. Staged Deletion: Supports a visual "Pending Delete" state for files.
 * 2. Role-based visibility: Dynamically shows/hides assignment and management buttons.
 * 3. AI Integration: Provides an entry point for Gemini-powered music descriptions.
 */
public class MusicXmlAdapter extends FirestoreRecyclerAdapter<MusicFile, MusicXmlAdapter.ViewHolder> {

    /** Callback for students to view AI-generated metadata. */
    private OnAiInfoClickListener aiInfoListener;
    
    /** Callback for teachers to assign specific files to students. */
    private OnAssignButtonClickListener assignListener;
    
    /** Callback for opening the sheet music player. */
    private OnItemClickListener itemClickListener;
    
    /** Callback for initiating file deletion. */
    private OnDeleteButtonClickListener deleteListener;
    
    /** Callback for editing the file's title. */
    private OnRenameListener renameListener;

    /** Display flag for teacher-specific controls (Assign/Delete). */
    private final boolean showButtons;

    /** Display flag for the Gemini AI info button. */
    private final boolean showAiButton;

    /** Local cache of document IDs that are marked for deletion but not yet saved. */
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
     * Initializes the adapter with specific UI capability flags.
     * @param options Firestore query configuration.
     * @param showButtons Enable administrative buttons.
     * @param showAiButton Enable the Gemini AI interface.
     */
    public MusicXmlAdapter(@NonNull FirestoreRecyclerOptions<MusicFile> options, boolean showButtons, boolean showAiButton) {
        super(options);
        this.showButtons = showButtons;
        this.showAiButton = showAiButton;
    }

    /**
     * Updates the list of items to be visually grayed out as "Pending Deletion".
     * @param pendingDeletions List of Firestore document IDs.
     */
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

    /**
     * Binds the MusicFile data and applies state-based UI transformations.
     */
    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull MusicFile model) {
        holder.fileNameTextView.setText(model.getTitle());
        String docId = getSnapshots().getSnapshot(position).getId();

        // Staged Delete Logic: If ID is in the pending list, apply strike-thru and transparency.
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

        // Theme Sync: Style the music file icon based on the current lesson's genre.
        com.example.conducto2.data.model.Lesson currentLesson = com.example.conducto2.data.manager.DataManager.getCurLesson();
        if (currentLesson != null) {
            int color = androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), currentLesson.getGenreColorResId());
            if (holder.iconTile.getBackground() != null) {
                holder.iconTile.getBackground().mutate().setTint(color);
            }
            holder.iconView.setImageResource(currentLesson.getGenreIconResId());
        }

        // Conditional Visibility: Control based on the flags provided in the constructor.
        holder.aiInfoButton.setVisibility(showAiButton ? View.VISIBLE : View.GONE);
        if (showButtons) {
            holder.assignButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            holder.assignButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        }

        // Interaction Delegation: Proxy clicks to the parent activity.
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && !isPendingDelete) itemClickListener.onItemClick(model);
        });
        holder.aiInfoButton.setOnClickListener(v -> {
            if (aiInfoListener != null) aiInfoListener.onAiInfoClick(model);
        });
        holder.assignButton.setOnClickListener(v -> {
            if (assignListener != null) assignListener.onAssignButtonClick(model);
        });
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteButtonClick(model, docId);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (renameListener != null && !isPendingDelete) {
                renameListener.onRename(model, docId);
                return true;
            }
            return false;
        });
    }

    /**
     * ViewHolder for MusicXML items. 
     * Encapsulates references to all management and interaction views.
     */
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

    // Listener setters...
    public void setOnAssignButtonClickListener(OnAssignButtonClickListener listener) { this.assignListener = listener; }
    public void setOnItemClickListener(OnItemClickListener listener) { this.itemClickListener = listener; }
    public void setOnDeleteButtonClickListener(OnDeleteButtonClickListener listener) { this.deleteListener = listener; }
    public void setOnRenameListener(OnRenameListener listener) { this.renameListener = listener; }
    public void setOnAiInfoClickListener(OnAiInfoClickListener listener) { this.aiInfoListener = listener; }
}