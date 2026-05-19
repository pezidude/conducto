package com.example.conducto2.ui.lessons;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.conducto2.R;
import com.example.conducto2.data.model.Lesson;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * LessonAdapter
 * 
 * A specialized RecyclerView adapter that provides a real-time reactive list of lessons
 * within a class. It leverages {@link FirestoreRecyclerAdapter} to synchronize 
 * with the database automatically.
 * 
 * This adapter implements polymorphic UI rendering, adjusting the visual style (colors, icons)
 * based on the lesson's genre and real-time "Live" or "Archived" status.
 */
public class LessonAdapter extends FirestoreRecyclerAdapter<Lesson, LessonAdapter.LessonViewHolder> {

    /** Listener for handling click events on individual lesson items. */
    private OnItemClickListener listener;

    /**
     * Constructs a new LessonAdapter with Firestore options.
     * @param options Configuration for the Firestore query and model mapping.
     */
    public LessonAdapter(@NonNull FirestoreRecyclerOptions<Lesson> options) {
        super(options);
    }

    /**
     * Binds a lesson document to a ViewHolder. 
     * Uses the polymorphic factory {@link Lesson#fromBase(Lesson)} to ensure genre-specific 
     * UI resources are available during the binding process.
     */
    @Override
    protected void onBindViewHolder(@NonNull LessonViewHolder holder, int position, @NonNull Lesson model) {
        // The model is already the correct polymorphic subclass thanks to the SnapshotParser in the RecyclerOptions.
        holder.bind(model);
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lesson_item, parent, false);
        return new LessonViewHolder(view);
    }

    /**
     * ViewHolder for lesson items. 
     * Implements the internal binding logic and handles click propagation to the parent.
     */
    class LessonViewHolder extends RecyclerView.ViewHolder {
        private TextView lessonTitle;
        private TextView lessonInfo;
        private TextView lessonDate;
        private View iconTile;
        private ImageView iconView;

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            lessonTitle = itemView.findViewById(R.id.lesson_title);
            lessonInfo = itemView.findViewById(R.id.lesson_info);
            lessonDate = itemView.findViewById(R.id.lesson_date);
            iconTile = itemView.findViewById(R.id.lesson_icon_tile);
            iconView = itemView.findViewById(R.id.lesson_icon);

            // Configure click listener to return the full document snapshot for context-aware navigation.
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && position < getSnapshots().size()) {
                    listener.onItemClick(getSnapshots().getSnapshot(position));
                }
            });
        }

        /**
         * Populates the UI elements with lesson data and applies conditional styling.
         * @param lesson The lesson model to bind.
         */
        public void bind(Lesson lesson) {
            lessonTitle.setText(lesson.getTitle());

            // Visual Logic: Highlight live lessons with high-contrast red.
            if (lesson.isLive()) {
                lessonTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.red_600));
            } else {
                lessonTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }
            
            // UI Enhancement: Prefix info text with the genre label if present.
            String infoText = lesson.getInfo();
            if (!lesson.getGenreLabel().isEmpty()) {
                infoText = "[" + lesson.getGenreLabel() + "] " + infoText;
            }
            lessonInfo.setText(infoText);
            
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));

            // Polymorphic Styling: Determine primary color and icon based on genre or live state.
            int color = ContextCompat.getColor(itemView.getContext(), 
                lesson.isLive() ? R.color.red_600 : lesson.getGenreColorResId());
            
            // Mutate the background drawable to apply tint without affecting other instances of the same resource.
            if (iconTile.getBackground() != null) {
                iconTile.getBackground().mutate().setTint(color);
            }
            
            if (lesson.isLive()) {
                iconView.setImageResource(R.drawable.ic_live_dot);
            } else {
                iconView.setImageResource(lesson.getGenreIconResId());
            }
            iconView.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.white));

            // State Logic: Dim the entire item if it has been archived to indicate completion.
            if (lesson.isArchived()) {
                itemView.setAlpha(0.5f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
    }

    /** Interface definition for lesson item click callbacks. */
    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot);
    }

    /**
     * Sets the click listener for the adapter.
     * @param listener The implementation to handle clicks.
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}