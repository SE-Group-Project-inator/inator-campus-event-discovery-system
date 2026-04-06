package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;

public class Event {

    private String id;
    private String title;
    private String description;
    private String venue;
    private String status;
    private String submittedByName;
    private String submittedByEmail;
    private String createdBy;
    private int capacity;
    private int registeredCount;
    private Timestamp date;
    private String time;
    private String category;

    public Event() {}

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVenue() { return venue; }
    public String getStatus() { return status; }
    public String getSubmittedByName() { return submittedByName; }
    public String getSubmittedByEmail() { return submittedByEmail; }
    public String getCreatedBy() { return createdBy; }
    public int getCapacity() { return capacity; }
    public int getRegisteredCount() { return registeredCount; }
    public Timestamp getDate() { return date; }
    public String getTime() { return time; }
    public String getCategory() { return category; }

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String d) { this.description = d; }
    public void setVenue(String venue) { this.venue = venue; }
    public void setStatus(String status) { this.status = status; }
    public void setSubmittedByName(String name) { this.submittedByName = name; }
    public void setSubmittedByEmail(String email) { this.submittedByEmail = email; }
    public void setCreatedBy(String uid) { this.createdBy = uid; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setRegisteredCount(int count) { this.registeredCount = count; }
    public void setDate(Timestamp date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setCategory(String category) { this.category = category; }
}