package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.utils.MusicXmlParser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private List<Role> roles = new ArrayList<>();
    private RoleAdapter roleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_grouping);

        initViews();
        parseIntent();
        loadSourceFile();
    }

    private void initViews() {
        tvSourceFile = findViewById(R.id.tv_source_file);
        btnAddRole = findViewById(R.id.btn_add_role);
        rvRoles = findViewById(R.id.rv_roles);
        btnSaveUpload = findViewById(R.id.btn_save_upload);
        pbLoading = findViewById(R.id.pb_loading);

        rvRoles.setLayoutManager(new LinearLayoutManager(this));
        roleAdapter = new RoleAdapter(roles, this::showVoiceSelectionDialog);
        rvRoles.setAdapter(roleAdapter);

        btnAddRole.setOnClickListener(v -> {
            roles.add(new Role("New Role " + (roles.size() + 1)));
            roleAdapter.notifyItemInserted(roles.size() - 1);
        });

        btnSaveUpload.setOnClickListener(v -> saveAndUpload());
    }

    private void parseIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("fileUri")) {
            sourceFileUri = Uri.parse(intent.getStringExtra("fileUri"));
        }
        classId = intent.getStringExtra("classId");
        lessonId = intent.getStringExtra("lessonId");
        originalTitle = intent.getStringExtra("title");

        tvSourceFile.setText("Source File: " + originalTitle);
    }

    private void loadSourceFile() {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                FileIO fileOps = new FileIO(this);
                String xmlContent = originalTitle.toLowerCase().endsWith(".mxl")
                        ? fileOps.readZippedXMLFromUri(sourceFileUri)
                        : fileOps.readTextFromUri(sourceFileUri);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                originalDoc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));

                partInfoMap = MusicXmlParser.getPartsAndVoices(originalDoc);

                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "File parsed successfully", Toast.LENGTH_SHORT).show();
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

    private void showVoiceSelectionDialog(int rolePosition) {
        if (partInfoMap == null) return;

        Role role = roles.get(rolePosition);
        List<String> voiceLabels = new ArrayList<>();
        List<String> partIds = new ArrayList<>();
        List<String> voices = new ArrayList<>();

        for (MusicXmlParser.PartInfo part : partInfoMap.values()) {
            for (String voice : part.voices) {
                voiceLabels.add("Part: " + part.name + " - Voice: " + voice);
                partIds.add(part.id);
                voices.add(voice);
            }
        }

        String[] labelsArray = voiceLabels.toArray(new String[0]);
        boolean[] checkedItems = new boolean[voiceLabels.size()];

        for (int i = 0; i < voiceLabels.size(); i++) {
            String pId = partIds.get(i);
            String v = voices.get(i);
            if (role.getSelectedVoicesPerPart().containsKey(pId) &&
                role.getSelectedVoicesPerPart().get(pId).contains(v)) {
                checkedItems[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Voices for " + role.getName());
        builder.setMultiChoiceItems(labelsArray, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            role.getSelectedVoicesPerPart().clear();
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    String pId = partIds.get(i);
                    String v = voices.get(i);
                    if (!role.getSelectedVoicesPerPart().containsKey(pId)) {
                        role.getSelectedVoicesPerPart().put(pId, new HashSet<>());
                    }
                    role.getSelectedVoicesPerPart().get(pId).add(v);
                }
            }
            roleAdapter.notifyItemChanged(rolePosition);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveAndUpload() {
        if (roles.isEmpty()) {
            Toast.makeText(this, "Add at least one role", Toast.LENGTH_SHORT).show();
            return;
        }

        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                for (Role role : roles) {
                    if (role.getSelectedVoicesPerPart().isEmpty()) continue;

                    Document filteredDoc = MusicXmlParser.filterVoices(originalDoc, role.getSelectedVoicesPerPart());
                    String xmlString = MusicXmlParser.documentToString(filteredDoc);
                    uploadRoleFile(role.getName(), xmlString);
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
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        String fileName = "role_" + roleName.replaceAll("\\s+", "_") + "_" + UUID.randomUUID().toString() + ".musicxml";
        StorageReference fileRef = storageRef.child("classes/" + classId + "/lessons/" + lessonId + "/" + fileName);

        byte[] data = content.getBytes(StandardCharsets.UTF_8);
        fileRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    MusicFile musicFile = new MusicFile(originalTitle + " - " + roleName, uri);
                    FirebaseFirestore.getInstance()
                            .collection("classes").document(classId)
                            .collection("lessons").document(lessonId)
                            .collection("musicFiles")
                            .add(musicFile);
                }))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to upload role file: " + roleName, e));
    }
}