package com.example.conducto2.data.model;

import com.example.conducto2.R;

public class JazzLesson extends Lesson {
    public JazzLesson() {
        super();
        setGenre("Jazz");
    }

    public JazzLesson(Lesson lesson) {
        super(lesson);
        setGenre("Jazz");
    }

    @Override
    public int getGenreColorResId() {
        return R.color.brand_accent; // Gold
    }

    @Override
    public int getGenreIconResId() {
        return R.drawable.speaker_24px;
    }

    @Override
    public String getGenreLabel() {
        return "Jazz";
    }

    @Override
    public int getRecentLessonTintResId() {
        return R.color.brand_accent;
    }
}
