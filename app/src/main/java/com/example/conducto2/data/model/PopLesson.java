package com.example.conducto2.data.model;

import com.example.conducto2.R;

/**
 * PopLesson
 * 
 * A polymorphic subclass of {@link Lesson} representing the Pop music genre.
 * It overrides base methods to provide genre-specific UI resources such as colors and icons.
 */
public class PopLesson extends Lesson {
    
    /**
     * Default constructor required for Firestore deserialization.
     */
    public PopLesson() {
        super();
        setGenre("Pop");
    }

    /**
     * Copy constructor used for polymorphic instantiation.
     * @param lesson The generic lesson to copy.
     */
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

    @Override
    public Lesson copy() {
        return new PopLesson(this);
    }
}