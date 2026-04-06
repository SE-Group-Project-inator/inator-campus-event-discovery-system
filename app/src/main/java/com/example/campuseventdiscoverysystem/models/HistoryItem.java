package com.example.campuseventdiscoverysystem.models;

public class HistoryItem {
    private String title;
    private String day;
    private String month;
    private String status; // "Attended", "Did Not Attend", "Recap"

    public HistoryItem(String title, String day, String month, String status) {
        this.title = title;
        this.day = day;
        this.month = month;
        this.status = status;
    }

    public String getTitle() { return title; }
    public String getDay() { return day; }
    public String getMonth() { return month; }
    public String getStatus() { return status; }
}