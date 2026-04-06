package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;

public class Registration {

    private String id;
    private String eventId;
    private String userId;
    private String userName;
    private String userEmail;
    private int seatNumber;
    private boolean confirmed;
    private Timestamp registeredAt;

    public Registration() {}

    public String getId() { return id; }
    public String getEventId() { return eventId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public int getSeatNumber() { return seatNumber; }
    public boolean isConfirmed() { return confirmed; }
    public Timestamp getRegisteredAt() { return registeredAt; }

    public void setId(String id) { this.id = id; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setSeatNumber(int seatNumber) { this.seatNumber = seatNumber; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public void setRegisteredAt(Timestamp registeredAt) { this.registeredAt = registeredAt; }
}