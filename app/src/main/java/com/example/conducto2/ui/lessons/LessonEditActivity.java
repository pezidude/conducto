package com.example.conducto2.ui.lessons;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.model.Class;
import com.example.conducto2.utils.FileHelper;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LessonEditActivity extends AppCompatActivity implements FirestoreManager.DBResult, MusicXmlAdapter.OnAssignButtonClickListener {

    private EditText lessonTitleInput;
    private EditText lessonInfoInput;
    private Button lessonDatePicker;
    private Button lessonTimePicker;
    private TextView dateTextView;
    private TextView timeTextView;
    private Button saveLessonButton;
    private Button uploadMusicXmlButton;
    private RecyclerView musicXmlRecyclerView;
    private MusicXmlAdapter musicXmlAdapter;

    private FirestoreManager firestoreManager;
    private ArrayList<String> classAttendees = new ArrayList<>();
    private List<User> allUsers = new ArrayList<>();
    private Lesson currentLesson;
    private String classId;
    private boolean isEditMode = false;
    private Calendar calendar = Calendar.getInstance();
    private List<MusicFile> musicFiles = new ArrayList<>();

    private final ActivityResultLauncher<Intent> musicXmlLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri fileUri = result.getData().getData();
                    String title = FileHelper.getTitleFromUri(this, fileUri);
                    uploadFileToStorage(fileUri, title);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);

        setupUI();

        if (getIntent().hasExtra("lesson")) {
            currentLesson = getIntent().getParcelableExtra("lesson");
            isEditMode = true;
            if (currentLesson.getDate() != null) {
                calendar.setTime(currentLesson.getDate());
            }
            if (currentLesson.getMusicXMLFiles() != null) {
                musicFiles.addAll(currentLesson.getMusicXMLFiles());
            }
            populateLessonData();
            saveLessonButton.setText("Save Changes");
        } else {
            isEditMode = false;
            saveLessonButton.setText("Add Lesson");
            currentLesson = new Lesson();
            uploadMusicXmlButton.setEnabled(false);
        }

        if (getIntent().hasExtra("classId")) {
            classId = getIntent().getStringExtra("classId");
            fetchClassAttendees();
        }

        updateDateAndTimeViews();
        setupRecyclerView();
        setupListeners();
        firestoreManager.getAllUsers(allUsers, this);
    }

    private void fetchClassAttendees() {
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
                });
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
        musicXmlRecyclerView = findViewById(R.id.music_xml_recycler_view);
    }

    private void setupRecyclerView() {
        musicXmlRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicXmlAdapter = new MusicXmlAdapter(musicFiles, this);
        musicXmlRecyclerView.setAdapter(musicXmlAdapter);
    }

    private void setupListeners() {
        lessonDatePicker.setOnClickListener(v -> showDatePickerDialog());
        lessonTimePicker.setOnClickListener(v -> showTimePickerDialog());
        uploadMusicXmlButton.setOnClickListener(v -> openFilePicker());
        saveLessonButton.setOnClickListener(v -> saveLesson());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        musicXmlLauncher.launch(intent);
    }

    private void uploadFileToStorage(Uri fileUri, String title) {
        if (classId == null || currentLesson.getId() == null) {
            Toast.makeText(this, "Lesson must be saved before uploading files.", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = "musicxml_" + UUID.randomUUID().toString();
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + currentLesson.getId() + "/" + fileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    MusicFile musicFile = new MusicFile(title, uri);
                    FirebaseFirestore.getInstance()
                            .collection("classes").document(classId)
                            .collection("lessons").document(currentLesson.getId())
                            .update("musicXMLFiles", FieldValue.arrayUnion(musicFile))
                            .addOnSuccessListener(aVoid -> {
                                musicFiles.add(musicFile);
                                musicXmlAdapter.notifyItemInserted(musicFiles.size() - 1);
                                Toast.makeText(LessonEditActivity.this, "File uploaded and saved.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(LessonEditActivity.this, "Failed to save file URL: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }))
                .addOnFailureListener(e -> Toast.makeText(LessonEditActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
        dateTextView.setText("Selected Date: " + DateFormat.getDateInstance().format(calendar.getTime()));
        timeTextView.setText("Selected Time: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime()));
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
        currentLesson.setAttendees(classAttendees);
        currentLesson.setMusicXMLFiles(musicFiles);
        // fileMapping is updated in onAssignButtonClick

        if (isEditMode) {
            firestoreManager.updateLesson(classId, currentLesson);
        } else {
            if (!FirebaseComm.isUserSignedIn()) {
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentLesson.setOwnerEmail(FirebaseComm.authUserEmail());
            currentLesson.setClassId(classId);
            firestoreManager.insertLesson(classId, currentLesson);
        }
    }

    @Override
    public void uploadResult(boolean success) {
        if (success) {
            if (!isEditMode) {
                uploadMusicXmlButton.setEnabled(true);
                isEditMode = true;
                saveLessonButton.setText("Save Changes");
                Toast.makeText(this, "Lesson saved. You can now upload files.", Toast.LENGTH_SHORT).show();
            } else {
                finish();
            }
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAssignButtonClick(MusicFile musicFile) {
        if (classAttendees.isEmpty()) {
            Toast.makeText(this, "There are no students in this class.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> availableStudents = new ArrayList<>();
        final Map<String, List<String>> fileMapping = currentLesson.getFileMapping() != null ? currentLesson.getFileMapping() : new HashMap<>();

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
            String displayName = studentEmail;
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
}