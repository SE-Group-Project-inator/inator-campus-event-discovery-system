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
 * PaymentStatusActivity
 *
 * Shows a student the live status of their payment request.
 * Uses a Firestore real-time listener so the status badge updates
 * automatically when the Event Manager approves or rejects it.
 *
 * Called from:
 *  • PaymentActivity (immediately after submission)
 *  • EventHistoryActivity (when student taps a registered event)
 */
public class PaymentStatusActivity extends AppCompatActivity {

    // ── Intent keys ────────────────────────────────────────────────────
    public static final String KEY_PAYMENT_ID  = "paymentId";
    public static final String KEY_STATUS      = "status";
    public static final String KEY_METHOD      = "method";
    public static final String KEY_EVENT_NAME  = "eventName";
    public static final String KEY_AMOUNT      = "amount";

    // ── UI ─────────────────────────────────────────────────────────────
    private TextView tvStatusBadge, tvStatusDescription;
    private TextView tvEventName, tvPaymentMethod, tvAmount;
    private CardView cardStatus;
    private ProgressBar progressBar;
    private MaterialButton btnGoHome, btnViewMyPayments;
    private TextView tvRejectionReason;
    private CardView cardRejection;

    // ── Firebase ───────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private ListenerRegistration statusListener;
    private String paymentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_status);

        db = FirebaseFirestore.getInstance();

        bindViews();
        populateFromIntent();
        setupButtons();
        startStatusListener();
    }

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
        tvRejectionReason   = findViewById(R.id.tvRejectionReason);
        cardRejection       = findViewById(R.id.cardRejection);
    }

    private void populateFromIntent() {
        Intent in = getIntent();
        paymentId = in.getStringExtra(KEY_PAYMENT_ID);
        String status    = in.getStringExtra(KEY_STATUS);
        String method    = in.getStringExtra(KEY_METHOD);
        String eventName = in.getStringExtra(KEY_EVENT_NAME);
        double amount    = in.getDoubleExtra(KEY_AMOUNT, 0.0);

        if (tvEventName != null && eventName != null) tvEventName.setText(eventName);
        if (tvPaymentMethod != null && method != null) {
            tvPaymentMethod.setText(getMethodLabel(method));
        }
        if (tvAmount != null) {
            tvAmount.setText(amount > 0
                    ? "PKR " + NumberFormat.getInstance().format((long) amount)
                    : "Free");
        }

        // Show initial status from intent before listener kicks in
        if (status != null) applyStatusUI(status, null);
    }

    private void setupButtons() {
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                startActivity(new Intent(this, StudentHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            });
        }
        if (btnViewMyPayments != null) {
            btnViewMyPayments.setOnClickListener(v -> {
                startActivity(new Intent(this, MyPaymentsActivity.class));
            });
        }
        View btnBack = findViewById(R.id.btnStatusBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Real-time Firestore listener — updates status badge without requiring a reload.
     */
    private void startStatusListener() {
        if (paymentId == null || paymentId.isEmpty()) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        statusListener = db.collection("payments").document(paymentId)
                .addSnapshotListener((snap, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (error != null || snap == null || !snap.exists()) return;

                    String status          = snap.getString("status");
                    String rejectionReason = snap.getString("rejectionReason");

                    applyStatusUI(status, rejectionReason);
                });
    }

    private void applyStatusUI(String status, String rejectionReason) {
        if (status == null) return;

        switch (status) {
            case Payment.STATUS_VERIFICATION_PENDING:
                setStatus("⏳ Pending Verification",
                        "Your payment screenshot has been submitted. " +
                                "The Event Manager will review it shortly.",
                        R.color.admin_warning,
                        R.color.admin_warning_bg);
                break;

            case Payment.STATUS_APPROVED:
                setStatus("✅ Payment Approved",
                        "Your payment has been verified. You're all set for the event!",
                        R.color.admin_success,
                        R.color.admin_success_bg);
                break;

            case Payment.STATUS_REJECTED:
                setStatus("❌ Payment Rejected",
                        "Your payment was not verified. Please contact the Event Manager.",
                        R.color.admin_error,
                        R.color.admin_error_bg);
                // Show rejection reason if available
                if (rejectionReason != null && !rejectionReason.isEmpty()
                        && cardRejection != null) {
                    cardRejection.setVisibility(View.VISIBLE);
                    if (tvRejectionReason != null) tvRejectionReason.setText(rejectionReason);
                }
                break;

            case Payment.STATUS_PENDING_CASH:
                setStatus("💵 Cash Payment Pending",
                        "Please pay at the event entrance. Bring your student ID.",
                        R.color.admin_info,
                        R.color.admin_info_bg);
                break;

            default:
                setStatus(status, "", R.color.text_grey, R.color.admin_surface_2);
        }
    }

    private void setStatus(String label, String description, int textColorRes, int bgColorRes) {
        if (tvStatusBadge != null) {
            tvStatusBadge.setText(label);
            try { tvStatusBadge.setTextColor(getResources().getColor(textColorRes, getTheme())); }
            catch (Exception ignored) {}
        }
        if (tvStatusDescription != null) tvStatusDescription.setText(description);
        if (cardStatus != null) {
            try { cardStatus.setCardBackgroundColor(getResources().getColor(bgColorRes, getTheme())); }
            catch (Exception ignored) {}
        }
    }

    private String getMethodLabel(String method) {
        if (method == null) return "Unknown";
        switch (method) {
            case Payment.METHOD_JAZZCASH:  return "JazzCash";
            case Payment.METHOD_EASYPAISA: return "Easypaisa";
            case Payment.METHOD_CASH:      return "Cash on Event";
            default:                       return method;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) statusListener.remove();
    }
}
