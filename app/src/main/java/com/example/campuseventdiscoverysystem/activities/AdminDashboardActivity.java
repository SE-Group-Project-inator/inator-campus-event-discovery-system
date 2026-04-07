package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.PendingEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * AdminDashboardActivity provides the main interface for administrators.
 * It allows admins to monitor pending event approvals, view system stats,
 * and navigate to the full events database.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvPendingCount, tvApprovedCount;
    private final List<Event> pendingList = new ArrayList<>();
    private PendingEventAdapter adapter;
    private final List<Event> allPendingList = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);

        setupRecyclerView();
        setupNavigation();
        setupFilters();
        loadStats();
        listenToPendingEvents();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvPendingEvents);
        adapter = new PendingEventAdapter(
                pendingList,
                eventId -> updateEventStatus(eventId, "active"),
                eventId -> updateEventStatus(eventId, "rejected")
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupNavigation() {
        // Sign out button
        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // "See All" navigation to EventsListActivity
        findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));

        // Navigation bar - Events button
        findViewById(R.id.navEvents).setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));
    }

    private void setupFilters() {
        // Implementation for chips if they exist in your layout
        try {
            findViewById(R.id.chipAll).setOnClickListener(v -> {
                currentFilter = "all";
                applyFilter();
            });
            findViewById(R.id.chipUrgent).setOnClickListener(v -> {
                currentFilter = "urgent";
                applyFilter();
            });
        } catch (Exception e) {
            // Chips might not be in the current layout version
        }
    }

    private void applyFilter() {
        List<Event> filtered = new ArrayList<>();
        Date today = new Date();

        if (currentFilter.equals("urgent")) {
            for (Event e : allPendingList) {
                if (e.getDate() != null) {
                    Date eventDate = e.getDate().toDate();
                    long diffMs = eventDate.getTime() - today.getTime();
                    long diffDays = diffMs / (1000 * 60 * 60 * 24);
                    if (diffDays <= 7 && diffDays >= 0) {
                        filtered.add(e);
                    }
                }
            }
        } else {
            filtered.addAll(allPendingList);
        }

        pendingList.clear();
        pendingList.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadStats() {
        db.collection("events").whereEqualTo("status", "pending_approval")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) tvPendingCount.setText(String.valueOf(snap.size()));
                });

        db.collection("events").whereEqualTo("status", "active")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) tvApprovedCount.setText(String.valueOf(snap.size()));
                });
    }

    private void listenToPendingEvents() {
        db.collection("events")
                .whereEqualTo("status", "pending_approval")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allPendingList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            allPendingList.add(event);
                        }
                    }
                    applyFilter();
                });
    }

    private void updateEventStatus(String eventId, String status) {
        db.collection("events").document(eventId)
                .update("status", status)
                .addOnSuccessListener(v -> {
                    String msg = status.equals("active") ? "✅ Approved!" : "❌ Declined";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
    }
}