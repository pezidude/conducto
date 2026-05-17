package com.example.conducto2.data.model;

import com.example.conducto2.R;

/**
 * JazzLesson
 * 
 * A polymorphic subclass of {@link Lesson} representing the Jazz music genre.
 * It overrides base methods to provide genre-specific UI resources such as colors and icons.
 */
public class JazzLesson extends Lesson {
    
    /**
     * Default constructor required for Firestore deserialization.
     */
    public JazzLesson() {
        super();
        setGenre("Jazz");
    }

    /**
     * Copy constructor used by the {@link Lesson#fromBase(Lesson)} factory method.
     * @param lesson The generic lesson to cast to Jazz.
     */
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