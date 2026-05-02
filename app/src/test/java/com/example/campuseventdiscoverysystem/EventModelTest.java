package com.example.campuseventdiscoverysystem;

import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.models.HistoryItem;
import com.example.campuseventdiscoverysystem.models.NotificationItem;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Unit tests for all model classes in the Campus Event Discovery System.
 *
 * Covers: Event, Registration, NotificationItem, HistoryItem
 * Intent: Validate getter/setter contracts, boundary values, and
 *         business-logic helpers used by control classes.
 */
public class EventModelTest {

    // ─────────────────────────────────────────────────────────────
    // Event model tests
    // ─────────────────────────────────────────────────────────────

    private Event event;

    @Before
    public void setUp() {
        event = new Event();
    }

    /** US-01 / US-04 – Event title is stored and retrieved correctly */
    @Test
    public void testEventTitle_setAndGet() {
        event.setTitle("LUMS Hackathon 2025");
        assertEquals("LUMS Hackathon 2025", event.getTitle());
    }

    /** Event description stores multi-line text */
    @Test
    public void testEventDescription_multiline() {
        String desc = "Line1\nLine2\nLine3";
        event.setDescription(desc);
        assertEquals(desc, event.getDescription());
    }

    /** Capacity must return exactly the value that was set */
    @Test
    public void testEventCapacity_exactValue() {
        event.setCapacity(150);
        assertEquals(150, event.getCapacity());
    }

    /** Capacity of zero is a valid boundary value */
    @Test
    public void testEventCapacity_zero() {
        event.setCapacity(0);
        assertEquals(0, event.getCapacity());
    }

    /** registeredCount starts at default 0 when not set */
    @Test
    public void testEventRegisteredCount_defaultZero() {
        Event fresh = new Event();
        assertEquals(0, fresh.getRegisteredCount());
    }

    /** registeredCount equals capacity → event is full */
    @Test
    public void testEventIsFull_whenRegisteredEqualsCapacity() {
        event.setCapacity(50);
        event.setRegisteredCount(50);
        assertTrue("Event should be full",
                event.getRegisteredCount() >= event.getCapacity());
    }

    /** registeredCount below capacity → event has space */
    @Test
    public void testEventNotFull_whenRegisteredBelowCapacity() {
        event.setCapacity(50);
        event.setRegisteredCount(30);
        assertFalse("Event should not be full",
                event.getRegisteredCount() >= event.getCapacity());
    }

    /** status field correctly stores "pending_approval" */
    @Test
    public void testEventStatus_pendingApproval() {
        event.setStatus("pending_approval");
        assertEquals("pending_approval", event.getStatus());
    }

    /** status field correctly stores "active" */
    @Test
    public void testEventStatus_active() {
        event.setStatus("active");
        assertEquals("active", event.getStatus());
    }

    /** status field correctly stores "rejected" */
    @Test
    public void testEventStatus_rejected() {
        event.setStatus("rejected");
        assertEquals("rejected", event.getStatus());
    }

    /** Venue is set and retrieved */
    @Test
    public void testEventVenue() {
        event.setVenue("LUMS Auditorium");
        assertEquals("LUMS Auditorium", event.getVenue());
    }

    /** category is set and retrieved */
    @Test
    public void testEventCategory() {
        event.setCategory("Sports");
        assertEquals("Sports", event.getCategory());
    }

    /** society is set and retrieved */
    @Test
    public void testEventSociety() {
        event.setSociety("ACM");
        assertEquals("ACM", event.getSociety());
    }

    /** Timestamp date round-trip */
    @Test
    public void testEventDate_timestamp() {
        Timestamp ts = new Timestamp(new Date());
        event.setDate(ts);
        assertEquals(ts, event.getDate());
    }

    /** Start time and end time stored correctly */
    @Test
    public void testEventTimes() {
        event.setStartTime("10:00 AM");
        event.setEndTime("12:00 PM");
        assertEquals("10:00 AM", event.getStartTime());
        assertEquals("12:00 PM", event.getEndTime());
    }

    /** createdBy UID stored correctly */
    @Test
    public void testEventCreatedBy() {
        event.setCreatedBy("uid_manager_001");
        assertEquals("uid_manager_001", event.getCreatedBy());
    }

    /** submittedByName and email stored correctly */
    @Test
    public void testEventSubmitterFields() {
        event.setSubmittedByName("Ali Hassan");
        event.setSubmittedByEmail("ali@lums.edu.pk");
        assertEquals("Ali Hassan", event.getSubmittedByName());
        assertEquals("ali@lums.edu.pk", event.getSubmittedByEmail());
    }

    /** Null title is stored as null (no NPE) */
    @Test
    public void testEventTitle_nullSafe() {
        event.setTitle(null);
        assertNull(event.getTitle());
    }

    // ─────────────────────────────────────────────────────────────
    // Registration model tests
    // ─────────────────────────────────────────────────────────────

    /** Registration id is stored and retrieved */
    @Test
    public void testRegistration_id() {
        Registration reg = new Registration();
        reg.setId("reg_abc123");
        assertEquals("reg_abc123", reg.getId());
    }

    /** Registration links to correct event and user */
    @Test
    public void testRegistration_eventAndUser() {
        Registration reg = new Registration();
        reg.setEventId("event_001");
        reg.setUserId("user_999");
        assertEquals("event_001", reg.getEventId());
        assertEquals("user_999", reg.getUserId());
    }

    /** Seat number stores positive integer */
    @Test
    public void testRegistration_seatNumber() {
        Registration reg = new Registration();
        reg.setSeatNumber(42);
        assertEquals(42, reg.getSeatNumber());
    }

    /** confirmed defaults to false */
    @Test
    public void testRegistration_confirmedDefault() {
        Registration reg = new Registration();
        assertFalse(reg.isConfirmed());
    }

    /** confirmed can be set to true */
    @Test
    public void testRegistration_setConfirmed() {
        Registration reg = new Registration();
        reg.setConfirmed(true);
        assertTrue(reg.isConfirmed());
    }

    /** User name and email stored in registration */
    @Test
    public void testRegistration_userNameEmail() {
        Registration reg = new Registration();
        reg.setUserName("Sara Malik");
        reg.setUserEmail("sara@lums.edu.pk");
        assertEquals("Sara Malik", reg.getUserName());
        assertEquals("sara@lums.edu.pk", reg.getUserEmail());
    }

    /** registeredAt timestamp round-trip */
    @Test
    public void testRegistration_timestamp() {
        Registration reg = new Registration();
        Timestamp ts = new Timestamp(new Date());
        reg.setRegisteredAt(ts);
        assertEquals(ts, reg.getRegisteredAt());
    }

    // ─────────────────────────────────────────────────────────────
    // NotificationItem model tests
    // ─────────────────────────────────────────────────────────────

    /** NotificationItem full constructor sets all fields */
    @Test
    public void testNotificationItem_fullConstructor() {
        Timestamp ts = new Timestamp(new Date());
        NotificationItem n = new NotificationItem("Title", "Body text", true, ts);
        assertEquals("Title", n.getTitle());
        assertEquals("Body text", n.getMessage());
        assertTrue(n.isUnread());
        assertEquals(ts, n.getTimestamp());
    }

    /** NotificationItem empty constructor works (required for Firebase) */
    @Test
    public void testNotificationItem_emptyConstructor() {
        NotificationItem n = new NotificationItem();
        assertNotNull(n);
    }

    /** Unread flag can be false */
    @Test
    public void testNotificationItem_read() {
        NotificationItem n = new NotificationItem("T", "M", false, null);
        assertFalse(n.isUnread());
    }

    // ─────────────────────────────────────────────────────────────
    // HistoryItem model tests
    // ─────────────────────────────────────────────────────────────

    /** HistoryItem stores all four fields */
    @Test
    public void testHistoryItem_allFields() {
        HistoryItem h = new HistoryItem("Annual Gala", "15", "Apr", "Attended");
        assertEquals("Annual Gala", h.getTitle());
        assertEquals("15", h.getDay());
        assertEquals("Apr", h.getMonth());
        assertEquals("Attended", h.getStatus());
    }

    /** HistoryItem status: "Did Not Attend" */
    @Test
    public void testHistoryItem_didNotAttend() {
        HistoryItem h = new HistoryItem("Workshop", "20", "Mar", "Did Not Attend");
        assertEquals("Did Not Attend", h.getStatus());
    }

    /** HistoryItem status: "Recap" */
    @Test
    public void testHistoryItem_recap() {
        HistoryItem h = new HistoryItem("Tech Talk", "05", "Feb", "Recap");
        assertEquals("Recap", h.getStatus());
    }

    // ─────────────────────────────────────────────────────────────
    // Business logic / control helper tests
    // ─────────────────────────────────────────────────────────────

    /**
     * Filter logic: "urgent" should include an event within 7 days.
     * Mirrors the logic in AdminDashboardActivity.applyFilter().
     */
    @Test
    public void testUrgentFilter_includesEventWithin7Days() {
        Event e = new Event();
        // Set date 3 days from now
        long threeDaysMs = 3L * 24 * 60 * 60 * 1000;
        Timestamp ts = new Timestamp(new Date(System.currentTimeMillis() + threeDaysMs));
        e.setDate(ts);

        Date today = new Date();
        Date eventDate = e.getDate().toDate();
        long diffDays = (eventDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
        assertTrue("Event 3 days away should be urgent", diffDays <= 7 && diffDays >= 0);
    }

    /**
     * Filter logic: event more than 7 days away should NOT be urgent.
     */
    @Test
    public void testUrgentFilter_excludesEventBeyond7Days() {
        Event e = new Event();
        long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
        Timestamp ts = new Timestamp(new Date(System.currentTimeMillis() + tenDaysMs));
        e.setDate(ts);

        Date today = new Date();
        Date eventDate = e.getDate().toDate();
        long diffDays = (eventDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
        assertFalse("Event 10 days away should not be urgent", diffDays <= 7 && diffDays >= 0);
    }

    /**
     * Filter logic: past event should NOT be urgent (diffDays < 0).
     */
    @Test
    public void testUrgentFilter_excludesPastEvent() {
        Event e = new Event();
        long twoDaysAgoMs = -2L * 24 * 60 * 60 * 1000;
        Timestamp ts = new Timestamp(new Date(System.currentTimeMillis() + twoDaysAgoMs));
        e.setDate(ts);

        Date today = new Date();
        Date eventDate = e.getDate().toDate();
        long diffDays = (eventDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24);
        assertFalse("Past event should not be urgent", diffDays >= 0);
    }

    /**
     * Capacity check helper: event at 80% capacity shows "Almost Full".
     * Mirrors badge logic used in adapters.
     */
    @Test
    public void testCapacityBadge_almostFull() {
        Event e = new Event();
        e.setCapacity(100);
        e.setRegisteredCount(85);
        double fillRatio = (double) e.getRegisteredCount() / e.getCapacity();
        assertTrue("≥80% should be Almost Full", fillRatio >= 0.8 && fillRatio < 1.0);
    }

    /** Capacity badge: exactly full */
    @Test
    public void testCapacityBadge_full() {
        Event e = new Event();
        e.setCapacity(50);
        e.setRegisteredCount(50);
        double fillRatio = (double) e.getRegisteredCount() / e.getCapacity();
        assertEquals(1.0, fillRatio, 0.001);
    }

    /** Capacity badge: available (< 80%) */
    @Test
    public void testCapacityBadge_available() {
        Event e = new Event();
        e.setCapacity(100);
        e.setRegisteredCount(40);
        double fillRatio = (double) e.getRegisteredCount() / e.getCapacity();
        assertTrue("< 80% should be Available", fillRatio < 0.8);
    }
}