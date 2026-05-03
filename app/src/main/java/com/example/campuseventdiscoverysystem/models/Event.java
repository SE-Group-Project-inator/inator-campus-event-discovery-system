package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * @IgnoreExtraProperties prevents Firestore from throwing a silent exception
 * (returning null) when it encounters a field in the document that does not
 * exist in this class.  Without this annotation, ANY unknown Firestore field
 * causes toObject(Event.class) to silently return null, dropping the entire
 * event from every list — which is why only ~3 "known-good" events showed up.
 */
@IgnoreExtraProperties
public class Event {

    private String id;
    private String title;
    private String description;
    private String venue;
    private String status;
    private String submittedByName;
    private String submittedByEmail;
    private String createdBy;
    private long capacity;           // long handles both int64 and smaller int types from Firestore
    private long registeredCount;
    private Timestamp date;
    private String startTime;
    private String endTime;
    private String category;
    private String society;
    private double price;            // stored as double/float64 in Firestore
    private String priceDisplay;     // pre-formatted "Rs. 30" or "FREE", stored in some docs
    private String imageUrl;
    private Timestamp submittedAt;
    private String societyId;
    private Timestamp updatedAt;     // present in some docs; kept here to avoid deserialization issues

    public Event() {}

    public String getId()                { return id; }
    public String getTitle()             { return title; }
    public String getDescription()       { return description; }
    public String getVenue()             { return venue; }
    public String getStatus()            { return status; }
    public String getSubmittedByName()   { return submittedByName; }
    public String getSubmittedByEmail()  { return submittedByEmail; }
    public String getCreatedBy()         { return createdBy; }
    public int    getCapacity()          { return (int) capacity; }
    public int    getRegisteredCount()   { return (int) registeredCount; }
    public Timestamp getDate()           { return date; }
    public String getStartTime()         { return startTime; }
    public String getEndTime()           { return endTime; }
    public String getCategory()          { return category; }
    public String getSociety()           { return society; }
    public double getPrice()             { return price; }
    public String getImageUrl()          { return imageUrl; }
    public Timestamp getSubmittedAt()    { return submittedAt; }
    public Timestamp getUpdatedAt()      { return updatedAt; }

    public void setId(String id)                     { this.id = id; }
    public void setTitle(String title)               { this.title = title; }
    public void setDescription(String d)             { this.description = d; }
    public void setVenue(String venue)               { this.venue = venue; }
    public void setStatus(String status)             { this.status = status; }
    public void setSubmittedByName(String name)      { this.submittedByName = name; }
    public void setSubmittedByEmail(String email)    { this.submittedByEmail = email; }
    public void setCreatedBy(String uid)             { this.createdBy = uid; }
    public void setCapacity(int capacity)            { this.capacity = capacity; }
    public void setRegisteredCount(int count)        { this.registeredCount = count; }
    public void setDate(Timestamp date)              { this.date = date; }
    public void setStartTime(String time)            { this.startTime = time; }
    public void setEndTime(String time)              { this.endTime = time; }
    public void setCategory(String category)         { this.category = category; }
    public void setSociety(String society)           { this.society = society; }
    public void setPrice(double price)               { this.price = price; }
    public void setImageUrl(String imageUrl)         { this.imageUrl = imageUrl; }
    public void setSubmittedAt(Timestamp t)          { this.submittedAt = t; }
    public void setUpdatedAt(Timestamp t)            { this.updatedAt = t; }
    public void setPriceDisplay(String s)            { this.priceDisplay = s; }

    public String getSocietyId() { return societyId; }
    public void setSocietyId(String societyId) { this.societyId = societyId; }

    /**
     * Returns "Rs. X" or "FREE" for display.
     * Falls back to the priceDisplay field stored in Firestore if price is 0.
     */
    public String getPriceDisplay() {
        if (price > 0) return "Rs. " + (int) price;
        if (priceDisplay != null && !priceDisplay.isEmpty()) return priceDisplay;
        return "FREE";
    }
}