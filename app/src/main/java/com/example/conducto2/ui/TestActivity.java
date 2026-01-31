package com.example.conducto2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.file.FilePickerHelper;
import com.example.conducto2.ui.player.MIDIPlayerActivity;

public class TestActivity extends AppCompatActivity implements FilePickerHelper.OnFilePickedListener {

    private View cardSelectFile;
    private Button btnViewSheetMusic;
    private TextView tvFilename;
    private FileIO fileio;
    private FilePickerHelper picker;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fileio = new FileIO(this);
        picker = new FilePickerHelper(this, fileio, this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardSelectFile = findViewById(R.id.cardSelectFile);
        tvFilename = findViewById(R.id.tvFilename);
        btnViewSheetMusic = findViewById(R.id.btnViewSheetMusic);
    }

    private void setupListeners() {
        if (cardSelectFile != null) {
            cardSelectFile.setOnClickListener(v -> selectFile());
        }

        btnViewSheetMusic.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                Intent intent = new Intent(TestActivity.this, MIDIPlayerActivity.class);
                intent.putExtra("fileUri", selectedFileUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectFile() {
        picker.openFilePicker();
    }

    @Override
    public void onFilePicked(Uri fileUri, String fileName) {
        tvFilename.setText(fileName);
        selectedFileUri = fileUri;
        btnViewSheetMusic.setEnabled(true);
    }
}