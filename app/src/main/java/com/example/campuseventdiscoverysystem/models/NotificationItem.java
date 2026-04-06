package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String title;
    private String message;
    private boolean unread;
    private Timestamp timestamp;

    // 1. Empty constructor (Absolutely required for Firebase)
    public NotificationItem() {}

    // 2. Full constructor
    public NotificationItem(String title, String message, boolean unread, Timestamp timestamp) {
        this.title = title;
        this.message = message;
        this.unread = unread;
        this.timestamp = timestamp;
    }

    // 3. Getters
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isUnread() { return unread; }
    public Timestamp getTimestamp() { return timestamp; }
}