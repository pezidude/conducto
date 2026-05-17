package com.example.conducto2.data.file;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

/**
 * FilePickerHelper
 * 
 * A specialized utility class that abstracts the complexity of the Android Activity Result API.
 * It provides a high-level interface for activities to request file selection from the system's 
 * document picker (SAF - Storage Access Framework).
 * 
 * This class is designed to be instantiated during the {@code onCreate()} phase of an Activity 
 * to ensure the result launcher is registered before the activity reaches the STARTED state.
 */
public class FilePickerHelper {

    /** The internal launcher responsible for managing the asynchronous activity result lifecycle. */
    private final ActivityResultLauncher<Intent> launcher;

    /** Callback listener assigned to receive the resulting Uri and filename. */
    private final OnFilePickedListener listener;

    /**
     * Interface definition for the file picking result callback.
     */
    public interface OnFilePickedListener {
        /**
         * Triggered when the user successfully selects a file.
         * @param fileUri The content Uri of the selected file.
         * @param fileName The display name of the selected file.
         */
        void onFilePicked(Uri fileUri, String fileName);
    }

    /**
     * Constructs a new FilePickerHelper and registers the internal result launcher.
     * 
     * @param activity The host activity (must be a subclass of AppCompatActivity).
     * @param fileio An instance of FileIO used to resolve the selected file's metadata.
     * @param listener The callback implementation to handle the result.
     */
    public FilePickerHelper(AppCompatActivity activity, FileIO fileio, OnFilePickedListener listener) {
        this.listener = listener;

        // Immediately register the result contract.
        // Registration must occur in onCreate() to comply with modern Android lifecycle standards.
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Logic: Verify user didn't cancel and data is non-null.
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            // Resolve the filename using the provided FileIO helper.
                            String name = fileio.getFileName(uri);
                            listener.onFilePicked(uri, name);
                        }
                    }
                }
        );
    }

    /**
     * Triggers the system document picker UI. 
     * Configured to request any file type ( * / * ) to ensure broad compatibility
     * with various MusicXML and MXL mime-type variations across different Android versions.
     */
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Note: MusicXML/MXL often have inconsistent MIME type registration on mobile.
        // Using */* ensures the picker doesn't filter out valid music resources.
        intent.setType("*/*");
        
        launcher.launch(intent);
    }
}