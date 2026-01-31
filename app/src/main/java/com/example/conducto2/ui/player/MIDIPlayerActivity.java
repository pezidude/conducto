package com.example.conducto2.ui.player;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.ui.player.widget.AnnotationView;
import com.example.conducto2.ui.player.widget.SheetMusicView;

public class MIDIPlayerActivity extends AppCompatActivity implements AnnotationToolbarFragment.ToolbarListener {

    private static final String TAG = "MIDIPlayerActivity";

    private SheetMusicView sheetMusicView;
    private AnnotationView annotationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midiplayer);

        sheetMusicView = findViewById(R.id.sheetMusicView);
        annotationView = findViewById(R.id.annotationView);

        setupAnnotationTouchListener();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("fileUri")) {
            String uriString = intent.getStringExtra("fileUri");
            Uri fileUri = Uri.parse(uriString);
            handleFileSelection(fileUri);
        }
    }

    private void setupAnnotationTouchListener() {
        annotationView.setOnTouchListener((v, event) -> {
            if (annotationView.getMode() == AnnotationView.AnnotationMode.TEXT && event.getAction() == MotionEvent.ACTION_DOWN) {
                showTextInputDialog(event.getX(), event.getY());
                return true; // Consume the event
            }
            // Let the AnnotationView handle its own onTouchEvent for drawing/scrolling
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
        Toast.makeText(this, "Loading: " + fileName, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                String xmlContent = fileName.toLowerCase().endsWith(".mxl")
                        ? fileOps.readZippedXMLFromUri(uri)
                        : fileOps.readTextFromUri(uri);

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

    // --- ToolbarListener Implementation ---
    @Override
    public void onToolSelected(AnnotationView.AnnotationMode mode) {
        annotationView.setMode(mode);
    }

    @Override
    public void onColorSelected(int color) {
        annotationView.setCurrentColor(color);
    }

    @Override
    public void onUndo() {
        annotationView.undo();
    }

    @Override
    public void onClear() {
        annotationView.clearAnnotations();
    }
}