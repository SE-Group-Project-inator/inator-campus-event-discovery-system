package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String type;
    private String eventId;
    private String eventName;
    private boolean unread;
    private boolean read;
    private Timestamp timestamp;

    public NotificationItem() {}

    public NotificationItem(String title, String message, boolean unread, Timestamp timestamp) {
        this.title = title;
        this.message = message;
        this.read = !unread;
        this.timestamp = timestamp;
    }

    public String getId()        { return id; }
    public String getTitle()     { return title; }
    public String getMessage()   { return message; }
    public String getType()      { return type; }
    public String getEventId()   { return eventId; }
    public String getEventName() { return eventName; }
    public boolean isUnread()    { return !read; }
    public boolean isRead()      { return read; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setId(String id)             { this.id = id; }
    public void setTitle(String title)       { this.title = title; }
    public void setMessage(String message)   { this.message = message; }
    public void setType(String type)         { this.type = type; }
    public void setEventId(String eventId)   { this.eventId = eventId; }
    public void setEventName(String n)       { this.eventName = n; }
    public void setUnread(boolean unread)    { this.read = !unread; }
    public void setRead(boolean read)        { this.read = read; }
    public void setTimestamp(Timestamp t)    { this.timestamp = t; }
}
