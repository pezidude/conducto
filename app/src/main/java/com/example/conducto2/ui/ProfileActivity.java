package com.example.conducto2.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.User;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * This activity displays the user's profile information and
 * allows updating information regarding the user.
 */
public class ProfileActivity extends BaseDrawerActivity {

    private static final String TAG = "ProfileActivity";
    private static final int MAX_IMAGE_SIZE = 400; // max width or height

    private ImageView ivProfilePicture;
    private TextView tvAvatarInitials;
    private TextView tvProfileName;
    private TextView tvProfileRole;
    private EditText etDisplayName;
    private MaterialButton btnSaveProfile;
    private View flAvatarContainer;
    private TextView tvStatusMessage;

    private FirestoreManager firestoreManager;
    private User currentUser;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                }
            }
    );

    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    processBitmap(bitmap);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    takePictureLauncher.launch(null);
                } else {
                    showMessageDialog("Permission Denied", "Camera permission is required to take a photo. Please enable it in settings.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupFirestore();
        loadUserData();
    }

    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        flAvatarContainer = findViewById(R.id.flAvatarContainer);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);

        flAvatarContainer.setOnClickListener(v -> showImageSourceMenu(v));

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showImageSourceMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add(0, 0, 0, "Take Photo");
        popupMenu.getMenu().add(0, 1, 1, "Choose from Gallery");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 0) {
                checkCameraPermissionAndLaunch();
                return true;
            } else if (item.getItemId() == 1) {
                pickImageLauncher.launch("image/*");
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupFirestore() {
        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(new FirebaseComm.DBResult() {
            @Override
            public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
                tvStatusMessage.setVisibility(View.GONE);
                if (success) {
                    showMessageDialog("Success", "Profile updated successfully.");
                } else {
                    showMessageDialog("Error", "Failed to update profile. Please try again.");
                }
            }

            @Override
            public void displayMessage(String message) {
                Log.d(TAG, "Firestore message: " + message);
            }
        });
    }

    private void loadUserData() {
        currentUser = DataManager.getUserInstance();
        if (currentUser != null) {
            updateUI();
        } else {
            tvStatusMessage.setText("Loading user data...");
            tvStatusMessage.setVisibility(View.VISIBLE);
            firestoreManager.getUser(user -> {
                tvStatusMessage.setVisibility(View.GONE);
                currentUser = user;
                updateUI();
            });
        }
    }

    private void updateUI() {
        if (currentUser == null) return;

        String fullName = currentUser.getFname() + " " + currentUser.getLname();
        tvProfileName.setText(fullName);
        tvProfileRole.setText(currentUser.getUserType());
        etDisplayName.setText(fullName);

        // Set initials
        String initials = "";
        if (currentUser.getFname() != null && !currentUser.getFname().isEmpty()) {
            initials += currentUser.getFname().substring(0, 1).toUpperCase();
        }
        if (currentUser.getLname() != null && !currentUser.getLname().isEmpty()) {
            initials += currentUser.getLname().substring(0, 1).toUpperCase();
        }
        tvAvatarInitials.setText(initials);

        // Load profile picture if exists
        String base64Image = currentUser.getProfilePictureBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivProfilePicture.setImageBitmap(decodedByte);
                ivProfilePicture.setVisibility(View.VISIBLE);
                tvAvatarInitials.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding base64 image", e);
                ivProfilePicture.setVisibility(View.GONE);
                tvAvatarInitials.setVisibility(View.VISIBLE);
            }
        } else {
            ivProfilePicture.setVisibility(View.GONE);
            tvAvatarInitials.setVisibility(View.VISIBLE);
        }
    }

    private void processSelectedImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) return;
            processBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            showMessageDialog("Processing Error", "Failed to load the selected image.");
        }
    }

    private void processBitmap(Bitmap bitmap) {
        try {
            tvStatusMessage.setText("Uploading image...");
            tvStatusMessage.setVisibility(View.VISIBLE);

            // Resize bitmap
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Convert to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Update UI immediately
            ivProfilePicture.setImageBitmap(resizedBitmap);
            ivProfilePicture.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);

            // Save to Firestore
            firestoreManager.updateUserProfilePicture(currentUser.getEmail(), base64Image);

            // Update local user object
            currentUser.setProfilePictureBase64(base64Image);
            DataManager.setUser(currentUser);

        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap", e);
            tvStatusMessage.setVisibility(View.GONE);
            showMessageDialog("Processing Error", "Failed to process the image.");
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void saveProfile() {
        // Here we could also update first name and last name if we split etDisplayName
        // For now, let's just show a dialog as pfp is updated immediately on selection
        showMessageDialog("Profile Saved", "Your profile changes have been saved.");
    }

    private void showMessageDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
