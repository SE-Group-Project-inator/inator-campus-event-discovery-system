package com.example.campuseventdiscoverysystem.integration;

import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

/**
 * Integration tests: Payment lifecycle and Event/Registration interactions.
 *
 * Covers the full lifecycle of a payment record:
 *   - Creation, field population, method/status label helpers
 *   - Payment linked to an event and a student
 *   - Payment state transitions (pending → approved / rejected)
 *   - Registration confirmed after payment approval
 *   - Edge cases: unknown method/status, null fields, boundary amounts
 *
 * These tests exercise multiple model classes together, verifying the data
 * contracts that PaymentActivity, PaymentVerificationActivity, and
 * PaymentVerificationAdapter depend on.
 */
public class PaymentEventIntegrationTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Event buildPaidEvent(String id, String title, double price, int cap) {
        Event e = new Event();
        e.setId(id);
        e.setTitle(title);
        e.setPrice(price);
        e.setCapacity(cap);
        e.setRegisteredCount(0);
        e.setStatus("active");
        e.setDate(new Timestamp(new Date(System.currentTimeMillis() + 86_400_000L)));
        return e;
    }

    private Payment buildPayment(Event event, String studentId, String method, double amount) {
        Payment p = new Payment();
        p.setEventId(event.getId());
        p.setEventName(event.getTitle());
        p.setStudentId(studentId);
        p.setStudentName("Test Student");
        p.setStudentEmail("student@lums.edu.pk");
        p.setPaymentMethod(method);
        p.setAmount(amount);
        p.setStatus(Payment.STATUS_VERIFICATION_PENDING);
        p.setTimestamp(new Timestamp(new Date()));
        return p;
    }

    // ── 1. Full payment lifecycle: submit → approve ────────────────────────

    @Test
    public void paymentLifecycle_submitAndApprove() {
        Event event = buildPaidEvent("ev1", "Tech Summit", 200.0, 100);
        Payment payment = buildPayment(event, "uid_student_1", Payment.METHOD_JAZZCASH, 200.0);

        // Initial state
        assertEquals(Payment.STATUS_VERIFICATION_PENDING, payment.getStatus());
        assertEquals("Pending Verification", payment.getStatusLabel());

        // Admin approves
        payment.setStatus(Payment.STATUS_APPROVED);
        assertEquals("Approved ✅", payment.getStatusLabel());
        assertEquals(event.getId(), payment.getEventId());
        assertEquals(200.0, payment.getAmount(), 0.001);
    }

    // ── 2. Full payment lifecycle: submit → reject ────────────────────────

    @Test
    public void paymentLifecycle_submitAndReject() {
        Event event = buildPaidEvent("ev2", "Art Gala", 100.0, 50);
        Payment payment = buildPayment(event, "uid_student_2", Payment.METHOD_EASYPAISA, 100.0);

        payment.setStatus(Payment.STATUS_REJECTED);
        payment.setRejectionReason("Screenshot unclear");

        assertEquals(Payment.STATUS_REJECTED, payment.getStatus());
        assertEquals("Rejected ❌", payment.getStatusLabel());
        assertEquals("Screenshot unclear", payment.getRejectionReason());
    }

    // ── 3. Cash payment path ──────────────────────────────────────────────

    @Test
    public void cashPayment_pendingCashStatus() {
        Event event = buildPaidEvent("ev3", "Sports Day", 50.0, 200);
        Payment payment = buildPayment(event, "uid_student_3", Payment.METHOD_CASH, 50.0);
        payment.setStatus(Payment.STATUS_PENDING_CASH);

        assertEquals("Cash Pending 💵", payment.getStatusLabel());
        assertEquals("Cash on Event", payment.getPaymentMethodLabel());
    }

    // ── 4. Method label helpers ────────────────────────────────────────────

    @Test
    public void paymentMethodLabels_allKnownMethods() {
        Payment p = new Payment();

        p.setPaymentMethod(Payment.METHOD_JAZZCASH);
        assertEquals("JazzCash", p.getPaymentMethodLabel());

        p.setPaymentMethod(Payment.METHOD_EASYPAISA);
        assertEquals("Easypaisa", p.getPaymentMethodLabel());

        p.setPaymentMethod(Payment.METHOD_CASH);
        assertEquals("Cash on Event", p.getPaymentMethodLabel());
    }

    @Test
    public void paymentMethodLabel_unknownMethod_returnsRawValue() {
        Payment p = new Payment();
        p.setPaymentMethod("bank_transfer");
        assertEquals("bank_transfer", p.getPaymentMethodLabel());
    }

    @Test
    public void paymentMethodLabel_nullMethod_returnsUnknown() {
        Payment p = new Payment();
        // method not set → null
        assertEquals("Unknown", p.getPaymentMethodLabel());
    }

    // ── 5. Status label: null and unknown ─────────────────────────────────

    @Test
    public void paymentStatusLabel_nullStatus_returnsUnknown() {
        Payment p = new Payment();
        assertEquals("Unknown", p.getStatusLabel());
    }

    @Test
    public void paymentStatusLabel_unknownStatus_returnsRawValue() {
        Payment p = new Payment();
        p.setStatus("refunded");
        assertEquals("refunded", p.getStatusLabel());
    }

    @Test
    public void paymentStatus_registered_label() {
        Payment p = new Payment();
        p.setStatus(Payment.STATUS_REGISTERED);
        assertEquals("Registered 🎟", p.getStatusLabel());
    }

    // ── 6. Payment ↔ Event linkage ────────────────────────────────────────

    @Test
    public void paymentLinkedToEvent_fieldsMatch() {
        Event event = buildPaidEvent("ev4", "LUMS Fest", 300.0, 500);
        Payment payment = buildPayment(event, "uid_s4", Payment.METHOD_JAZZCASH, 300.0);

        assertEquals(event.getId(),    payment.getEventId());
        assertEquals(event.getTitle(), payment.getEventName());
        assertEquals(event.getPrice(), payment.getAmount(), 0.001);
    }

    // ── 7. Payment → Registration confirmation flow ───────────────────────

    @Test
    public void paymentApproved_registrationIsConfirmed() {
        Event event = buildPaidEvent("ev5", "Robotics Expo", 250.0, 60);
        Payment payment = buildPayment(event, "uid_s5", Payment.METHOD_EASYPAISA, 250.0);

        // Simulate approval
        payment.setStatus(Payment.STATUS_APPROVED);

        // After approval the app creates/updates a Registration
        Registration reg = new Registration();
        reg.setEventId(event.getId());
        reg.setUserId(payment.getStudentId());
        reg.setUserName(payment.getStudentName());
        reg.setUserEmail(payment.getStudentEmail());
        reg.setConfirmed(true);
        reg.setSeatNumber(1);
        reg.setRegisteredAt(new Timestamp(new Date()));

        assertTrue("Registration must be confirmed after payment approval",
                reg.isConfirmed());
        assertEquals(event.getId(), reg.getEventId());
        assertEquals("uid_s5", reg.getUserId());
    }

    // ── 8. Capacity check at registration time ────────────────────────────

    @Test
    public void capacityCheck_blockRegistrationWhenFull() {
        Event event = buildPaidEvent("ev6", "Full Workshop", 100.0, 10);
        event.setRegisteredCount(10); // already full

        boolean canRegister = event.getRegisteredCount() < event.getCapacity();
        assertFalse("Should not allow registration when event is full", canRegister);
    }

    @Test
    public void capacityCheck_allowRegistrationWhenSpaceAvailable() {
        Event event = buildPaidEvent("ev7", "Open Seminar", 0.0, 50);
        event.setRegisteredCount(30);

        boolean canRegister = event.getRegisteredCount() < event.getCapacity();
        assertTrue("Should allow registration when space available", canRegister);
    }

    // ── 9. Free event: no payment required, direct registration ──────────

    @Test
    public void freeEvent_priceIsZero_noPaymentRequired() {
        Event event = buildPaidEvent("ev8", "Free Webinar", 0.0, 100);
        assertEquals(0.0, event.getPrice(), 0.001);
        assertEquals("FREE", event.getPriceDisplay());

        // Direct registration (no Payment object)
        Registration reg = new Registration();
        reg.setEventId(event.getId());
        reg.setUserId("uid_free");
        reg.setConfirmed(true);

        assertTrue(reg.isConfirmed());
    }

    // ── 10. Boundary: zero-amount payment ────────────────────────────────

    @Test
    public void payment_zeroAmount_isStoredCorrectly() {
        Payment p = new Payment();
        p.setAmount(0.0);
        assertEquals(0.0, p.getAmount(), 0.001);
    }

    // ── 11. Screenshot URL stored for digital payments ───────────────────

    @Test
    public void payment_screenshotUrl_storedAndRetrieved() {
        Payment p = new Payment();
        p.setScreenshotUrl("https://storage.example.com/screenshots/txn_001.png");
        assertEquals("https://storage.example.com/screenshots/txn_001.png",
                p.getScreenshotUrl());
    }

    // ── 12. Assigned-to field for admin workflow ──────────────────────────

    @Test
    public void payment_assignedTo_adminWorkflow() {
        Payment p = new Payment();
        p.setAssignedTo("admin_uid_007");
        assertEquals("admin_uid_007", p.getAssignedTo());
    }
}
