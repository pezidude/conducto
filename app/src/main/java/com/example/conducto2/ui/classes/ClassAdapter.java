package com.example.conducto2.ui.classes;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

/**
 * ClassAdapter
 * 
 * A specialized RecyclerView adapter for managing and displaying a user's classroom list.
 * It leverages {@link FirestoreRecyclerAdapter} to synchronize with the database automatically.
 * 
 * It supports:
 * 1. Real-time Synchronization: Updates the list as classrooms are added or modified in Firestore.
 * 2. Contextual Navigation: Launching ClassActivity with the selected classroom context.
 */
public class ClassAdapter extends FirestoreRecyclerAdapter<Class, ClassAdapter.ClassViewHolder> {

    /** DAO for fetching teacher profiles. */
    private final FirestoreManager firestoreManager = new FirestoreManager();

    /**
     * Constructs a new ClassAdapter with Firestore options.
     * @param options Configuration for the Firestore query and model mapping.
     */
    public ClassAdapter(@NonNull FirestoreRecyclerOptions<Class> options) {
        super(options);
    }

    /**
     * Binds classroom metadata and configures the navigation click listener.
     */
    @Override
    protected void onBindViewHolder(@NonNull ClassViewHolder holder, int position, @NonNull Class model) {
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
            
            // Teacher Name Resolution Logic:
            String ownerEmail = aClass.getOwnerEmail();
            classTeacher.setText("Loading...");
            firestoreManager.getUserByEmail(ownerEmail, user -> {
                if (user != null) {
                    String fullName = user.getFname() + " " + user.getLname();
                    // Only update if the holder is still representing the same class (prevent async race conditions).
                    if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                        Class currentItem = getItem(getAdapterPosition());
                        if (currentItem != null && ownerEmail.equals(currentItem.getOwnerEmail())) {
                            classTeacher.setText(fullName);
                        }
                    }
                } else {
                    classTeacher.setText("Unknown Teacher");
                }
            });
        }
    }
}