package com.example.conducto2.data.model;

import com.google.firebase.firestore.DocumentId;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

/**
 * Class
 * 
 * Represents a classroom group within the application. 
 * This model serves as the top-level container for students and teachers, 
 * coordinating access to collective lessons. 
 * 
 * It manages class-wide identification, descriptive metadata, and membership 
 * lists. Additionally, it implements a security mechanism via unique 'Join Codes' 
 * to regulate user enrollment.
 */
public class Class {
    
    /** The name of the class (e.g., "Symphony Orchestra"). */
    private String name;
    
    /** A short description of the class goals or schedule. */
    private String description;
    
    /** The display name of the primary teacher. */
    private String teacherName;
    
    /** The unique email of the class creator/owner. */
    private String ownerEmail;
    
    /** A list of email addresses for all students enrolled in the class. */
    private ArrayList<String> members;
    
    /** The unique Firestore document ID of this class. */
    private String id;
    
    /** A 6-character alphanumeric code used for student registration. */
    private String joinCode;
    
    /** Flag indicating if a live lesson is currently active in this class. */
    private boolean isActive;

    /** 
     * Default constructor required for Firestore deserialization. 
     * Initializes activity state to false.
     */
    public Class() {
        this.isActive = false;
    }

    /**
     * Initializes a new class with provided metadata and generates a new Join Code.
     */
    public Class(String name, String description, String teacherName, String ownerEmail) {
        this.name = name;
        this.description = description;
        this.teacherName = teacherName;
        this.ownerEmail = ownerEmail;
        this.members = new ArrayList<>();
        this.joinCode = generateNewJoinCode();
        this.isActive = false;
    }

    /**
     * Copy constructor used to create a deep copy of a Class instance.
     * @param other The Class object to copy.
     */
    public Class(Class other) {
        this.name = other.name;
        this.description = other.description;
        this.teacherName = other.teacherName;
        this.ownerEmail = other.ownerEmail;
        this.members = other.members != null ? new ArrayList<>(other.members) : new ArrayList<>();
        this.id = other.id;
        this.joinCode = other.joinCode;
        this.isActive = other.isActive;
    }

    /**
     * Generates a random 6-character uppercase alphanumeric string for the join code.
     * @return The newly generated join code.
     */
    private String generateNewJoinCode() {
        // TODO
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Legacy support method to ensure older class documents without codes 
     * are correctly migrated.
     */
    public void ensureJoinCode() {
        if (joinCode == null || joinCode.isEmpty()) {
            joinCode = generateNewJoinCode();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<String> members) {
        this.members = members;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJoinCode() {
        ensureJoinCode(); 
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        // Enforce immutability of the join code once it has been assigned.
        if (this.joinCode == null || this.joinCode.isEmpty()) {
            this.joinCode = joinCode;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}