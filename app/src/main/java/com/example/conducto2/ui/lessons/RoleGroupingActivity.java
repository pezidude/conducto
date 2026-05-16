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

public class RoleGroupingActivity extends AppCompatActivity {

    private static final String TAG = "RoleGroupingActivity";

    private TextView tvSourceFile;
    private Button btnAddRole;
    private RecyclerView rvRoles;
    private Button btnSaveUpload;
    private ProgressBar pbLoading;

    private Uri sourceFileUri;
    private String classId;
    private String lessonId;
    private String originalTitle;

    private Document originalDoc;
    private Map<String, MusicXmlParser.PartInfo> partInfoMap;
    private RoleAdapter roleAdapter;
    private FirestoreManager firestoreManager;
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

    private void checkAndAddDefaultRoles() {
        firestoreManager.getDraftRolesQuery(classId, lessonId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                Role partitura = new Role("Partitura");
                if (partInfoMap != null) {
                    List<String> allParts = new ArrayList<>(partInfoMap.keySet());
                    partitura.setSelectedPartIds(allParts);
                }
                firestoreManager.addDraftRole(classId, lessonId, partitura);
            }
        });
    }

    private void findViews() {
        tvSourceFile = findViewById(R.id.tv_source_file);
        btnAddRole = findViewById(R.id.btn_add_role);
        rvRoles = findViewById(R.id.rv_roles);
        btnSaveUpload = findViewById(R.id.btn_save_upload);
        pbLoading = findViewById(R.id.pb_loading);

        rvRoles.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initFirestoreComponents() {
        if (classId == null || lessonId == null) {
            Toast.makeText(this, "Error: Missing lesson context", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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

    private void showDeleteConfirmationDialog(String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Role")
                .setMessage("Are you sure you want to delete this role?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestoreManager.deleteDraftRole(classId, lessonId, docId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

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
        firestoreManager.updateDraftRole(classId, lessonId, docId, updates);
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

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("fileUri")) {
            sourceFileUri = Uri.parse(intent.getStringExtra("fileUri"));
        }
        classId = intent.getStringExtra("classId");
        lessonId = intent.getStringExtra("lessonId");
        originalTitle = intent.getStringExtra("title");

        if (tvSourceFile != null) {
            tvSourceFile.setText("Source File: " + (originalTitle != null ? originalTitle : "Unknown"));
        }
    }

    private void loadSourceFile() {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                FileIO fileOps = new FileIO(this);
                String xmlContent = fileOps.readMusicXmlContent(sourceFileUri);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                originalDoc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

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

    private void showPartSelectionDialog(Role role, String docId) {
        if (partInfoMap == null) return;

        List<String> partNames = new ArrayList<>();
        List<String> partIds = new ArrayList<>();

        for (MusicXmlParser.PartInfo part : partInfoMap.values()) {
            partNames.add(part.name + " (" + part.id + ")");
            partIds.add(part.id);
        }

        String[] labelsArray = partNames.toArray(new String[0]);
        boolean[] checkedItems = new boolean[partNames.size()];

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
            Map<String, Object> updates = new HashMap<>();
            updates.put("selectedPartIds", newSelection);
            firestoreManager.updateDraftRole(classId, lessonId, docId, updates);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveAndUpload() {
        if (roleAdapter.getItemCount() == 0) {
            Toast.makeText(this, "Add at least one role", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                for (int i = 0; i < roleAdapter.getItemCount(); i++) {
                    Role role = roleAdapter.getItem(i);
                    if (role.getSelectedPartIds() == null || role.getSelectedPartIds().isEmpty()) continue;

                    Document filteredDoc = MusicXmlParser.filterParts(originalDoc, role.getSelectedPartIds());
                    String xmlString = MusicXmlParser.documentToString(filteredDoc);
                    uploadRoleFile(role.getName(), xmlString);
                }

                // Clean up draft roles after upload
                for (int i = 0; i < roleAdapter.getItemCount(); i++) {
                    String docId = roleAdapter.getSnapshots().getSnapshot(i).getId();
                    firestoreManager.deleteDraftRole(classId, lessonId, docId);
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

    private void uploadRoleFile(String roleName, String content) {
        fileStorage.uploadRoleMusicFile(classId, lessonId, originalTitle, roleName, content, DataManager.getUserInstance().getEmail());
    }
}