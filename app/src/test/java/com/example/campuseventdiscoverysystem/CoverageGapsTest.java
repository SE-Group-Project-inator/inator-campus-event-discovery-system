package com.example.campuseventdiscoverysystem;

import com.example.campuseventdiscoverysystem.activities.SessionManager;
import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.models.HistoryItem;
import com.example.campuseventdiscoverysystem.models.NotificationItem;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Fills in plain-Java coverage gaps that the other test files miss:
 *   - Event.getPriceDisplay (FREE / Rs. X)
 *   - Event price/imageUrl getters
 *   - NotificationItem read/unread toggle coupling and extra setters
 *   - HistoryItem extra fields (eventId, venue, capacity, registered, dateMillis, description)
 *   - SessionManager timeout constants
 */
public class CoverageGapsTest {

    // ── Event.getPriceDisplay ────────────────────────────────────

    @Test
    public void event_priceDisplay_freeWhenZero() {
        Event e = new Event();
        e.setPrice(0);
        assertEquals("FREE", e.getPriceDisplay());
    }

    @Test
    public void event_priceDisplay_freeWhenNegative() {
        Event e = new Event();
        e.setPrice(-50);
        assertEquals("FREE", e.getPriceDisplay());
    }

    @Test
    public void event_priceDisplay_paid() {
        Event e = new Event();
        e.setPrice(500);
        assertEquals("Rs. 500", e.getPriceDisplay());
    }

    @Test
    public void event_priceDisplay_truncatesDecimal() {
        Event e = new Event();
        e.setPrice(199.99);
        assertEquals("Rs. 199", e.getPriceDisplay());
    }

    @Test
    public void event_priceRoundTrip() {
        Event e = new Event();
        e.setPrice(1234.5);
        assertEquals(1234.5, e.getPrice(), 0.001);
    }

    @Test
    public void event_imageUrl_roundTrip() {
        Event e = new Event();
        e.setImageUrl("https://cdn/banner.jpg");
        assertEquals("https://cdn/banner.jpg", e.getImageUrl());
    }

    @Test
    public void event_id_roundTrip() {
        Event e = new Event();
        e.setId("evt_42");
        assertEquals("evt_42", e.getId());
    }

    // ── NotificationItem read/unread coupling ───────────────────

    @Test
    public void notification_setUnread_true_marksUnread() {
        NotificationItem n = new NotificationItem();
        n.setUnread(true);
        assertTrue(n.isUnread());
        assertFalse(n.isRead());
    }

    @Test
    public void notification_setUnread_false_marksRead() {
        NotificationItem n = new NotificationItem();
        n.setUnread(false);
        assertFalse(n.isUnread());
        assertTrue(n.isRead());
    }

    @Test
    public void notification_setRead_true_clearsUnread() {
        NotificationItem n = new NotificationItem();
        n.setRead(true);
        assertTrue(n.isRead());
        assertFalse(n.isUnread());
    }

    @Test
    public void notification_extraFieldsRoundTrip() {
        NotificationItem n = new NotificationItem();
        n.setId("n1");
        n.setType("event_reminder");
        n.setEventId("e9");
        n.setEventName("Grand Finale");
        assertEquals("n1", n.getId());
        assertEquals("event_reminder", n.getType());
        assertEquals("e9", n.getEventId());
        assertEquals("Grand Finale", n.getEventName());
    }

    @Test
    public void notification_fourArgCtor_setsTimestampAndReadState() {
        Timestamp ts = new Timestamp(new Date());
        NotificationItem n = new NotificationItem("T", "M", true, ts);
        assertEquals(ts, n.getTimestamp());
        assertTrue(n.isUnread());
    }

    // ── HistoryItem extra fields ────────────────────────────────

    @Test
    public void historyItem_extraFieldsRoundTrip() {
        HistoryItem h = new HistoryItem("Tech Talk", "05", "Feb", "Attended");
        h.setEventId("evt_1");
        h.setVenue("Auditorium A");
        h.setDescription("AI panel");
        h.setCapacity(200);
        h.setRegistered(180);
        h.setDateMillis(1700000000000L);

        assertEquals("evt_1", h.getEventId());
        assertEquals("Auditorium A", h.getVenue());
        assertEquals("AI panel", h.getDescription());
        assertEquals(200, h.getCapacity());
        assertEquals(180, h.getRegistered());
        assertEquals(1700000000000L, h.getDateMillis());
    }

    @Test
    public void historyItem_extrasDefaultZero() {
        HistoryItem h = new HistoryItem("X", "1", "Jan", "Recap");
        assertEquals(0, h.getCapacity());
        assertEquals(0, h.getRegistered());
        assertEquals(0L, h.getDateMillis());
    }

    // ── SessionManager timeout constants ────────────────────────

    @Test
    public void session_warningTimeout_is15Min() {
        assertEquals(15L * 60 * 1000, SessionManager.WARNING_TIMEOUT_MS);
    }

    @Test
    public void session_logoutTimeout_is20Min() {
        assertEquals(20L * 60 * 1000, SessionManager.LOGOUT_TIMEOUT_MS);
    }

    @Test
    public void session_logoutComesAfterWarning() {
        assertTrue(SessionManager.LOGOUT_TIMEOUT_MS > SessionManager.WARNING_TIMEOUT_MS);
    }
}
