package com.example.campuseventdiscoverysystem.integration;

import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests: RSVP / Registration ↔ Event capacity management.
 *
 * Simulates the end-to-end flow that spans RsvpActivity, EventDetailActivity,
 * and the Firestore write logic:
 *   1. Student tries to register → capacity check
 *   2. Registration object is built and confirmed
 *   3. Event's registeredCount is incremented (client-side model)
 *   4. Attempting to register after the event is full is blocked
 *   5. Cancellation decrements the count and re-opens the slot
 *   6. Seat numbers are assigned sequentially
 *   7. Duplicate registrations (same user, same event) are detected
 */
public class RegistrationCapacityIntegrationTest {

    private Event event;
    private List<Registration> registrations;

    @Before
    public void setUp() {
        event = new Event();
        event.setId("event_test_001");
        event.setTitle("Integration Summit");
        event.setStatus("active");
        event.setCapacity(3); // small cap for easy testing
        event.setRegisteredCount(0);
        event.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86_400_000L)));
        event.setPrice(0);

        registrations = new ArrayList<>();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Attempt to register a student. Returns the Registration or null if full/duplicate. */
    private Registration tryRegister(String userId, String userName, String email) {
        // Duplicate check
        boolean alreadyRegistered = registrations.stream()
                .anyMatch(r -> r.getUserId().equals(userId)
                        && r.getEventId().equals(event.getId())
                        && r.isConfirmed());
        if (alreadyRegistered) return null;

        // Capacity check
        if (event.getRegisteredCount() >= event.getCapacity()) return null;

        // Build registration
        Registration reg = new Registration();
        reg.setId("reg_" + userId);
        reg.setEventId(event.getId());
        reg.setUserId(userId);
        reg.setUserName(userName);
        reg.setUserEmail(email);
        reg.setSeatNumber(event.getRegisteredCount() + 1);
        reg.setConfirmed(true);
        reg.setRegisteredAt(new Timestamp(new Date()));

        registrations.add(reg);
        event.setRegisteredCount(event.getRegisteredCount() + 1);
        return reg;
    }

    /** Cancel a registration. Returns true if the cancellation succeeded. */
    private boolean cancelRegistration(String userId) {
        for (Registration r : registrations) {
            if (r.getUserId().equals(userId) && r.isConfirmed()) {
                r.setConfirmed(false);
                event.setRegisteredCount(event.getRegisteredCount() - 1);
                return true;
            }
        }
        return false;
    }

    // ── 1. Basic registration succeeds when space available ───────────────

    @Test
    public void register_succeeds_whenSpaceAvailable() {
        Registration reg = tryRegister("u1", "Alice", "alice@lums.edu.pk");
        assertNotNull("Registration should succeed", reg);
        assertTrue(reg.isConfirmed());
        assertEquals("u1", reg.getUserId());
        assertEquals(event.getId(), reg.getEventId());
        assertEquals(1, event.getRegisteredCount());
    }

    // ── 2. Seat numbers assigned sequentially ─────────────────────────────

    @Test
    public void seatNumbers_assignedSequentially() {
        Registration r1 = tryRegister("u1", "Alice", "alice@lums.edu.pk");
        Registration r2 = tryRegister("u2", "Bob",   "bob@lums.edu.pk");
        Registration r3 = tryRegister("u3", "Carol", "carol@lums.edu.pk");

        assertNotNull(r1); assertNotNull(r2); assertNotNull(r3);
        assertEquals(1, r1.getSeatNumber());
        assertEquals(2, r2.getSeatNumber());
        assertEquals(3, r3.getSeatNumber());
        assertEquals(3, event.getRegisteredCount());
    }

    // ── 3. Registration blocked when event is full ────────────────────────

    @Test
    public void register_blocked_whenEventFull() {
        tryRegister("u1", "Alice", "a@lums.edu.pk");
        tryRegister("u2", "Bob",   "b@lums.edu.pk");
        tryRegister("u3", "Carol", "c@lums.edu.pk");

        assertEquals(3, event.getRegisteredCount()); // capacity = 3, now full
        Registration r4 = tryRegister("u4", "Dave", "d@lums.edu.pk");
        assertNull("4th registration should be blocked", r4);
        assertEquals("Count must stay at capacity", 3, event.getRegisteredCount());
    }

    // ── 4. Cancellation opens a slot ──────────────────────────────────────

    @Test
    public void cancel_opensSlotForNextStudent() {
        tryRegister("u1", "Alice", "a@lums.edu.pk");
        tryRegister("u2", "Bob",   "b@lums.edu.pk");
        tryRegister("u3", "Carol", "c@lums.edu.pk");

        // Event is full
        assertNull(tryRegister("u4", "Dave", "d@lums.edu.pk"));

        // Alice cancels
        boolean cancelled = cancelRegistration("u1");
        assertTrue("Cancellation should succeed", cancelled);
        assertEquals(2, event.getRegisteredCount());

        // Now Dave can register
        Registration r4 = tryRegister("u4", "Dave", "d@lums.edu.pk");
        assertNotNull("Dave should now be able to register", r4);
        assertEquals(3, event.getRegisteredCount());
    }

    // ── 5. Duplicate registration is blocked ─────────────────────────────

    @Test
    public void register_blocked_whenAlreadyRegistered() {
        tryRegister("u1", "Alice", "a@lums.edu.pk");
        assertEquals(1, event.getRegisteredCount());

        Registration duplicate = tryRegister("u1", "Alice", "a@lums.edu.pk");
        assertNull("Duplicate registration should be blocked", duplicate);
        assertEquals("Count should not increase", 1, event.getRegisteredCount());
    }

    // ── 6. Cancellation of non-existent registration fails gracefully ─────

    @Test
    public void cancel_nonExistentRegistration_returnsFalse() {
        boolean result = cancelRegistration("u_ghost");
        assertFalse("Cancelling a non-existent registration should return false", result);
        assertEquals(0, event.getRegisteredCount());
    }

    // ── 7. Full registration flow: 3 registers, 1 cancel, 1 re-register ──

    @Test
    public void fullFlow_fillCancelRefill() {
        Registration r1 = tryRegister("u1", "Alice", "a@lums.edu.pk");
        Registration r2 = tryRegister("u2", "Bob",   "b@lums.edu.pk");
        Registration r3 = tryRegister("u3", "Carol", "c@lums.edu.pk");

        assertNotNull(r1); assertNotNull(r2); assertNotNull(r3);
        assertEquals(3, event.getRegisteredCount());

        // Bob cancels
        assertTrue(cancelRegistration("u2"));
        assertEquals(2, event.getRegisteredCount());

        // Eve takes the slot
        Registration r5 = tryRegister("u5", "Eve", "e@lums.edu.pk");
        assertNotNull("Eve should get Bob's slot", r5);
        assertEquals(3, event.getRegisteredCount());

        // Bob can't re-register (event is full again)
        assertNull(tryRegister("u2", "Bob", "b@lums.edu.pk"));
    }

    // ── 8. Registration stores all required fields ────────────────────────

    @Test
    public void registration_allFieldsPopulated() {
        Registration reg = tryRegister("u_check", "Full Student", "full@lums.edu.pk");
        assertNotNull(reg);
        assertEquals("reg_u_check",      reg.getId());
        assertEquals(event.getId(),       reg.getEventId());
        assertEquals("u_check",           reg.getUserId());
        assertEquals("Full Student",      reg.getUserName());
        assertEquals("full@lums.edu.pk",  reg.getUserEmail());
        assertEquals(1,                   reg.getSeatNumber());
        assertTrue(reg.isConfirmed());
        assertNotNull("registeredAt should be set", reg.getRegisteredAt());
    }

    // ── 9. Event model reflects correct available capacity ────────────────

    @Test
    public void event_availableCapacity_calculatedCorrectly() {
        tryRegister("u1", "A", "a@lums.edu.pk");
        tryRegister("u2", "B", "b@lums.edu.pk");

        int available = event.getCapacity() - event.getRegisteredCount();
        assertEquals("2 registered of 3 → 1 available", 1, available);
    }

    // ── 10. Registrations list tracks all confirmed registrations ─────────

    @Test
    public void registrationsList_onlyContainsConfirmed() {
        tryRegister("u1", "A", "a@lums.edu.pk");
        tryRegister("u2", "B", "b@lums.edu.pk");
        cancelRegistration("u1"); // u1 is now unconfirmed

        long confirmedCount = registrations.stream().filter(Registration::isConfirmed).count();
        assertEquals("Only 1 confirmed after 1 cancellation", 1, confirmedCount);
    }
}
