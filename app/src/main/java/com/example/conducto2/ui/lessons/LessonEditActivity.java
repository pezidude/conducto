package com.example.conducto2.ui.lessons;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * This activity serves a dual purpose: creating a new lesson and editing an existing one.
 * The mode of operation is determined by whether a "lesson" object is passed via the Intent.
 * If a lesson is passed, the activity enters 'edit mode'. Otherwise, it's in 'create mode'.
 */
public class LessonEditActivity extends AppCompatActivity implements FirestoreManager.DBResult {

    private EditText lessonTitleInput;
    private EditText lessonInfoInput;
    private Button lessonDatePicker;
    private Button lessonTimePicker;
    private TextView dateTextView;
    private TextView timeTextView;
    private Button saveLessonButton;
    private Button selectAttendeesButton;

    private FirestoreManager firestoreManager;
    private ArrayList<String> selectedAttendees = new ArrayList<>();
    private List<User> allUsers = new ArrayList<>();
    private Lesson currentLesson;
    private String classId;
    private boolean isEditMode = false;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);

        // Initialize UI components
        lessonTitleInput = findViewById(R.id.lesson_title_input);
        lessonInfoInput = findViewById(R.id.lesson_info_input);
        lessonDatePicker = findViewById(R.id.lesson_date_picker);
        lessonTimePicker = findViewById(R.id.lesson_time_picker);
        dateTextView = findViewById(R.id.date_text_view);
        timeTextView = findViewById(R.id.time_text_view);
        saveLessonButton = findViewById(R.id.save_lesson_button);
        selectAttendeesButton = findViewById(R.id.select_attendees_button);

        // Check if the activity was started with a lesson to edit
        if (getIntent().hasExtra("lesson")) {
            currentLesson = getIntent().getParcelableExtra("lesson");
            isEditMode = true;
            if (currentLesson.getDate() != null) {
                calendar.setTime(currentLesson.getDate());
            }
            populateLessonData();
            saveLessonButton.setText("Save Changes");
        } else {
            // No lesson passed, so we are creating a new one
            isEditMode = false;
            saveLessonButton.setText("Add Lesson");
        }

        if (getIntent().hasExtra("classId")) {
            classId = getIntent().getStringExtra("classId");
        }

        updateDateAndTimeViews();

        lessonDatePicker.setOnClickListener(v -> showDatePickerDialog());
        lessonTimePicker.setOnClickListener(v -> showTimePickerDialog());
        saveLessonButton.setOnClickListener(v -> saveLesson());
        selectAttendeesButton.setOnClickListener(v -> showUserSelectionDialog());
        firestoreManager.getAllUsers(allUsers, this); // Fetch all users for the attendee selection dialog
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

    /**
     * If in edit mode, this method populates the UI fields with the data
     * from the current lesson.
     */
    private void populateLessonData() {
        lessonTitleInput.setText(currentLesson.getTitle());
        lessonInfoInput.setText(currentLesson.getInfo());

        if (currentLesson.getAttendees() != null) {
            selectedAttendees = new ArrayList<>(currentLesson.getAttendees());
        }
    }

    /**
     * Fetches all users from Firestore and displays them in a multi-choice
     * dialog for attendee selection.
     */
    private void showUserSelectionDialog() {
        if (allUsers.isEmpty()) {
            Toast.makeText(this, "No users to display.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userDisplayInfo = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            User user = allUsers.get(i);
            userDisplayInfo[i] = user.getFname() + " " + user.getLname() + " (" + user.getEmail() + ")";
        }

        boolean[] checkedItems = new boolean[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            if (selectedAttendees.contains(allUsers.get(i).getEmail())) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Attendees");
        builder.setMultiChoiceItems(userDisplayInfo, checkedItems, (dialog, which, isChecked) -> {
            String selectedEmail = allUsers.get(which).getEmail();
            if (isChecked) {
                if (!selectedAttendees.contains(selectedEmail)) {
                    selectedAttendees.add(selectedEmail);
                }
            } else {
                selectedAttendees.remove(selectedEmail);
            }
        });
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }


    /**
     * Saves the lesson. If in edit mode, it updates the existing lesson.
     * If in create mode, it creates a new lesson.
     */
    private void saveLesson() {
        String title = lessonTitleInput.getText().toString().trim();
        String info = lessonInfoInput.getText().toString().trim();

        if (title.isEmpty() || info.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Date date = calendar.getTime();

        if (isEditMode) {
            // Update existing lesson
            Lesson updatedLesson = new Lesson(currentLesson);
            updatedLesson.setTitle(title);
            updatedLesson.setInfo(info);
            updatedLesson.setDate(date);
            updatedLesson.setAttendees(selectedAttendees);
            firestoreManager.updateLesson(classId, updatedLesson);
        } else {
            // Create new lesson
            if (!FirebaseComm.isUserSignedIn()) {
                Toast.makeText(this, "You must be logged in to add a lesson", Toast.LENGTH_SHORT).show();
                return;
            }
            String ownerEmail = FirebaseComm.authUserEmail();
            Lesson newLesson = new Lesson(title, info, date, ownerEmail, classId);
            newLesson.setAttendees(selectedAttendees);
            firestoreManager.insertLesson(classId, newLesson);
        }
    }

    @Override
    public void uploadResult(boolean success) {
        if (success) {
            finish(); // On successful save, close the activity
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}