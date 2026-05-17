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
 * ProfileActivity
 * 
 * This activity provides users with the ability to view and modify their personal profiles. 
 * It manages biographical information (Display Name, Bio) and profile iconography.
 * 
 * Key Roles:
 * 1. Image Lifecycle Management: Handles capturing photos via camera or selecting from the gallery.
 * 2. Binary Data Processing: Resizes images and converts them to Base64 strings for NoSQL storage.
 * 3. Permission Orchestration: Manages runtime camera permission requests.
 * 4. Data Sync: Ensures changes are persisted both to the global DataManager cache and Firestore.
 */
public class ProfileActivity extends BaseDrawerActivity {

    /** Identifier for logging. */
    private static final String TAG = "ProfileActivity";

    /** Maximum dimension (width or height) for profile images to ensure efficient storage and loading. */
    private static final int MAX_IMAGE_SIZE = 400; 

    /** Displays the user's profile picture. */
    private ImageView ivProfilePicture;

    /** Text placeholder shown when a profile image is missing. */
    private TextView tvAvatarInitials;

    /** Non-editable display for the user's full name. */
    private TextView tvProfileName;

    /** Non-editable display for the user's system role (Student/Teacher). */
    private TextView tvProfileRole;

    /** Editable field for modifying the display name. */
    private EditText etDisplayName;

    /** Editable field for the user's biography. */
    private EditText etBio;

    /** Button to trigger the persistence logic. */
    private MaterialButton btnSaveProfile;

    /** Interaction area that triggers the image source selection menu. */
    private View flAvatarContainer;

    /** Feedback label for asynchronous operations. */
    private TextView tvStatusMessage;

    /** Local copy of the user model currently being edited. */
    private User currentUser;

    /** Activity Result Launcher for selecting an existing image from the system gallery. */
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    processSelectedImage(uri);
                }
            }
    );

    /** Activity Result Launcher for capturing a new photo via the system camera. */
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            bitmap -> {
                if (bitmap != null) {
                    processBitmap(bitmap);
                }
            }
    );

    /** Activity Result Launcher for requesting camera permissions at runtime. */
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

    /**
     * Binds UI components and sets up interaction listeners.
     */
    private void initViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        etDisplayName = findViewById(R.id.etDisplayName);
        etBio = findViewById(R.id.etBio);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        flAvatarContainer = findViewById(R.id.flAvatarContainer);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);

        flAvatarContainer.setOnClickListener(v -> showImageSourceMenu(v));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    /**
     * Displays a contextual menu allowing the user to choose between Camera and Gallery sources.
     * @param anchor The view to attach the menu to.
     */
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

    /**
     * Verifies camera permission state. Launches the camera if granted, otherwise requests permission.
     */
    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(null);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Configures the database manager to handle and provide feedback for profile update operations.
     */
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

    /**
     * Retrieves the current user data from the global cache or Firestore.
     */
    private void loadUserData() {
        currentUser = DataManager.getUserInstance();
        if (currentUser != null) {
            updateUI();
        } else {
            // Loading Track: Trigger fetch if cache is cold.
            tvStatusMessage.setText("Loading user data...");
            tvStatusMessage.setVisibility(View.VISIBLE);
            firestoreManager.getUser(user -> {
                tvStatusMessage.setVisibility(View.GONE);
                currentUser = user;
                updateUI();
            });
        }
    }

    /**
     * Synchronizes the UI elements with the current state of the currentUser model.
     * Includes logic for generating initials and decoding Base64 image data.
     */
    private void updateUI() {
        if (currentUser == null) return;

        String fullName = currentUser.getFname() + " " + currentUser.getLname();
        tvProfileName.setText(fullName);
        tvProfileRole.setText(currentUser.getUserType());
        etDisplayName.setText(fullName);
        etBio.setText(currentUser.getDescription());

        // --- Avatar Initials Logic ---
        String initials = "";
        if (currentUser.getFname() != null && !currentUser.getFname().isEmpty()) {
            initials += currentUser.getFname().substring(0, 1).toUpperCase();
        }
        if (currentUser.getLname() != null && !currentUser.getLname().isEmpty()) {
            initials += currentUser.getLname().substring(0, 1).toUpperCase();
        }
        tvAvatarInitials.setText(initials);

        // --- Profile Picture Decoding ---
        String base64Image = currentUser.getProfilePictureBase64();
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                // Decode the String from Firestore into a native Bitmap.
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

    /**
     * Retrieves an image from the provided gallery Uri and passes it to the processing pipeline.
     */
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

    /**
     * The core image processing pipeline. 
     * Resizes the bitmap to the max allowed dimension, compresses it, 
     * encodes it to Base64, and initiates the cloud upload.
     * 
     * Sequential Steps:
     * 1. Scaling: Reduce dimensions to conserve database space.
     * 2. Serialization: Compress to JPEG and encode to String.
     * 3. Sync: Update local UI, local cache, and remote database.
     */
    private void processBitmap(Bitmap bitmap) {
        try {
            tvStatusMessage.setText("Uploading image...");
            tvStatusMessage.setVisibility(View.VISIBLE);

            // Step 1: Scale the raw bitmap to fit within MAX_IMAGE_SIZE.
            Bitmap resizedBitmap = resizeBitmap(bitmap, MAX_IMAGE_SIZE);

            // Step 2: Convert to compressed Base64 string.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Step 3: Immediate UI feedback.
            ivProfilePicture.setImageBitmap(resizedBitmap);
            ivProfilePicture.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);

            // Step 4: Persist to remote storage.
            firestoreManager.updateUserProfilePicture(currentUser.getEmail(), base64Image);

            // Step 5: Persist to global memory cache.
            currentUser.setProfilePictureBase64(base64Image);
            DataManager.setUser(currentUser);

        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap", e);
            tvStatusMessage.setVisibility(View.GONE);
            showMessageDialog("Processing Error", "Failed to process the image.");
        }
    }

    /**
     * Logic for aspect-ratio-aware resizing of bitmaps.
     */
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

    /**
     * Validates and saves the textual profile metadata.
     * Handles splitting the display name back into first/last name components.
     */
    private void saveProfile() {
        if (currentUser == null) return;

        String displayName = etDisplayName.getText().toString().trim();
        String description = etBio.getText().toString().trim();

        // Sequential Logic: Parse name components from a single input field.
        String[] parts = displayName.split("\\s+", 2);
        if (parts.length > 0) currentUser.setFname(parts[0]);
        if (parts.length > 1) currentUser.setLname(parts[1]);

        currentUser.setDescription(description);

        tvStatusMessage.setText("Saving profile...");
        tvStatusMessage.setVisibility(View.VISIBLE);

        // Synchronized Commit: Update both database and local cache.
        firestoreManager.updateUser(currentUser);
        DataManager.setUser(currentUser);
    }

    private void showMessageDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    finish();
                })
                .show();
    }
}