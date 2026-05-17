package com.example.conducto2.data.manager;

import androidx.annotation.Nullable;

import com.example.conducto2.data.model.Lesson;
import com.example.conducto2.data.model.User;
import com.example.conducto2.data.model.Class;

/**
 * DataManager
 * 
 * A centralized singleton-style utility that provides global access to the application's 
 * primary data models (User, Lesson, Class). It acts as an in-memory cache, allowing 
 * different activities and fragments to share state without complex Intent passing.
 * 
 * The class implements "safe-set" logic, using copy constructors to prevent 
 * reference-sharing side effects across different components.
 */
public class DataManager {

    /** The currently authenticated user profile. */
    private static User user;

    /** The lesson currently being viewed, edited, or played. */
    private static Lesson curLesson;

    /** The classroom context currently active in the UI. */
    private static Class curClass;

    /**
     * Retrieves the global User instance.
     * @return The cached User object.
     */
    public static User getUserInstance(){
        return user;
    }

    /**
     * Sets the global User instance. 
     * Creates a deep copy of the provided user to ensure that modifications to 
     * local activity variables do not accidentally corrupt the global state.
     * 
     * @param other The user object to cache.
     */
    public  static void setUser(User other){
        // Logic: Instantiate a new User object to break reference with the source.
        user = new User(other.getEmail(), other.getFname(), other.getLname(), other.getUserType(), other.getProfilePictureBase64());
        user.setDescription(other.getDescription());
    }

    /**
     * Retrieves the lesson currently in focus.
     * @return The cached Lesson object.
     */
    public static  Lesson getCurLesson(){return curLesson;}

    /**
     * Sets the lesson currently in focus. 
     * Handles the transition between viewing an existing lesson and initiating 
     * the creation of a new one.
     * 
     * @param lesson The lesson to cache, or null to clear the cache for a new lesson.
     */
    public static void setCurLesson(@Nullable Lesson lesson){
        if (lesson == null) { 
            // Logic: Null indicates a state reset for lesson creation.
            curLesson = null;
            return;
        }
        // Logic: Use copy constructor to isolate the global state from UI-thread mutations.
        curLesson = new Lesson(lesson);
    }

    /**
     * Retrieves the classroom context.
     * @return The cached Class object.
     */
    public static Class getCurClass(){
        return curClass;
    }

    /**
     * Sets the active classroom context.
     * @param cls The Class object to cache.
     */
    public static void setCurClass(Class cls){
        curClass = cls;
    }
}