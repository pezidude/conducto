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
        return R.drawable.ic_music_note; // Could use a different icon if available
    }

    @Override
    public String getGenreLabel() {
        return "Jazz";
    }
}
