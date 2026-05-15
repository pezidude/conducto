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

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PersonViewHolder> {

    private List<User> users = new ArrayList<>();
    private String teacherEmail;

    public PeopleAdapter(String teacherEmail) {
        this.teacherEmail = teacherEmail;
    }

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

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user, user.getEmail().equals(teacherEmail));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

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

        public void bind(User user, boolean isTeacher) {
            String fullName = user.getFname() + " " + user.getLname();
            tvName.setText(fullName);
            tvEmail.setText(user.getEmail());

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
                    Log.e("PeopleAdapter", "Error decoding base64 image", e);
                    ivProfilePicture.setVisibility(View.GONE);
                    tvInitials.setVisibility(View.VISIBLE);
                }
            } else {
                ivProfilePicture.setVisibility(View.GONE);
                tvInitials.setVisibility(View.VISIBLE);
            }

            // Styling for teacher
            if (isTeacher) {
                tvRoleBadge.setVisibility(View.VISIBLE);
                // Highlight the card slightly
                card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.brand_accent));
                card.setStrokeWidth(4);
                // You could also change the background or add a "Teacher" badge
            } else {
                tvRoleBadge.setVisibility(View.GONE);
                card.setStrokeWidth(0);
            }
        }
    }
}
