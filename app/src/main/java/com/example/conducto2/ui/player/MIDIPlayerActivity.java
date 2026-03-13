package com.example.conducto2.ui.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.ui.player.widget.AnnotationView;
import com.example.conducto2.ui.player.widget.ObservableWebView;

/**
 * This activity is responsible for displaying sheet music and handling annotations.
 * It uses a WebView to render the sheet music and an {@link AnnotationView} overlay for drawing.
 */
public class MIDIPlayerActivity extends AppCompatActivity implements AnnotationToolbarFragment.ToolbarListener, ObservableWebView.OnTransformationChangeListener {

    /** Log tag for the activity. */
    private static final String TAG = "MIDIPlayerActivity";

    /** The WebView that displays the sheet music. */
    private ObservableWebView sheetMusicView;
    /** The view that handles drawing annotations over the sheet music. */
    private AnnotationView annotationView;
    /** A flag to indicate if the WebView's rendering engine is ready. */
    private boolean isEngineReady = false;
    /** Stores the XML data of the sheet music if it's available before the WebView is ready. */
    private String pendingXmlData = null;

    /**
     * Initializes the activity, views, and WebView. It also handles the intent to load the sheet music file.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midiplayer);

        sheetMusicView = findViewById(R.id.sheetMusicView);
        // this is an overlay above the sheetmusicview that handles annotations
        annotationView = findViewById(R.id.annotationView);

        setupWebView();
        sheetMusicView.setOnTransformationChangeListener(this);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("readOnly", false)) {
                Fragment toolbarFragment = getSupportFragmentManager().findFragmentById(R.id.annotation_toolbar_fragment);
                if (toolbarFragment != null) {
                    getSupportFragmentManager().beginTransaction().hide(toolbarFragment).commit();
                }
            }

            if (intent.hasExtra("fileUri")) {
                String uriString = intent.getStringExtra("fileUri");
                Uri fileUri = Uri.parse(uriString);
                handleFileSelection(fileUri);
            }
        }
    }

    /**
     * Configures the WebView settings and sets up a client to know when the page is loaded.
     */
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

    /**
     * A callback that is invoked when the WebView is scrolled.
     * It updates the scroll position of the annotation view.
     * @param scrollX The new horizontal scroll position.
     * @param scrollY The new vertical scroll position.
     */
    @Override
    public void onScrollChange(int scrollX, int scrollY) {
        annotationView.setScroll(scrollX, scrollY);
    }

    /**
     * A callback that is invoked when the WebView is scaled.
     * It updates the scale of the annotation view.
     * @param scale The new scale factor.
     */
    @Override
    public void onScaleChange(float scale) {
        annotationView.setScale(scale);
    }

    /**
     * Loads the given MusicXML data into the WebView.
     * If the WebView is not ready, it stores the data in {@link #pendingXmlData}.
     * @param xmlData The MusicXML data to load.
     */
    private void loadXmlInWebView(String xmlData) {
        if (!isEngineReady) {
            pendingXmlData = xmlData;
            return;
        }
        String escapedXml = xmlData.replace("`", "\\`").replace("$", "\\$");
        sheetMusicView.evaluateJavascript("loadScore(`" + escapedXml + "`);", null);
    }

    /**
     * Handles the selection of a sheet music file.
     * It reads the file content in a background thread and then loads it into the WebView.
     * @param uri The URI of the selected file.
     */
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

    /**
     * Sets the annotation tool mode in the {@link AnnotationView}.
     * @param mode The annotation mode to set.
     */
    @Override
    public void onToolSelected(AnnotationView.AnnotationMode mode) {
        annotationView.setMode(mode);
    }

    /**
     * Sets the annotation color in the {@link AnnotationView}.
     * @param color The color to set.
     */
    @Override
    public void onColorSelected(int color) {
        annotationView.setCurrentColor(color);
    }

    /**
     * Undoes the last annotation action.
     */
    @Override
    public void onUndo() {
        annotationView.undo();
    }

    /**
     * Clears all annotations.
     */
    @Override
    public void onClear() {
        annotationView.clearAnnotations();
    }
}