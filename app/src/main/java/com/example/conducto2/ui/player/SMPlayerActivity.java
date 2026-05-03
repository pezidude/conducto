package com.example.conducto2.ui.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;

/**
 * acronym - SheetMusicPlayer. This activity is responsible for displaying sheet music.
 * It uses a WebView to render the sheet music and provides playback controls.
 */
public class SMPlayerActivity extends AppCompatActivity implements PlaybackFragment.PlaybackControlsListener {

    // log tag
    private static final String TAG = "SMPlayerActivity";

    private WebView sheetMusicView;
    private boolean isPlaying = false;
    private int currentBPM = 100;
    // int bpm = 120; // default bpm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smplayer);

        sheetMusicView = findViewById(R.id.sheetMusicView);
        setupWebView();
    }

    /**
     * A bridge class to handle callbacks from JavaScript.
     */
    private class WebAppInterface {
        @JavascriptInterface
        public void onEngineReady() {
            runOnUiThread(() -> {
                // now that all JS has executed, it's safe to load the file.
                Toast.makeText(SMPlayerActivity.this, "OSMD Engine Ready", Toast.LENGTH_SHORT).show();
                Intent intent = getIntent();
                if (intent.hasExtra("fileUri")) {
                    String uriString = intent.getStringExtra("fileUri");
                    Uri fileUri = Uri.parse(uriString);
                    loadFile(fileUri);
                }
            });
        }

        @JavascriptInterface
        public void onLoadScoreFinished() {
            runOnUiThread(() -> {
                Toast.makeText(SMPlayerActivity.this, "File loaded.", Toast.LENGTH_SHORT).show();
                sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
            });
        }
    }

    /**
     * Configures the WebView settings and sets up a client to know when the page is loaded.
     */
    private void setupWebView() {
        WebSettings settings = sheetMusicView.getSettings();
        settings.setJavaScriptEnabled(true);
        // JavaScript is required for interactive with OSMD and OSMD audio player
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // Register the bridge interface
        sheetMusicView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        sheetMusicView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                runOnUiThread(() -> {
                    Toast.makeText(SMPlayerActivity.this, "Page finished loading", Toast.LENGTH_SHORT).show();
                });
            }
        });
        sheetMusicView.loadUrl("file:///android_asset/viewer.html");
    }



    /**
     * Loads the given MusicXML data into the WebView.
     * @param xmlData The MusicXML data to load.
     */
    private void loadXmlInWebView(String xmlData) {
        double zoomLevel = 0.75;
        // basic attempt of preventing XSS. TODO: improve for production.
        String escapedXml = xmlData.replace("`", "\\`").replace("$", "\\$");
        sheetMusicView.evaluateJavascript("loadScore(`" + escapedXml + "`, " + zoomLevel + ");", null);
    }

    /**
     * Loads a given sheet music file.
     * It reads the file content in a background thread and then loads it into the WebView.
     * @param uri The URI of the selected file.
     */
    private void loadFile(Uri uri) {
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
                Log.d(TAG, "Error reading file", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load file: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sheetMusicView != null) {
            sheetMusicView.pauseTimers(); // Pauses JS timers and background tasks
            sheetMusicView.onPause();     // Pauses WebView rendering

            // Optional: Tell JS to pause the music if it's currently playing
            sheetMusicView.evaluateJavascript("if(window.pauseMusic) window.pauseMusic();", null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sheetMusicView != null) {
            sheetMusicView.resumeTimers(); // Wakes up JS timers
            sheetMusicView.onResume();     // Wakes up WebView rendering

            // CRUCIAL: Tell JS to wake up the AudioContext
            sheetMusicView.evaluateJavascript("if(window.wakeUpAudio) window.wakeUpAudio();", null);
        }
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
        Toast.makeText(this, "Click!", Toast.LENGTH_SHORT).show();
        if (isPlaying) {
            sheetMusicView.evaluateJavascript("play();", null);
        } else {
            sheetMusicView.evaluateJavascript("pause();", null);
        }
    }

    @Override
    public void onResetClicked() {
        sheetMusicView.evaluateJavascript("stop();", null);
    }

    @Override
    public void onSpeedChanged(int bpmValue) {
        currentBPM = bpmValue;
        if (isPlaying) {
            sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
            handlePlayback(); // restart interval with new speed
        }
    }
}