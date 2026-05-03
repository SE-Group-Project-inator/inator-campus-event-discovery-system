package com.example.campuseventdiscoverysystem.recommendations;

import java.util.ArrayList;
import java.util.List;

/**
 * Cold-start interest signal collected during student onboarding.
 *
 * Stored at users/{uid}.preferences.
 * categories holds the canonical Event.category values the student picked.
 */
public class UserPreferences {

    private List<String> categories;

    public UserPreferences() {
        this.categories = new ArrayList<>();
    }

    public UserPreferences(List<String> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public boolean isEmpty() {
        return categories == null || categories.isEmpty();
    }
}
