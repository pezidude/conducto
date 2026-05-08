package com.example.conducto2.ui.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * acronym - SheetMusicPlayer. This activity is responsible for displaying sheet music.
 * It uses a WebView to render the sheet music and provides playback controls via a fragment overlay.
 */
public class SMPlayerActivity extends AppCompatActivity implements PlaybackFragment.PlaybackControlsListener {

    // log tag
    private static final String TAG = "SMPlayerActivity";

    private WebView sheetMusicView;
    private View playbackFragmentContainer;
    private ImageButton btnTogglePlayback;
    private boolean isPlaying = false;
    private int currentBPM = 100;
    private FirestoreManager firestoreManager;
    private ListenerRegistration statusListener;
    private boolean isLive = false;
    private boolean canControlPlayback = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smplayer);

        isLive = getIntent().getBooleanExtra("isLive", false);
        canControlPlayback = getIntent().getBooleanExtra("canControlPlayback", true);

        firestoreManager = new FirestoreManager();

        initViews();

        setupWebView();
    }

    public void initViews() {
        sheetMusicView = findViewById(R.id.sheetMusicView);
        playbackFragmentContainer = findViewById(R.id.playback_fragment_container);
        btnTogglePlayback = findViewById(R.id.btn_toggle_playback);

        if (!canControlPlayback) {
            btnTogglePlayback.setVisibility(View.GONE);
        }

        btnTogglePlayback.setOnClickListener(v -> {
            if (playbackFragmentContainer.getVisibility() == View.VISIBLE) {
                playbackFragmentContainer.setVisibility(View.GONE);
                // Rotate arrow to point up (suggesting expansion)
                btnTogglePlayback.animate().rotation(270).setDuration(200).start();
            } else {
                playbackFragmentContainer.setVisibility(View.VISIBLE);
                // Rotate arrow back to 90 degrees (pointing down towards the panel)
                btnTogglePlayback.animate().rotation(90).setDuration(200).start();
            }
        });
        // disable the toggle button until OSMD initializes
        btnTogglePlayback.setEnabled(false);

    }

    public void enablePlayback() {
        if (canControlPlayback) {
            btnTogglePlayback.setEnabled(true);
            btnTogglePlayback.callOnClick(); // show the playback controls
        }
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
                Log.d(TAG, "OSMD Engine Ready");
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
                Log.d(TAG, "File loaded.");
                sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
                enablePlayback();
            });
        }

        @JavascriptInterface
        public void onStepSelected(int exactTargetStep) {
            runOnUiThread(() -> {
                Toast.makeText(SMPlayerActivity.this, "Selected step: " + exactTargetStep, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Selected step: " + exactTargetStep);
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
                    Log.d(TAG, "Page finished loading");
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
        Log.d(TAG, "Loading: " + fileName);

        new Thread(() -> { // run in background so UI execution is not blocked
            try {
                String xmlContent = fileOps.readMusicXmlContent(uri);

                if (xmlContent == null || xmlContent.isEmpty()) {
                    throw new Exception("Empty or invalid file content");
                }
                runOnUiThread(() -> {
                    // basic file format check
                    if (xmlContent.contains("<?xml") || xmlContent.contains("<score-partwise")) {
                        loadXmlInWebView(xmlContent);
                    } else {
                        Toast.makeText(this, "File format not recognized.", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "File format not recognized.");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error reading file", e);
                runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to load file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Failed to load file: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopStatusListener();
        if (sheetMusicView != null) {
            sheetMusicView.pauseTimers(); // Pauses JS timers and background tasks
            sheetMusicView.onPause();     // Pauses WebView rendering
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStatusListener();
        if (sheetMusicView != null) {
            sheetMusicView.onResume();     // Wakes up WebView rendering
            sheetMusicView.resumeTimers(); // Wakes up JS timers
        }
    }

    private void startStatusListener() {
        User user = DataManager.getUserInstance();
        com.example.conducto2.data.model.Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        // Only listen for status changes in a live lesson
        if (isLive && user != null && cls != null && lesson != null) {
            statusListener = FirebaseFirestore.getInstance()
                    .collection("classes").document(cls.getId())
                    .collection("lessons").document(lesson.getId())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            String status = snapshot.getString("status");
                            if (status != null) {
                                handleStatusChange(status);
                            }
                        }
                    });
        }
    }

    private void stopStatusListener() {
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
    }

    private void handleStatusChange(String status) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);

        switch (status) {
            case Lesson.STATUS_PLAYING:
                if (!isPlaying) {
                    isPlaying = true;
                    sheetMusicView.evaluateJavascript("play();", null);
                    if (playbackFragment != null) {
                        playbackFragment.setPlaying(true);
                    }
                }
                break;
            case Lesson.STATUS_PAUSED:
                if (isPlaying) {
                    isPlaying = false;
                    sheetMusicView.evaluateJavascript("pause();", null);
                    if (playbackFragment != null) {
                        playbackFragment.setPlaying(false);
                    }
                }
                break;
            case Lesson.STATUS_STOPPED:
                isPlaying = false;
                sheetMusicView.evaluateJavascript("stop();", null);
                if (playbackFragment != null) {
                    playbackFragment.setPlaying(false);
                }
                break;
        }
    }

    /**
     * Called when the play/pause button in the PlaybackFragment is clicked.
     */
    @Override
    @SuppressLint("ClickableViewAccessibility") // silence android onTouch warning
    public void onPlayPauseClicked() {
        isPlaying = !isPlaying;
        if (isPlaying) {
            sheetMusicView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // This prevents the user from manually scrolling the webview.
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
            });
        } else {
            sheetMusicView.setOnTouchListener(null); // re-enable scrolling and touching
        }
        handlePlayback();
    }

    /**
     * Starts or stops the playback cursor in the WebView.
     */
    private void handlePlayback() {
        if (isPlaying) {
            sheetMusicView.evaluateJavascript("play();", null);
        } else {
            sheetMusicView.evaluateJavascript("pause();", null);
        }

        updateFirestoreStatus(isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED);
    }

    private void updateFirestoreStatus(String status) {
        User user = DataManager.getUserInstance();
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        // Only push status updates in a live lesson (teacher only)
        if (isLive && user != null && "teacher".equals(user.getUserType()) && cls != null && lesson != null && cls.isActive()) {
            firestoreManager.updateLessonStatus(cls.getId(), lesson.getId(), status);
        }
    }

    /*
     * Called when the functions in PlaybackFragment is clicked.
     */
    @Override
    public void onResetClicked() {
        sheetMusicView.evaluateJavascript("stop();", null);
        isPlaying = false;
        updateFirestoreStatus(Lesson.STATUS_STOPPED);
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
