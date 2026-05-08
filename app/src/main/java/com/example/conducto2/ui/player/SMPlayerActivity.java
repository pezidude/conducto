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

import android.view.WindowManager;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.os.Handler;
import android.os.Looper;

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
    private DatabaseReference syncRef;
    private ValueEventListener syncListener;
    private DatabaseReference offsetRef;
    private ValueEventListener offsetListener;
    private long serverTimeOffset = 0;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private boolean isLive = false;
    private boolean canControlPlayback = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smplayer);
        
        // Keep the screen on while this activity is in the foreground
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        playbackHandler.removeCallbacksAndMessages(null);
        if (sheetMusicView != null) {
            sheetMusicView.pauseTimers(); // Pauses JS timers and background tasks
            sheetMusicView.onPause();     // Pauses WebView rendering
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStatusListener();
        startOffsetListener();
        if (sheetMusicView != null) {
            sheetMusicView.onResume();     // Wakes up WebView rendering
            sheetMusicView.resumeTimers(); // Wakes up JS timers
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up resources");
        
        // 1. Stop all listeners
        stopStatusListener();
        
        // 2. Clear any pending playback handlers
        playbackHandler.removeCallbacksAndMessages(null);
        
        // 3. Command the JS engine to shut down audio and clear references
        if (sheetMusicView != null) {
            sheetMusicView.evaluateJavascript("destroyEngine();", null);
            
            // 4. Properly dismantle the WebView
            sheetMusicView.removeJavascriptInterface("AndroidInterface");
            sheetMusicView.stopLoading();
            sheetMusicView.setWebViewClient(null);
            sheetMusicView.clearHistory();
            sheetMusicView.removeAllViews();
            sheetMusicView.destroy();
            sheetMusicView = null;
        }
        
        super.onDestroy();
    }

    private void startOffsetListener() {
        if (offsetRef == null) {
            offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        }
        if (offsetListener == null) {
            offsetListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.getValue() != null) {
                        serverTimeOffset = snapshot.getValue(Long.class);
                        Log.d(TAG, "Server time offset updated: " + serverTimeOffset);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w(TAG, "Offset listener cancelled", error.toException());
                }
            };
            offsetRef.addValueEventListener(offsetListener);
        }
    }

    private void startStatusListener() {
        User user = DataManager.getUserInstance();
        com.example.conducto2.data.model.Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        // Only listen for status changes in a live lesson
        if (isLive && user != null && cls != null && lesson != null) {
            // Keep Firestore listener for background/state sync if needed, 
            // but rely on RTDB for high-precision playback sync.
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
                            // Firestore status can be used for UI updates that don't need sub-second precision
                        }
                    });

            // Add RTDB listener for high-precision sync
            syncRef = FirebaseDatabase.getInstance().getReference("playback_sync").child(lesson.getId());
            syncListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String status = snapshot.child("status").getValue(String.class);
                        Long targetTime = snapshot.child("targetTime").getValue(Long.class);
                        handleSyncChange(status, targetTime);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.w(TAG, "Sync listener cancelled", error.toException());
                }
            };
            syncRef.addValueEventListener(syncListener);
        }
    }

    private void stopStatusListener() {
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
        if (syncRef != null && syncListener != null) {
            syncRef.removeEventListener(syncListener);
            syncListener = null;
        }
        if (offsetRef != null && offsetListener != null) {
            offsetRef.removeEventListener(offsetListener);
            offsetListener = null;
        }
    }

    private void handleSyncChange(String status, Long targetTime) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);

        // Cancel any pending playback commands
        playbackHandler.removeCallbacksAndMessages(null);

        if (Lesson.STATUS_PLAYING.equals(status) && targetTime != null) {
            long currentTime = System.currentTimeMillis() + serverTimeOffset;
            long delay = targetTime - currentTime;

            if (delay > 0) {
                playbackHandler.postDelayed(() -> {
                    executePlay();
                    if (playbackFragment != null) playbackFragment.setPlaying(true);
                }, delay);
            } else {
                executePlay();
                if (playbackFragment != null) playbackFragment.setPlaying(true);
            }
        } else if (Lesson.STATUS_PAUSED.equals(status)) {
            executePause();
            if (playbackFragment != null) playbackFragment.setPlaying(false);
        } else if (Lesson.STATUS_STOPPED.equals(status)) {
            executeStop();
            if (playbackFragment != null) playbackFragment.setPlaying(false);
        }
    }

    private void executePlay() {
        isPlaying = true;
        sheetMusicView.evaluateJavascript("play();", null);
    }

    private void executePause() {
        isPlaying = false;
        sheetMusicView.evaluateJavascript("pause();", null);
    }

    private void executeStop() {
        isPlaying = false;
        sheetMusicView.evaluateJavascript("stop();", null);
    }

    private void handleStatusChange(String status) {
        // This method is now secondary to handleSyncChange for live lessons.
        // It can be kept for non-RTDB scenarios if necessary.
    }

    /**
     * Called when the play/pause button in the PlaybackFragment is clicked.
     */
    @Override
    @SuppressLint("ClickableViewAccessibility") // silence android onTouch warning
    public void onPlayPauseClicked() {
        isPlaying = !isPlaying;
        
        // Pre-warm AudioContext for teachers when they press Play
        if (isPlaying && isLive) {
            User user = DataManager.getUserInstance();
            if (user != null && "teacher".equals(user.getUserType())) {
                sheetMusicView.evaluateJavascript("if(playbackManager && playbackManager.ac) playbackManager.ac.resume();", null);
            }
        }
        
        handlePlayback();
    }

    /**
     * Starts or stops the playback cursor in the WebView.
     */
    private void handlePlayback() {
        String status = isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED;
        User user = DataManager.getUserInstance();

        if (isLive && user != null && "teacher".equals(user.getUserType())) {
            // Teacher initiates sync via RTDB
            updateRTDBStatus(status);
        } else if (!isLive) {
            // Local playback for non-live sessions
            if (isPlaying) {
                executePlay();
            } else {
                executePause();
            }
        }

        // Move Firestore update to background to not block the main thread
        // during critical playback timing.
        new Thread(() -> updateFirestoreStatus(status)).start();
    }

    private void updateRTDBStatus(String status) {
        if (syncRef != null && isLive) {
            long targetTime = 0;
            if (Lesson.STATUS_PLAYING.equals(status)) {
                // Agree on a future start time (1500ms delay to allow network/JS/Audio buffer)
                targetTime = System.currentTimeMillis() + serverTimeOffset + 1500;
            }

            java.util.Map<String, Object> syncData = new java.util.HashMap<>();
            syncData.put("status", status);
            syncData.put("targetTime", targetTime);
            
            syncRef.setValue(syncData);
        }
    }

    private void updateFirestoreStatus(String status) {
        User user = DataManager.getUserInstance();
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        Log.d(TAG, "updateFirestoreStatus: status=" + status + ", isLive=" + isLive + ", userType=" + (user != null ? user.getUserType() : "null"));

        if (cls == null) Log.d(TAG, "updateFirestoreStatus: Class is null");
        if (lesson == null) Log.d(TAG, "updateFirestoreStatus: Lesson is null");

        // Only push status updates in a live lesson (teacher only)
        if (isLive && user != null && "teacher".equals(user.getUserType()) && cls != null && lesson != null) {
            Log.d(TAG, "updateFirestoreStatus: Updating Firestore. ClassID=" + cls.getId() + ", LessonID=" + lesson.getId());
            firestoreManager.updateLessonStatus(cls.getId(), lesson.getId(), status);
        } else {
            Log.d(TAG, "updateFirestoreStatus: Conditions not met for update.");
        }
    }

    /*
     * Called when the functions in PlaybackFragment is clicked.
     */
    @Override
    public void onResetClicked() {
        isPlaying = false;
        User user = DataManager.getUserInstance();

        if (isLive && user != null && "teacher".equals(user.getUserType())) {
            updateRTDBStatus(Lesson.STATUS_STOPPED);
        } else if (!isLive) {
            executeStop();
        }
        
        new Thread(() -> updateFirestoreStatus(Lesson.STATUS_STOPPED)).start();
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
