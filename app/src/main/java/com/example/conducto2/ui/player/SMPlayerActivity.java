package com.example.conducto2.ui.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

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
    private ImageButton btnOpenMixer;
    private Button btnEndLiveLesson;
    private DrawerLayout drawerLayout;
    private RecyclerView mixerRecyclerView;
    private MixerAdapter mixerAdapter;

    private boolean isPlaying = false;
    private boolean isScoreLoaded = false;
    private String pendingStatus = null;
    private long pendingTargetTimestamp = 0;
    private int pendingMeasure = -1;
    private int pendingBpm = -1;
    private int currentBPM = 100;
    private FirestoreManager firestoreManager;
    private ListenerRegistration statusListener;
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());
    private boolean isLive = false;
    private boolean canControlPlayback = true;
    private long serverTimeOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smplayer);
        
        // Keep the screen on while this activity is in the foreground
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isLive = getIntent().getBooleanExtra("isLive", false);
        canControlPlayback = getIntent().getBooleanExtra("canControlPlayback", true);

        firestoreManager = new FirestoreManager();
        firestoreManager.calculateServerTimeOffset(offset -> {
            serverTimeOffset = offset;
            Log.d(TAG, "Server time offset: " + serverTimeOffset + "ms");
        });

        initViews();

        setupWebView();
    }

    public void initViews() {
        sheetMusicView = findViewById(R.id.sheetMusicView);
        playbackFragmentContainer = findViewById(R.id.playback_fragment_container);
        btnTogglePlayback = findViewById(R.id.btn_toggle_playback);
        btnOpenMixer = findViewById(R.id.btn_open_mixer);
        btnEndLiveLesson = findViewById(R.id.btn_end_live_lesson);
        drawerLayout = findViewById(R.id.drawer_layout);
        mixerRecyclerView = findViewById(R.id.mixer_recycler_view);

        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            btnOpenMixer.setVisibility(View.VISIBLE);
            btnOpenMixer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
            
            mixerRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            if (isLive) {
                btnEndLiveLesson.setVisibility(View.VISIBLE);
                btnEndLiveLesson.setOnClickListener(v -> showEndLiveLessonConfirmation());
            }
        } else {
            btnOpenMixer.setVisibility(View.GONE);
            // Disable drawer for students
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

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
        public void onLoadScoreFinished(String instrumentNamesJson) {
            runOnUiThread(() -> {
                Toast.makeText(SMPlayerActivity.this, "File loaded.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "File loaded.");
                isScoreLoaded = true;
                sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
                
                User user = DataManager.getUserInstance();
                if (isLive && user != null) {
                    if ("student".equals(user.getUserType())) {
                        // Mute students in live sessions
                        sheetMusicView.evaluateJavascript("setGlobalMute(true);", null);
                    } else if ("teacher".equals(user.getUserType())) {
                        // Setup mixer for teacher
                        setupMixer(instrumentNamesJson);
                    }
                }

                enablePlayback();

                if (pendingStatus != null) {
                    Log.d(TAG, "onLoadScoreFinished: Handling deferred status: " + pendingStatus);
                    handleStatusChange(pendingStatus, pendingTargetTimestamp, pendingMeasure, pendingBpm);
                    pendingStatus = null;
                    pendingTargetTimestamp = 0;
                    pendingMeasure = -1;
                    pendingBpm = -1;
                }
            });
        }

        @JavascriptInterface
        public void onMeasureSelected(int measureIndex) {
            runOnUiThread(() -> {
                Log.d(TAG, "onMeasureSelected: " + measureIndex);
                User user = DataManager.getUserInstance();
                if (isLive && user != null && "teacher".equals(user.getUserType())) {
                    // Update Firestore with the new measure.
                    new Thread(() -> updateFirestoreStatus(isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED, 0, measureIndex, currentBPM)).start();
                }
            });
        }
    }

    private void setupMixer(String instrumentNamesJson) {
        try {
            JSONArray array = new JSONArray(instrumentNamesJson);
            List<String> instrumentNames = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                instrumentNames.add(array.getString(i));
            }

            mixerAdapter = new MixerAdapter(instrumentNames, (index, volume, mute, solo) -> {
                sheetMusicView.evaluateJavascript("setPartMix(" + index + ", " + volume + ", " + mute + ", " + solo + ");", null);
            });
            mixerRecyclerView.setAdapter(mixerAdapter);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing instrument names", e);
        }
    }

    private void showEndLiveLessonConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("End Live Lesson")
                .setMessage("Are you sure you want to end this live lesson for all students?")
                .setPositiveButton("End Lesson", (dialog, which) -> endLiveLesson())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void endLiveLesson() {
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        if (cls != null && lesson != null) {
            // 1. Set isLive to false in the lesson
            firestoreManager.updateLessonLiveStatus(cls.getId(), lesson.getId(), false);
            // 2. Set isActive to false in the class
            firestoreManager.updateClassActivity(cls.getId(), false);

            Toast.makeText(this, "Live lesson ended", Toast.LENGTH_SHORT).show();
            finish();
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
                            // Check if the lesson is still live
                            Boolean live = snapshot.getBoolean("isLive");
                            if (live != null && !live && isLive) {
                                // Lesson was live but is no longer live
                                showLessonEndedDialog();
                                return;
                            }

                            String status = snapshot.getString("status");
                            Long targetTimestamp = snapshot.getLong("targetTimestamp");
                            Long currentMeasure = snapshot.getLong("currentMeasure");
                            Long bpm = snapshot.getLong("bpm");
                            
                            handleStatusChange(status, 
                                    targetTimestamp != null ? targetTimestamp : 0,
                                    currentMeasure != null ? currentMeasure.intValue() : -1,
                                    bpm != null ? bpm.intValue() : -1);
                        }
                    });
        }
    }

    private void showLessonEndedDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("Lesson Ended")
                .setMessage("The teacher has ended this live lesson for everyone.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void stopStatusListener() {
        if (statusListener != null) {
            statusListener.remove();
            statusListener = null;
        }
    }

    private void handleStatusChange(String status, long targetTimestamp, int currentMeasure, int bpm) {
        if (!isScoreLoaded) {
            pendingStatus = status;
            pendingTargetTimestamp = targetTimestamp;
            pendingMeasure = currentMeasure;
            pendingBpm = bpm;
            Log.d(TAG, "handleStatusChange: Score not loaded yet, deferring status: " + status);
            return;
        }

        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);

        // Cancel any pending playback commands
        playbackHandler.removeCallbacksAndMessages(null);

        // Sync BPM if provided
        if (bpm > 0 && bpm != currentBPM) {
            currentBPM = bpm;
            sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
            if (playbackFragment != null) playbackFragment.updateBpmUI(currentBPM);
        }

        // Sync playback measure if provided
        if (currentMeasure >= 0) {
            sheetMusicView.evaluateJavascript("jumpToMeasure(" + currentMeasure + ");", null);
        }

        if (Lesson.STATUS_PLAYING.equals(status)) {
            if (isLive && targetTimestamp > 0) {
                long currentServerTime = System.currentTimeMillis() + serverTimeOffset;
                long delay = 1000 - (currentServerTime - targetTimestamp);
                Log.d(TAG, "handleStatusChange: Syncing playback. Delay: " + delay + "ms");
                if (delay > 0) {
                    playbackHandler.postDelayed(this::executePlay, delay);
                } else {
                    executePlay();
                }
            } else {
                executePlay();
            }
            if (playbackFragment != null) playbackFragment.setPlaying(true);
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

        if (isLive && "teacher".equals(user.getUserType())) {
            if (isPlaying) {
                // Synchronized start for teacher and students
                long targetTimestamp = System.currentTimeMillis() + serverTimeOffset;
                updateFirestoreStatus(status, targetTimestamp, -1, currentBPM);
                // Local execution will be handled by handleStatusChange when the Firestore update cycles back
            } else {
                // Pause - get current measure first to resync students
                sheetMusicView.evaluateJavascript("getCurrentMeasure();", value -> {
                    int measure = -1;
                    try {
                        if (value != null && !value.equals("null")) {
                            measure = Integer.parseInt(value);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing measure: " + value);
                    }
                    final int finalMeasure = measure;
                    executePause();
                    new Thread(() -> updateFirestoreStatus(Lesson.STATUS_PAUSED, 0, finalMeasure, currentBPM)).start();
                });
            }
        } else {
            // Local execution for immediate feedback
            if (isPlaying) {
                executePlay();
            } else {
                executePause();
            }
            new Thread(() -> updateFirestoreStatus(status, 0, -1, currentBPM)).start();
        }
    }

    private void updateFirestoreStatus(String status, long targetTimestamp, int currentMeasure, int bpm) {
        User user = DataManager.getUserInstance();
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        Log.d(TAG, "updateFirestoreStatus: status=" + status + ", isLive=" + isLive + ", userType=" + (user != null ? user.getUserType() : "null"));

        // Only push status updates in a live lesson (teacher only)
        if (isLive && user != null && "teacher".equals(user.getUserType()) && cls != null && lesson != null) {
            Log.d(TAG, "updateFirestoreStatus: Updating Firestore. ClassID=" + cls.getId() + ", LessonID=" + lesson.getId());
            firestoreManager.updateLessonStatus(cls.getId(), lesson.getId(), status, targetTimestamp, currentMeasure, bpm);
        }
    }

    /*
     * Called when the functions in PlaybackFragment is clicked.
     */
    @Override
    public void onResetClicked() {
        isPlaying = false;
        executeStop();
        new Thread(() -> updateFirestoreStatus(Lesson.STATUS_STOPPED, 0, 0, currentBPM)).start();
    }

    @Override
    public void onSpeedChanged(int bpmValue) {
        currentBPM = bpmValue;
        
        User user = DataManager.getUserInstance();
        if (isLive && user != null && "teacher".equals(user.getUserType())) {
            new Thread(() -> updateFirestoreStatus(isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED, 0, -1, currentBPM)).start();
        }

        sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
        if (isPlaying) {
            handlePlayback(); // restart interval with new speed
        }
    }
}