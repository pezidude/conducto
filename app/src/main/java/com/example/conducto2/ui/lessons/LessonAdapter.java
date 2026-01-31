package com.example.conducto2.ui.lessons;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        holder.bind(model);
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

        public LessonViewHolder(@NonNull View itemView) {
            super(itemView);
            lessonTitle = itemView.findViewById(R.id.lesson_title);
            lessonInfo = itemView.findViewById(R.id.lesson_info);
            lessonDate = itemView.findViewById(R.id.lesson_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getSnapshots().getSnapshot(position));
                }
            });
        }

        public void bind(Lesson lesson) {
            lessonTitle.setText(lesson.getTitle());
            lessonInfo.setText(lesson.getInfo());
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));
        }
    }

    public interface OnItemClickListener {
        void onItemClick(DocumentSnapshot documentSnapshot);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}