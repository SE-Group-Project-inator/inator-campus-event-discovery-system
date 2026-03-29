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
        // Sign out
        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // See all
        findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());
    }
    private void setupFilters() {
        com.google.android.material.chip.Chip chipAll = findViewById(R.id.chipAll);
        com.google.android.material.chip.Chip chipUrgent = findViewById(R.id.chipUrgent);
        com.google.android.material.chip.Chip chipNewest = findViewById(R.id.chipNewest);

        chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            applyFilter();
        });
        chipUrgent.setOnClickListener(v -> {
            currentFilter = "urgent";
            applyFilter();
        });
        chipNewest.setOnClickListener(v -> {
            currentFilter = "newest";
            applyFilter();
        });
    }

    private void applyFilter() {
        List<Event> filtered = new ArrayList<>();
        Date today = new Date();

        switch (currentFilter) {
            case "urgent":
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
                break;
            case "newest":
                filtered.addAll(allPendingList);
                filtered.sort((a, b) -> {
                    if (a.getDate() == null || b.getDate() == null) return 0;
                    return b.getDate().compareTo(a.getDate());
                });
                break;
            default:
                filtered.addAll(allPendingList);
                break;
        }

        pendingList.clear();
        pendingList.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadStats() {
        db.collection("events")
                .whereEqualTo("status", "pending_approval")
                .get()
                .addOnSuccessListener(snap ->
                        tvPendingCount.setText(String.valueOf(snap.size())));

        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap ->
                        tvApprovedCount.setText(String.valueOf(snap.size())));
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
                    tvPendingCount.setText(String.valueOf(allPendingList.size()));
                });
    }

    private void updateEventStatus(String eventId, String status) {
        db.collection("events").document(eventId)
                .update("status", status)
                .addOnSuccessListener(v -> {
                    String msg = status.equals("active")
                            ? "✅ Event Approved!" : "❌ Event Declined";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    loadStats();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}