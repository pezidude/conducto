package com.example.conducto2.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lesson implements Parcelable {
    private String title;
    private String info;
    private Date date;
    private String classId;
    private List<MusicFile> musicXMLFiles; // List of MusicFile objects
    private Map<String, List<String>> fileMapping; // Map of file URL to list of student emails
    private String status; // "STOPPED", "PAUSED", "PLAYING"
    private boolean isLive;
    private boolean isArchived;
    private long targetTimestamp;
    private int currentMeasure;
    private int bpm;

    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_PAUSED = "PAUSED";
    public static final String STATUS_PLAYING = "PLAYING";

    private String id;

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
        this.musicXMLFiles = new ArrayList<>();
        if (event.musicXMLFiles != null) {
            this.musicXMLFiles.addAll(event.musicXMLFiles);
        }
        this.fileMapping = new HashMap<>();
        if (event.fileMapping != null) {
            this.fileMapping.putAll(event.fileMapping);
        }
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "title='" + title + '\'' +
                ", info='" + info + '\'' +
                ", date='" + date + '\'' +
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
        this.musicXMLFiles = new ArrayList<>();
        this.fileMapping = new HashMap<>();
    }
    public Lesson() {
        this("", "", new Date(), "");
    }

    protected Lesson(Parcel in) {
        title = in.readString();
        info = in.readString();
        long tmpDate = in.readLong();
        date = tmpDate == -1 ? null : new Date(tmpDate);
        id = in.readString();
        classId = in.readString();
        status = in.readString();
        isLive = in.readByte() != 0;
        isArchived = in.readByte() != 0;
        targetTimestamp = in.readLong();
        currentMeasure = in.readInt();
        bpm = in.readInt();
        musicXMLFiles = in.createTypedArrayList(MusicFile.CREATOR);
        fileMapping = new HashMap<>();
        in.readMap(fileMapping, List.class.getClassLoader());
    }

    public static final Creator<Lesson> CREATOR = new Creator<Lesson>() {
        @Override
        public Lesson createFromParcel(Parcel in) {
            return new Lesson(in);
        }

        @Override
        public Lesson[] newArray(int size) {
            return new Lesson[size];
        }
    };

    public void setId(String id){ this.id = id;}
    public String getId(){return id;}

    @Override
    public int describeContents() {
        return 0;
    }

    public String getInfo() {
        return info;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(info);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeString(id);
        dest.writeString(classId);
        dest.writeString(status);
        dest.writeByte((byte) (isLive ? 1 : 0));
        dest.writeByte((byte) (isArchived ? 1 : 0));
        dest.writeLong(targetTimestamp);
        dest.writeInt(currentMeasure);
        dest.writeInt(bpm);
        dest.writeTypedList(musicXMLFiles);
        dest.writeMap(fileMapping);
    }
}
