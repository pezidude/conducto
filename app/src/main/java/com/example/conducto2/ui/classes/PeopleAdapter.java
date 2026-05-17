package com.example.conducto2.ui.classes;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.User;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * PeopleAdapter
 * 
 * a specialized RecyclerView adapter for rendering the participants of a classroom.
 * This class handles the complex visual representation of users, including:
 * 1. Base64 decoding of profile pictures into Android Bitmaps.
 * 2. Generating dynamic initial-based avatars for users without profile images.
 * 3. Role-based styling to visually distinguish the teacher from students.
 */
public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PersonViewHolder> {

    /** The current list of users to display in the roster. */
    private List<User> users = new ArrayList<>();

    /** The unique email of the classroom's instructor, used for visual highlighting. */
    private String teacherEmail;

    /**
     * Constructs a new PeopleAdapter.
     * @param teacherEmail The email address of the class teacher.
     */
    public PeopleAdapter(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

    /**
     * Updates the dataset of the adapter.
     * @param users The new list of users.
     */
    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(view);
    }

    /**
     * Binds a user model to a ViewHolder and determines if they should receive teacher styling.
     */
    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        User user = users.get(position);
        // Logical check: Identify if the current user is the classroom teacher.
        holder.bind(user, user.getEmail().equals(teacherEmail));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * ViewHolder for person items.
     * Encapsulates the UI logic for profile rendering and role-based highlighting.
     */
    static class PersonViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView card;
        private ImageView ivProfilePicture;
        private TextView tvInitials;
        private View avatarBackground;
        private TextView tvName;
        private TextView tvEmail;
        private TextView tvRoleBadge;

        public PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivProfilePicture = itemView.findViewById(R.id.iv_profile_picture);
            tvInitials = itemView.findViewById(R.id.tv_initials);
            avatarBackground = itemView.findViewById(R.id.avatar_background);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRoleBadge = itemView.findViewById(R.id.tv_role_badge);
        }

        /**
         * Populates views and handles the conditional decoding of image data.
         * @param user The user data model.
         * @param isTeacher True if this user should be styled as an instructor.
         */
        public void bind(User user, boolean isTeacher) {
            String fullName = user.getFname() + " " + user.getLname();
            tvName.setText(fullName);
            tvEmail.setText(user.getEmail());

            // --- Initials Processing ---
            // Construct initials from first and last name for placeholder avatars.
            String initials = "";
            if (user.getFname() != null && !user.getFname().isEmpty()) {
                initials += user.getFname().substring(0, 1).toUpperCase();
            }
            if (user.getLname() != null && !user.getLname().isEmpty()) {
                initials += user.getLname().substring(0, 1).toUpperCase();
            }
            tvInitials.setText(initials);

            // --- Binary Image Decoding ---
            // Decode profile picture from Base64 string into a high-fidelity Bitmap.
            String base64Image = user.getProfilePictureBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    ivProfilePicture.setImageBitmap(decodedByte);
                    ivProfilePicture.setVisibility(View.VISIBLE);
                    tvInitials.setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.e("PeopleAdapter", "Error decoding base64 image", e);
                    // Fallback to initials if decoding fails.
                    ivProfilePicture.setVisibility(View.GONE);
                    tvInitials.setVisibility(View.VISIBLE);
                }
            } else {
                ivProfilePicture.setVisibility(View.GONE);
                tvInitials.setVisibility(View.VISIBLE);
            }

            // --- Role-Based Styling ---
            // Highlight the instructor with a specialized badge and card border.
            if (isTeacher) {
                tvRoleBadge.setVisibility(View.VISIBLE);
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.brand_accent));
                card.setStrokeWidth(4);
            } else {
                tvRoleBadge.setVisibility(View.GONE);
                card.setStrokeWidth(0);
            }
        }
    }
}