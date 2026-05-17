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

/**
 * ClassAdapter
 * 
 * A specialized RecyclerView adapter for managing and displaying a user's classroom list.
 * Unlike standard real-time adapters, this class implements manual in-memory filtering 
 * and sorting logic to provide a responsive search experience.
 * 
 * It supports:
 * 1. Dynamic Search: Filtering classrooms by name in real-time.
 * 2. Alphabetical Sorting: Toggling between original database order and A-Z sorting.
 * 3. Contextual Navigation: Launching ClassActivity with the selected classroom context.
 */
public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    /** The complete master list of classrooms retrieved from the database. */
    private List<Class> allItems = new ArrayList<>();

    /** The subset of classrooms currently visible to the user after filtering and sorting. */
    private List<Class> displayedItems = new ArrayList<>();

    /**
     * Default constructor for ClassAdapter.
     */
    public ClassAdapter() {
    }

    /**
     * Updates the master dataset and resets any active filters.
     * @param items The new list of Class objects from Firestore.
     */
    public void updateData(List<Class> items) {
        this.allItems = new ArrayList<>(items);
        filter("", false); // Reset filter to show all new items
    }

    /**
     * Performs an in-memory search and sort operation on the classroom list.
     * Uses Java 8 Streams for efficient data transformation.
     * 
     * @param query The search string to match against classroom names.
     * @param isSortedByName True if the results should be sorted alphabetically (A-Z).
     */
    public void filter(String query, boolean isSortedByName) {
        String lowerQuery = query.toLowerCase().trim();
        
        displayedItems = allItems.stream()
                .filter(item -> item.getName().toLowerCase().contains(lowerQuery))
                .sorted((c1, c2) -> {
                    if (isSortedByName) {
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                    return 0; // Maintain original order if sorting is disabled
                })
                .collect(Collectors.toList());
        
        // Notify the RecyclerView to refresh the UI with the filtered subset.
        notifyDataSetChanged();
    }

    /**
     * Retrieves the classroom model at a specific visible position.
     * @param position The position in the filtered list.
     * @return The Class object.
     */
    public Class getItem(int position) {
        return displayedItems.get(position);
    }

    @Override
    public int getItemCount() {
        return displayedItems.size();
    }

    /**
     * Binds classroom metadata and configures the navigation click listener.
     */
    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        Class model = displayedItems.get(position);
        holder.bind(model);
        
        // Navigation Logic: Update global context and launch the detail hub.
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

    /**
     * ViewHolder for classroom items.
     * Binds text fields for classroom name, description, and teacher name.
     */
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

        /**
         * Populates the view elements with specific classroom data.
         * @param aClass The classroom model.
         */
        public void bind(Class aClass) {
            classTitle.setText(aClass.getName());
            classInfo.setText(aClass.getDescription());
            classTeacher.setText(aClass.getTeacherName());
        }
    }
}