package com.example.conducto2.ui.lessons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Role {
    private String id;
    private String name;
    private Map<String, List<String>> selectedVoicesPerPart = new HashMap<>();

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

    public Map<String, List<String>> getSelectedVoicesPerPart() {
        return selectedVoicesPerPart;
    }

    public void setSelectedVoicesPerPart(Map<String, List<String>> selectedVoicesPerPart) {
        this.selectedVoicesPerPart = selectedVoicesPerPart;
    }
}