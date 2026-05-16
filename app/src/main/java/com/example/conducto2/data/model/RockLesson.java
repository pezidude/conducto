package com.example.conducto2.data.model;

import com.example.conducto2.R;

public class RockLesson extends Lesson {
    public RockLesson() {
        super();
        setGenre("Rock");
    }

    public RockLesson(Lesson lesson) {
        super(lesson);
        setGenre("Rock");
    }

    @Override
    public int getGenreColorResId() {
        return R.color.error; // Red
    }

    @Override
    public int getGenreIconResId() {
        return R.drawable.ic_music_note;
    }

    @Override
    public String getGenreLabel() {
        return "Rock";
    }

    @Override
    public int getRecentLessonTintResId() {
        return R.color.error;
    }
}
