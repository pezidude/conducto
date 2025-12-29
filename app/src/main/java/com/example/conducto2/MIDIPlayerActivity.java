package com.example.conducto2;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.ui.SheetMusicView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MIDIPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MIDIPlayerActivity";

    // UI Components
    private Button btnSelectFile;
    private TextView txtFileName;
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

        btnSelectFile = findViewById(R.id.btnSelectFile);
        txtFileName = findViewById(R.id.txtFileName);
        sheetMusicView = findViewById(R.id.sheetMusicView);

        btnSelectFile.setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Allow generic types because .mxl mime-types vary wildly
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void handleFileSelection(Uri uri) {
        String fileName = getFileName(uri);
        txtFileName.setText(fileName);

        // Run file loading in a background thread to avoid freezing UI on large files
        new Thread(() -> {
            try {
                String xmlContent;

                // CHECK: Is it a compressed MXL file?
                if (fileName.toLowerCase().endsWith(".mxl")) {
                    xmlContent = readMxlFromUri(uri);
                } else {
                    // Assume standard XML/MusicXML
                    xmlContent = readTextFromUri(uri);
                }

                if (xmlContent == null) {
                    throw new Exception("Empty or invalid file content");
                }

                // Update UI on the main thread
                runOnUiThread(() -> {
                    // Simple validation check
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

    /**
     * Unzips the .mxl file and finds the first valid .xml entry inside.
     */
    private String readMxlFromUri(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();

                // We ignore the container metadata file and look for the actual music sheet
                if (!name.contains("META-INF") && (name.endsWith(".xml") || name.endsWith(".musicxml"))) {

                    // Found the XML file inside the ZIP! Read it.
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    return stringBuilder.toString();
                }
            }
        }
        throw new Exception("No valid MusicXML file found inside .mxl package");
    }

    private String readTextFromUri(Uri uri) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}