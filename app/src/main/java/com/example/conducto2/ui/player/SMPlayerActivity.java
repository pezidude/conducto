package com.example.conducto2.ui.player;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
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

import com.example.conducto2.data.firebase.FirebaseComm;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

/**
 * SMPlayerActivity (Sheet Music Player)
 * 
 * This class is the central hub for sheet music interaction in the Conducto application.
 * It manages a WebView-based rendering engine (OSMD) to display MusicXML files and provides
 * a native Android interface for playback control, tempo adjustment, and audio mixing.
 * 
 * In Live Lesson mode, it coordinates real-time synchronization between a teacher's device
 * and multiple student devices using Firebase Firestore, ensuring all participants are
 * viewing and playing the same measure at the same tempo.
 */
public class SMPlayerActivity extends AppCompatActivity implements PlaybackFragment.PlaybackControlsListener {

    /** Identifier for logging and debugging purposes. */
    private static final String TAG = "SMPlayerActivity";

    /** The WebView used to host the OpenSheetMusicDisplay (OSMD) rendering engine. */
    private WebView sheetMusicView;

    /** The UI container for the PlaybackFragment overlay. */
    private View playbackFragmentContainer;

    /** Button to toggle the visibility of the playback controls. */
    private ImageButton btnTogglePlayback;

    /** Button to open the side drawer containing the mixer and participant list. */
    private ImageButton btnOpenMixer;

    /** Button visible to teachers to terminate a live session for all students. */
    private Button btnEndLiveLesson;

    /** Root layout providing the sliding drawer functionality. */
    private DrawerLayout drawerLayout;

    /** List view for controlling individual instrument volumes and states (mute/solo). */
    private RecyclerView mixerRecyclerView;

    /** Adapter managing the data binding for the mixer UI. */
    private MixerAdapter mixerAdapter;

    /** List view displaying the current participants in the class/lesson. */
    private RecyclerView participantRecyclerView;

    /** Adapter managing the data binding for the participant list UI. */
    private ParticipantAdapter participantAdapter;

    /** UI text element for the visual metronome lead-in countdown. */
    private TextView tvCountdown;

    /** Flag indicating whether the countdown metronome is currently running. */
    private boolean isCountdownActive = false;

    /** Tracks the last received playback status (PLAYING, PAUSED, STOPPED) to detect changes. */
    private String lastStatus = null;

    /** Tracks the last received measure index to detect synchronization jumps. */
    private int lastMeasure = -1;

    /** Local state indicating if the audio engine is currently playing. */
    private boolean isPlaying = false;

    /** Flag indicating if the OSMD engine has finished parsing and rendering the score. */
    private boolean isScoreLoaded = false;

    /** Buffer for a status update received before the score was ready to be manipulated. */
    private String pendingStatus = null;

    /** Buffer for a target timestamp received before the score was ready. */
    private long pendingTargetTimestamp = 0;

    /** Buffer for a measure index received before the score was ready. */
    private int pendingMeasure = -1;

    /** Buffer for a BPM value received before the score was ready. */
    private int pendingBpm = -1;

    /** The current playback speed in Beats Per Minute. Default is 100. */
    private int currentBPM = 100;

    /** Facilitates all interactions with the Firebase Firestore database. */
    private FirestoreManager firestoreManager;

    /** Reference to the real-time listener for lesson status updates from Firestore. */
    private ListenerRegistration statusListener;

    /** 
     * playbackHandler
     * 
     * A central timing and synchronization engine for the activity. 
     * It uses the Android {@link Handler} system linked to the Main (UI) Looper to manage:
     * 1. Periodic Progress Polling: Scheduling recurring tasks to update the progress bar.
     * 2. Visual Metronome: Managing nested delayed UI updates for the countdown.
     * 3. Network Synchronization: Delaying local execution of "Play" commands to align
     *    with the calculated server-time offset across multiple devices.
     * 4. UI Thread Marshaling: Posting updates from background threads or JavaScript callbacks.
     */
    private final Handler playbackHandler = new Handler(Looper.getMainLooper());

    /** Flag indicating if the current session is a real-time live lesson. */
    private boolean isLive = false;

    /** Permission flag determined by user role or lesson configuration. */
    private boolean canControlPlayback = true;

    /** Logic flag to prevent jitter/jumps during the initial connection to a live session. */
    private boolean isFirstSync = true;

    /** The measured difference between local device time and Firebase server time. */
    private long serverTimeOffset = 0;

    /**
     * Runnable that periodically polls the JavaScript engine for current playback progress.
     * Updates the native progress bar to stay in sync with the WebView cursor.
     */
    private final Runnable progressPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (sheetMusicView != null && isPlaying && !isFinishing() && !isDestroyed()) {
                sheetMusicView.evaluateJavascript("getPlaybackProgress();", value -> {
                    try {
                        if (value != null && !value.equals("null")) {
                            // Extract JSON string from JS evaluation result.
                            String jsonStr = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                            org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                            int current = json.getInt("currentMeasure");
                            int total = json.getInt("totalMeasures");
                            
                            updateProgressBar(current, total);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing progress: " + value, e);
                    }
                });
                playbackHandler.postDelayed(this, 500); // Poll every 500ms
            }
        }
    };

    /**
     * Updates the progress bar in the PlaybackFragment.
     * @param current The current measure index.
     * @param total The total number of measures in the score.
     */
    private void updateProgressBar(int current, int total) {
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);
        if (playbackFragment != null) {
            playbackFragment.updateProgress(current, total);
        }
    }

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
        setupBackNavigation();

        Lesson curLesson = DataManager.getCurLesson();
        if (curLesson != null) {
            String email = firestoreManager.authUserEmail();
            firestoreManager.logLessonAccess(email, curLesson.getClassId(), curLesson.getId(), curLesson.getTitle(), curLesson.getGenre());
        }
    }

    /**
     * Initializes the UI components and sets up view-specific configurations based on the user's role.
     * It configures the participant list and, for teachers, sets up the audio mixer and live lesson controls.
     */
    public void initViews() {
        sheetMusicView = findViewById(R.id.sheetMusicView);
        playbackFragmentContainer = findViewById(R.id.playback_fragment_container);
        btnTogglePlayback = findViewById(R.id.btn_toggle_playback);
        btnOpenMixer = findViewById(R.id.btn_open_mixer);
        btnEndLiveLesson = findViewById(R.id.btn_end_live_lesson);
        drawerLayout = findViewById(R.id.drawer_layout);
        mixerRecyclerView = findViewById(R.id.mixer_recycler_view);
        participantRecyclerView = findViewById(R.id.participant_recycler_view);
        tvCountdown = findViewById(R.id.tv_countdown);

        User user = DataManager.getUserInstance();
        if (user != null) {
            btnOpenMixer.setVisibility(View.VISIBLE);
            btnOpenMixer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));
            
            setupParticipantRecyclerView();

            if ("teacher".equals(user.getUserType())) {
                mixerRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                if (isLive) {
                    btnEndLiveLesson.setVisibility(View.VISIBLE);
                    btnEndLiveLesson.setOnClickListener(v -> showEndLiveLessonConfirmation());
                }
            } else {
                mixerRecyclerView.setVisibility(View.GONE);
                findViewById(R.id.tv_mixer_header).setVisibility(View.GONE);
            }
        } else {
            btnOpenMixer.setVisibility(View.GONE);
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        if (!canControlPlayback) {
            btnTogglePlayback.setVisibility(View.GONE);
        }

        btnTogglePlayback.setOnClickListener(v -> {
            if (playbackFragmentContainer.getVisibility() == View.VISIBLE) {
                playbackFragmentContainer.setVisibility(View.GONE);
                btnTogglePlayback.animate().rotation(270).setDuration(200).start();
            } else {
                playbackFragmentContainer.setVisibility(View.VISIBLE);
                btnTogglePlayback.animate().rotation(90).setDuration(200).start();
            }
        });
        btnTogglePlayback.setEnabled(false);
    }

    /**
     * Configures the Participant RecyclerView with a real-time FirestoreRecyclerAdapter.
     */
    private void setupParticipantRecyclerView() {
        Class curClass = DataManager.getCurClass();
        if (curClass == null || curClass.getMembers() == null) return;

        List<String> allEmails = new ArrayList<>(curClass.getMembers());
        if (curClass.getOwnerEmail() != null && !allEmails.contains(curClass.getOwnerEmail())) {
            allEmails.add(curClass.getOwnerEmail());
        }

        if (allEmails.isEmpty()) return;

        Query query = FirebaseComm.getCollectionReference("users")
                .whereIn("email", allEmails)
                .orderBy("fname", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                .setQuery(query, User.class)
                .build();

        participantAdapter = new ParticipantAdapter(options, new ArrayList<>());
        participantRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        participantRecyclerView.setAdapter(participantAdapter);
    }

    /**
     * Enables playback controls if the user has permission.
     */
    public void enablePlayback() {
        if (canControlPlayback) {
            btnTogglePlayback.setEnabled(true);
            btnTogglePlayback.callOnClick();
        }
    }

    /**
     * Configures the back button behavior to show confirmation dialogs during live lessons.
     */
    private void setupBackNavigation() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                User user = DataManager.getUserInstance();
                if (isLive && user != null) {
                    if ("teacher".equals(user.getUserType())) {
                        showTeacherBackDialog();
                    } else {
                        showStudentBackDialog();
                    }
                } else {
                    setEnabled(false);
                    onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showStudentBackDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leaving Live Lesson")
                .setMessage("Are you sure you want to leave the live lesson?")
                .setPositiveButton("Leave", (dialog, which) -> finish())
                .setNegativeButton("Stay", null)
                .show();
    }

    private void showTeacherBackDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leaving Live Lesson")
                .setMessage("Would you like to end the live lesson for all students or just leave?")
                .setPositiveButton("End Lesson", (dialog, which) -> endLiveLesson())
                .setNeutralButton("Just Leave", (dialog, which) -> {
                    if (isPlaying) {
                        isPlaying = false;
                        pauseAndSync(this::finish);
                    } else {
                        finish();
                    }
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    /**
     * Pauses playback and synchronizes the pause state to Firestore before executing a callback.
     * @param onComplete Callback to run after synchronization is initiated.
     */
    private void pauseAndSync(Runnable onComplete) {
        User user = DataManager.getUserInstance();
        if (isLive && user != null && "teacher".equals(user.getUserType())) {
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
                new Thread(() -> {
                    updateFirestoreStatus(Lesson.STATUS_PAUSED, 0, finalMeasure, currentBPM);
                    if (onComplete != null) {
                        playbackHandler.post(onComplete);
                    }
                }).start();
            });
        } else {
            executePause();
            if (onComplete != null) onComplete.run();
        }
    }

    /**
     * Interface exposed to the WebView JavaScript engine.
     */
    private class WebAppInterface {
        @JavascriptInterface
        public void onEngineReady() {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || sheetMusicView == null) return;
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
                if (isFinishing() || isDestroyed() || sheetMusicView == null) return;
                Toast.makeText(SMPlayerActivity.this, "File loaded.", Toast.LENGTH_SHORT).show();
                isScoreLoaded = true;

                User user = DataManager.getUserInstance();
                Class cls = DataManager.getCurClass();
                Lesson lesson = DataManager.getCurLesson();
                if (isLive && user != null && cls != null && lesson != null) {
                    firestoreManager.updateStudentPresence(cls.getId(), lesson.getId(), user.getEmail(), true);
                }

                sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
                
                if (isLive && user != null) {
                    if ("student".equals(user.getUserType())) {
                        sheetMusicView.evaluateJavascript("setGlobalMute(true);", null);
                    } else if ("teacher".equals(user.getUserType())) {
                        setupMixer(instrumentNamesJson);
                    }
                }

                if (canControlPlayback) {
                    sheetMusicView.evaluateJavascript("setClickToSeekEnabled(true);", null);
                } else {
                    sheetMusicView.evaluateJavascript("setClickToSeekEnabled(false);", null);
                }

                PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.playback_fragment_container);
                if (playbackFragment != null) {
                    playbackFragment.setSpeedControlEnabled(canControlPlayback);
                }

                enablePlayback();

                if (pendingStatus != null) {
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
                    new Thread(() -> updateFirestoreStatus(isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED, 0, measureIndex, currentBPM)).start();
                }
            });
        }
    }

    /**
     * Initializes the audio mixer UI based on the instrument names found in the score.
     * @param instrumentNamesJson JSON array of instrument names.
     */
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

    /**
     * Terminates the live lesson and updates the archived state in Firestore.
     */
    private void endLiveLesson() {
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        if (cls != null && lesson != null) {
            firestoreManager.updateLessonLiveStatus(cls.getId(), lesson.getId(), false);
            firestoreManager.updateLessonArchivedStatus(cls.getId(), lesson.getId(), true);
            firestoreManager.updateClassActivity(cls.getId(), false);
            
            cls.setActive(false);
            lesson.setLive(false);
            lesson.setArchived(true);

            Toast.makeText(this, "Live lesson ended", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Configures the WebView settings and registers the JavaScript bridge.
     */
    private void setupWebView() {
        WebSettings settings = sheetMusicView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        sheetMusicView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        sheetMusicView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Page finished loading");
            }
        });
        sheetMusicView.loadUrl("file:///android_asset/viewer.html");
    }

    /**
     * Injects the MusicXML data into the OSMD renderer via JavaScript.
     * @param xmlData The raw MusicXML string.
     */
    private void loadXmlInWebView(String xmlData) {
        double zoomLevel = 0.75;
        String escapedXml = xmlData.replace("`", "\\`").replace("$", "\\$");
        if (sheetMusicView != null ) {
            sheetMusicView.evaluateJavascript("loadScore(`" + escapedXml + "`, " + zoomLevel + ");", null);
        }
    }

    /**
     * Reads a file from a URI and loads it into the WebView cursor.
     * Performs file I/O on a background thread.
     * @param uri The URI of the MusicXML file.
     */
    private void loadFile(Uri uri) {
        FileIO fileOps = new FileIO(this);
        String fileName = fileOps.getFileName(uri);
        Log.d(TAG, "Loading: " + fileName);

        new Thread(() -> { 
            try {
                String xmlContent = fileOps.readMusicXmlContent(uri);
                if (xmlContent == null || xmlContent.isEmpty()) {
                    throw new Exception("Empty or invalid file content");
                }
                runOnUiThread(() -> {
                    if (xmlContent.contains("<?xml") || xmlContent.contains("<score-partwise")) {
                        loadXmlInWebView(xmlContent);
                    } else {
                        showErrorDialog("File Not Recognized", "The selected file does not appear to be a valid MusicXML file.");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error reading file", e);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showErrorDialog("Failed to Load File", "An error occurred while reading the file: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showErrorDialog(String title, String message) {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelCountdown();
        User user = DataManager.getUserInstance();
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();
        if (isLive && user != null && cls != null && lesson != null) {
            firestoreManager.updateStudentPresence(cls.getId(), lesson.getId(), user.getEmail(), false);
        }
        stopStatusListener();
        if (participantAdapter != null) {
            participantAdapter.stopListening();
        }
        playbackHandler.removeCallbacksAndMessages(null);
        if (sheetMusicView != null) {
            sheetMusicView.pauseTimers();
            sheetMusicView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStatusListener();
        if (participantAdapter != null) {
            participantAdapter.startListening();
        }
        if (sheetMusicView != null) {
            sheetMusicView.onResume();
            sheetMusicView.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        stopStatusListener();
        playbackHandler.removeCallbacksAndMessages(null);
        if (sheetMusicView != null) {
            if (sheetMusicView.getParent() instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) sheetMusicView.getParent()).removeView(sheetMusicView);
            }
            sheetMusicView.evaluateJavascript("destroyEngine();", null);
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

    /**
     * Subscribes to the lesson document in Firestore to receive real-time updates.
     */
    private void startStatusListener() {
        User user = DataManager.getUserInstance();
        com.example.conducto2.data.model.Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();

        if (isLive && user != null && cls != null && lesson != null) {
            statusListener = FirebaseFirestore.getInstance()
                    .collection("classes").document(cls.getId())
                    .collection("lessons").document(lesson.getId())
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) return;
                        if (snapshot != null && snapshot.exists()) {
                            Boolean live = snapshot.getBoolean("isLive");
                            if (live != null && !live && isLive) {
                                showLessonEndedDialog();
                                return;
                            }
                            String status = snapshot.getString("status");
                            Long targetTimestamp = snapshot.getLong("targetTimestamp");
                            Long currentMeasure = snapshot.getLong("currentMeasure");
                            Long bpm = snapshot.getLong("bpm");
                            List<String> connectedStudents = (List<String>) snapshot.get("connectedStudents");
                            if (participantAdapter != null) {
                                runOnUiThread(() -> participantAdapter.setConnectedEmails(connectedStudents));
                            }
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

    /**
     * Resolves updates received from the network and adjusts the local state accordingly.
     * This method handles state transitions (play, pause, stop), tempo changes, and measure jumps.
     * 
     * @param status The new lesson status (PLAYING, PAUSED, STOPPED).
     * @param targetTimestamp The server time when playback should start.
     * @param currentMeasure The measure index to jump to.
     * @param bpm The new tempo in BPM.
     */
    private void handleStatusChange(String status, long targetTimestamp, int currentMeasure, int bpm) {
        if (!isScoreLoaded) {
            pendingStatus = status;
            pendingTargetTimestamp = targetTimestamp;
            pendingMeasure = currentMeasure;
            pendingBpm = bpm;
            return;
        }

        boolean statusChanged = status != null && !status.equals(lastStatus);
        boolean measureChanged = currentMeasure >= 0 && currentMeasure != lastMeasure;
        boolean bpmChanged = bpm > 0 && bpm != currentBPM;

        if (!statusChanged && !measureChanged && !bpmChanged) return;

        lastStatus = status;
        lastMeasure = currentMeasure;

        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);

        if (bpmChanged) {
            currentBPM = bpm;
            sheetMusicView.evaluateJavascript("setBpm(" + currentBPM + ");", null);
            if (playbackFragment != null) playbackFragment.updateBpmUI(currentBPM);
        }

        if (!statusChanged && !measureChanged) return;

        playbackHandler.removeCallbacksAndMessages(null);
        cancelCountdown();

        if (Lesson.STATUS_PLAYING.equals(status) && isFirstSync && isLive) {
            isFirstSync = false;
            if (playbackFragment != null) playbackFragment.setPlaying(false);
            return;
        }

        if (currentMeasure >= 0) {
            sheetMusicView.evaluateJavascript("jumpToMeasure(" + currentMeasure + ");", null);
            sheetMusicView.evaluateJavascript("getPlaybackProgress();", value -> {
                try {
                    if (value != null && !value.equals("null")) {
                        String jsonStr = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                        org.json.JSONObject json = new org.json.JSONObject(jsonStr);
                        updateProgressBar(json.getInt("currentMeasure"), json.getInt("totalMeasures"));
                    }
                } catch (Exception ignored) {}
            });
        }

        if (Lesson.STATUS_PLAYING.equals(status)) {
            isFirstSync = false;
            if (isLive && targetTimestamp > 0) {
                long currentServerTime = System.currentTimeMillis() + serverTimeOffset;
                long delay = 1000 - (currentServerTime - targetTimestamp);
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
            isFirstSync = false;
            executePause();
            if (playbackFragment != null) playbackFragment.setPlaying(false);
        } else if (Lesson.STATUS_STOPPED.equals(status)) {
            isFirstSync = false;
            executeStop();
            if (playbackFragment != null) playbackFragment.setPlaying(false);
        }
    }

    /**
     * Executes the play command in the WebView and starts progress polling.
     */
    private void executePlay() {
        isPlaying = true;
        sheetMusicView.evaluateJavascript("setClickToSeekEnabled(false);", null);
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);
        if (playbackFragment != null) {
            playbackFragment.setSpeedControlEnabled(false);
        }
        startCountdown(() -> {
            sheetMusicView.evaluateJavascript("play();", null);
            playbackHandler.post(progressPollRunnable);
        });
    }

    /**
     * Executes the pause command in the WebView.
     */
    private void executePause() {
        isPlaying = false;
        playbackHandler.removeCallbacks(progressPollRunnable);
        cancelCountdown();
        if (canControlPlayback) {
            sheetMusicView.evaluateJavascript("setClickToSeekEnabled(true);", null);
        }
        sheetMusicView.evaluateJavascript("pause();", null);
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);
        if (playbackFragment != null && canControlPlayback) {
            playbackFragment.setSpeedControlEnabled(true);
        }
    }

    /**
     * Executes the stop command in the WebView and resets UI progress.
     */
    private void executeStop() {
        isPlaying = false;
        playbackHandler.removeCallbacks(progressPollRunnable);
        cancelCountdown();
        if (canControlPlayback) {
            sheetMusicView.evaluateJavascript("setClickToSeekEnabled(true);", null);
        }
        sheetMusicView.evaluateJavascript("stop();", null);
        updateProgressBar(0, 1);
        PlaybackFragment playbackFragment = (PlaybackFragment) getSupportFragmentManager()
                .findFragmentById(R.id.playback_fragment_container);
        if (playbackFragment != null && canControlPlayback) {
            playbackFragment.setSpeedControlEnabled(true);
        }
    }

    /**
     * Runs a visual metronome countdown on the UI thread.
     * @param onFinished Callback to run when the countdown reaches zero.
     */
    private void startCountdown(Runnable onFinished) {
        if (isCountdownActive) return;
        isCountdownActive = true;
        tvCountdown.setVisibility(View.VISIBLE);
        tvCountdown.setText("4");
        long beatDuration = 60000 / Math.max(1, currentBPM);
        playbackHandler.postDelayed(() -> {
            if (!isCountdownActive) return;
            tvCountdown.setText("3");
            playbackHandler.postDelayed(() -> {
                if (!isCountdownActive) return;
                tvCountdown.setText("2");
                playbackHandler.postDelayed(() -> {
                    if (!isCountdownActive) return;
                    tvCountdown.setText("1");
                    playbackHandler.postDelayed(() -> {
                        if (!isCountdownActive) return;
                        tvCountdown.setVisibility(View.GONE);
                        isCountdownActive = false;
                        if (onFinished != null) onFinished.run();
                    }, beatDuration);
                }, beatDuration);
            }, beatDuration);
        }, beatDuration);
    }

    /**
     * Cancels the active metronome countdown.
     */
    private void cancelCountdown() {
        isCountdownActive = false;
        if (tvCountdown != null) tvCountdown.setVisibility(View.GONE);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onPlayPauseClicked() {
        isPlaying = !isPlaying;
        if (isPlaying && isLive) {
            User user = DataManager.getUserInstance();
            if (user != null && "teacher".equals(user.getUserType())) {
                sheetMusicView.evaluateJavascript("if(playbackManager && playbackManager.ac) playbackManager.ac.resume();", null);
            }
        }
        handlePlayback();
    }

    /**
     * Centralized logic to trigger playback locally and sync the state to Firestore.
     */
    private void handlePlayback() {
        String status = isPlaying ? Lesson.STATUS_PLAYING : Lesson.STATUS_PAUSED;
        User user = DataManager.getUserInstance();
        if (isLive && "teacher".equals(user.getUserType())) {
            if (isPlaying) {
                long targetTimestamp = System.currentTimeMillis() + serverTimeOffset;
                updateFirestoreStatus(status, targetTimestamp, -1, currentBPM);
            } else {
                pauseAndSync(null);
            }
        } else {
            if (isPlaying) executePlay();
            else executePause();
            new Thread(() -> updateFirestoreStatus(status, 0, -1, currentBPM)).start();
        }
    }

    /**
     * Updates the lesson document in Firestore with current playback metadata.
     */
    private void updateFirestoreStatus(String status, long targetTimestamp, int currentMeasure, int bpm) {
        User user = DataManager.getUserInstance();
        Class cls = DataManager.getCurClass();
        Lesson lesson = DataManager.getCurLesson();
        if (isLive && user != null && "teacher".equals(user.getUserType()) && cls != null && lesson != null) {
            firestoreManager.updateLessonStatus(cls.getId(), lesson.getId(), status, targetTimestamp, currentMeasure, bpm);
        }
    }

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
        if (isPlaying) handlePlayback();
    }
}