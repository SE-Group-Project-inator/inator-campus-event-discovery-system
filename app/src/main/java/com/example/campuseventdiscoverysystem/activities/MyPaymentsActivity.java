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
 * MyPaymentsActivity — student's personal payment history/status board.
 *
 * Shows all payment requests made by the current student, with live status badges.
 * Tapping a payment card navigates to PaymentStatusActivity for detail.
 */
public class MyPaymentsActivity extends AppCompatActivity {

    private RecyclerView rvPayments;
    private ProgressBar progressBar;
    private View tvEmpty;

    private final List<Payment> paymentList = new ArrayList<>();
    private StudentPaymentAdapter adapter;

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_payments);

        db = FirebaseFirestore.getInstance();

        rvPayments  = findViewById(R.id.rvMyPayments);
        progressBar = findViewById(R.id.progressBarMyPayments);
        tvEmpty     = findViewById(R.id.tvEmptyPayments);

        View btnBack = findViewById(R.id.btnMyPaymentsBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        startListener();
    }

    private void setupRecyclerView() {
        adapter = new StudentPaymentAdapter(paymentList, payment -> {
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

    private void startListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        listener = db.collection("payments")
                .whereEqualTo("studentId", user.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((query, error) -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (error != null || query == null) return;

                    paymentList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Payment p = doc.toObject(Payment.class);
                        if (p != null) {
                            p.setPaymentId(doc.getId());
                            paymentList.add(p);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (tvEmpty != null) {
                        tvEmpty.setVisibility(paymentList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
