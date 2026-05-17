package com.example.conducto2.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Role
 * 
 * A data model representing a logical grouping of instrumental parts within a MusicXML score.
 * This class serves as a Plain Old Java Object (POJO) that maps directly to documents 
 * within the 'draftRoles' subcollection in Firestore. 
 * 
 * It allows teachers to define a name for a specific instrument group (e.g., "Strings") 
 * and associate it with multiple instrumental part IDs extracted from the master score.
 */
public class Role {

    /** The unique Firestore document ID associated with this role. */
    private String id;

    /** The display name of the role (e.g., "Violin I", "Rhythm Section"). */
    private String name;

    /** A list of XML 'id' strings (e.g., "P1", "P2") that are mapped to this role. */
    private List<String> selectedPartIds = new ArrayList<>();

    /** Required no-argument constructor for Firestore deserialization. */
    public Role() {} 

    /**
     * Initializes a new role with a specific name.
     * @param name The name to assign to this role.
     */
    public Role(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSelectedPartIds() {
        return selectedPartIds;
    }

    public void setSelectedPartIds(List<String> selectedPartIds) {
        this.selectedPartIds = selectedPartIds;
    }
}