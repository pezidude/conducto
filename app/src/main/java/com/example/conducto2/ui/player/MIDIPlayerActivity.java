package com.example.conducto2.ui.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.R;
import com.example.conducto2.ui.player.widget.AnnotationView;
import com.example.conducto2.ui.player.widget.SheetMusicView;

public class MIDIPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MIDIPlayerActivity";

    // UI Components
    private Button btnSelectFile, btnBrush, btnText, btnUndo, btnClear;
    private TextView tvFilename;
    private SheetMusicView sheetMusicView;
    private AnnotationView annotationView;

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
        annotationView = findViewById(R.id.annotationView);

        btnBrush = findViewById(R.id.btn_brush);
        btnText = findViewById(R.id.btn_text);
        btnUndo = findViewById(R.id.btn_undo);
        btnClear = findViewById(R.id.btn_clear);

        setupAnnotationControls();

        // Check if a file URI was passed from TestActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("fileUri")) {
            String uriString = intent.getStringExtra("fileUri");
            Uri fileUri = Uri.parse(uriString);
            handleFileSelection(fileUri);
        }
    }

    private void setupAnnotationControls() {
        btnBrush.setOnClickListener(v -> annotationView.setMode(AnnotationView.AnnotationMode.BRUSH));
        btnText.setOnClickListener(v -> annotationView.setMode(AnnotationView.AnnotationMode.TEXT));
        btnUndo.setOnClickListener(v -> annotationView.undo());
        btnClear.setOnClickListener(v -> annotationView.clearAnnotations());

        annotationView.setOnTouchListener((v, event) -> {
            if (annotationView.getMode() == AnnotationView.AnnotationMode.TEXT && event.getAction() == MotionEvent.ACTION_DOWN) {
                showTextInputDialog(event.getX(), event.getY());
                return true; // Consume the event
            }
            // Let the AnnotationView handle its own touch events for drawing
            return annotationView.onTouchEvent(event);
        });
    }

    private void showTextInputDialog(final float x, final float y) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Text");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();
            annotationView.addTextAnnotation(text, x, y);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }



    private void handleFileSelection(Uri uri) {
        FileIO fileOps = new FileIO(this);
        String fileName = fileOps.getFileName(uri);
        // tvFilename is not in the layout, so we can't set it. A toast is a good alternative.
        Toast.makeText(this, "Loading: " + fileName, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String xmlContent;
                if (fileName.toLowerCase().endsWith(".mxl")) {
                    xmlContent = fileOps.readZippedXMLFromUri(uri);
                } else {
                    xmlContent = fileOps.readTextFromUri(uri);
                }

                if (xmlContent == null) {
                    throw new Exception("Empty or invalid file content");
                }

                runOnUiThread(() -> {
                    if (xmlContent.contains("<?xml") || xmlContent.contains("<score-partwise")) {
                        sheetMusicView.loadXml(xmlContent);
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