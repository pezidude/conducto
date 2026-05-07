package com.example.conducto2.ui.lessons;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Role {
    private String name;
    private Map<String, Set<String>> selectedVoicesPerPart = new HashMap<>();

    public Role(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Set<String>> getSelectedVoicesPerPart() {
        return selectedVoicesPerPart;
    }

    public void setSelectedVoicesPerPart(Map<String, Set<String>> selectedVoicesPerPart) {
        this.selectedVoicesPerPart = selectedVoicesPerPart;
    }
}