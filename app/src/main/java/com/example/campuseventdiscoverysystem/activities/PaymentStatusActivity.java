package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;

import java.text.NumberFormat;

/**
 * =============================================================================
 * PaymentStatusActivity
 * =============================================================================
 * Shows real-time payment status for a student after submitting payment.
 *
 * Features:
 * - Live Firestore listener for payment updates
 * - Displays approval / rejection / pending states
 * - Shows event details and payment method
 * - Handles navigation (home, tickets, payments)
 * - Displays rejection reason if payment fails
 */
public class PaymentStatusActivity extends AppCompatActivity {

    /** Intent keys used to receive payment data */
    public static final String KEY_PAYMENT_ID = "paymentId";
    public static final String KEY_STATUS     = "status";
    public static final String KEY_METHOD     = "method";
    public static final String KEY_EVENT_NAME = "eventName";
    public static final String KEY_AMOUNT     = "amount";

    // ========================= UI COMPONENTS =========================
    private TextView tvStatusBadge, tvStatusDescription;
    private TextView tvEventName, tvPaymentMethod, tvAmount;
    private CardView cardStatus;
    private ProgressBar progressBar;
    private MaterialButton btnGoHome, btnViewMyPayments, btnViewTickets;
    private TextView tvRejectionReason;
    private CardView cardRejection;

    // ========================= FIREBASE =========================
    private FirebaseFirestore db;
    private ListenerRegistration statusListener;

    // Payment ID for real-time tracking
    private String paymentId;

    /**
     * Called when activity is created.
     * Initializes UI, Firestore, and listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_status);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        bindViews();
        populateFromIntent();
        setupButtons();
        startStatusListener();
    }

    /**
     * Binds XML views to Java variables
     */
    private void bindViews() {
        tvStatusBadge       = findViewById(R.id.tvStatusBadge);
        tvStatusDescription = findViewById(R.id.tvStatusDescription);
        tvEventName         = findViewById(R.id.tvStatusEventName);
        tvPaymentMethod     = findViewById(R.id.tvStatusPaymentMethod);
        tvAmount            = findViewById(R.id.tvStatusAmount);
        cardStatus          = findViewById(R.id.cardStatusMain);
        progressBar         = findViewById(R.id.progressBarStatus);
        btnGoHome           = findViewById(R.id.btnGoHome);
        btnViewMyPayments   = findViewById(R.id.btnViewMyPayments);
        btnViewTickets      = findViewById(R.id.btnViewTickets);
        tvRejectionReason   = findViewById(R.id.tvRejectionReason);
        cardRejection       = findViewById(R.id.cardRejection);
    }

    /**
     * Reads payment details passed via Intent and updates UI
     */
    private void populateFromIntent() {
        Intent in = getIntent();

        paymentId        = in.getStringExtra(KEY_PAYMENT_ID);
        String status    = in.getStringExtra(KEY_STATUS);
        String method    = in.getStringExtra(KEY_METHOD);
        String eventName = in.getStringExtra(KEY_EVENT_NAME);
        double amount    = in.getDoubleExtra(KEY_AMOUNT, 0.0);

        // Set event name
        if (tvEventName != null && eventName != null)
            tvEventName.setText(eventName);

        // Set payment method
        if (tvPaymentMethod != null && method != null)
            tvPaymentMethod.setText(getMethodLabel(method));

        // Set amount display
        if (tvAmount != null) {
            tvAmount.setText(amount > 0
                    ? "PKR " + NumberFormat.getInstance().format((long) amount)
                    : "Free");
        }

        // Apply initial status UI
        if (status != null)
            applyStatusUI(status, null);
    }

    /**
     * Sets up button click listeners (navigation actions)
     */
    private void setupButtons() {

        // Go to home screen
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            });
        }

        // View all payments
        if (btnViewMyPayments != null) {
            btnViewMyPayments.setOnClickListener(v ->
                    startActivity(new Intent(this, MyPaymentsActivity.class)));
        }

        // View tickets screen
        if (btnViewTickets != null) {
            btnViewTickets.setOnClickListener(v ->
                    startActivity(new Intent(this, TicketsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        }

        // Back button
        View btnBack = findViewById(R.id.btnStatusBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Starts Firestore listener for real-time payment status updates
     */
    private void startStatusListener() {

        if (paymentId == null || paymentId.isEmpty()) return;

        if (progressBar != null)
            progressBar.setVisibility(View.VISIBLE);

        statusListener = db.collection("payments").document(paymentId)
                .addSnapshotListener((snap, error) -> {

                    if (progressBar != null)
                        progressBar.setVisibility(View.GONE);

                    if (error != null || snap == null || !snap.exists())
                        return;

                    // Update UI whenever status changes
                    applyStatusUI(
                            snap.getString("status"),
                            snap.getString("rejectionReason")
                    );
                });
    }

    /**
     * Applies UI based on payment status
     */
    private void applyStatusUI(String status, String rejectionReason) {

        if (status == null) return;

        // Hide rejection card by default
        if (cardRejection != null)
            cardRejection.setVisibility(View.GONE);

        switch (status) {

            case Payment.STATUS_REGISTERED:
                setStatus("🎉 You're Registered!",
                        "Your registration is confirmed. Please bring your student ID and pay cash at the entrance.",
                        R.color.admin_success, R.color.admin_success_bg);
                showTicketsButton(true);
                break;

            case Payment.STATUS_VERIFICATION_PENDING:
                setStatus("⏳ Awaiting Verification",
                        "Your payment screenshot has been submitted. The Event Manager will review it shortly.",
                        R.color.admin_warning, R.color.admin_warning_bg);
                showTicketsButton(false);
                break;

            case Payment.STATUS_APPROVED:
                setStatus("✅ Payment Approved — You're In!",
                        "Your payment has been verified. You're officially registered for this event! 🎊",
                        R.color.admin_success, R.color.admin_success_bg);
                showTicketsButton(true);
                break;

            case Payment.STATUS_REJECTED:
                setStatus("❌ Payment Rejected",
                        "Your payment was not verified. Please contact the Event Manager or try again.",
                        R.color.admin_error, R.color.admin_error_bg);
                showTicketsButton(false);

                // Show rejection reason if available
                if (rejectionReason != null && !rejectionReason.isEmpty() && cardRejection != null) {
                    cardRejection.setVisibility(View.VISIBLE);
                    if (tvRejectionReason != null)
                        tvRejectionReason.setText(rejectionReason);
                }
                break;

            case Payment.STATUS_PENDING_CASH:
                setStatus("💵 Cash Payment Pending",
                        "Please pay at the event entrance. Bring your student ID.",
                        R.color.admin_info, R.color.admin_info_bg);
                showTicketsButton(false);
                break;

            default:
                setStatus(status, "", R.color.text_grey, R.color.admin_surface_2);
                showTicketsButton(false);
        }
    }

    /**
     * Shows or hides tickets button
     */
    private void showTicketsButton(boolean show) {
        if (btnViewTickets != null)
            btnViewTickets.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Updates status badge UI (text, color, background)
     */
    private void setStatus(String label, String description, int textColorRes, int bgColorRes) {

        if (tvStatusBadge != null) {
            tvStatusBadge.setText(label);

            try {
                tvStatusBadge.setTextColor(getResources().getColor(textColorRes, getTheme()));
            } catch (Exception ignored) {}
        }

        if (tvStatusDescription != null)
            tvStatusDescription.setText(description);

        if (cardStatus != null) {
            try {
                cardStatus.setCardBackgroundColor(getResources().getColor(bgColorRes, getTheme()));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Converts payment method code to readable label
     */
    private String getMethodLabel(String method) {
        if (method == null) return "Unknown";

        switch (method) {
            case Payment.METHOD_JAZZCASH:  return "JazzCash";
            case Payment.METHOD_EASYPAISA: return "Easypaisa";
            case Payment.METHOD_CASH:      return "Cash on Event";
            default:                       return method;
        }
    }

    /**
     * Removes Firestore listener when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null)
            statusListener.remove();
    }
}