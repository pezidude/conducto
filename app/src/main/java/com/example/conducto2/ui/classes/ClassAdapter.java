package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<Class> allItems = new ArrayList<>();
    private List<Class> displayedItems = new ArrayList<>();

    public ClassAdapter() {
    }

    public void updateData(List<Class> items) {
        this.allItems = new ArrayList<>(items);
        filter("", false); // Reset filter
    }

    public void filter(String query, boolean isSortedByName) {
        String lowerQuery = query.toLowerCase().trim();
        
        displayedItems = allItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerQuery))
                .sorted((c1, c2) -> {
                    if (isSortedByName) {
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        
        notifyDataSetChanged();
    }

    public Class getItem(int position) {
        return displayedItems.get(position);
    }

    @Override
    public int getItemCount() {
        return displayedItems.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        Class model = displayedItems.get(position);
        holder.bind(model);
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ClassActivity.class);
            DataManager.setCurClass(model);
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