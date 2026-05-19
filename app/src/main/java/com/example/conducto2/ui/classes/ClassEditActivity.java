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
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Class;

import java.util.ArrayList;

/**
 * ClassEditActivity
 * 
 * An administrative activity designed for instructors to manage classroom metadata.
 * It supports both the creation of new classrooms and the modification of existing ones.
 * 
 * The activity handles data validation for classroom titles and descriptions, 
 * coordinates the generation of unique student Join Codes, and ensures the 
 * local application cache (DataManager) remains synchronized with Firestore updates.
 */
public class ClassEditActivity extends AppCompatActivity implements FirebaseComm.DBResult {

    /** Input field for the classroom's display name. */
    private EditText classNameEditText;

    /** Input field for the class description or schedule info. */
    private EditText classDescriptionEditText;

    /** Display field for the unique 6-character Join Code (Edit mode only). */
    private TextView joinCodeTextView;

    /** Button to commit changes to the Firestore database. */
    private Button saveClassButton;

    /** DAO for managing Firestore CRUD operations. */
    private FirestoreManager firestoreManager;

    /** The data model instance being created or modified. */
    private Class currentClass;

    /** 
     * Flag indicating the operation mode. 
     * True if modifying an existing document, False if creating a new one. 
     */
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_edit);

        firestoreManager = new FirestoreManager();
        firestoreManager.setDbResult(this);

        classNameEditText = findViewById(R.id.class_name_edit_text);
        classDescriptionEditText = findViewById(R.id.class_description_edit_text);
        joinCodeTextView = findViewById(R.id.join_code_text_view);
        saveClassButton = findViewById(R.id.save_class_button);

        // State Initialization: Determine if we are editing based on DataManager state.
        if (DataManager.getCurClass() != null) {
            currentClass = DataManager.getCurClass();
            isEditMode = true;
            populateClassData();
            saveClassButton.setText("Save Changes");
        } else {
            isEditMode = false;
            saveClassButton.setText("Add Class");
            currentClass = new Class();
        }

        saveClassButton.setOnClickListener(v -> saveClass());
    }

    /**
     * Maps the attributes of the currentClass model to the UI EditText components.
     * Invoked only when in Edit Mode.
     */
    private void populateClassData() {
        classNameEditText.setText(currentClass.getName());
        classDescriptionEditText.setText(currentClass.getDescription());
        if (currentClass.getJoinCode() != null) {
            joinCodeTextView.setText("Join Code: " + currentClass.getJoinCode());
            joinCodeTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Orchestrates the validation and persistence of classroom data.
     * 
     * Sequential Logic:
     * 1. Extract and trim text from inputs.
     * 2. Validate that all required fields are non-empty.
     * 3. Mode Check:
     *    - If Edit: Update existing model and invoke firestoreManager.updateClass().
     *    - If New: Instantiate new Class object, set owner email, and invoke firestoreManager.insertClass().
     */
    private void saveClass() {
        String name = classNameEditText.getText().toString().trim();
        String description = classDescriptionEditText.getText().toString().trim();

        // Validation Measure: Prevent empty documents from being created.
        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEditMode) {
            currentClass.setName(name);
            currentClass.setDescription(description);
            // Ensure data integrity by generating a code if the legacy document lacks one.
            firestoreManager.updateClass(currentClass);
            
            // Sync local singleton to ensure the 'ClassActivity' view updates immediately upon return.
            if (DataManager.getCurClass() != null && currentClass.getId().equals(DataManager.getCurClass().getId())) {
                DataManager.setCurClass(currentClass);
            }
        } else {
            // Permission Check: Verify authentication before attempting a write.
            if (!FirebaseComm.isUserSignedIn()) {
                Toast.makeText(this, "You must be logged in to add a class", Toast.LENGTH_SHORT).show();
                return;
            }
            String ownerEmail = FirebaseComm.authUserEmail();
            Class newClass = new Class(name, description, ownerEmail);
            ArrayList<String> members = new ArrayList<>();
            // Auto-enroll the creator as the first member.
            members.add(ownerEmail);
            newClass.setMembers(members);
            firestoreManager.insertClassWithUniqueCode(newClass);
        }
    }

    /**
     * Handles the callback from FirestoreManager. 
     * Terminates the activity upon successful database confirmation.
     */
    @Override
    public void uploadResult(boolean success, FirebaseComm.DbOperation operation) {
        if (success) {
            finish();
        }
    }

    @Override
    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}