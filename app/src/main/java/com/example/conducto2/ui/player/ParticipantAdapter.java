package com.example.conducto2.ui.player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private List<User> participants;
    private List<String> connectedEmails;

    public ParticipantAdapter(List<User> participants, List<String> connectedEmails) {
        this.participants = participants;
        this.connectedEmails = connectedEmails != null ? connectedEmails : new ArrayList<>();
    }

    public void setConnectedEmails(List<String> connectedEmails) {
        this.connectedEmails = connectedEmails != null ? connectedEmails : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        User user = participants.get(position);
        holder.tvName.setText(user.getFname());

        // Decode profile picture
        if (user.getProfilePictureBase64() != null && !user.getProfilePictureBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(user.getProfilePictureBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivProfile.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                holder.ivProfile.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_person);
        }

        // Apply visual state based on connection
        boolean isConnected = connectedEmails.contains(user.getEmail());
        if (isConnected) {
            holder.ivProfile.clearColorFilter();
            holder.ivProfile.setAlpha(1.0f);
            holder.tvName.setAlpha(1.0f);
        } else {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            holder.ivProfile.setColorFilter(new ColorMatrixColorFilter(matrix));
            holder.ivProfile.setAlpha(0.4f);
            holder.tvName.setAlpha(0.4f);
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_participant_profile);
            tvName = itemView.findViewById(R.id.tv_participant_name);
        }
    }
}
