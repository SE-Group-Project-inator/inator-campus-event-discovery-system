package com.example.campuseventdiscoverysystem.models;

public class HistoryItem {

    private String title;
    private String day;
    private String month;
    private String status;

    // 🔥 EXTRA FIELDS (add these)
    private String eventId;
    private String venue;
    private String description;
    private int capacity;
    private int registered;
    private long dateMillis;

    public HistoryItem(String title, String day, String month, String status) {
        this.title = title;
        this.day = day;
        this.month = month;
        this.status = status;
    }

    // --- Getters ---
    public String getTitle() { return title; }
    public String getDay() { return day; }
    public String getMonth() { return month; }
    public String getStatus() { return status; }

    public String getEventId() { return eventId; }
    public String getVenue() { return venue; }
    public String getDescription() { return description; }
    public int getCapacity() { return capacity; }
    public int getRegistered() { return registered; }
    public long getDateMillis() { return dateMillis; }

    // --- Setters ---
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setVenue(String venue) { this.venue = venue; }
    public void setDescription(String description) { this.description = description; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setRegistered(int registered) { this.registered = registered; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
}