package com.example.conducto2.ui.lessons;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
import com.example.conducto2.data.firebase.FileStorage;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.data.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * LessonEditActivity
 * 
 * An administrative activity for teachers to create new lessons or edit existing ones.
 * This class acts as the orchestration hub for the lesson configuration pipeline.
 * 
 * Key Responsibilities:
 * 1. Form Validation & Data Binding: Managing Title, Info, Genre, and Date inputs.
 * 2. Unsaved Changes Tracking: Preventing accidental data loss upon exit.
 * 3. File Management: Interfacing with device storage to upload base MusicXML files.
 * 4. Workflow Routing: Providing the entry point to {@link RoleGroupingActivity} and 
 *    managing staged file deletions.
 */
public class LessonEditActivity extends BaseDrawerActivity implements FirebaseComm.DBResult, MusicXmlAdapter.OnAssignButtonClickListener, MusicXmlAdapter.OnDeleteButtonClickListener, MusicXmlAdapter.OnRenameListener {

    private EditText lessonTitleInput;
    private EditText lessonInfoInput;
    private AutoCompleteTextView genreSelector;
    private android.widget.ImageButton lessonDateTimePicker;
    private TextView dateTextView;
    private TextView timeTextView;
    private Button saveLessonButton;
    private Button uploadMusicXmlButton;
    private Button groupVoicesPickerButton;
    private TextView errorTextView;
    private RecyclerView musicXmlRecyclerView;
    private MusicXmlAdapter musicXmlAdapter;
    private LinearProgressIndicator loadingProgress;

    private FileStorage fileStorage;

    private final ArrayList<String> classAttendees = new ArrayList<>();
    private final List<User> allUsers = new ArrayList<>();
    private Lesson currentLesson;
    private String classId;
    private boolean isEditMode = false;
    private final Calendar calendar = Calendar.getInstance();
    private int pendingTasks = 0;
    private boolean shouldFinishOnTasksEnd = false;
    
    // Staged deletion tracking
    private final List<String> pendingDeletions = new ArrayList<>();
    private final List<String> pendingUrlDeletions = new ArrayList<>();
    
    // State tracking for "Unsaved Changes" logic
    private boolean originalIsArchived = false;
    private Map<String, List<String>> originalFileMapping = new HashMap<>();

    /**
     * Activity Result Launcher for basic MusicXML file uploads.
     * Validates the file extension before passing to FileStorage.
     */
    private final ActivityResultLauncher<Intent> musicXmlLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri fileUri = result.getData().getData();
                    // Validation: Ensure the selected file is actually an XML/MXL document.
                    if (FileHelper.isValidMusicXml(this, fileUri)) {
                        hideError();
                        String title = FileHelper.getTitleFromUri(this, fileUri);
                        uploadFileToStorage(fileUri, title);
                    } else {
                        showError("Invalid MusicXML or MXL file");
                    }
                }
            });

    /**
     * Activity Result Launcher for the Role Grouping workflow.
     * Selects a master score, validates it, and passes it to RoleGroupingActivity.
     */
    private final ActivityResultLauncher<Intent> groupVoicesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (FileHelper.isValidMusicXml(this, fileUri)) {
                        hideError();
                        String title = FileHelper.getTitleFromUri(this, fileUri);
                        Intent intent = new Intent(this, RoleGroupingActivity.class);
                        intent.putExtra("fileUri", fileUri.toString());
                        intent.putExtra("classId", classId);
                        intent.putExtra("lessonId", currentLesson.getId());
                        intent.putExtra("title", title);
                        startActivity(intent);
                    } else {
                        showError("Invalid MusicXML or MXL file");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);
        fileStorage = new FileStorage();
        fileStorage.setDbResult(this);

        setupUI();

        // State Determination: Are we creating a new lesson or editing an existing one?
        if (DataManager.getCurLesson() != null) {
            currentLesson = DataManager.getCurLesson();
            isEditMode = true;
            originalIsArchived = currentLesson.isArchived();
            originalFileMapping = currentLesson.getFileMapping() != null ? new HashMap<>(currentLesson.getFileMapping()) : new HashMap<>();
            if (currentLesson.getDate() != null) {
                calendar.setTime(currentLesson.getDate());
            }
            populateLessonData();
            saveLessonButton.setText(R.string.btn_save);
        } else {
            isEditMode = false;
            saveLessonButton.setText(R.string.btn_save_lesson);
            currentLesson = new Lesson();
            originalIsArchived = false;
            originalFileMapping = new HashMap<>();
            DataManager.setCurLesson(currentLesson); 
        }

        if (DataManager.getCurClass() != null) {
            classId = DataManager.getCurClass().getId();
            fetchClassAttendees();
        }

        updateDateAndTimeViews();
        setupRecyclerView();
        setupListeners();
        
        // Intercept back button to perform dirty state checking.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        // Pre-fetch users for the assignment dialog to prevent UI lag later.
        startTask(getString(R.string.status_loading));
        firestoreManager.getAllUsers(allUsers, users -> endTask());
    }

    /**
     * Intercepts exit events to warn the user if they have unsaved changes.
     */
    private void handleExit() {
        if (hasUnsavedChanges()) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Do you want to save them before exiting?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        // Flag to ensure the activity finishes only after async DB operations complete.
                        shouldFinishOnTasksEnd = true;
                        saveLesson();
                    })
                    .setNegativeButton("Discard", (dialog, which) -> finish())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            finish();
        }
    }

    /**
     * Comprehensive validation method that compares current UI state against
     * the original loaded state.
     * @return True if the form is "dirty" and requires saving.
     */
    private boolean hasUnsavedChanges() {
        if (currentLesson == null) return false;

        String currentTitle = lessonTitleInput.getText().toString().trim();
        String currentInfo = lessonInfoInput.getText().toString().trim();
        String currentGenre = genreSelector.getText().toString().trim();

        String originalTitle = isEditMode && currentLesson.getTitle() != null ? currentLesson.getTitle() : "";
        String originalInfo = isEditMode && currentLesson.getInfo() != null ? currentLesson.getInfo() : "";
        String originalGenre = isEditMode && currentLesson.getGenreLabel() != null ? currentLesson.getGenreLabel() : "";

        // Check text fields
        if (!currentTitle.equals(originalTitle)) return true;
        if (!currentInfo.equals(originalInfo)) return true;
        if (!currentGenre.equals(originalGenre)) return true;

        // Check Date/Time
        if (isEditMode && currentLesson.getDate() != null) {
            Calendar originalCal = Calendar.getInstance();
            originalCal.setTime(currentLesson.getDate());
            if (originalCal.get(Calendar.YEAR) != calendar.get(Calendar.YEAR) ||
                originalCal.get(Calendar.MONTH) != calendar.get(Calendar.MONTH) ||
                originalCal.get(Calendar.DAY_OF_MONTH) != calendar.get(Calendar.DAY_OF_MONTH) ||
                originalCal.get(Calendar.HOUR_OF_DAY) != calendar.get(Calendar.HOUR_OF_DAY) ||
                originalCal.get(Calendar.MINUTE) != calendar.get(Calendar.MINUTE)) {
                return true;
            }
        } else if (!isEditMode) {
            // If new lesson, any text input means unsaved changes
            if (!currentTitle.isEmpty() || !currentInfo.isEmpty() || !currentGenre.isEmpty()) return true;
        }

        // Check toggles and mapping
        if (currentLesson.isArchived() != originalIsArchived) return true;

        Map<String, List<String>> currentFileMapping = currentLesson.getFileMapping();
        if (currentFileMapping == null) currentFileMapping = new HashMap<>();
        if (!currentFileMapping.equals(originalFileMapping)) return true;

        // Check staged actions
        return !pendingDeletions.isEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicXmlAdapter != null) musicXmlAdapter.startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (musicXmlAdapter != null) musicXmlAdapter.stopListening();
    }

    private void fetchClassAttendees() {
        startTask(getString(R.string.status_loading));
        firestoreManager.getClassById(classId, currentClass -> {
            if (currentClass != null && currentClass.getMembers() != null) {
                classAttendees.clear();
                classAttendees.addAll(currentClass.getMembers());
            }
            endTask();
        });
    }

    private void setupUI() {
        lessonTitleInput = findViewById(R.id.lesson_title_input);
        lessonInfoInput = findViewById(R.id.lesson_info_input);
        genreSelector = findViewById(R.id.genre_autocomplete);
        lessonDateTimePicker = findViewById(R.id.lesson_date_time_picker);
        dateTextView = findViewById(R.id.date_text_view);
        timeTextView = findViewById(R.id.time_text_view);
        saveLessonButton = findViewById(R.id.save_lesson_button);
        uploadMusicXmlButton = findViewById(R.id.upload_music_xml_button);
        groupVoicesPickerButton = findViewById(R.id.btn_group_voices_picker);
        errorTextView = findViewById(R.id.error_text_view);
        musicXmlRecyclerView = findViewById(R.id.music_xml_recycler_view);
        loadingProgress = findViewById(R.id.loading_progress);
    }

    private void setupRecyclerView() {
        musicXmlRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicXmlRecyclerView.setItemAnimator(null); 

        if (classId != null && currentLesson != null && currentLesson.getId() != null) {
            Query query = FirebaseComm.getCollectionReference("classes").document(classId)
                    .collection("lessons").document(currentLesson.getId())
                    .collection("musicFiles");

            FirestoreRecyclerOptions<MusicFile> options = new FirestoreRecyclerOptions.Builder<MusicFile>()
                    .setQuery(query, MusicFile.class)
                    .build();

            // Initialize adapter with administrative buttons active (showButtons = true)
            musicXmlAdapter = new MusicXmlAdapter(options, true, false);
            musicXmlAdapter.setPendingDeletions(pendingDeletions);
            musicXmlAdapter.setOnAssignButtonClickListener(this);
            musicXmlAdapter.setOnDeleteButtonClickListener(this);
            musicXmlAdapter.setOnRenameListener(this);
            musicXmlRecyclerView.setAdapter(musicXmlAdapter);
            musicXmlAdapter.startListening();
        }
    }

    private void setupListeners() {
        lessonDateTimePicker.setOnClickListener(v -> showDatePickerDialog());
        
        // Workflow Validation: Prevent file operations on unsaved documents 
        // since they require a valid parent Document ID.
        uploadMusicXmlButton.setOnClickListener(v -> {
            if (isEditMode) openFilePicker(musicXmlLauncher);
            else showError("Please save the lesson first before uploading files.");
        });
        groupVoicesPickerButton.setOnClickListener(v -> {
            if (isEditMode) openFilePicker(groupVoicesLauncher);
            else showError("Please save the lesson first before grouping voices.");
        });
        
        saveLessonButton.setOnClickListener(v -> saveLesson());

        String[] genres = {"Classical", "Jazz", "Pop", "Rock"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown, genres);
        genreSelector.setAdapter(adapter);
    }

    private void showError(String message) {
        if (errorTextView != null) {
            errorTextView.setText(message);
            errorTextView.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (errorTextView != null) errorTextView.setVisibility(View.GONE);
    }

    /**
     * Toggles the local archive state. This change is not pushed to Firestore 
     * until saveLesson() is called (Staged Archiving).
     */
    private void archiveLesson() {
        if (currentLesson == null) return;
        boolean newArchiveStatus = !currentLesson.isArchived();
        
        currentLesson.setArchived(newArchiveStatus);
        invalidateOptionsMenu();
        
        String message = newArchiveStatus ? "Lesson will be archived on save" : "Lesson will be restored on save";
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> deleteLesson())
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void deleteLesson() {
        if (classId == null || currentLesson == null || currentLesson.getId() == null) return;
        startTask(getString(R.string.status_deleting));
        FirebaseComm.getCollectionReference("classes")
                .document(classId).collection("lessons")
                .document(currentLesson.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    displayMessage("Lesson deleted");
                    shouldFinishOnTasksEnd = true;
                    endTask();
                })
                .addOnFailureListener(e -> {
                    displayMessage("Failed to delete lesson");
                    endTask();
                });
    }

    private void openFilePicker(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); 
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
        
        fileStorage.uploadMusicFile(classId, currentLesson.getId(), fileUri, title, extension);
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePickerDialog();
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
        dateTextView.setText(DateFormat.getDateInstance().format(calendar.getTime()));
        timeTextView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime()));
    }

    private void populateLessonData() {
        lessonTitleInput.setText(currentLesson.getTitle());
        lessonInfoInput.setText(currentLesson.getInfo());
        genreSelector.setText(currentLesson.getGenreLabel(), false);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.menu_lesson_edit, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        if (isEditMode) {
            android.view.MenuItem archiveItem = menu.findItem(R.id.action_archive);
            if (archiveItem != null && currentLesson != null) {
                // Toggle action bar iconography based on current local state
                if (currentLesson.isArchived()) {
                    archiveItem.setIcon(R.drawable.unarchive_24px);
                    archiveItem.setTitle("Restore");
                } else {
                    archiveItem.setIcon(R.drawable.archive_24px);
                    archiveItem.setTitle("Archive");
                }
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            handleExit();
            return true;
        } else if (id == R.id.action_archive) {
            archiveLesson();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Form Validation and Database Commit logic.
     * Ensures required fields are populated, executes any staged deletions, 
     * and pushes the updated Lesson model to Firestore.
     */
    private void saveLesson() {
        String title = lessonTitleInput.getText().toString().trim();
        String info = lessonInfoInput.getText().toString().trim();
        String genre = genreSelector.getText().toString().trim();

        // Data Validation: Title and Genre are mandatory.
        if (title.isEmpty() || genre.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields (Title and Genre are required)", Toast.LENGTH_SHORT).show();
            return;
        }

        currentLesson.setTitle(title);
        currentLesson.setInfo(info);
        currentLesson.setDate(calendar.getTime());
        currentLesson.setGenre(genre);

        // Security/Cleanup: Remove mappings for files that are about to be deleted.
        Map<String, List<String>> fileMapping = currentLesson.getFileMapping();
        if (fileMapping != null) {
            for (String url : pendingUrlDeletions) {
                fileMapping.remove(url);
            }
            currentLesson.setFileMapping(fileMapping);
        }

        startTask(getString(R.string.status_saving));
        
        // Execute network calls for staged file deletions.
        performPendingDeletions();

        if (isEditMode) {
            firestoreManager.updateLesson(classId, currentLesson);
        } else {
            if (!FirebaseComm.isUserSignedIn()) {
                endTask();
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show();
                return;
            }
            currentLesson.setClassId(classId);
            firestoreManager.insertLesson(classId, currentLesson);
        }
    }

    /**
     * Dispatches network calls to FileStorage to remove documents staged in pendingDeletions.
     */
    private void performPendingDeletions() {
        if (classId == null || currentLesson == null || currentLesson.getId() == null) return;

        for (String docId : pendingDeletions) {
            startTask(null);
            fileStorage.deleteMusicFile(classId, currentLesson.getId(), docId, task -> endTask());
        }
        pendingDeletions.clear();
        pendingUrlDeletions.clear();
    }

    @Override
    public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
        endTask();
        if (success) {
            if (operation == FirebaseComm.DbOperation.INSERT_LESSON || operation == FirebaseComm.DbOperation.UPDATE_LESSON) {
                hideError();

                // Reset baseline state to prevent false-positive "Unsaved Changes" warnings
                originalIsArchived = currentLesson.isArchived();
                originalFileMapping = currentLesson.getFileMapping() != null ? new HashMap<>(currentLesson.getFileMapping()) : new HashMap<>();

                if (!isEditMode) {
                    isEditMode = true;
                    saveLessonButton.setText(R.string.btn_save);
                    setupRecyclerView();
                } else {
                    shouldFinishOnTasksEnd = true;
                }

                if (shouldFinishOnTasksEnd && pendingTasks == 0) finish();
            }
        }
    }

    @Override
    public void displayMessage(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Centralized task tracking. Prevents the Activity from closing prematurely 
     * while asynchronous operations (like file uploads/deletions) are still running.
     * @param message Text to display on the Snackbar, or null for silent tracking.
     */
    private void startTask(String message) {
        pendingTasks++;
        if (loadingProgress != null) loadingProgress.setVisibility(View.VISIBLE);
        if (message != null && !message.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Decrements the active task counter and checks if the Activity should terminate. */
    private void endTask() {
        pendingTasks--;
        if (pendingTasks <= 0) {
            pendingTasks = 0;
            if (loadingProgress != null) loadingProgress.setVisibility(View.GONE);
            if (shouldFinishOnTasksEnd) finish();
        }
    }

    @Override
    public void onAssignButtonClick(MusicFile musicFile) {
        if (classAttendees.isEmpty()) {
            Toast.makeText(this, "There are no students in this class.", Toast.LENGTH_SHORT).show();
            return;
        }

        final Map<String, List<String>> fileMapping = currentLesson.getFileMapping() != null ? currentLesson.getFileMapping() : new HashMap<>();

        List<String> assignedStudentsForThisFile = fileMapping.get(musicFile.getUrl());
        if (assignedStudentsForThisFile == null) assignedStudentsForThisFile = new ArrayList<>();

        List<String> studentsAssignedToOtherFiles = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : fileMapping.entrySet()) {
            if (!entry.getKey().equals(musicFile.getUrl())) {
                studentsAssignedToOtherFiles.addAll(entry.getValue());
            }
        }

        List<User> availableStudentObjects = new ArrayList<>();
        for (User user : allUsers) {
            if (classAttendees.contains(user.getEmail()) && !studentsAssignedToOtherFiles.contains(user.getEmail())) {
                availableStudentObjects.add(user);
            }
        }

        if (availableStudentObjects.isEmpty()) {
            Toast.makeText(this, "No available students to assign.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_student_assignment, null);
        RecyclerView rvStudents = dialogView.findViewById(R.id.rv_students);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        
        StudentAssignmentAdapter adapter = new StudentAssignmentAdapter(availableStudentObjects, assignedStudentsForThisFile);
        rvStudents.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle("Assign " + musicFile.getTitle() + " to:")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    fileMapping.put(musicFile.getUrl(), adapter.getSelectedEmails());
                    currentLesson.setFileMapping(fileMapping);
                    Toast.makeText(this, "Assignments updated.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        if (musicXmlAdapter != null) musicXmlAdapter.setPendingDeletions(pendingDeletions);
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
            if (!newTitle.isEmpty()) renameFileInFirestore(documentId, newTitle);
            else Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void renameFileInFirestore(String documentId, String newTitle) {
        if (classId == null || currentLesson == null || currentLesson.getId() == null) return;
        startTask(null);
        fileStorage.renameMusicFile(classId, currentLesson.getId(), documentId, newTitle);
    }
}