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
    private String startTime;
    private String endTime;
    private String category;
    private String society;
    private double price;       // ticket price in PKR; 0 = free
    private String imageUrl;    // optional banner image URL

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
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getCategory() { return category; }

    public String getSociety() {return society;}

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
    public void setStartTime(String time) { this.startTime = time; }
    public void setEndTime(String time) { this.endTime = time; }
    public void setCategory(String category) { this.category = category; }

    public void setSociety(String society) { this.society = society; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    /** Returns "Rs. X" or "FREE" for display */
    public String getPriceDisplay() {
        if (price <= 0) return "FREE";
        return "Rs. " + (int) price;
    }
}