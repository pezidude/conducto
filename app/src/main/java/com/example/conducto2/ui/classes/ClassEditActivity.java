package com.example.conducto2.ui.classes;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.conducto2.R;
import com.example.conducto2.data.firebase.FirebaseComm;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.model.Class;

import java.util.ArrayList;

public class ClassEditActivity extends AppCompatActivity implements FirestoreManager.DBResult {

    private EditText classNameEditText;
    private EditText classDescriptionEditText;
    private EditText teacherNameEditText;
    private TextView joinCodeTextView;
    private Button saveClassButton;

    private FirestoreManager firestoreManager;
    private Class currentClass;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);

        classNameEditText = findViewById(R.id.class_name_edit_text);
        classDescriptionEditText = findViewById(R.id.class_description_edit_text);
        teacherNameEditText = findViewById(R.id.teacher_name_edit_text);
        joinCodeTextView = findViewById(R.id.join_code_text_view);
        saveClassButton = findViewById(R.id.save_class_button);

        if (getIntent().hasExtra("class")) {
            currentClass = getIntent().getParcelableExtra("class");
            isEditMode = true;
            populateClassData();
            saveClassButton.setText("Save Changes");
        } else {
            isEditMode = false;
            saveClassButton.setText("Add Class");
        }

        saveClassButton.setOnClickListener(v -> saveClass());
    }

    private void populateClassData() {
        classNameEditText.setText(currentClass.getName());
        classDescriptionEditText.setText(currentClass.getDescription());
        teacherNameEditText.setText(currentClass.getTeacherName());
        if (currentClass.getJoinCode() != null) {
            joinCodeTextView.setText("Join Code: " + currentClass.getJoinCode());
            joinCodeTextView.setVisibility(View.VISIBLE);
        }
    }

    private void saveClass() {
        String name = classNameEditText.getText().toString().trim();
        String description = classDescriptionEditText.getText().toString().trim();
        String teacherName = teacherNameEditText.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || teacherName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            currentClass.setName(name);
            currentClass.setDescription(description);
            currentClass.setTeacherName(teacherName);
            currentClass.ensureJoinCode(); // This will generate a code if it's missing
            firestoreManager.updateClass(currentClass);
        } else {
            if (!FirebaseComm.isUserSignedIn()) {
                Toast.makeText(this, "You must be logged in to add a class", Toast.LENGTH_SHORT).show();
                return;
            }
            String ownerEmail = FirebaseComm.authUserEmail();
            Class newClass = new Class(name, description, teacherName, ownerEmail);
            ArrayList<String> members = new ArrayList<>();
            members.add(ownerEmail);
            newClass.setMembers(members);
            firestoreManager.insertClass(newClass);
        }
    }

    @Override
    public void uploadResult(boolean success) {
        if (success) {
            finish();
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}