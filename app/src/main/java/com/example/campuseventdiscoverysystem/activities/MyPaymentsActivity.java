package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.StudentPaymentAdapter;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * MyPaymentsActivity
 * ============================================================
 *
 * PURPOSE:
 * Displays a student's complete payment history in real-time.
 *
 * FEATURES:
 * - Shows all payments made by logged-in student
 * - Live updates using Firestore snapshot listener
 * - Displays payment status (pending, completed, failed, etc.)
 * - Navigates to PaymentStatusActivity on item click
 * - Handles empty state UI
 *
 * FIRESTORE:
 * - payments collection filtered by studentId
 *
 * USER ROLE:
 * Student
 */
public class MyPaymentsActivity extends AppCompatActivity {

    // RecyclerView for payment list
    private RecyclerView rvPayments;

    // Loading indicator
    private ProgressBar progressBar;

    // Empty state view (no payments)
    private View tvEmpty;

    // Local dataset for RecyclerView
    private final List<Payment> paymentList = new ArrayList<>();

    // Adapter for binding payment data
    private StudentPaymentAdapter adapter;

    // Firestore instance
    private FirebaseFirestore db;

    // Listener reference for cleanup
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_payments);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // UI bindings
        rvPayments  = findViewById(R.id.rvMyPayments);
        progressBar = findViewById(R.id.progressBarMyPayments);
        tvEmpty     = findViewById(R.id.tvEmptyPayments);

        // Back button (safe null check for layout flexibility)
        View btnBack = findViewById(R.id.btnMyPaymentsBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView + Adapter
        setupRecyclerView();

        // Start real-time Firestore listener
        startListener();
    }

    /**
     * Initializes RecyclerView and handles item click navigation
     * to PaymentStatusActivity for detailed view.
     */
    private void setupRecyclerView() {

        adapter = new StudentPaymentAdapter(paymentList, payment -> {

            // Navigate to payment details screen
            Intent intent = new Intent(this, PaymentStatusActivity.class);

            intent.putExtra(PaymentStatusActivity.KEY_PAYMENT_ID, payment.getPaymentId());
            intent.putExtra(PaymentStatusActivity.KEY_STATUS,     payment.getStatus());
            intent.putExtra(PaymentStatusActivity.KEY_METHOD,     payment.getPaymentMethod());
            intent.putExtra(PaymentStatusActivity.KEY_EVENT_NAME, payment.getEventName());
            intent.putExtra(PaymentStatusActivity.KEY_AMOUNT,     payment.getAmount());

            startActivity(intent);
        });

        rvPayments.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(adapter);
    }

    /**
     * Starts Firestore real-time listener for student's payments.
     *
     * Flow:
     * 1. Get current user
     * 2. Query payments where studentId == userId
     * 3. Sort by latest transaction
     * 4. Update UI in real time
     */
    private void startListener() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Show loading spinner while fetching data
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("payments")

                // Only fetch payments belonging to current student
                .whereEqualTo("studentId", user.getUid())

                // Show newest payments first
                .orderBy("timestamp", Query.Direction.DESCENDING)

                // Real-time updates
                .addSnapshotListener((query, error) -> {

                    // Hide loading spinner once data is received
                    if (progressBar != null) progressBar.setVisibility(View.GONE);

                    if (error != null || query == null) return;

                    // Clear old data before reloading
                    paymentList.clear();

                    // Convert Firestore documents to Payment objects
                    for (DocumentSnapshot doc : query.getDocuments()) {

                        Payment p = doc.toObject(Payment.class);

                        if (p != null) {
                            p.setPaymentId(doc.getId());
                            paymentList.add(p);
                        }
                    }

                    // Refresh RecyclerView UI
                    adapter.notifyDataSetChanged();

                    // Handle empty state visibility
                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(
                                paymentList.isEmpty() ? View.VISIBLE : View.GONE
                        );
                    }
                });
    }

    /**
     * Cleanup Firestore listener to prevent memory leaks
     * when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}