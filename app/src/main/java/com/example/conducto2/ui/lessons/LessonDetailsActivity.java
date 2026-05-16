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
 * Activity that displays the details of a specific lesson, including its title, date, notes, and music files.
 * Provides a "Go Live" functionality for teachers to set this lesson as the active live lesson for the class.
 */
public class LessonDetailsActivity extends BaseDrawerActivity implements FirebaseComm.DBResult {

    private TextView lessonTitle;
    private TextView lessonDate;
    private TextView lessonInfo;
    private RecyclerView musicXmlFilesRecyclerView;
    private MaterialButton btnGoLive;
    private TextView tvStatusMessage;
    private MusicXmlAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_details);

        firestoreManager.setDbResult(this);
        initViews();
        // populateLessonView(); // This is called in onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.startListening();
        }
        populateLessonView(); // Refresh lesson details when returning from editing
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    private void initViews() {
        lessonTitle = findViewById(R.id.lesson_details_title);
        lessonDate = findViewById(R.id.lesson_details_date);
        lessonInfo = findViewById(R.id.lesson_details_info);
        musicXmlFilesRecyclerView = findViewById(R.id.music_xml_files_recyclerview);
        musicXmlFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicXmlFilesRecyclerView.setNestedScrollingEnabled(false);
        //musicXmlFilesRecyclerView.setItemAnimator(null); // fix bug in recycle view
        btnGoLive = findViewById(R.id.btn_go_live);
        tvStatusMessage = findViewById(R.id.tv_status_message);
    }

    /**
     * Loads the lesson data from the DataManager and populates the UI.
     * Also sets up the teacher-specific "Live" button.
     */
    private void populateLessonView() {
        Lesson lesson = DataManager.getCurLesson();

        if (lesson == null) {
            return;
        }

        // Log this access for the "Recent Lessons" feature
        String email = firestoreManager.authUserEmail();
        firestoreManager.logLessonAccess(email, lesson.getClassId(), lesson.getId(), lesson.getTitle());

        lessonTitle.setText(lesson.getTitle());
        if (lesson.getDate() != null) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));
        } else {
            lessonDate.setText("No date set");
        }
        lessonInfo.setText(lesson.getInfo());

        setupMusicFilesList();
        setupGoLiveButton();

    }

    /**
     * Configures the music files list with an adapter and click listeners and appropriate
     * query for the student-specific music files based on the file mapping in the lesson.
     */
    private void setupMusicFilesList() {
        Lesson lesson = DataManager.getCurLesson();
        User user = DataManager.getUserInstance();

        if (lesson == null || lesson.getId() == null || DataManager.getCurClass() == null || user == null) {
            return;
        }

        Query query = FirebaseFirestore.getInstance()
                .collection("classes").document(DataManager.getCurClass().getId())
                .collection("lessons").document(lesson.getId())
                .collection("musicFiles");

        // If the user is a student, filter the music files based on the file mapping.
        if (user.getUserType() != null && user.getUserType().equals("student")) {
            Map<String, List<String>> fileMapping = lesson.getFileMapping();
            List<String> assignedUrls = new ArrayList<>();
            String userEmail = user.getEmail();

            if (fileMapping != null && userEmail != null) {
                for (Map.Entry<String, List<String>> entry : fileMapping.entrySet()) {
                    List<String> students = entry.getValue();
                    if (students != null && students.contains(userEmail)) {
                        assignedUrls.add(entry.getKey());
                    }
                }
            }

            if (assignedUrls.isEmpty()) {
                // If no mapping exists for this student, return a query that yields no results.
                query = query.whereEqualTo("url", "NON_EXISTENT_MAPPING");
            } else {
                // Display only the files assigned to the student, limited to 1.
                query = query.whereIn("url", assignedUrls).limit(1);
            }
        }

        FirestoreRecyclerOptions<MusicFile> options = new FirestoreRecyclerOptions.Builder<MusicFile>()
                .setQuery(query, MusicFile.class)
                .build();

        // If an adapter already exists, stop it before creating a new one to prevent leaks
        if (adapter != null) {
            adapter.stopListening();
        }

        adapter = new MusicXmlAdapter(options, false);
        adapter.setOnItemClickListener(selectedFile -> {
            Intent intent = new Intent(this, SMPlayerActivity.class);
            intent.putExtra("isLive", false);
            intent.putExtra("canControlPlayback", true);
            if (selectedFile.getUri() != null) {
                // fileUri contains the path to the musicFile in Firebase Cloud Storage.
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
     * Builds a prompt for the music file and uses Gemini to generate a description.
     * Displays a loading dialog during the process and an AlertDialog with the result.
     */
    private void showAiDescription(MusicFile musicFile) {
        if (musicFile == null || musicFile.getTitle() == null) {
            Toast.makeText(this, "File information is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Build the prompt
        String prompt = "Provide a brief and engaging description (2-3 sentences) about the music piece titled: \"" 
                + musicFile.getTitle() + "\". Focus on its musical style or historical context if known, otherwise give a general description suitable for a student.";

        // 2. Show loading dialog
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("AI is thinking...")
                .setMessage("Generating a description for " + musicFile.getTitle() + "...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // 3. Call Gemini
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
     * Sets up the "Go Live" button's visibility and action based on the user type and lesson status.
     */
    private void setupGoLiveButton() {
        User user = DataManager.getUserInstance();
        Lesson lesson = DataManager.getCurLesson();
        
        boolean isTeacher = user != null && "teacher".equals(user.getUserType());
        boolean isLive = lesson != null && lesson.isLive();
        
        if (isTeacher || isLive) {
            btnGoLive.setVisibility(View.VISIBLE);
            if (isLive) {
                btnGoLive.setText("Live");
                btnGoLive.setEnabled(true);
                btnGoLive.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ClassActivity.class);
                    intent.putExtra("target_tab", 2); // Index of LiveFragment in ClassActivity
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            } else {
                // Only teacher reaches here when isLive is false
                btnGoLive.setText("Go Live");
                btnGoLive.setEnabled(true);
                btnGoLive.setOnClickListener(v -> goLive());
            }
        } else {
            btnGoLive.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the class and lesson in Firestore to set this lesson as the current live lesson.
     */
    private void goLive() {
        Lesson lesson = DataManager.getCurLesson();
        com.example.conducto2.data.model.Class currentClass = DataManager.getCurClass();
        
        if (currentClass == null || lesson == null || lesson.getId() == null) {
            Toast.makeText(this, "Error: Missing data to go live.", Toast.LENGTH_SHORT).show();
            return;
        }

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

        // Set this lesson as live and mark the class as active
        firestoreManager.updateLessonLiveStatus(classId, lesson.getId(), true);
        firestoreManager.updateClassActivity(classId, true);
        
        // Update local state to prevent multiple clicks if updateClassActivity is slow
        currentClass.setActive(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        User user = DataManager.getUserInstance();
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
