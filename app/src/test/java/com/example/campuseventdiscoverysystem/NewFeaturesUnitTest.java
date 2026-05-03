package com.example.campuseventdiscoverysystem;

import com.example.campuseventdiscoverysystem.chat.ChatMessage;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.example.campuseventdiscoverysystem.recommendations.RsvpAttendance;
import com.example.campuseventdiscoverysystem.recommendations.UserPreferences;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the units added in the final milestone:
 *   - US-22 recommendations DTOs (UserPreferences, RsvpAttendance)
 *   - US-28 chat (ChatMessage)
 *   - Payment status/method label helpers
 */
public class NewFeaturesUnitTest {

    // ── ChatMessage (US-28) ──────────────────────────────────────

    @Test
    public void chatMessage_userType_storesFields() {
        ChatMessage m = new ChatMessage(ChatMessage.TYPE_USER, "hi");
        assertEquals(ChatMessage.TYPE_USER, m.type);
        assertEquals("hi", m.text);
    }

    @Test
    public void chatMessage_botType_storesFields() {
        ChatMessage m = new ChatMessage(ChatMessage.TYPE_BOT, "hello!");
        assertEquals(ChatMessage.TYPE_BOT, m.type);
        assertEquals("hello!", m.text);
    }

    @Test
    public void chatMessage_typesAreDistinct() {
        assertNotEquals(ChatMessage.TYPE_USER, ChatMessage.TYPE_BOT);
    }

    @Test
    public void chatMessage_emptyTextAllowed() {
        ChatMessage m = new ChatMessage(ChatMessage.TYPE_USER, "");
        assertEquals("", m.text);
    }

    // ── UserPreferences (US-22 cold-start) ───────────────────────

    @Test
    public void userPreferences_defaultCtor_isEmpty() {
        UserPreferences p = new UserPreferences();
        assertTrue(p.isEmpty());
        assertNotNull(p.getCategories());
    }

    @Test
    public void userPreferences_nullCtor_treatedAsEmpty() {
        UserPreferences p = new UserPreferences(null);
        assertTrue(p.isEmpty());
        assertNotNull(p.getCategories());
    }

    @Test
    public void userPreferences_withCategories_notEmpty() {
        UserPreferences p = new UserPreferences(Arrays.asList("Sports", "Tech"));
        assertFalse(p.isEmpty());
        assertEquals(2, p.getCategories().size());
        assertEquals("Sports", p.getCategories().get(0));
    }

    @Test
    public void userPreferences_setterRoundTrip() {
        UserPreferences p = new UserPreferences();
        p.setCategories(Collections.singletonList("Arts"));
        assertEquals(1, p.getCategories().size());
        assertEquals("Arts", p.getCategories().get(0));
    }

    // ── RsvpAttendance (US-22 input DTO) ─────────────────────────

    @Test
    public void rsvpAttendance_fieldsExposed() {
        RsvpAttendance a = new RsvpAttendance("e1", "ACM", 1234L);
        assertEquals("e1", a.eventId);
        assertEquals("ACM", a.society);
        assertEquals(1234L, a.dateMillis);
    }

    @Test
    public void rsvpAttendance_nullSocietyAllowed() {
        RsvpAttendance a = new RsvpAttendance("e2", null, 0L);
        assertEquals("e2", a.eventId);
        assertEquals(null, a.society);
    }

    // ── Payment label helpers ────────────────────────────────────

    @Test
    public void payment_methodLabel_jazzcash() {
        Payment p = new Payment();
        p.setPaymentMethod(Payment.METHOD_JAZZCASH);
        assertEquals("JazzCash", p.getPaymentMethodLabel());
    }

    @Test
    public void payment_methodLabel_easypaisa() {
        Payment p = new Payment();
        p.setPaymentMethod(Payment.METHOD_EASYPAISA);
        assertEquals("Easypaisa", p.getPaymentMethodLabel());
    }

    @Test
    public void payment_methodLabel_cash() {
        Payment p = new Payment();
        p.setPaymentMethod(Payment.METHOD_CASH);
        assertEquals("Cash on Event", p.getPaymentMethodLabel());
    }

    @Test
    public void payment_methodLabel_nullIsUnknown() {
        Payment p = new Payment();
        assertEquals("Unknown", p.getPaymentMethodLabel());
    }

    @Test
    public void payment_statusLabel_pendingVerification() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_VERIFICATION_PENDING);
        assertEquals("Pending Verification", p.getStatusLabel());
    }

    @Test
    public void payment_statusLabel_approved() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_APPROVED);
        assertTrue(p.getStatusLabel().startsWith("Approved"));
    }

    @Test
    public void payment_statusLabel_rejected() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_REJECTED);
        assertTrue(p.getStatusLabel().startsWith("Rejected"));
    }

    @Test
    public void payment_statusLabel_pendingCash() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_PENDING_CASH);
        assertTrue(p.getStatusLabel().startsWith("Cash Pending"));
    }

    @Test
    public void payment_statusLabel_registered() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_REGISTERED);
        assertTrue(p.getStatusLabel().startsWith("Registered"));
    }

    @Test
    public void payment_statusLabel_nullIsUnknown() {
        Payment p = new Payment();
        assertEquals("Unknown", p.getStatusLabel());
    }

    @Test
    public void payment_amountRoundTrip() {
        Payment p = new Payment();
        p.setAmount(1500.0);
        assertEquals(1500.0, p.getAmount(), 0.001);
    }
}
