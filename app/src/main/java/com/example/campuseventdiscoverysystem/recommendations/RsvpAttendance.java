package com.example.campuseventdiscoverysystem.recommendations;

/**
 * One attended event used as input to the recommendation algorithm.
 * Plain data carrier so the ranking logic can be unit-tested without Firestore.
 */
public class RsvpAttendance {

    public final String eventId;
    public final String society;
    public final long dateMillis;

    public RsvpAttendance(String eventId, String society, long dateMillis) {
        this.eventId = eventId;
        this.society = society;
        this.dateMillis = dateMillis;
    }
}
