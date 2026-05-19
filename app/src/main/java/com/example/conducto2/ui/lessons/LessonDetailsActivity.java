package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.manager.GeminiManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.classes.ClassActivity;
import com.example.conducto2.ui.player.SMPlayerActivity;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * LessonDetailsActivity
 * 
 * This activity provides the read-only overview of a specific lesson. It displays
 * basic metadata (Title, Date, Description) and lists the associated MusicXML files.
 * 
 * Key Responsibilities:
 * 1. Role-Based Rendering: Teachers see the "Go Live" functionality; students see
 *    only the music files specifically assigned to them.
 * 2. AI Integration: Interfaces with {@link GeminiManager} to generate automated 
 *    educational descriptions of sheet music.
 * 3. Navigation: Acts as the launchpad into the {@link SMPlayerActivity}.
 */
public class LessonDetailsActivity extends BaseDrawerActivity implements FirebaseComm.DBResult {

    private TextView lessonTitle;
    private TextView lessonDate;
    private TextView lessonInfo;
    private RecyclerView musicXmlFilesRecyclerView;
    private TextView tvNoFiles;
    private MaterialButton btnGoLive;
    private TextView tvStatusMessage;
    private MusicXmlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_details);

        firestoreManager.setDbResult(this);
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startListening();
        }
        
        // Refresh class data to ensure we have the latest isActive status before rendering the Live button
        if (DataManager.getCurClass() != null) {
            firestoreManager.getClassById(DataManager.getCurClass().getId(), updatedClass -> {
                if (updatedClass != null) {
                    DataManager.setCurClass(updatedClass);
                    setupGoLiveButton(); 
                }
            });
        }
        
        populateLessonView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    /**
     * Binds UI components to their layout IDs.
     */
    private void initViews() {
        lessonTitle = findViewById(R.id.lesson_details_title);
        lessonDate = findViewById(R.id.lesson_details_date);
        lessonInfo = findViewById(R.id.lesson_details_info);
        musicXmlFilesRecyclerView = findViewById(R.id.music_xml_files_recyclerview);
        tvNoFiles = findViewById(R.id.tv_no_files);
        musicXmlFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicXmlFilesRecyclerView.setNestedScrollingEnabled(false);
        btnGoLive = findViewById(R.id.btn_go_live);
        tvStatusMessage = findViewById(R.id.tv_status_message);
    }

    /**
     * Loads the lesson data from the DataManager and populates the UI text elements.
     * Also triggers the "Recent Lessons" logging via FirestoreManager.
     */
    private void populateLessonView() {
        Lesson lesson = DataManager.getCurLesson();

        if (lesson == null) {
            return;
        }

        // History Feature: Log this access for the personalized dashboard
        String email = firestoreManager.authUserEmail();
        firestoreManager.logLessonAccess(email, lesson.getClassId(), lesson.getId(), lesson.getTitle(), lesson.getGenre());

        lessonTitle.setText(lesson.getTitle());
        if (lesson.getDate() != null) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));
        } else {
            lessonDate.setText("No date set");
        }
        
        // UI Polish: Style empty descriptions clearly
        if (lesson.getInfo() == null || lesson.getInfo().trim().isEmpty()) {
            lessonInfo.setText(R.string.label_lesson_no_notes);
            lessonInfo.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            lessonInfo.setTypeface(null, android.graphics.Typeface.ITALIC);
        } else {
            lessonInfo.setText(lesson.getInfo());
            lessonInfo.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            lessonInfo.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        setupMusicFilesList();
        setupGoLiveButton();
    }

    /**
     * Configures the RecyclerView to display the correct music files.
     * Implementation includes strong Role-Based Access Control (RBAC):
     * - Teachers see all files.
     * - Students only see files mapped explicitly to their email address.
     */
    private void setupMusicFilesList() {
        Lesson lesson = DataManager.getCurLesson();
        User user = DataManager.getUserInstance();

        if (lesson == null || lesson.getId() == null || DataManager.getCurClass() == null || user == null) {
            return;
        }

        // Base query targets all files for this lesson
        Query query = FirebaseFirestore.getInstance()
                .collection("classes").document(DataManager.getCurClass().getId())
                .collection("lessons").document(lesson.getId())
                .collection("musicFiles");

        // Authorization Logic: Filter files if the user is a student
        if (user.getUserType() != null && user.getUserType().equals("student")) {
            Map<String, List<String>> fileMapping = lesson.getFileMapping();
            List<String> assignedUrls = new ArrayList<>();
            String userEmail = user.getEmail();

            if (fileMapping != null && userEmail != null) {
                // Scan the mapping table for the student's email
                for (Map.Entry<String, List<String>> entry : fileMapping.entrySet()) {
                    List<String> students = entry.getValue();
                    if (students != null && students.contains(userEmail)) {
                        assignedUrls.add(entry.getKey());
                    }
                }
            }

            if (assignedUrls.isEmpty()) {
                // Security: If no assignment exists, forcefully break the query
                query = query.whereEqualTo("url", "NON_EXISTENT_MAPPING");
            } else {
                // Display only the assigned files (typically limited to 1 for students)
                query = query.whereIn("url", assignedUrls).limit(1);
            }
        }

        FirestoreRecyclerOptions<MusicFile> options = new FirestoreRecyclerOptions.Builder<MusicFile>()
                .setQuery(query, MusicFile.class)
                .build();

        if (adapter != null) {
            adapter.stopListening();
        }

        // Initialize adapter in "View Mode" (showButtons=false, showAiButton=true)
        adapter = new MusicXmlAdapter(options, false, true) {
            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (tvNoFiles != null) {
                    tvNoFiles.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
                }
                if (musicXmlFilesRecyclerView != null) {
                    musicXmlFilesRecyclerView.setVisibility(getItemCount() == 0 ? View.GONE : View.VISIBLE);
                }
            }
        };
        
        // Navigation Logic: Launch SMPlayerActivity
        adapter.setOnItemClickListener(selectedFile -> {
            Intent intent = new Intent(this, SMPlayerActivity.class);
            intent.putExtra("isLive", false);
            // In View Details mode, users generally have playback control unless it's a live session
            intent.putExtra("canControlPlayback", true);
            if (selectedFile.getUri() != null) {
                intent.putExtra("fileUri", selectedFile.getUri().toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "File URI is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnAiInfoClickListener(this::showAiDescription);

        musicXmlFilesRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    /**
     * Integrates with the Google Gemini API to dynamically generate educational 
     * descriptions of the selected sheet music.
     * 
     * @param musicFile The file object containing the title to be queried.
     */
    private void showAiDescription(MusicFile musicFile) {
        if (musicFile == null || musicFile.getTitle() == null) {
            Toast.makeText(this, "File information is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Context Engineering: Build a strict prompt for the LLM
        String prompt = "Provide a brief and engaging description (2-3 sentences) about the music piece titled: \"" 
                + musicFile.getTitle() + "\". Focus on its musical style or historical context if known, otherwise give a general description suitable for a student.";

        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("AI is thinking...")
                .setMessage("Generating a description for " + musicFile.getTitle() + "...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Network Call: Delegate prompt to the GeminiManager wrapper
        GeminiManager.getInstance(this).sendMessage(prompt, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    new AlertDialog.Builder(LessonDetailsActivity.this)
                            .setTitle(musicFile.getTitle())
                            .setMessage(result)
                            .setPositiveButton("Close", null)
                            .show();
                });
            }

            @Override
            public void onError(Throwable error) {
                runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(LessonDetailsActivity.this, "AI Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Configures the "Go Live" button. Validates that the user is a teacher 
     * and checks if a live session is already running in the current class.
     */
    private void setupGoLiveButton() {
        User user = DataManager.getUserInstance();
        Lesson lesson = DataManager.getCurLesson();
        
        boolean isTeacher = user != null && "teacher".equals(user.getUserType());
        boolean isLive = lesson != null && lesson.isLive();
        
        if (isLive) {
            // State: Lesson is already live. Change button to a navigation shortcut.
            btnGoLive.setVisibility(View.VISIBLE);
            btnGoLive.setText("Live");
            btnGoLive.setEnabled(true);
            btnGoLive.setOnClickListener(v -> {
                Intent intent = new Intent(this, ClassActivity.class);
                intent.putExtra("target_tab", 2); // Jump directly to LiveFragment
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });
        } else if (isTeacher) {
            // State: Teacher viewing an inactive lesson. Allow them to initiate broadcast.
            btnGoLive.setVisibility(View.VISIBLE);
            btnGoLive.setText("Go Live");
            btnGoLive.setEnabled(true);
            btnGoLive.setOnClickListener(v -> goLive());
        } else {
            // State: Student viewing an inactive lesson. Hide the button entirely.
            btnGoLive.setVisibility(View.GONE);
        }
    }

    /**
     * Executes the process to set the current lesson as the active Live broadcast 
     * across the entire class context.
     */
    private void goLive() {
        Lesson lesson = DataManager.getCurLesson();
        com.example.conducto2.data.model.Class currentClass = DataManager.getCurClass();
        
        if (currentClass == null || lesson == null || lesson.getId() == null) {
            Toast.makeText(this, "Error: Missing data to go live.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validation: Prevent multiple simultaneous live lessons in the same class.
        if (currentClass.isActive()) {
            displayMessage("A lesson is already active for this class. Please stop it first.");
            return;
        }

        String classId = currentClass.getId();

        btnGoLive.setEnabled(false);
        if (tvStatusMessage != null) {
            tvStatusMessage.setText("Going live...");
            tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvStatusMessage.setVisibility(View.VISIBLE);
        }

        // Database Sync: Update both the lesson flag and the class-wide active flag.
        firestoreManager.updateLessonLiveStatus(classId, lesson.getId(), true);
        firestoreManager.updateClassActivity(classId, true);
        
        // Local Cache Sync
        currentClass.setActive(true);
        lesson.setLive(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        User user = DataManager.getUserInstance();
        // UI Modification: Only teachers get the pencil icon to enter LessonEditActivity.
        if (user != null && "teacher".equals(user.getUserType())) {
            MenuItem editItem = menu.add(Menu.NONE, 1001, Menu.NONE, "Edit");
            editItem.setIcon(R.drawable.ic_edit);
            editItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            editItem.setOnMenuItemClickListener(v -> {
                Intent intent = new Intent(this, LessonEditActivity.class);
                startActivity(intent);
                return true;
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Handles the callback from FirestoreManager after attempting to "Go Live".
     */
    @Override
    public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
        if (operation == FirebaseComm.DbOperation.UPDATE_LESSON_LIVE_STATUS) {
            runOnUiThread(() -> {
                if (success) {
                    Lesson lesson = DataManager.getCurLesson();
                    if (lesson != null) {
                        lesson.setLive(true);
                    }
                    setupGoLiveButton();
                    if (tvStatusMessage != null) {
                        tvStatusMessage.setText("Lesson is now live!");
                        tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.text_success));
                        tvStatusMessage.setVisibility(View.VISIBLE);
                    }
                } else {
                    btnGoLive.setEnabled(true);
                    if (tvStatusMessage != null) {
                        tvStatusMessage.setText("Failed to go live. Please try again.");
                        tvStatusMessage.setTextColor(ContextCompat.getColor(this, R.color.text_error));
                        tvStatusMessage.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    @Override
    public void displayMessage(String message) {
        if (tvStatusMessage != null) {
            tvStatusMessage.setText(message);
            tvStatusMessage.setVisibility(View.VISIBLE);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}