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

public class StudentAssignmentAdapter extends RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder> {

    private final List<User> students;
    private final List<String> selectedEmails;

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

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        holder.bind(student, selectedEmails.contains(student.getEmail()));
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public List<String> getSelectedEmails() {
        return selectedEmails;
    }

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

        public void bind(User user, boolean isSelected) {
            String fullName = user.getFname() + " " + user.getLname();
            tvName.setText(fullName);
            tvEmail.setText(user.getEmail());
            cbAssigned.setChecked(isSelected);

            // Initials
            String initials = "";
            if (user.getFname() != null && !user.getFname().isEmpty()) {
                initials += user.getFname().substring(0, 1).toUpperCase();
            }
            if (user.getLname() != null && !user.getLname().isEmpty()) {
                initials += user.getLname().substring(0, 1).toUpperCase();
            }
            tvInitials.setText(initials);

            // Profile Picture
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
