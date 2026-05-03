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

public class PaymentStatusActivity extends AppCompatActivity {

    public static final String KEY_PAYMENT_ID = "paymentId";
    public static final String KEY_STATUS     = "status";
    public static final String KEY_METHOD     = "method";
    public static final String KEY_EVENT_NAME = "eventName";
    public static final String KEY_AMOUNT     = "amount";

    private TextView tvStatusBadge, tvStatusDescription;
    private TextView tvEventName, tvPaymentMethod, tvAmount;
    private CardView cardStatus;
    private ProgressBar progressBar;
    private MaterialButton btnGoHome, btnViewMyPayments, btnViewTickets;
    private TextView tvRejectionReason;
    private CardView cardRejection;

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
        btnViewTickets      = findViewById(R.id.btnViewTickets);
        tvRejectionReason   = findViewById(R.id.tvRejectionReason);
        cardRejection       = findViewById(R.id.cardRejection);
    }

    private void populateFromIntent() {
        Intent in = getIntent();
        paymentId        = in.getStringExtra(KEY_PAYMENT_ID);
        String status    = in.getStringExtra(KEY_STATUS);
        String method    = in.getStringExtra(KEY_METHOD);
        String eventName = in.getStringExtra(KEY_EVENT_NAME);
        double amount    = in.getDoubleExtra(KEY_AMOUNT, 0.0);

        if (tvEventName     != null && eventName != null) tvEventName.setText(eventName);
        if (tvPaymentMethod != null && method    != null) tvPaymentMethod.setText(getMethodLabel(method));
        if (tvAmount != null) {
            tvAmount.setText(amount > 0
                    ? "PKR " + NumberFormat.getInstance().format((long) amount)
                    : "Free");
        }
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
            btnViewMyPayments.setOnClickListener(v ->
                    startActivity(new Intent(this, MyPaymentsActivity.class)));
        }
        if (btnViewTickets != null) {
            btnViewTickets.setOnClickListener(v ->
                    startActivity(new Intent(this, TicketsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        }
        View btnBack = findViewById(R.id.btnStatusBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void startStatusListener() {
        if (paymentId == null || paymentId.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        statusListener = db.collection("payments").document(paymentId)
                .addSnapshotListener((snap, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (error != null || snap == null || !snap.exists()) return;
                    applyStatusUI(snap.getString("status"), snap.getString("rejectionReason"));
                });
    }

    private void applyStatusUI(String status, String rejectionReason) {
        if (status == null) return;
        // Hide rejection card by default
        if (cardRejection != null) cardRejection.setVisibility(View.GONE);

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
                if (rejectionReason != null && !rejectionReason.isEmpty() && cardRejection != null) {
                    cardRejection.setVisibility(View.VISIBLE);
                    if (tvRejectionReason != null) tvRejectionReason.setText(rejectionReason);
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

    private void showTicketsButton(boolean show) {
        if (btnViewTickets != null)
            btnViewTickets.setVisibility(show ? View.VISIBLE : View.GONE);
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
