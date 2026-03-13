
package com.example.conducto2.ui.lessons;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.conducto2.R;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.MusicFile;
import com.example.conducto2.ui.BaseDrawerActivity;
import com.example.conducto2.ui.player.MIDIPlayerActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LessonDetailsActivity extends BaseDrawerActivity {

    private TextView lessonTitle;
    private TextView lessonDate;
    private TextView lessonInfo;
    private ListView musicXmlFilesList;
    private Lesson lesson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_details);

        lessonTitle = findViewById(R.id.lesson_details_title);
        lessonDate = findViewById(R.id.lesson_details_date);
        lessonInfo = findViewById(R.id.lesson_details_info);
        musicXmlFilesList = findViewById(R.id.music_xml_files_list);

        lesson = getIntent().getParcelableExtra("lesson");

        if (lesson != null) {
            lessonTitle.setText(lesson.getTitle());
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));
            lessonInfo.setText(lesson.getInfo());

            if (lesson.getMusicXMLFiles() != null && !lesson.getMusicXMLFiles().isEmpty()) {
                List<String> fileTitles = new ArrayList<>();
                for (MusicFile musicFile : lesson.getMusicXMLFiles()) {
                    fileTitles.add(musicFile.getTitle());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, fileTitles);
                musicXmlFilesList.setAdapter(adapter);

                musicXmlFilesList.setOnItemClickListener((parent, view, position, id) -> {
                    MusicFile selectedFile = lesson.getMusicXMLFiles().get(position);
                    Intent intent = new Intent(this, MIDIPlayerActivity.class);
                    intent.putExtra("readOnly", true);
                    intent.putExtra("fileUri", selectedFile.getUri());
                    startActivity(intent);
                });
            }
        }
    }
}
