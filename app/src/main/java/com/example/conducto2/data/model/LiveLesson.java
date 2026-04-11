package com.example.conducto2.data.model;

import java.io.Serializable;

/**
 * Describes a live lesson session.
 */
public class LiveLesson implements Serializable {
    private String classID;
    private String lessonID;
    private boolean isActive;

    public enum lessonState {
        PLAYING,
        PAUSED,
        STOPPED
    }
    private lessonState state;


    public LiveLesson() {
        // Default constructor required for Firestore
    }

    public LiveLesson(String classID, String lessonID, boolean isActive) {
        this.classID = classID;
        this.lessonID = lessonID;
        this.isActive = isActive;
        state = lessonState.STOPPED; // default state for our case
    }

    public lessonState getState() {
        return state;
    }

    public void setState(lessonState state) {
        this.state = state;
    }

    public String getClassID() {
        return classID;
    }

    public void setClassID(String classID) {
        this.classID = classID;
    }

    public String getLessonID() {
        return lessonID;
    }

    public void setLessonID(String lessonID) {
        this.lessonID = lessonID;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
