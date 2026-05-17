package com.example.conducto2.ui.lessons;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * StudentAssignmentAdapter
 * 
 * A specialized RecyclerView adapter used within the teacher's "Assign Student" dialog.
 * Unlike the real-time Firestore adapters, this class manages an in-memory list of users
 * and tracks selection states locally before they are persisted to the database.
 * 
 * It provides a high-fidelity list including profile pictures (decoded from Base64) 
 * and initials for student identification.
 */
public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    /** The full list of student users eligible for assignment. */
    private final List<User> students;

    /** Tracks the email addresses of students currently selected in the UI. */
    private final List<String> selectedEmails;

    /**
     * Initializes the adapter with a list of students and their current selection status.
     * @param students The source list of User objects.
     * @param initialSelectedEmails The emails of students already assigned to this part.
     */
    public StudentAssignmentAdapter(List<User> students, List<String> initialSelectedEmails) {
        this.students = students;
        this.selectedEmails = new ArrayList<>(initialSelectedEmails);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_assignment, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds student metadata to the UI and sets the checkbox state.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        // Logical check: compare email against the local selection list.
        holder.bind(student, selectedEmails.contains(student.getEmail()));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    /**
     * Retrieves the final list of selected emails to be saved to Firestore.
     * @return List of student email strings.
     */
    public List<String> getSelectedEmails() {
        return selectedEmails;
    }

    /**
     * ViewHolder for the student assignment item.
     * Implements local toggle logic for the selection state.
     */
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProfilePicture;
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvEmail;
        private final CheckBox cbAssigned;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.iv_profile_picture);
            tvInitials = itemView.findViewById(R.id.tv_initials);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            cbAssigned = itemView.findViewById(R.id.cb_assigned);

            // Toggle Logic: Update the local 'selectedEmails' list and refresh the item view.
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    User student = students.get(pos);
                    if (selectedEmails.contains(student.getEmail())) {
                        selectedEmails.remove(student.getEmail());
                    } else {
                        selectedEmails.add(student.getEmail());
                    }
                    notifyItemChanged(pos);
                }
            });
        }

        /**
         * Populates views and handles the decoding of Base64 profile images.
         * @param user The student data.
         * @param isSelected Whether the checkbox should be checked.
         */
        public void bind(User user, boolean isSelected) {
            String fullName = user.getFname() + " " + user.getLname();
            tvName.setText(fullName);
            tvEmail.setText(user.getEmail());
            cbAssigned.setChecked(isSelected);

            // UI Detail: Construct initials if name fields are populated.
            String initials = "";
            if (user.getFname() != null && !user.getFname().isEmpty()) {
                initials += user.getFname().substring(0, 1).toUpperCase();
            }
            if (user.getLname() != null && !user.getLname().isEmpty()) {
                initials += user.getLname().substring(0, 1).toUpperCase();
            }
            tvInitials.setText(initials);

            // Binary Processing: Decode profile picture from Base64 string to Android Bitmap.
            String base64Image = user.getProfilePictureBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProfilePicture.setImageBitmap(decodedByte);
                    ivProfilePicture.setVisibility(View.VISIBLE);
                    tvInitials.setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.e("StudentAssignmentAdapter", "Error decoding base64 image", e);
                    ivProfilePicture.setVisibility(View.GONE);
                    tvInitials.setVisibility(View.VISIBLE);
                }
            } else {
                ivProfilePicture.setVisibility(View.GONE);
                tvInitials.setVisibility(View.VISIBLE);
            }
        }
    }
}