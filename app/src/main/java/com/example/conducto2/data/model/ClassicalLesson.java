package com.example.conducto2.data.model;

import com.example.conducto2.R;

/**
 * ClassicalLesson
 * 
 * A polymorphic subclass of {@link Lesson} representing the Classical music genre.
 * It overrides base methods to provide genre-specific UI resources such as colors and icons.
 */
public class ClassicalLesson extends Lesson {
    
    /**
     * Default constructor required for Firestore deserialization.
     */
    public ClassicalLesson() {
        super();
        setGenre("Classical");
    }

    /**
     * Copy constructor used for polymorphic instantiation.
     * @param lesson The generic lesson to copy.
     */
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
        return R.drawable.ic_treble_clef;
    }

    @Override
    public String getGenreLabel() {
        return "Classical";
    }

    @Override
    public int getRecentLessonTintResId() {
        return R.color.info;
    }

    @Override
    public Lesson copy() {
        return new ClassicalLesson(this);
    }
}