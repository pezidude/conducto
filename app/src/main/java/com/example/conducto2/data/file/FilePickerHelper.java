package com.example.conducto2.data.file;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class FilePickerHelper {

    /*
    * This class exposes an API for the activities for the File Picking functionality of android by
    * wrapping the Activity result contract of android.
    * */

    private final ActivityResultLauncher<Intent> launcher;
    private final OnFilePickedListener listener;

    // Sign this inteface for a callback when a file is picked
    public interface OnFilePickedListener {
        void onFilePicked(Uri fileUri, String fileName);
    }

    // call constructor in OnCreate of the activity so we don't miss the file pick event.
    public FilePickerHelper(AppCompatActivity activity, FileIO fileio, OnFilePickedListener listener) {
        this.listener = listener;


        // Register the launcher immediately
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String name = fileio.getFileName(uri);
                            listener.onFilePicked(uri, name);
                        }
                    }
                }
        );
    }

    /**
     * Call this method when the button is clicked
     */
    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // We use */* because Android often fails to recognize specific MusicXML mime types
        intent.setType("*/*");
        launcher.launch(intent);
    }
}
