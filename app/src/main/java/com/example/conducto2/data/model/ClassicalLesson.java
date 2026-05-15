package com.example.conducto2.data.model;

import com.example.conducto2.R;

public class ClassicalLesson extends Lesson {
    public ClassicalLesson() {
        super();
        setGenre("Classical");
    }

    public ClassicalLesson(Lesson lesson) {
        super(lesson);
        setGenre("Classical");
    }

    @Override
    public int getGenreColorResId() {
        return R.color.info; // Blue
    }

    @Override
    public int getGenreIconResId() {
        return R.drawable.ic_music_note;
    }

    @Override
    public String getGenreLabel() {
        return "Classical";
    }
}
