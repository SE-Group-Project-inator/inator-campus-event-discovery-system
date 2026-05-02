package com.example.campuseventdiscoverysystem.models;

import com.google.firebase.Timestamp;

/**
 * Payment model representing a payment request in Firestore.
 * Supports both manual (cash) and screenshot-based (JazzCash/Easypaisa) payments.
 */
public class Payment {

    public static final String STATUS_VERIFICATION_PENDING = "verification_pending";
    public static final String STATUS_APPROVED             = "approved";
    public static final String STATUS_REJECTED             = "rejected";
    public static final String STATUS_PENDING_CASH         = "pending_cash";
    public static final String STATUS_REGISTERED           = "registered";

    public static final String METHOD_CASH       = "cash";
    public static final String METHOD_JAZZCASH   = "jazzcash";
    public static final String METHOD_EASYPAISA  = "easypaisa";

    private String paymentId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String eventId;
    private String eventName;
    private String paymentMethod;
    private String screenshotUrl;
    private double amount;
    private String status;
    private String assignedTo;
    private Timestamp timestamp;
    private String rejectionReason;

    public Payment() {}

    // --- Getters ---
    public String getPaymentId()       { return paymentId; }
    public String getStudentId()       { return studentId; }
    public String getStudentName()     { return studentName; }
    public String getStudentEmail()    { return studentEmail; }
    public String getEventId()         { return eventId; }
    public String getEventName()       { return eventName; }
    public String getPaymentMethod()   { return paymentMethod; }
    public String getScreenshotUrl()   { return screenshotUrl; }
    public double getAmount()          { return amount; }
    public String getStatus()          { return status; }
    public String getAssignedTo()      { return assignedTo; }
    public Timestamp getTimestamp()    { return timestamp; }
    public String getRejectionReason() { return rejectionReason; }

    // --- Setters ---
    public void setPaymentId(String paymentId)           { this.paymentId = paymentId; }
    public void setStudentId(String studentId)           { this.studentId = studentId; }
    public void setStudentName(String studentName)       { this.studentName = studentName; }
    public void setStudentEmail(String studentEmail)     { this.studentEmail = studentEmail; }
    public void setEventId(String eventId)               { this.eventId = eventId; }
    public void setEventName(String eventName)           { this.eventName = eventName; }
    public void setPaymentMethod(String paymentMethod)   { this.paymentMethod = paymentMethod; }
    public void setScreenshotUrl(String screenshotUrl)   { this.screenshotUrl = screenshotUrl; }
    public void setAmount(double amount)                 { this.amount = amount; }
    public void setStatus(String status)                 { this.status = status; }
    public void setAssignedTo(String assignedTo)         { this.assignedTo = assignedTo; }
    public void setTimestamp(Timestamp timestamp)        { this.timestamp = timestamp; }
    public void setRejectionReason(String r)             { this.rejectionReason = r; }

    /** Human-readable label for payment method */
    public String getPaymentMethodLabel() {
        if (paymentMethod == null) return "Unknown";
        switch (paymentMethod) {
            case METHOD_JAZZCASH:  return "JazzCash";
            case METHOD_EASYPAISA: return "Easypaisa";
            case METHOD_CASH:      return "Cash on Event";
            default:               return paymentMethod;
        }
    }

    /** Human-readable status label */
    public String getStatusLabel() {
        if (status == null) return "Unknown";
        switch (status) {
            case STATUS_VERIFICATION_PENDING: return "Pending Verification";
            case STATUS_APPROVED:             return "Approved ✅";
            case STATUS_REJECTED:             return "Rejected ❌";
            case STATUS_PENDING_CASH:         return "Cash Pending 💵";
            case STATUS_REGISTERED:           return "Registered 🎟";
            default:                          return status;
        }
    }
}
