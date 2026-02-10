package com.example.conducto2.ui.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.ui.player.widget.AnnotationView;
import com.example.conducto2.ui.player.widget.ObservableWebView;

public class MIDIPlayerActivity extends AppCompatActivity implements AnnotationToolbarFragment.ToolbarListener, ObservableWebView.OnTransformationChangeListener {

    private static final String TAG = "MIDIPlayerActivity";

    private ObservableWebView sheetMusicView;
    private AnnotationView annotationView;
    private boolean isEngineReady = false;
    private String pendingXmlData = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midiplayer);

        sheetMusicView = findViewById(R.id.sheetMusicView);
        // this is an overlay above the sheetmusicview that handles annotations
        annotationView = findViewById(R.id.annotationView);

        setupWebView();
        sheetMusicView.setOnTransformationChangeListener(this);
        setupAnnotationTouchListener();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("fileUri")) {
            String uriString = intent.getStringExtra("fileUri");
            Uri fileUri = Uri.parse(uriString);
            handleFileSelection(fileUri);
        }
    }

    private void setupWebView() {
        WebSettings settings = sheetMusicView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        sheetMusicView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                isEngineReady = true;
                if (pendingXmlData != null) {
                    loadXmlInWebView(pendingXmlData);
                    pendingXmlData = null;
                }
            }
        });
        sheetMusicView.loadUrl("file:///android_asset/viewer.html");
    }

    @Override
    public void onScrollChange(int scrollX, int scrollY) {
        annotationView.setScroll(scrollX, scrollY);
    }

    @Override
    public void onScaleChange(float scale) {
        annotationView.setScale(scale);
    }

    private void setupAnnotationTouchListener() {
        annotationView.setOnTouchListener((v, event) -> {
            float scale = annotationView.getScale();
            if (annotationView.getMode() == AnnotationView.AnnotationMode.TEXT && event.getAction() == MotionEvent.ACTION_DOWN && scale > 0) {
                // Apply the same transformation logic as in AnnotationView's onTouchEvent
                float adjustedX = (event.getX() + annotationView.getScrollXPosition()) / scale;
                float adjustedY = (event.getY() + annotationView.getScrollYPosition()) / scale;
                showTextInputDialog(adjustedX, adjustedY);
                return true;
            }
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
    
    private void loadXmlInWebView(String xmlData) {
        if (!isEngineReady) {
            pendingXmlData = xmlData;
            return;
        }
        String escapedXml = xmlData.replace("`", "\\`").replace("$", "\\$");
        sheetMusicView.evaluateJavascript("loadScore(`" + escapedXml + "`);", null);
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
                        loadXmlInWebView(xmlContent);
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