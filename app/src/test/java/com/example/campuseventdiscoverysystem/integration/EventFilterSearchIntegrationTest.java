package com.example.campuseventdiscoverysystem.integration;

import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.models.NotificationItem;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration tests: Event filtering, search, and notification pipeline.
 *
 * Mirrors the real filtering and search logic found in:
 *   - AdminDashboardActivity.applyFilter()
 *   - EventsListActivity / SearchActivity query logic
 *   - NotificationsActivity read/unread state
 *
 * Tests use pure Java data manipulation — no Android or Firebase mocking needed.
 */
public class EventFilterSearchIntegrationTest {

    private List<Event> allEvents;
    private static final long DAY_MS = 24 * 60 * 60 * 1000L;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event make(String id, String title, String status, String category,
                       String society, double price, long dateOffsetMs, int registered, int capacity) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setStatus(status);
        e.setCategory(category);
        e.setSociety(society);
        e.setPrice(price);
        e.setDate(new Timestamp(new Date(System.currentTimeMillis() + dateOffsetMs)));
        e.setRegisteredCount(registered);
        e.setCapacity(capacity);
        return e;
    }

    @Before
    public void setUp() {
        allEvents = new ArrayList<>();
        allEvents.add(make("e1", "AI Workshop",       "active",           "Technology", "ACM",   0,        2 * DAY_MS,   10, 50));
        allEvents.add(make("e2", "Music Night",        "active",           "Arts",       "Music", 100.0,    5 * DAY_MS,   45, 50));
        allEvents.add(make("e3", "Football Match",     "active",           "Sports",     "Sports",0,        1 * DAY_MS,   40, 40)); // full
        allEvents.add(make("e4", "Drama Showcase",     "pending_approval", "Arts",       "Drama", 50.0,     3 * DAY_MS,   5,  60));
        allEvents.add(make("e5", "Cybersecurity Talk", "active",           "Technology", "IEEE",  200.0,    6 * DAY_MS,   20, 80));
        allEvents.add(make("e6", "Old Workshop",       "active",           "Technology", "ACM",   0,       -1 * DAY_MS,   30, 50)); // past
        allEvents.add(make("e7", "Hackathon 2025",     "active",           "Technology", "ACM",   300.0,    7 * DAY_MS,   38, 40)); // almost full
        allEvents.add(make("e8", "Rejected Event",     "rejected",         "Arts",       "Drama", 0,        4 * DAY_MS,    0, 30));
    }

    // ── 1. Filter: active events only ─────────────────────────────────────

    @Test
    public void filter_activeEventsOnly() {
        List<Event> active = allEvents.stream()
                .filter(e -> "active".equals(e.getStatus()))
                .collect(Collectors.toList());
        assertEquals(6, active.size());
        assertTrue(active.stream().allMatch(e -> "active".equals(e.getStatus())));
    }

    // ── 2. Filter: pending approval ───────────────────────────────────────

    @Test
    public void filter_pendingApprovalEvents() {
        List<Event> pending = allEvents.stream()
                .filter(e -> "pending_approval".equals(e.getStatus()))
                .collect(Collectors.toList());
        assertEquals(1, pending.size());
        assertEquals("Drama Showcase", pending.get(0).getTitle());
    }

    // ── 3. Filter: upcoming (future) active events ────────────────────────

    @Test
    public void filter_upcomingActiveEvents() {
        long now = System.currentTimeMillis();
        List<Event> upcoming = allEvents.stream()
                .filter(e -> "active".equals(e.getStatus()))
                .filter(e -> e.getDate() != null && e.getDate().toDate().getTime() > now)
                .collect(Collectors.toList());
        // e6 is past, e3/e1/e2/e5/e7 are future → 5 upcoming active
        assertEquals(5, upcoming.size());
        assertTrue(upcoming.stream().noneMatch(e -> "Old Workshop".equals(e.getTitle())));
    }

    // ── 4. Filter: urgent (within 7 days) ─────────────────────────────────

    @Test
    public void filter_urgentEvents_within7Days() {
        long now = System.currentTimeMillis();
        List<Event> urgent = allEvents.stream()
                .filter(e -> "active".equals(e.getStatus()))
                .filter(e -> {
                    if (e.getDate() == null) return false;
                    long diff = e.getDate().toDate().getTime() - now;
                    long diffDays = diff / DAY_MS;
                    return diffDays >= 0 && diffDays <= 7;
                })
                .collect(Collectors.toList());
        // All 5 upcoming active events are within 7 days (max offset is 7 days)
        assertEquals(5, urgent.size());
    }

    // ── 5. Filter: by category ────────────────────────────────────────────

    @Test
    public void filter_byCategory_technology() {
        List<Event> tech = allEvents.stream()
                .filter(e -> "Technology".equals(e.getCategory()))
                .collect(Collectors.toList());
        assertEquals(4, tech.size()); // e1, e5, e6, e7
    }

    @Test
    public void filter_byCategory_arts() {
        List<Event> arts = allEvents.stream()
                .filter(e -> "Arts".equals(e.getCategory()))
                .collect(Collectors.toList());
        assertEquals(3, arts.size()); // e2, e4, e8
    }

    // ── 6. Filter: by society ─────────────────────────────────────────────

    @Test
    public void filter_bySociety_ACM() {
        List<Event> acm = allEvents.stream()
                .filter(e -> "ACM".equals(e.getSociety()))
                .collect(Collectors.toList());
        assertEquals(3, acm.size()); // e1, e6, e7
    }

    // ── 7. Search: title contains query (case-insensitive) ────────────────

    @Test
    public void search_titleContains_caseInsensitive() {
        String query = "workshop";
        List<Event> results = allEvents.stream()
                .filter(e -> e.getTitle() != null &&
                        e.getTitle().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
        assertEquals(2, results.size()); // AI Workshop, Old Workshop
    }

    @Test
    public void search_titleContains_noMatch_returnsEmpty() {
        List<Event> results = allEvents.stream()
                .filter(e -> e.getTitle() != null &&
                        e.getTitle().toLowerCase().contains("xyz_no_match"))
                .collect(Collectors.toList());
        assertTrue(results.isEmpty());
    }

    @Test
    public void search_partialTitle_hackat() {
        List<Event> results = allEvents.stream()
                .filter(e -> e.getTitle() != null &&
                        e.getTitle().toLowerCase().contains("hackat"))
                .collect(Collectors.toList());
        assertEquals(1, results.size());
        assertEquals("Hackathon 2025", results.get(0).getTitle());
    }

    // ── 8. Sort: by date ascending ────────────────────────────────────────

    @Test
    public void sort_byDateAscending_firstEventIsSoonest() {
        List<Event> active = allEvents.stream()
                .filter(e -> "active".equals(e.getStatus()))
                .filter(e -> e.getDate() != null &&
                        e.getDate().toDate().getTime() > System.currentTimeMillis())
                .collect(Collectors.toList());

        active.sort((a, b) -> Long.compare(
                a.getDate().toDate().getTime(),
                b.getDate().toDate().getTime()));

        // e3 (1 day), e1 (2 days), e4... wait e4 is pending. Among active upcoming:
        // e3=1d, e1=2d, e2=5d, e5=6d, e7=7d
        assertEquals("Football Match", active.get(0).getTitle());
    }

    // ── 9. Sort: by popularity (registeredCount desc) ─────────────────────

    @Test
    public void sort_byPopularityDescending() {
        List<Event> active = allEvents.stream()
                .filter(e -> "active".equals(e.getStatus()))
                .collect(Collectors.toList());

        active.sort((a, b) -> Integer.compare(b.getRegisteredCount(), a.getRegisteredCount()));

        // e3=40, e2=45... e2 has 45 which is highest among active
        assertEquals("Music Night", active.get(0).getTitle());
    }

    // ── 10. Capacity badge logic: full, almost-full, available ────────────

    @Test
    public void capacityBadge_fullEvent() {
        Event e3 = allEvents.stream().filter(e -> "e3".equals(e.getId())).findFirst().get();
        double ratio = (double) e3.getRegisteredCount() / e3.getCapacity();
        assertEquals(1.0, ratio, 0.001); // exactly full
    }

    @Test
    public void capacityBadge_almostFull() {
        Event e7 = allEvents.stream().filter(e -> "e7".equals(e.getId())).findFirst().get();
        double ratio = (double) e7.getRegisteredCount() / e7.getCapacity();
        assertTrue("38/40 = 95% → almost full", ratio >= 0.8 && ratio < 1.0);
    }

    @Test
    public void capacityBadge_available() {
        Event e1 = allEvents.stream().filter(e -> "e1".equals(e.getId())).findFirst().get();
        double ratio = (double) e1.getRegisteredCount() / e1.getCapacity();
        assertTrue("10/50 = 20% → available", ratio < 0.8);
    }

    // ── 11. Free vs paid filter ───────────────────────────────────────────

    @Test
    public void filter_freeEventsOnly() {
        List<Event> free = allEvents.stream()
                .filter(e -> e.getPrice() == 0.0)
                .collect(Collectors.toList());
        // e1, e3, e6, e8 are free
        assertEquals(4, free.size());
        assertTrue(free.stream().allMatch(e -> e.getPrice() == 0.0));
    }

    @Test
    public void filter_paidEventsOnly() {
        List<Event> paid = allEvents.stream()
                .filter(e -> e.getPrice() > 0.0)
                .collect(Collectors.toList());
        // e2=100, e4=50, e5=200, e7=300
        assertEquals(4, paid.size());
    }

    // ── 12. Notification read/unread state ───────────────────────────────

    @Test
    public void notification_unreadFlagSetCorrectly() {
        NotificationItem n = new NotificationItem("Event Reminder", "Your event starts soon!", true, new Timestamp(new Date()));
        assertTrue(n.isUnread());
        assertFalse(n.isRead());
    }

    @Test
    public void notification_markAsRead() {
        NotificationItem n = new NotificationItem("Update", "Event cancelled", true, new Timestamp(new Date()));
        assertTrue(n.isUnread());

        n.setRead(true);
        assertFalse(n.isUnread());
        assertTrue(n.isRead());
    }

    @Test
    public void notificationList_unreadCount() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem("A", "msg1", true,  new Timestamp(new Date())));
        notifications.add(new NotificationItem("B", "msg2", false, new Timestamp(new Date())));
        notifications.add(new NotificationItem("C", "msg3", true,  new Timestamp(new Date())));
        notifications.add(new NotificationItem("D", "msg4", false, new Timestamp(new Date())));

        long unreadCount = notifications.stream().filter(NotificationItem::isUnread).count();
        assertEquals(2, unreadCount);
    }

    @Test
    public void notificationList_markAllRead() {
        List<NotificationItem> notifications = new ArrayList<>();
        notifications.add(new NotificationItem("A", "msg1", true,  new Timestamp(new Date())));
        notifications.add(new NotificationItem("B", "msg2", true,  new Timestamp(new Date())));

        notifications.forEach(n -> n.setRead(true));

        long unreadCount = notifications.stream().filter(NotificationItem::isUnread).count();
        assertEquals(0, unreadCount);
    }
}
