package com.example.campuseventdiscoverysystem.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.PaymentVerificationAdapter;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

/**
 * =============================================================================
 * PaymentVerificationActivity
 * =============================================================================
 * Event Manager Panel for verifying student payments.
 *
 * Features:
 * - Real-time Firestore payment monitoring
 * - Filtering (Pending, Approved, Rejected, Cash, All)
 * - Screenshot preview dialog (Base64 decoding)
 * - Approve / Reject payment workflow
 * - Automatic RSVP + attendance + notification updates
 */
public class PaymentVerificationActivity extends AppCompatActivity {

    // ========================= UI COMPONENTS =========================
    private RecyclerView rvPayments;
    private ProgressBar progressBar;
    private TextView tvEmpty, tvPendingBadge;
    private TabLayout tabLayout;

    // ========================= DATA LISTS =========================
    private final List<Payment> allPayments     = new ArrayList<>();
    private final List<Payment> filteredPayments = new ArrayList<>();
    private PaymentVerificationAdapter adapter;

    // ========================= FIREBASE =========================
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    // Current filter tab state
    private String currentFilter = "pending"; // default tab

    /**
     * Called when activity starts.
     * Initializes UI, tabs, recycler view, and Firestore listener.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_verification);

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupTabs();
        setupRecyclerView();
        setupBackButton();
        startListener();
    }

    /**
     * Binds XML views to Java variables
     */
    private void bindViews() {
        rvPayments     = findViewById(R.id.rvVerificationPayments);
        progressBar    = findViewById(R.id.progressBarVerification);
        tvEmpty        = findViewById(R.id.tvEmptyVerification);
        tvPendingBadge = findViewById(R.id.tvPendingBadge);
        tabLayout      = findViewById(R.id.tabsVerification);
    }

    /**
     * Sets up tab filtering (Pending / All / Approved / Rejected / Cash)
     */
    private void setupTabs() {

        if (tabLayout == null) return;

        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Approved"));
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"));
        tabLayout.addTab(tabLayout.newTab().setText("Cash"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "pending";  break;
                    case 1: currentFilter = "all";      break;
                    case 2: currentFilter = "approved"; break;
                    case 3: currentFilter = "rejected"; break;
                    case 4: currentFilter = "cash";     break;
                }
                applyFilter();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    /**
     * Initializes RecyclerView and adapter
     */
    private void setupRecyclerView() {
        adapter = new PaymentVerificationAdapter(
                filteredPayments,
                this::showScreenshotDialog,
                this::showApproveConfirmation,
                this::showRejectDialog
        );

        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(adapter);
    }

    /**
     * Back button handler
     */
    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnVerificationBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ─────────────────────────────────────────────────────────────
    // FIRESTORE REAL-TIME LISTENER
    // ─────────────────────────────────────────────────────────────

    /**
     * Listens to real-time updates of payments collection
     */
    private void startListener() {

        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("payments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((query, error) -> {

                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);

                    if (error != null || query == null) return;

                    allPayments.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Payment p = doc.toObject(Payment.class);
                        if (p != null) {
                            p.setPaymentId(doc.getId());
                            allPayments.add(p);
                        }
                    }

                    // Update pending badge count
                    long pendingCount = allPayments.stream()
                            .filter(p -> Payment.STATUS_VERIFICATION_PENDING.equals(p.getStatus()))
                            .count();

                    if (tvPendingBadge != null) {
                        tvPendingBadge.setText(String.valueOf(pendingCount));
                        tvPendingBadge.setVisibility(pendingCount > 0 ? View.VISIBLE : View.GONE);
                    }

                    applyFilter();
                });
    }

    /**
     * Filters payment list based on selected tab
     */
    private void applyFilter() {

        filteredPayments.clear();

        for (Payment p : allPayments) {

            switch (currentFilter) {

                case "pending":
                    if (Payment.STATUS_VERIFICATION_PENDING.equals(p.getStatus()))
                        filteredPayments.add(p);
                    break;

                case "approved":
                    if (Payment.STATUS_APPROVED.equals(p.getStatus()))
                        filteredPayments.add(p);
                    break;

                case "rejected":
                    if (Payment.STATUS_REJECTED.equals(p.getStatus()))
                        filteredPayments.add(p);
                    break;

                case "cash":
                    if (Payment.STATUS_PENDING_CASH.equals(p.getStatus()))
                        filteredPayments.add(p);
                    break;

                default:
                    filteredPayments.add(p);
                    break;
            }
        }

        adapter.notifyDataSetChanged();

        if (tvEmpty != null) {
            tvEmpty.setVisibility(filteredPayments.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ACTIONS (APPROVE / REJECT)
    // ─────────────────────────────────────────────────────────────

    /**
     * Shows confirmation dialog before approving payment
     */
    private void showApproveConfirmation(Payment payment) {

        new AlertDialog.Builder(this)
                .setTitle("Approve Payment")
                .setMessage("Approve payment from " + payment.getStudentName()
                        + " for " + payment.getEventName() + "?")
                .setPositiveButton("✅ Approve", (d, w) -> approvePayment(payment))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Approves payment and updates Firestore + RSVP + notifications
     */
    private void approvePayment(Payment payment) {

        db.collection("payments").document(payment.getPaymentId())
                .update("status", Payment.STATUS_APPROVED)
                .addOnSuccessListener(v -> {

                    Toast.makeText(this, "Payment approved ✅", Toast.LENGTH_SHORT).show();

                    if (payment.getEventId() != null) {

                        // Increment event registration count
                        db.collection("events").document(payment.getEventId())
                                .update("registeredCount",
                                        com.google.firebase.firestore.FieldValue.increment(1));

                        // Add attendee record
                        if (payment.getStudentId() != null) {

                            java.util.Map<String, Object> attendee = new java.util.HashMap<>();
                            attendee.put("userId", payment.getStudentId());
                            attendee.put("studentName", payment.getStudentName());
                            attendee.put("studentEmail", payment.getStudentEmail());
                            attendee.put("approvedAt",
                                    com.google.firebase.firestore.FieldValue.serverTimestamp());

                            db.collection("event_attendees")
                                    .document(payment.getEventId())
                                    .collection("attendees")
                                    .document(payment.getStudentId())
                                    .set(attendee);

                            // Create RSVP record
                            String rsvpId = payment.getStudentId() + "_" + payment.getEventId();

                            java.util.Map<String, Object> rsvp = new java.util.HashMap<>();
                            rsvp.put("userId", payment.getStudentId());
                            rsvp.put("eventId", payment.getEventId());
                            rsvp.put("eventName", payment.getEventName());
                            rsvp.put("status", "confirmed");
                            rsvp.put("paymentId", payment.getPaymentId());
                            rsvp.put("paymentStatus", Payment.STATUS_APPROVED);
                            rsvp.put("paymentMethod", payment.getPaymentMethod());
                            rsvp.put("paidBadge", true);
                            rsvp.put("createdAt",
                                    com.google.firebase.firestore.FieldValue.serverTimestamp());

                            db.collection("rsvps").document(rsvpId)
                                    .set(rsvp, com.google.firebase.firestore.SetOptions.merge());

                            // Send notification to student
                            java.util.Map<String, Object> notif = new java.util.HashMap<>();
                            notif.put("title", "✅ Payment Approved — You're In!");
                            notif.put("message",
                                    "Your " + payment.getPaymentMethodLabel()
                                            + " payment for \"" + payment.getEventName()
                                            + "\" has been approved. You are now registered!");
                            notif.put("type", "payment_approved");
                            notif.put("eventId", payment.getEventId());
                            notif.put("eventName", payment.getEventName());
                            notif.put("read", false);
                            notif.put("timestamp",
                                    com.google.firebase.firestore.FieldValue.serverTimestamp());

                            db.collection("users")
                                    .document(payment.getStudentId())
                                    .collection("notifications")
                                    .add(notif);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Shows rejection dialog with reason input
     */
    private void showRejectDialog(Payment payment) {

        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_reject_payment, null);

        EditText etReason = dialogView.findViewById(R.id.etRejectionReason);

        new AlertDialog.Builder(this)
                .setTitle("Reject Payment")
                .setView(dialogView)
                .setPositiveButton("❌ Reject", (d, w) -> {
                    String reason = etReason != null
                            ? etReason.getText().toString().trim()
                            : "";
                    rejectPayment(payment, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Rejects payment and stores rejection reason
     */
    private void rejectPayment(Payment payment, String reason) {

        db.collection("payments").document(payment.getPaymentId())
                .update(
                        "status", Payment.STATUS_REJECTED,
                        "rejectionReason", reason
                )
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                "Payment rejected ❌",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Displays payment screenshot in full-screen dialog (Base64 decode)
     */
    private void showScreenshotDialog(String screenshotData) {

        if (screenshotData == null || screenshotData.isEmpty()) {
            Toast.makeText(this,
                    "No screenshot available",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater()
                .inflate(R.layout.dialog_screenshot_preview, null);

        ImageView ivPreview = dialogView.findViewById(R.id.ivScreenshotFull);

        if (ivPreview != null) {
            try {
                String base64 = screenshotData;

                if (base64.contains(",")) {
                    base64 = base64.substring(base64.indexOf(",") + 1);
                }

                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                ivPreview.setImageBitmap(bitmap);

            } catch (Exception e) {
                Toast.makeText(this,
                        "Could not load image",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        dialog.show();
    }

    /**
     * Cleans up Firestore listener when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}