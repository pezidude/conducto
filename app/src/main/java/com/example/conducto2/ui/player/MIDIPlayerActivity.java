package com.example.conducto2.ui.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.R;
import com.example.conducto2.ui.player.widget.SheetMusicView;

public class MIDIPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MIDIPlayerActivity";

    // UI Components
    private Button btnSelectFile;
    private TextView tvFilename;
    private SheetMusicView sheetMusicView;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        handleFileSelection(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midiplayer);


        sheetMusicView = findViewById(R.id.sheetMusicView);

    }



    private void handleFileSelection(Uri uri) {
        // helper for managing files
        FileIO fileOps  = new FileIO(this);
        String fileName = fileOps.getFileName(uri);
        tvFilename.setText(fileName);

        // Run file loading in a background thread to avoid freezing UI on large files
        new Thread(() -> {
            try {
                String xmlContent;

                // CHECK: Is it a compressed MXL file (ZIP)?
                if (fileName.toLowerCase().endsWith(".mxl")) {
                    xmlContent = fileOps.readZippedXMLFromUri(uri);
                } else {
                    // Assume standard XML/MusicXML file format
                    xmlContent = fileOps.readTextFromUri(uri);
                }

                if (xmlContent == null) {
                    throw new Exception("Empty or invalid file content");
                }

                // Update UI on the main thread
                runOnUiThread(() -> {
                    // Simple xml validation check
                    if (xmlContent.contains("<?xml") || xmlContent.contains("<score-partwise")) {
                        sheetMusicView.loadXml(xmlContent);
                        Toast.makeText(this, "Loading Score...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "File format not recognized.", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error reading file", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load file: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


}