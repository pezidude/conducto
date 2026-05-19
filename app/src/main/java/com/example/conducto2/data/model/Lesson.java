package com.example.conducto2.data.model;

import com.google.firebase.firestore.DocumentId;
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
 * This class serves as the base for a polymorphic inheritance structure where 
 * specific genres (Classical, Jazz, etc.) extend this class to provide custom UI rendering metadata.
 * 
 * It manages:
 * - Basic metadata (title, info, date).
 * - Live synchronization state (status, targetTimestamp, currentMeasure, bpm).
 * - File distribution (musicXMLFiles list, fileMapping map).
 */
public class Lesson {
    
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
    public Lesson(Lesson event) {
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
     * Designed to be overridden by subclasses (e.g., ClassicalLesson).
     */
    public int getGenreColorResId() {
        return com.example.conducto2.R.color.brand_accent;
    }

    /**
     * Polymorphic method to get the icon for this lesson type.
     * Designed to be overridden by subclasses.
     */
    public int getGenreIconResId() {
        return com.example.conducto2.R.drawable.ic_music_note;
    }

    /**
     * Polymorphic method to get the display label for this lesson type.
     * Designed to be overridden by subclasses.
     */
    public String getGenreLabel() {
        return genre != null ? genre : "";
    }

    /**
     * Polymorphic method to get the tint color for recent lesson menu items.
     * Designed to be overridden by subclasses.
     */
    public int getRecentLessonTintResId() {
        return getGenreColorResId();
    }

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

    public Lesson(String title, String info, Date date, String classId) {
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
    public Lesson() {
        this("", "", new Date(), "");
    }

    /**
     * Factory method implementing the Polymorphic creation pattern.
     * Evaluates the generic 'base' Lesson fetched from Firestore and returns
     * an instance of the specific subclass required for proper UI rendering.
     * 
     * @param base The plain Lesson object deserialized by Firestore.
     * @return A subclassed Lesson (e.g., JazzLesson), or the base lesson if genre is unknown.
     */
    public static Lesson fromBase(Lesson base) {
        if (base == null || base.getGenre() == null) return base;
        
        switch (base.getGenre()) {
            case "Classical":
                return new ClassicalLesson(base);
            case "Jazz":
                return new JazzLesson(base);
            case "Pop":
                return new PopLesson(base);
            case "Rock":
                return new RockLesson(base);
            default:
                return base;
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