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

/**
 * ParticipantAdapter
 * 
 * A specialized RecyclerView adapter used for real-time presence monitoring during live lessons.
 * It displays a horizontal list of students in the class and visually distinguishes 
 * between those who are currently connected to the lesson and those who are offline.
 * 
 * This class implements binary image processing (Base64 decoding) and uses 
 * ColorMatrix filters to implement a grayscale "Offline" effect.
 */
public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    /** The full list of users in the class context. */
    private List<User> participants;

    /** The list of emails for students who have a currently active websocket/listener connection. */
    private List<String> connectedEmails;

    /**
     * Initializes the adapter with the participant list and initial connection state.
     * @param participants All users in the class.
     * @param connectedEmails Emails of students currently in the lesson.
     */
    public ParticipantAdapter(List<User> participants, List<String> connectedEmails) {
        this.participants = participants;
        this.connectedEmails = connectedEmails != null ? connectedEmails : new ArrayList<>();
    }

    /**
     * Updates the connection state based on Firestore presence tracking.
     * @param connectedEmails The new list of active student emails.
     */
    public void setConnectedEmails(List<String> connectedEmails) {
        this.connectedEmails = connectedEmails != null ? connectedEmails : new ArrayList<>();
        // Trigger UI refresh to update grayscale filters.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    /**
     * Binds user data and applies connection-based visual filters.
     */
    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        User user = participants.get(position);
        holder.tvName.setText(user.getFname());

        // Binary Processing: Decode the Base64 profile picture string into a Bitmap.
        if (user.getProfilePictureBase64() != null && !user.getProfilePictureBase64().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(user.getProfilePictureBase64(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivProfile.setImageBitmap(decodedByte);
            } catch (IllegalArgumentException e) {
                // Fallback to placeholder if decoding fails.
                holder.ivProfile.setImageResource(R.drawable.ic_person);
            }
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_person);
        }

        // Presence Logic: Check if the user is in the connected list.
        boolean isConnected = connectedEmails.contains(user.getEmail());
        if (isConnected) {
            // "Online" state: Normal colors and full opacity.
            holder.ivProfile.clearColorFilter();
            holder.ivProfile.setAlpha(1.0f);
            holder.tvName.setAlpha(1.0f);
        } else {
            // "Offline" state: Apply grayscale filter and reduce opacity.
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0); // 0 saturation converts image to grayscale.
            holder.ivProfile.setColorFilter(new ColorMatrixColorFilter(matrix));
            holder.ivProfile.setAlpha(0.4f);
            holder.tvName.setAlpha(0.4f);
        }
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    /**
     * ViewHolder for participant items.
     */
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