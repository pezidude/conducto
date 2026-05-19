package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.firebase.FileStorage;
import com.example.conducto2.data.firebase.FirestoreManager;
import com.example.conducto2.data.manager.DataManager;
import com.example.conducto2.data.model.Role;
import com.example.conducto2.utils.MusicXmlParser;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.Query;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * RoleGroupingActivity
 * 
 * This activity provides a administrative interface for teachers to partition a master
 * MusicXML score into specific "Roles" (e.g., Violin I, Piano, etc.). 
 * 
 * It allows for:
 * 1. Selecting which specific instrumental parts from the original score belong to a role.
 * 2. Creating multiple roles from a single source file.
 * 3. Dynamically generating new filtered MusicXML files for each role and uploading them
 *    to cloud storage for student access.
 */
public class RoleGroupingActivity extends AppCompatActivity {

    /** Tag for logging. */
    private static final String TAG = "RoleGroupingActivity";

    /** UI element displaying the name of the source score. */
    private TextView tvSourceFile;

    /** Button to trigger the creation of a new draft role. */
    private Button btnAddRole;

    /** List displaying all current draft roles for the lesson. */
    private RecyclerView rvRoles;

    /** Button to initiate the final XML generation and cloud upload process. */
    private Button btnSaveUpload;

    /** UI indicator for background parsing and uploading tasks. */
    private ProgressBar pbLoading;

    /** The location of the original MusicXML score in local storage. */
    private Uri sourceFileUri;

    /** The original display title of the master score. */
    private String originalTitle;

    /** The parsed DOM Document representation of the original score. */
    private Document originalDoc;

    /** A mapping of part IDs to their metadata (names, IDs) extracted from the score. */
    private Map<String, MusicXmlParser.PartInfo> partInfoMap;

    /** Adapter managing the real-time Firestore synchronization for the roles list. */
    private RoleAdapter roleAdapter;

    /** Helper for Firestore database updates. */
    private FirestoreManager firestoreManager;

    /** Helper for Firebase Storage file uploads. */
    private FileStorage fileStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_grouping);

        firestoreManager = new FirestoreManager();
        fileStorage = new FileStorage();
        findViews();
        parseIntent();
        
        initFirestoreComponents();
        loadSourceFile();
    }

    /**
     * Ensures that every lesson starts with at least one default "Partitura" role
     * containing all available instrumental parts.
     */
    private void checkAndAddDefaultRoles() {
        String classId = DataManager.getCurClass().getId();
        String lessonId = DataManager.getCurLesson().getId();
        firestoreManager.getDraftRolesQuery(classId, lessonId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                Role partitura = new Role("Partitura");
                // If the score is already parsed, pre-select all parts for the full score.
                if (partInfoMap != null) {
                    List<String> allParts = new ArrayList<>(partInfoMap.keySet());
                    partitura.setSelectedPartIds(allParts);
                }
                firestoreManager.addDraftRole(classId, lessonId, partitura);
            }
        });
    }

    /**
     * Binds UI components to their XML layout identifiers and configures basic layout managers.
     */
    private void findViews() {
        tvSourceFile = findViewById(R.id.tv_source_file);
        btnAddRole = findViewById(R.id.btn_add_role);
        rvRoles = findViewById(R.id.rv_roles);
        btnSaveUpload = findViewById(R.id.btn_save_upload);
        pbLoading = findViewById(R.id.pb_loading);

        rvRoles.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Initializes the Firestore Recycler Adapter to provide a real-time list of 
     * draft roles as they are modified by the teacher.
     */
    private void initFirestoreComponents() {
        if (DataManager.getCurClass() == null || DataManager.getCurLesson() == null) {
            Toast.makeText(this, "Error: Missing lesson context", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String classId = DataManager.getCurClass().getId();
        String lessonId = DataManager.getCurLesson().getId();

        // Create a query for the "draftRoles" subcollection of the current lesson.
        Query query = firestoreManager.getDraftRolesQuery(classId, lessonId);

        FirestoreRecyclerOptions<Role> options = new FirestoreRecyclerOptions.Builder<Role>()
                .setQuery(query, Role.class)
                .build();

        roleAdapter = new RoleAdapter(options, this::showPartSelectionDialog, this::showEditNameDialog, this::showDeleteConfirmationDialog);
        rvRoles.setAdapter(roleAdapter);

        btnAddRole.setOnClickListener(v -> {
            Role newRole = new Role("New Role " + (roleAdapter.getItemCount() + 1));
            firestoreManager.addDraftRole(classId, lessonId, newRole);
        });

        btnSaveUpload.setOnClickListener(v -> saveAndUpload());
    }

    /**
     * Displays a confirmation dialog before deleting a draft role from Firestore.
     * @param docId The Firestore document ID of the role to delete.
     */
    private void showDeleteConfirmationDialog(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Role")
                .setMessage("Are you sure you want to delete this role?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestoreManager.deleteDraftRole(DataManager.getCurClass().getId(), DataManager.getCurLesson().getId(), docId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Displays a text input dialog to modify the display name of a specific role.
     * @param role The role object to modify.
     * @param docId The Firestore document ID of the role.
     */
    private void showEditNameDialog(Role role, String docId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Role Name");

        final EditText input = new EditText(this);
        input.setText(role.getName());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateRoleName(docId, newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateRoleName(String docId, String newName) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        firestoreManager.updateDraftRole(DataManager.getCurClass().getId(), DataManager.getCurLesson().getId(), docId, updates);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (roleAdapter != null) roleAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (roleAdapter != null) roleAdapter.stopListening();
    }

    /**
     * Extracts lesson context and source file information from the incoming Intent.
     */
    private void parseIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("fileUri")) {
            sourceFileUri = Uri.parse(intent.getStringExtra("fileUri"));
        }
        originalTitle = intent.getStringExtra("title");

        if (tvSourceFile != null) {
            tvSourceFile.setText("Source File: " + (originalTitle != null ? originalTitle : "Unknown"));
        }
    }

    /**
     * Loads and parses the source MusicXML file into a DOM Document.
     * This operation is offloaded to a background thread to prevent UI freezing.
     */
    private void loadSourceFile() {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                FileIO fileOps = new FileIO(this);
                // Step 1: Read raw XML string from local URI.
                String xmlContent = fileOps.readMusicXmlContent(sourceFileUri);

                // Step 2: Initialize XML Document Builder and parse the content.
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                originalDoc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

                // Step 3: Extract part metadata (names/IDs) to populate selection dialogs later.
                partInfoMap = MusicXmlParser.getPartsAndVoices(originalDoc);

                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "File parsed successfully", Toast.LENGTH_SHORT).show();
                    checkAndAddDefaultRoles();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading source file", e);
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error parsing file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Displays a multi-choice selection dialog containing all instrumental parts 
     * identified in the master score.
     * 
     * @param role The role object whose part selection is being modified.
     * @param docId The Firestore document ID of the role.
     */
    private void showPartSelectionDialog(Role role, String docId) {
        if (partInfoMap == null) return;

        List<String> partNames = new ArrayList<>();
        List<String> partIds = new ArrayList<>();

        // Collect names and IDs from the metadata map.
        for (MusicXmlParser.PartInfo part : partInfoMap.values()) {
            partNames.add(part.name + " (" + part.id + ")");
            partIds.add(part.id);
        }

        String[] labelsArray = partNames.toArray(new String[0]);
        boolean[] checkedItems = new boolean[partNames.size()];

        // Pre-check parts that are already part of this role.
        List<String> selectedParts = role.getSelectedPartIds();
        for (int i = 0; i < partIds.size(); i++) {
            if (selectedParts != null && selectedParts.contains(partIds.get(i))) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Parts for " + role.getName());
        builder.setMultiChoiceItems(labelsArray, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            List<String> newSelection = new ArrayList<>();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    newSelection.add(partIds.get(i));
                }
            }
            // Update Firestore with the new selection to ensure persistence.
            Map<String, Object> updates = new HashMap<>();
            updates.put("selectedPartIds", newSelection);
            firestoreManager.updateDraftRole(DataManager.getCurClass().getId(), DataManager.getCurLesson().getId(), docId, updates);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    /**
     * Finalizes the role grouping process. For each role defined by the teacher:
     * 1. Filters the original XML document to keep only selected parts.
     * 2. Converts the resulting DOM back to a String.
     * 3. Uploads the unique part-score to cloud storage.
     * 4. Cleans up the temporary "draft" role entries in Firestore.
     */
    private void saveAndUpload() {
        if (roleAdapter.getItemCount() == 0) {
            Toast.makeText(this, "Add at least one role", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                // Iterate through all draft roles defined in the adapter.
                for (int i = 0; i < roleAdapter.getItemCount(); i++) {
                    Role role = roleAdapter.getItem(i);
                    if (role.getSelectedPartIds() == null || role.getSelectedPartIds().isEmpty()) continue;

                    // Step 1: Filter original DOM to strip out unselected parts.
                    Document filteredDoc = MusicXmlParser.filterParts(originalDoc, role.getSelectedPartIds());
                    
                    // Step 2: Transform the DOM back into a formatted XML string.
                    String xmlString = MusicXmlParser.documentToString(filteredDoc);
                    
                    // Step 3: Trigger the async upload to Firebase Storage.
                    uploadRoleFile(role.getName(), xmlString);
                }

                // Step 4: Clean up draft roles after successful processing and upload start.
                for (int i = 0; i < roleAdapter.getItemCount(); i++) {
                    String docId = roleAdapter.getSnapshots().getSnapshot(i).getId();
                    firestoreManager.deleteDraftRole(DataManager.getCurClass().getId(), DataManager.getCurLesson().getId(), docId);
                }

                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Roles processed and upload started", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error generating role files", e);
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Interfaces with the FileStorage helper to persist the generated XML file.
     * @param roleName The name of the role (used as filename suffix).
     * @param content The filtered MusicXML content.
     */
    private void uploadRoleFile(String roleName, String content) {
        fileStorage.uploadRoleMusicFile(DataManager.getCurClass().getId(), DataManager.getCurLesson().getId(), originalTitle, roleName, content, DataManager.getUserInstance().getEmail());
    }
}