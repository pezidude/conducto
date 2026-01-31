package com.example.conducto2.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.conducto2.R;
import com.example.conducto2.data.file.FileIO;
import com.example.conducto2.data.file.FilePickerHelper;
import com.example.conducto2.ui.lessons.ClassActivity;

public class TestActivity extends AppCompatActivity implements FilePickerHelper.OnFilePickedListener {

    private View cardSelectFile;
    private Button btnViewLessons;
    private TextView tvFilename;
    private FileIO fileio;
    private FilePickerHelper picker;

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
        btnViewLessons = findViewById(R.id.btnViewLessons);
    }

    private void setupListeners() {
        if (cardSelectFile != null) {
            cardSelectFile.setOnClickListener(v -> selectFile());
        }
        btnViewLessons.setOnClickListener(v -> {
            Intent intent = new Intent(TestActivity.this, ClassActivity.class);
            startActivity(intent);
        });
    }

    private void selectFile() {
        picker.openFilePicker();
    }

    @Override
    public void onFilePicked(Uri fileUri, String fileName) {
        tvFilename.setText(fileName);
    }
}