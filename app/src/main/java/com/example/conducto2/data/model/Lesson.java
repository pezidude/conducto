package com.example.conducto2.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;

public class Lesson implements Parcelable {
    private String title;
    private String info;
    private Date date;
    private String ownerEmail;
    private ArrayList<String> attendees;
    private String classId;

    private String id;


    public Lesson(Lesson event) {
        this.title = event.title;
        this.info = event.info;
        this.date = event.date;
        this.ownerEmail = event.ownerEmail;
        this.id = event.id;
        this.classId = event.classId;
        attendees = new ArrayList<>();
        if (event.attendees != null) {
            for (int i=0; i<event.attendees.size(); i++){
                attendees.add(event.attendees.get(i));
            }
        }
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "title='" + title + '\'' +
                ", info='" + info + '\'' +
                ", date='" + date + '\'' +
                ", attendees=" + attendees.toString() +
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

    public void setAttendees(ArrayList<String> attendees) {
        this.attendees = attendees;
    }

    public String getTitle() {
        return title;
    }

    public String getInfo() {
        return info;
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

    public ArrayList<String> getAttendees() {
        return attendees;
    }

    public Lesson(String title, String info, Date date, String ownerEmail, String classId) {
        this.title = title;
        this.info = info;
        this.date = date;
        this.attendees = new ArrayList<>();
        this.ownerEmail = ownerEmail;
        this.classId = classId;
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
        attendees = in.createStringArrayList();
        id = in.readString();
        classId = in.readString();
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
    public void addAtendee(String email){
        if (attendees == null) {
            attendees = new ArrayList<>();
        }
        attendees.add(email);
    }

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
        dest.writeStringList(attendees);
        dest.writeString(id);
        dest.writeString(classId);
    }
}