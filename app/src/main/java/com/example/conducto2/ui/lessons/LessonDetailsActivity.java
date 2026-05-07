package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.LiveLesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
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
public class LessonDetailsActivity extends BaseDrawerActivity implements FirestoreManager.DBResult {

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
            intent.putExtra("readOnly", true);
            if (selectedFile.getUri() != null) {
                // fileUri contains the path to the musicFile in Firebase Cloud Storage.
                intent.putExtra("fileUri", selectedFile.getUri().toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "File URI is missing.", Toast.LENGTH_SHORT).show();
            }
        });

        musicXmlFilesRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    /**
     * Sets up the "Go Live" button's visibility and action based on the user type.
     */
    private void setupGoLiveButton() {
        User user = DataManager.getUserInstance();
        if (user != null && "teacher".equals(user.getUserType())) {
            btnGoLive.setVisibility(View.VISIBLE);
            btnGoLive.setOnClickListener(v -> goLive());
        } else {
            btnGoLive.setVisibility(View.GONE);
        }
    }

    /**
     * Updates the class document in Firestore to set this lesson as the current live lesson.
     */
    private void goLive() {
        Lesson lesson = DataManager.getCurLesson();
        String classId = DataManager.getCurClass().getId();
        if (classId == null || lesson == null || lesson.getId() == null) {
            Toast.makeText(this, "Error: Missing data to go live.", Toast.LENGTH_SHORT).show();
            return;
        }

        LiveLesson liveLesson = new LiveLesson(classId, lesson.getId(), true);
        firestoreManager.setLiveLesson(liveLesson);
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
    public void uploadResult(boolean success, FirestoreManager.DbOperation operation) {
        // Handled by FirestoreManager displayMessage if success/fail toast is needed
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
