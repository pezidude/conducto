package com.example.conducto2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.file.FilePickerHelper;
import com.example.conducto2.ui.player.MIDIPlayerActivity;

/**
 * This activity is for testing purposes, specifically for selecting and viewing sheet music from a file.
 * It allows the user to pick a file, and then view it in the {@link MIDIPlayerActivity}.
 */
public class TestActivity extends AppCompatActivity implements FilePickerHelper.OnFilePickedListener {

    /** A card view that the user can click to select a file. */
    private View cardSelectFile;
    /** A button to view the selected sheet music. */
    private Button btnViewSheetMusic;
    /** A text view to display the name of the selected file. */
    private TextView tvFilename;
    /** An instance of FileIO for handling file operations. */
    private FileIO fileio;
    /** An instance of FilePickerHelper to handle file selection. */
    private FilePickerHelper picker;
    /** The URI of the file selected by the user. */
    private Uri selectedFileUri;

    /**
     * Initializes the activity, toolbar, file helpers, views, and listeners.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileio = new FileIO(this);
        picker = new FilePickerHelper(this, fileio, this);

        initViews();
        setupListeners();
    }

    /**
     * Initializes the UI components.
     */
    private void initViews() {
        cardSelectFile = findViewById(R.id.cardSelectFile);
        tvFilename = findViewById(R.id.tvFilename);
        btnViewSheetMusic = findViewById(R.id.btnViewSheetMusic);
    }

    /**
     * Sets up click listeners for the file selection card and the view sheet music button.
     */
    private void setupListeners() {
        if (cardSelectFile != null) {
            cardSelectFile.setOnClickListener(v -> selectFile());
        }

        btnViewSheetMusic.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                Intent intent = new Intent(TestActivity.this, MIDIPlayerActivity.class);
                intent.putExtra("fileUri", selectedFileUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the file picker to allow the user to select a file.
     */
    private void selectFile() {
        picker.openFilePicker();
    }

    /**
     * A callback method that is invoked when a file has been successfully picked.
     * It updates the UI with the file name and enables the "View Sheet Music" button.
     * @param fileUri The URI of the picked file.
     * @param fileName The name of the picked file.
     */
    @Override
    public void onFilePicked(Uri fileUri, String fileName) {
        tvFilename.setText(fileName);
        selectedFileUri = fileUri;
        btnViewSheetMusic.setEnabled(true);
    }
}