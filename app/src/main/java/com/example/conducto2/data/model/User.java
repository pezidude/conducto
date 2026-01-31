package com.example.conducto2.data.model;

import com.google.firebase.firestore.DocumentId;

public class User {
    // This annotation tells Firestore: "Populate this field with the Document ID"
    @DocumentId
    private String userId;

    private String fname;
    private String lname;
    private String email;
    private String userType;

    public User(String email, String fname, String lname) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.userType = "teacher"; // Default to teacher for existing users
    }

    public User(String email, String fname, String lname, String userType) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.userType = userType;
    }

    public User() {} // needed for firestore

    @Override
    public String toString() {
        return "User{" +
                "fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", email='" + email + '\'' +
                ", userType='" + userType + '\'' +
                '}';
    }

    public void setFname(String name) {
        this.fname = name;
    }
    public void setLname(String name) {
        this.lname = name;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getFname() {
        return fname;
    }
    public String getLname() {
        return lname;
    }
    public String getEmail() {
        return email;
    }
    public String getUserType() {
        return userType;
    }
    public String getUserId() {
        return userId;
    }
}
