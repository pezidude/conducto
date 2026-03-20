package com.example.conducto2.ui.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.ui.player.widget.InteractiveWebView;

/**
 * acronym - SheetMusicPlayer. This activity is responsible for displaying sheet music.
 * It uses a WebView to render the sheet music and provides playback controls.
 */
public class SMPlayerActivity extends AppCompatActivity implements PlaybackFragment.PlaybackControlsListener {

    // log tag
    private static final String TAG = "SMPlayerActivity";

    private InteractiveWebView sheetMusicView;
    private boolean isEngineReady = false; // flag
    private String pendingXmlData = null;
    private boolean isPlaying = false;
    private int currentSpeedPercentage = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smplayer);

        sheetMusicView = findViewById(R.id.sheetMusicView);
        setupWebView();

        Intent intent = getIntent();

        if (intent.hasExtra("fileUri")) {
            String uriString = intent.getStringExtra("fileUri");
            Uri fileUri = Uri.parse(uriString);
            handleFileSelection(fileUri);
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
                sheetMusicView.evaluateJavascript("window.playbackIntervalId = null;", null);
                if (pendingXmlData != null) {
                    loadXmlInWebView(pendingXmlData);
                    pendingXmlData = null;
                }
            }
        });
        sheetMusicView.loadUrl("file:///android_asset/viewer.html");
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
        sheetMusicView.evaluateJavascript("osmd.cursor.reset();", null);
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

        new Thread(() -> { // run in background so UI execution is not blocked
            try {
                String xmlContent = fileName.toLowerCase().endsWith(".mxl")
                        ? fileOps.readZippedXMLFromUri(uri)
                        : fileOps.readTextFromUri(uri);

                if (xmlContent == null) {
                    throw new Exception("Empty or invalid file content");
                }
                runOnUiThread(() -> {
                    // basic file format check
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
     * Called when the play/pause button in the PlaybackFragment is clicked.
     */
    @Override
    public void onPlayPauseClicked() {
        isPlaying = !isPlaying;
        handlePlayback();
    }

    /**
     * Starts or stops the playback cursor in the WebView.
     */
    private void handlePlayback() {
        if (isPlaying) {
            int bpm = 120; // default bpm
            int interval = (int) ((1000.0 / (bpm/60.0))/ (currentSpeedPercentage / 100.0));
            sheetMusicView.evaluateJavascript("osmd.cursor.show();", null);
            sheetMusicView.evaluateJavascript("if(window.playbackIntervalId) clearInterval(window.playbackIntervalId);", null);
            sheetMusicView.evaluateJavascript("window.playbackIntervalId = setInterval(function(){ osmd.cursor.next(); }, " + interval + ");", null);
        } else {
            sheetMusicView.evaluateJavascript("clearInterval(window.playbackIntervalId);", null);
            sheetMusicView.evaluateJavascript("window.playbackIntervalId = null;", null);
        }
    }

    @Override
    public void onResetClicked() {
        sheetMusicView.evaluateJavascript("osmd.cursor.reset();", null);
    }

    @Override
    public void onSpeedChanged(int speedPercentage) {
        currentSpeedPercentage = speedPercentage;
        if (isPlaying) {
            // this.onPlayPauseClicked();
            // sheetMusicView.evaluateJavascript("clearInterval(window.playbackIntervalId);", null);
            handlePlayback(); // restart interval with new speed
        }
    }
}