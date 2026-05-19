package com.example.conducto2.data.model;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lesson
 * 
 * The core data model representing a teaching session within a Class. 
 * This class is abstract to enforce a polymorphic inheritance structure where 
 * specific genres (Classical, Jazz, etc.) extend this class to provide custom UI rendering metadata.
 * 
 * It manages:
 * - Basic metadata (title, info, date).
 * - Live synchronization state (status, targetTimestamp, currentMeasure, bpm).
 * - File distribution (musicXMLFiles list, fileMapping map).
 */
public abstract class Lesson {

    /** list of all currently supported lesson genres **/
    final public static String[] GENRES = {"Classical", "Jazz", "Pop", "Rock"};
    
    /** The title of the lesson. */
    private String title;
    
    /** Additional description or instructions for the lesson. */
    private String info;
    
    /** The scheduled date/time of the lesson. */
    private Date date;
    
    /** The Firestore ID of the parent class this lesson belongs to. */
    private String classId;
    
    /** List of basic MusicFile objects (metadata + URL) associated with the lesson. */
    private List<MusicFile> musicXMLFiles; 
    
    /** A mapping defining which specific MusicXML file URL is assigned to which student emails. */
    private Map<String, List<String>> fileMapping; 
    
    /** Current playback state: "STOPPED", "PAUSED", "PLAYING". */
    private String status; 
    
    /** Flag indicating if the lesson is currently broadcasting real-time updates. */
    private boolean isLive;
    
    /** Flag indicating if the lesson has been completed and moved to history. */
    private boolean isArchived;
    
    /** Unix timestamp (server-aligned) defining when all clients should begin playback. */
    private long targetTimestamp;
    
    /** The current musical measure index the class is focused on. */
    private int currentMeasure;
    
    /** The tempo (Beats Per Minute) for the current session. */
    private int bpm;
    
    /** A String classifier used by the factory method to instantiate the correct subclass. */
    private String genre; 
    
    /** A real-time list of student emails currently viewing the live lesson. */
    private List<String> connectedStudents; 

    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_PLAYING = "PLAYING";

    /** The unique Firestore document ID of this lesson. */
    private String id;

    /**
     * Copy constructor used primarily during the polymorphic factory instantiation.
     * @param event The base Lesson object to copy fields from.
     */
    protected Lesson(Lesson event) {
        this.title = event.title;
        this.info = event.info;
        this.date = event.date;
        this.id = event.id;
        this.classId = event.classId;
        this.status = event.status != null ? event.status : STATUS_STOPPED;
        this.isLive = event.isLive;
        this.isArchived = event.isArchived;
        this.targetTimestamp = event.targetTimestamp;
        this.genre = event.genre;
        this.musicXMLFiles = new ArrayList<>();
        if (event.musicXMLFiles != null) {
            this.musicXMLFiles.addAll(event.musicXMLFiles);
        }
        this.fileMapping = new HashMap<>();
        if (event.fileMapping != null) {
            this.fileMapping.putAll(event.fileMapping);
        }
        this.connectedStudents = new ArrayList<>();
        if (event.connectedStudents != null) {
            this.connectedStudents.addAll(event.connectedStudents);
        }
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    /**
     * Polymorphic method to get the theme color for this lesson type.
     * Must be implemented by subclasses.
     */
    @Exclude
    public abstract int getGenreColorResId();

    /**
     * Polymorphic method to get the icon for this lesson type.
     * Must be implemented by subclasses.
     */
    @Exclude
    public abstract int getGenreIconResId();

    /**
     * Polymorphic method to get the display label for this lesson type.
     * Must be implemented by subclasses.
     */
    @Exclude
    public abstract String getGenreLabel();

    /**
     * Polymorphic method to get the tint color for recent lesson menu items.
     * Must be implemented by subclasses.
     */
    @Exclude
    public abstract int getRecentLessonTintResId();

    /**
     * Abstract method to create a deep copy of the lesson, preserving its concrete type.
     * @return A new instance of the same subclass with copied data.
     */
    public abstract Lesson copy();

    @Override
    public String toString() {
        return "Lesson{" +
                "title='" + title + '\'' +
                ", info='" + info + '\'' +
                ", date='" + date + '\'' +
                ", genre='" + genre + '\'' +
                ", musicXMLFiles=" + musicXMLFiles.toString() +
                ", fileMapping=" + fileMapping.toString() +
                ", status='" + status + '\'' +
                ", isLive=" + isLive +
                ", isArchived=" + isArchived +
                ", targetTimestamp=" + targetTimestamp +
                '}';
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public List<MusicFile> getMusicXMLFiles() {
        return musicXMLFiles;
    }

    public void setMusicXMLFiles(List<MusicFile> musicXMLFiles) {
        this.musicXMLFiles = musicXMLFiles;
    }

    public Map<String, List<String>> getFileMapping() {
        return fileMapping;
    }

    public void setFileMapping(Map<String, List<String>> fileMapping) {
        this.fileMapping = fileMapping;
    }

    public List<String> getConnectedStudents() {
        return connectedStudents;
    }

    public void setConnectedStudents(List<String> connectedStudents) {
        this.connectedStudents = connectedStudents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("isLive")
    public boolean isLive() {
        return isLive;
    }

    @PropertyName("isLive")
    public void setLive(boolean live) {
        isLive = live;
    }

    // without this annotation firebase calls it "archived" by stripping the is prefix.
    @PropertyName("isArchived")
    public boolean isArchived() {
        return isArchived;
    }

    @PropertyName("isArchived")
    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public long getTargetTimestamp() {
        return targetTimestamp;
    }

    public void setTargetTimestamp(long targetTimestamp) {
        this.targetTimestamp = targetTimestamp;
    }

    public int getCurrentMeasure() {
        return currentMeasure;
    }

    public void setCurrentMeasure(int currentMeasure) {
        this.currentMeasure = currentMeasure;
    }

    public int getBpm() {
        return bpm;
    }

    public void setBpm(int bpm) {
        this.bpm = bpm;
    }

    protected Lesson(String title, String info, Date date, String classId) {
        this.title = title;
        this.info = info;
        this.date = date;
        this.classId = classId;
        this.status = STATUS_STOPPED;
        this.isLive = false;
        this.isArchived = false;
        this.targetTimestamp = 0;
        this.currentMeasure = 0;
        this.bpm = 100;
        this.genre = null;
        this.musicXMLFiles = new ArrayList<>();
        this.fileMapping = new HashMap<>();
        this.connectedStudents = new ArrayList<>();
    }

    /**
     * Default constructor required for Firestore deserialization in subclasses.
     */
    protected Lesson() {
        this("", "", new Date(), "");
    }

    /**
     * Factory method implementing the Polymorphic creation pattern.
     * Evaluates the generic 'base' Lesson fetched from Firestore and returns
     * an instance of the specific subclass required for proper UI rendering.
     * 
     * @param snapshot The DocumentSnapshot fetched from Firestore.
     * @return A subclassed Lesson (e.g., JazzLesson), or null if genre is unknown.
     */
    public static Lesson fromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) return null;
        String genre = snapshot.getString("genre");
        Lesson lesson;
        if ("Classical".equals(genre)) lesson = snapshot.toObject(ClassicalLesson.class);
        else if ("Jazz".equals(genre)) lesson = snapshot.toObject(JazzLesson.class);
        else if ("Pop".equals(genre)) lesson = snapshot.toObject(PopLesson.class);
        else if ("Rock".equals(genre)) lesson = snapshot.toObject(RockLesson.class);
        else return null;

        if (lesson != null) {
            lesson.setId(snapshot.getId());
        }
        return lesson;
    }

    /**
     * Creates a new instance of a specific Lesson subclass based on the provided genre.
     * @param genre The genre string (e.g., "Classical").
     * @return A concrete Lesson instance.
     */
    public static Lesson createByGenre(String genre) {
        if (genre == null) return null;
        switch (genre) {
            case "Classical": return new ClassicalLesson();
            case "Jazz": return new JazzLesson();
            case "Pop": return new PopLesson();
            case "Rock": return new RockLesson();
            default: return null;
        }
    }

    public void setId(String id){ this.id = id;}
    public String getId(){return id;}

    public String getInfo() {
        return info;
    }

    public String getTitle() {
        return title;
    }
}