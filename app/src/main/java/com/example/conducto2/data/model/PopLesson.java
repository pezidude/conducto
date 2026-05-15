package com.example.conducto2.data.model;

import com.example.conducto2.R;

public class PopLesson extends Lesson {
    public PopLesson() {
        super();
        setGenre("Pop");
    }

    public PopLesson(Lesson lesson) {
        super(lesson);
        setGenre("Pop");
    }

    @Override
    public int getGenreColorResId() {
        return R.color.success; // Green
    }

    @Override
    public int getGenreIconResId() {
        return R.drawable.baseline_headphones_24;
    }

    @Override
    public String getGenreLabel() {
        return "Pop";
    }

    @Override
    public int getRecentLessonTintResId() {
        return R.color.success;
    }
}
