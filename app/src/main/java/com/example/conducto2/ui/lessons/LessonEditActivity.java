package com.example.conducto2.ui.lessons;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.utils.FileHelper;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LessonEditActivity extends BaseDrawerActivity implements FirestoreManager.DBResult, MusicXmlAdapter.OnAssignButtonClickListener, MusicXmlAdapter.OnDeleteButtonClickListener, MusicXmlAdapter.OnRenameListener {

    private EditText lessonTitleInput;
    private EditText lessonInfoInput;
    private Button lessonDatePicker;
    private Button lessonTimePicker;
    private TextView dateTextView;
    private TextView timeTextView;
    private Button saveLessonButton;
    private Button uploadMusicXmlButton;
    private Button groupVoicesPickerButton;
    private RecyclerView musicXmlRecyclerView;
    private MusicXmlAdapter musicXmlAdapter;
    private LinearProgressIndicator loadingProgress;

    private final ArrayList<String> classAttendees = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private Lesson currentLesson;
    private String classId;
    private boolean isEditMode = false;
    private final Calendar calendar = Calendar.getInstance();
    private int pendingTasks = 0;
    private boolean shouldFinishOnTasksEnd = false;
    private final List<String> pendingDeletions = new ArrayList<>();
    private final List<String> pendingUrlDeletions = new ArrayList<>();

    private final ActivityResultLauncher<Intent> musicXmlLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    // TODO: validate file ZIP header + xml 'score-partwise' tag inside it.
                    Uri fileUri = result.getData().getData();
                    String title = FileHelper.getTitleFromUri(this, fileUri);
                    uploadFileToStorage(fileUri, title);
                }
            });

    private final ActivityResultLauncher<Intent> groupVoicesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri fileUri = result.getData().getData();
                    String title = FileHelper.getTitleFromUri(this, fileUri);
                    Intent intent = new Intent(this, RoleGroupingActivity.class);
                    intent.putExtra("fileUri", fileUri.toString());
                    intent.putExtra("classId", classId);
                    intent.putExtra("lessonId", currentLesson.getId());
                    intent.putExtra("title", title);
                    startActivity(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);

        setupUI();

        if (DataManager.getCurLesson() != null) {
            currentLesson = DataManager.getCurLesson();
            isEditMode = true;
            if (currentLesson.getDate() != null) {
                calendar.setTime(currentLesson.getDate());
            }
            populateLessonData();
            saveLessonButton.setText(R.string.btn_save);
        } else {
            isEditMode = false;
            saveLessonButton.setText(R.string.btn_save_lesson);
            currentLesson = new Lesson();
            DataManager.setCurLesson(currentLesson); // hold the reference in DataManager
            uploadMusicXmlButton.setEnabled(false);
            groupVoicesPickerButton.setEnabled(false);
            // TODO: change the style / set enabled to true but give an error.
            //  the current state is confusing
        }

        if (DataManager.getCurClass() != null) {
            classId = DataManager.getCurClass().getId();
            fetchClassAttendees();
        }

        updateDateAndTimeViews();
        setupRecyclerView();
        setupListeners();
        
        startTask(getString(R.string.status_loading));
        firestoreManager.getAllUsers(allUsers, users -> endTask());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicXmlAdapter != null) {
            musicXmlAdapter.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (musicXmlAdapter != null) {
            musicXmlAdapter.stopListening();
        }
    }

    private void fetchClassAttendees() {
        startTask(getString(R.string.status_loading));
        FirebaseFirestore.getInstance().collection("classes").document(classId)
                .get()
                .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {
                    if (documentSnapshot.exists()) {
                        Class currentClass = documentSnapshot.toObject(Class.class);
                        if (currentClass != null && currentClass.getMembers() != null) {
                            classAttendees.clear();
                            classAttendees.addAll(currentClass.getMembers());
                        }
                    }
                    endTask();
                })
                .addOnFailureListener(e -> endTask());
    }

    private void setupUI() {
        lessonTitleInput = findViewById(R.id.lesson_title_input);
        lessonInfoInput = findViewById(R.id.lesson_info_input);
        lessonDatePicker = findViewById(R.id.lesson_date_picker);
        lessonTimePicker = findViewById(R.id.lesson_time_picker);
        dateTextView = findViewById(R.id.date_text_view);
        timeTextView = findViewById(R.id.time_text_view);
        saveLessonButton = findViewById(R.id.save_lesson_button);
        uploadMusicXmlButton = findViewById(R.id.upload_music_xml_button);
        groupVoicesPickerButton = findViewById(R.id.btn_group_voices_picker);
        musicXmlRecyclerView = findViewById(R.id.music_xml_recycler_view);
        loadingProgress = findViewById(R.id.loading_progress);
    }

    private void setupRecyclerView() {
        musicXmlRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicXmlRecyclerView.setItemAnimator(null); // fix bug in recycle view

        if (classId != null && currentLesson != null && currentLesson.getId() != null) {
            Query query = FirebaseFirestore.getInstance()
                    .collection("classes").document(classId)
                    .collection("lessons").document(currentLesson.getId())
                    .collection("musicFiles");

            FirestoreRecyclerOptions<MusicFile> options = new FirestoreRecyclerOptions.Builder<MusicFile>()
                    .setQuery(query, MusicFile.class)
                    .build();

            musicXmlAdapter = new MusicXmlAdapter(options, true);
            musicXmlAdapter.setPendingDeletions(pendingDeletions);
            musicXmlAdapter.setOnAssignButtonClickListener(this);
            musicXmlAdapter.setOnDeleteButtonClickListener(this);
            musicXmlAdapter.setOnRenameListener(this);
            musicXmlRecyclerView.setAdapter(musicXmlAdapter);
            musicXmlAdapter.startListening();
        }
    }

    private void setupListeners() {
        lessonDatePicker.setOnClickListener(v -> showDatePickerDialog());
        lessonTimePicker.setOnClickListener(v -> showTimePickerDialog());
        uploadMusicXmlButton.setOnClickListener(v -> openFilePicker(musicXmlLauncher));
        groupVoicesPickerButton.setOnClickListener(v -> openFilePicker(groupVoicesLauncher));
        saveLessonButton.setOnClickListener(v -> saveLesson());
    }

    private void openFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // wildcard for all file types
        launcher.launch(intent);
    }

    private void uploadFileToStorage(Uri fileUri, String title) {
        if (classId == null || currentLesson.getId() == null) {
            Toast.makeText(this, "Lesson must be saved before uploading files.", Toast.LENGTH_SHORT).show();
            return;
        }

        startTask(getString(R.string.status_uploading));
        FileIO fileOps = new FileIO(this);
        String extension = fileOps.getExtension(fileUri);
        
        firestoreManager.uploadMusicFile(classId, currentLesson.getId(), fileUri, title, extension);
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateAndTimeViews();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    updateDateAndTimeViews();
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePickerDialog.show();
    }

    private void updateDateAndTimeViews() {
        dateTextView.setText(getString(R.string.label_lesson_date) + ": " + DateFormat.getDateInstance().format(calendar.getTime()));
        timeTextView.setText(getString(R.string.label_lesson_time) + ": " + DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime()));
    }

    private void populateLessonData() {
        lessonTitleInput.setText(currentLesson.getTitle());
        lessonInfoInput.setText(currentLesson.getInfo());
    }

    private void saveLesson() {
        String title = lessonTitleInput.getText().toString().trim();
        String info = lessonInfoInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        currentLesson.setTitle(title);
        currentLesson.setInfo(info);
        currentLesson.setDate(calendar.getTime());

        // Remove assignments for files pending deletion
        Map<String, List<String>> fileMapping = currentLesson.getFileMapping();
        if (fileMapping != null) {
            for (String url : pendingUrlDeletions) {
                fileMapping.remove(url);
            }
            currentLesson.setFileMapping(fileMapping);
        }

        startTask(getString(R.string.status_saving));
        
        performPendingDeletions();

        if (isEditMode) {
            firestoreManager.updateLesson(classId, currentLesson);
        } else {
            if (!FirebaseComm.isUserSignedIn()) {
                endTask();
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentLesson.setOwnerEmail(FirebaseComm.authUserEmail());
            currentLesson.setClassId(classId);
            firestoreManager.insertLesson(classId, currentLesson);
        }
    }

    private void performPendingDeletions() {
        if (classId == null || currentLesson == null || currentLesson.getId() == null) return;

        for (String docId : pendingDeletions) {
            startTask(null);
            FirebaseFirestore.getInstance()
                    .collection("classes").document(classId)
                    .collection("lessons").document(currentLesson.getId())
                    .collection("musicFiles").document(docId)
                    .delete()
                    .addOnCompleteListener(task -> endTask());
        }
        pendingDeletions.clear();
        pendingUrlDeletions.clear();
    }

    @Override
    public void uploadResult(boolean success, FirestoreManager.DbOperation operation) {
        endTask();
        if (success) {
            if (operation == FirestoreManager.DbOperation.INSERT_LESSON || operation == FirestoreManager.DbOperation.UPDATE_LESSON) {
                if (!isEditMode) {
                    uploadMusicXmlButton.setEnabled(true);
                    groupVoicesPickerButton.setEnabled(true);
                    isEditMode = true;
                    saveLessonButton.setText(R.string.btn_save);
                    // Also need to update the query in adapter since currentLesson.getId() is now available
                    setupRecyclerView();
                } else {
                    shouldFinishOnTasksEnd = true;
                    if (pendingTasks == 0) {
                        finish();
                    }
                }
            }
        }
    }

    @Override
    public void displayMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private void startTask(String message) {
        pendingTasks++;
        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.VISIBLE);
        }
        if (message != null && !message.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void endTask() {
        pendingTasks--;
        if (pendingTasks <= 0) {
            pendingTasks = 0;
            if (loadingProgress != null) {
                loadingProgress.setVisibility(View.GONE);
            }
            if (shouldFinishOnTasksEnd) {
                finish();
            }
        }
    }

    @Override
    public void onAssignButtonClick(MusicFile musicFile) {
        if (classAttendees.isEmpty()) {
            Toast.makeText(this, "There are no students in this class.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> availableStudents = new ArrayList<>();
        final Map<String, List<String>> fileMapping = currentLesson.getFileMapping() != null ? currentLesson.getFileMapping() : new HashMap<>();

        // Get students assigned for this file
        List<String> assignedStudentsForThisFile = fileMapping.get(musicFile.getUrl());
        if (assignedStudentsForThisFile == null) {
            assignedStudentsForThisFile = new ArrayList<>();
        }

        // Get all students assigned to other files
        List<String> studentsAssignedToOtherFiles = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : fileMapping.entrySet()) {
            if (!entry.getKey().equals(musicFile.getUrl())) {
                studentsAssignedToOtherFiles.addAll(entry.getValue());
            }
        }

        // Populate available students: lesson attendees not assigned to other files
        for (String studentEmail : classAttendees) {
            if (!studentsAssignedToOtherFiles.contains(studentEmail)) {
                availableStudents.add(studentEmail);
            }
        }

        String[] studentDisplayInfo = new String[availableStudents.size()];
        boolean[] checkedItems = new boolean[availableStudents.size()];
        for (int i = 0; i < availableStudents.size(); i++) {
            String studentEmail = availableStudents.get(i);
            // Find user object to display full name
            String displayName = studentEmail; // default to email
            for (User user : allUsers) {
                if (user.getEmail().equals(studentEmail)) {
                    displayName = user.getFname() + " " + user.getLname();
                    break;
                }
            }
            studentDisplayInfo[i] = displayName;
            if (assignedStudentsForThisFile.contains(studentEmail)) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Assign " + musicFile.getTitle() + " to:");
        List<String> finalAssignedStudentsForThisFile = new ArrayList<>(assignedStudentsForThisFile);
        builder.setMultiChoiceItems(studentDisplayInfo, checkedItems, (dialog, which, isChecked) -> {
            String selectedEmail = availableStudents.get(which);
            if (isChecked) {
                if (!finalAssignedStudentsForThisFile.contains(selectedEmail)) {
                    finalAssignedStudentsForThisFile.add(selectedEmail);
                }
            } else {
                finalAssignedStudentsForThisFile.remove(selectedEmail);
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            fileMapping.put(musicFile.getUrl(), finalAssignedStudentsForThisFile);
            currentLesson.setFileMapping(fileMapping);
            Toast.makeText(this, "Assignments updated.", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    @Override
    public void onDeleteButtonClick(MusicFile musicFile, String documentId) {
        if (pendingDeletions.contains(documentId)) {
            pendingDeletions.remove(documentId);
            pendingUrlDeletions.remove(musicFile.getUrl());
        } else {
            pendingDeletions.add(documentId);
            pendingUrlDeletions.add(musicFile.getUrl());
        }
        if (musicXmlAdapter != null) {
            musicXmlAdapter.setPendingDeletions(pendingDeletions);
        }
    }

    @Override
    public void onRename(MusicFile musicFile, String documentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename File");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setText(musicFile.getTitle());
        input.setSelectAllOnFocus(true);
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newTitle = input.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                renameFileInFirestore(documentId, newTitle);
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void renameFileInFirestore(String documentId, String newTitle) {
        if (classId == null || currentLesson == null || currentLesson.getId() == null) return;

        startTask(null);
        firestoreManager.renameMusicFile(classId, currentLesson.getId(), documentId, newTitle);
    }
}