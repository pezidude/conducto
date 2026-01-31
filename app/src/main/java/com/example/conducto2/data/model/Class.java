package com.example.conducto2.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.UUID;

public class Class implements Parcelable {
    private String name;
    private String description;
    private String teacherName;
    private String ownerEmail;
    private ArrayList<String> members;
    private String id;
    private String joinCode;

    public Class() {
        // Default constructor required for calls to DataSnapshot.getValue(Class.class)
    }

    public Class(String name, String description, String teacherName, String ownerEmail) {
        this.name = name;
        this.description = description;
        this.teacherName = teacherName;
        this.ownerEmail = ownerEmail;
        this.members = new ArrayList<>();
        this.joinCode = generateNewJoinCode();
    }

    protected Class(Parcel in) {
        name = in.readString();
        description = in.readString();
        teacherName = in.readString();
        ownerEmail = in.readString();
        members = in.createStringArrayList();
        id = in.readString();
        joinCode = in.readString();
    }

    public static final Creator<Class> CREATOR = new Creator<Class>() {
        @Override
        public Class createFromParcel(Parcel in) {
            return new Class(in);
        }

        @Override
        public Class[] newArray(int size) {
            return new Class[size];
        }
    };

    private String generateNewJoinCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Ensures that a join code exists for this class.
     * This is a temporary method for migrating older classes that don't have a join code.
     * TODO: This method can be removed in a future version after all classes have been migrated.
     * TODO: Delete also in getJoinCode
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
        ensureJoinCode(); // delete me
        return joinCode;
    }

    public void setJoinCode(String joinCode) {
        // Allow setting the join code only if it's not already set.
        if (this.joinCode == null || this.joinCode.isEmpty()) {
            this.joinCode = joinCode;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(teacherName);
        dest.writeString(ownerEmail);
        dest.writeStringList(members);
        dest.writeString(id);
        dest.writeString(joinCode);
    }
}