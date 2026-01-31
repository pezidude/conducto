package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.ui.lessons.ClassActivity;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class ClassAdapter extends FirestoreRecyclerAdapter<Class, ClassAdapter.ClassViewHolder> {

    public ClassAdapter(@NonNull FirestoreRecyclerOptions<Class> options) {
        super(options);
    }

    @Override
    protected void onBindViewHolder(@NonNull ClassViewHolder holder, int position, @NonNull Class model) {
        holder.bind(model);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ClassActivity.class);
            model.setId(getSnapshots().getSnapshot(position).getId());
            intent.putExtra("class_id", model.getId());
            v.getContext().startActivity(intent);
        });
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.class_item, parent, false);
        return new ClassViewHolder(view);
    }

    class ClassViewHolder extends RecyclerView.ViewHolder {
        private TextView classTitle;
        private TextView classInfo;
        private TextView classTeacher;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            classTitle = itemView.findViewById(R.id.class_title);
            classInfo = itemView.findViewById(R.id.class_info);
            classTeacher = itemView.findViewById(R.id.class_teacher);
        }

        public void bind(Class aClass) {
            classTitle.setText(aClass.getName());
            classInfo.setText(aClass.getDescription());
            classTeacher.setText(aClass.getTeacherName());
        }
    }
}