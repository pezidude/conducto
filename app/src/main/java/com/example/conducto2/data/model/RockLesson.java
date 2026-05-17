package com.example.conducto2.data.model;

import com.example.conducto2.R;

/**
 * RockLesson
 * 
 * A polymorphic subclass of {@link Lesson} representing the Rock music genre.
 * It overrides base methods to provide genre-specific UI resources such as colors and icons.
 */
public class RockLesson extends Lesson {
    
    /**
     * Default constructor required for Firestore deserialization.
     */
    public RockLesson() {
        super();
        setGenre("Rock");
    }

    /**
     * Copy constructor used by the {@link Lesson#fromBase(Lesson)} factory method.
     * @param lesson The generic lesson to cast to Rock.
     */
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