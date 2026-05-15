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

public class LessonAdapter extends FirestoreRecyclerAdapter<Lesson, LessonAdapter.LessonViewHolder> {
    private OnItemClickListener listener;

    public LessonAdapter(@NonNull FirestoreRecyclerOptions<Lesson> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull LessonViewHolder holder, int position, @NonNull Lesson model) {
        // Use polymorphism to get specific behavior
        Lesson polymorphicLesson = Lesson.fromBase(model);
        holder.bind(polymorphicLesson);
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.lesson_item, parent, false);
        return new LessonViewHolder(view);
    }

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

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null && position < getSnapshots().size()) {
                    listener.onItemClick(getSnapshots().getSnapshot(position));
                }
            });
        }

        public void bind(Lesson lesson) {
            lessonTitle.setText(lesson.getTitle());
            
            // Show genre label if it's not the default
            String infoText = lesson.getInfo();
            if (!"General".equals(lesson.getGenreLabel())) {
                infoText = "[" + lesson.getGenreLabel() + "] " + infoText;
            }
            lessonInfo.setText(infoText);
            
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));

            // Apply polymorphic styling
            int color = ContextCompat.getColor(itemView.getContext(), lesson.getGenreColorResId());
            iconTile.getBackground().setTint(color);
            iconView.setImageResource(lesson.getGenreIconResId());
            iconView.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.white));

            // Gray out if archived
            if (lesson.isArchived()) {
                itemView.setAlpha(0.5f);
            } else {
                itemView.setAlpha(1.0f);
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}