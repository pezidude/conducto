package com.example.conducto2.data.model;

import com.google.firebase.firestore.DocumentId;

/**
 * User
 * 
 * Represents a registered user within the Conducto application. 
 * This model stores profile information, identity classification (teacher vs student), 
 * and personalization data like profile pictures and descriptions.
 * 
 * It is used for authentication state persistence and for populating participant 
 * lists in classes and lessons.
 */
public class User {

    /** 
     * The unique identifier assigned by Firestore. 
     * Annotated with @DocumentId to automatically map the document's name to this field.
     */
    @DocumentId
    private String userId;

    /** The user's first name. */
    private String fname;

    /** The user's last name. */
    private String lname;

    /** The user's registered email address (used as the primary key in manual lookups). */
    private String email;

    /** The role of the user, typically "teacher" or "student". */
    private String userType;

    /** A Base64 encoded string representation of the user's profile picture. */
    private String profilePictureBase64;

    /** A short bio or description provided by the user. */
    private String description;

    /**
     * Initializes a new user with basic name and email. Default role is "teacher".
     */
    public User(String email, String fname, String lname) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.userType = "teacher"; 
    }

    /**
     * Initializes a new user with specific role.
     */
    public User(String email, String fname, String lname, String userType) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.userType = userType;
    }

    /**
     * Full initialization constructor including profile picture.
     */
    public User(String email, String fname, String lname, String userType, String profilePictureBase64) {
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.userType = userType;
        this.profilePictureBase64 = profilePictureBase64;
    }

    /** Required no-argument constructor for Firestore deserialization. */
    public User() {} 

    @Override
    public String toString() {
        return "User{" +
                "fname='" + fname + '\'' +
                ", lname='" + lname + '\'' +
                ", email='" + email + '\'' +
                ", userType='" + userType + '\'' +
                ", profilePictureBase64='" + (profilePictureBase64 != null ? "exists" : "null") + '\'' +
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
    public void setProfilePictureBase64(String profilePictureBase64) {
        this.profilePictureBase64 = profilePictureBase64;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public String getProfilePictureBase64() {
        return profilePictureBase64;
    }

    public String getDescription() {
        return description;
    }
}