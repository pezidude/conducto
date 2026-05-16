package com.example.conducto2.data.model;

import java.util.ArrayList;
import java.util.List;

public class Role {
    private String id;
    private String name;
    private List<String> selectedPartIds = new ArrayList<>();

    public Role() {} // Required for Firestore

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