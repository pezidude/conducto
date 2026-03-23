package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.LiveLesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.player.SMPlayerActivity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity that displays the details of a specific lesson, including its title, date, notes, and music files.
 * Provides a "Go Live" functionality for teachers to set this lesson as the active live lesson for the class.
 */
public class LessonDetailsActivity extends BaseDrawerActivity implements FirestoreManager.DBResult {

    private TextView lessonTitle;
    private TextView lessonDate;
    private TextView lessonInfo;
    private ListView musicXmlFilesList;
    private MaterialButton btnGoLive;
    private TextView tvStatusMessage;
    private Lesson lesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_details);

        firestoreManager.setDbResult(this);
        initViews();
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // Refresh lesson details when returning from editing
    }

    private void initViews() {
        lessonTitle = findViewById(R.id.lesson_details_title);
        lessonDate = findViewById(R.id.lesson_details_date);
        lessonInfo = findViewById(R.id.lesson_details_info);
        musicXmlFilesList = findViewById(R.id.music_xml_files_list);
        btnGoLive = findViewById(R.id.btn_go_live);
        tvStatusMessage = findViewById(R.id.tv_status_message);
    }

    /**
     * Loads the lesson data from the DataManager and populates the UI.
     * Also sets up the teacher-specific "Go Live" button.
     */
    private void loadData() {
        lesson = DataManager.getCurLessonInstance();

        if (lesson != null) {
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
    }

    /**
     * Configures the music files list with an adapter and click listeners.
     */
    private void setupMusicFilesList() {
        if (lesson.getMusicXMLFiles() != null && !lesson.getMusicXMLFiles().isEmpty()) {
            List<String> fileTitles = new ArrayList<>();
            for (MusicFile musicFile : lesson.getMusicXMLFiles()) {
                fileTitles.add(musicFile.getTitle());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, fileTitles);
            musicXmlFilesList.setAdapter(adapter);

            // Adjust height to show all elements inside the ScrollView
            setListViewHeightBasedOnChildren(musicXmlFilesList);

            musicXmlFilesList.setOnItemClickListener((parent, view, position, id) -> {
                MusicFile selectedFile = lesson.getMusicXMLFiles().get(position);
                Intent intent = new Intent(this, SMPlayerActivity.class);
                intent.putExtra("readOnly", true);
                if (selectedFile.getUri() != null) {
                    intent.putExtra("fileUri", selectedFile.getUri().toString());
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "File URI is missing.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            musicXmlFilesList.setAdapter(null);
        }
    }

    /**
     * Helper to set ListView height based on its children so it expands fully inside a ScrollView.
     */
    private void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
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
        String classId = DataManager.getCurClassID();
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
    public void uploadResult(boolean success) {
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
