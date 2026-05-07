package com.example.conducto2.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lesson implements Parcelable {
    private String title;
    private String info;
    private Date date;
    private String ownerEmail;
    private String classId;
    private List<MusicFile> musicXMLFiles; // List of MusicFile objects
    private Map<String, List<String>> fileMapping; // Map of file URL to list of student emails

    private String id;

    public Lesson(Lesson event) {
        this.title = event.title;
        this.info = event.info;
        this.date = event.date;
        this.ownerEmail = event.ownerEmail;
        this.id = event.id;
        this.classId = event.classId;
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
                '}';
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
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

    public Lesson(String title, String info, Date date, String ownerEmail, String classId) {
        this.title = title;
        this.info = info;
        this.date = date;
        this.ownerEmail = ownerEmail;
        this.classId = classId;
        this.musicXMLFiles = new ArrayList<>();
        this.fileMapping = new HashMap<>();
    }
    public Lesson() {
        this("", "", new Date(), "", "");
    }

    protected Lesson(Parcel in) {
        title = in.readString();
        info = in.readString();
        long tmpDate = in.readLong();
        date = tmpDate == -1 ? null : new Date(tmpDate);
        ownerEmail = in.readString();
        id = in.readString();
        classId = in.readString();
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(info);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeString(ownerEmail);
        dest.writeString(id);
        dest.writeString(classId);
        dest.writeTypedList(musicXMLFiles);
        dest.writeMap(fileMapping);
    }
}