package com.example.conducto2.ui.lessons;

import android.os.Bundle;
import android.widget.TextView;
import com.example.conducto2.R;
import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.ui.BaseDrawerActivity;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LessonDetailsActivity extends BaseDrawerActivity {

    private TextView lessonTitle;
    private TextView lessonDate;
    private TextView lessonInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lesson_details);

        lessonTitle = findViewById(R.id.lesson_details_title);
        lessonDate = findViewById(R.id.lesson_details_date);
        lessonInfo = findViewById(R.id.lesson_details_info);

        Lesson lesson = getIntent().getParcelableExtra("lesson");

        if (lesson != null) {
            lessonTitle.setText(lesson.getTitle());
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
            lessonDate.setText(dateTimeFormat.format(lesson.getDate()));
            lessonInfo.setText(lesson.getInfo());
        }
    }
}