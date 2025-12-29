package com.example.conducto2;

import java.util.ArrayList;

public class Lesson {
    private String title;
    private String info;
    private String date;
    private String ownerEmail;
    private ArrayList<String> attendees;

    private String id;


    public Lesson(Lesson event) {
        this.title = event.title;
        this.info = event.info;
        this.date = event.date;
        this.ownerEmail = event.ownerEmail;
        this.id = event.id;
        attendees = new ArrayList<>();
        for (int i=0; i<event.attendees.size(); i++){
            attendees.add(event.attendees.get(i));
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

    public void setDate(String date) {
        this.date = date;
    }

    public void setAtendees(ArrayList<String> attendees) {
        this.attendees = attendees;
    }

    public String getTitle() {
        return title;
    }

    public String getInfo() {
        return info;
    }

    public String getDate() {
        return date;
    }

    public ArrayList<String> getAtendees() {
        return attendees;
    }

    public Lesson(String title, String info, String date, String ownerEmail) {
        this.title = title;
        this.info = info;
        this.date = date;
        this.attendees = new ArrayList<>();
        this.ownerEmail = ownerEmail;
    }
    public Lesson() {
        this.title = "";
        this.info = "";
        this.date = "";
        this.attendees = new ArrayList<>();
    }

    public void setId(String id){ this.id = id;}
    public String getId(){return id;}
    public void addAtendee(String email){
        attendees.add(email);
    }
}
