package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.PendingEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class EventsListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> displayList = new ArrayList<>();
    private PendingEventAdapter adapter;
    private TextView tvTotalCount, tvApprovedCount;
    private String currentCardFilter = "all"; // "all", "approved", "pending"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);

        db = FirebaseFirestore.getInstance();
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);

        RecyclerView rv = findViewById(R.id.rvAllEvents);
        adapter = new PendingEventAdapter(
                displayList,
                eventId -> updateStatus(eventId, "active"),
                eventId -> updateStatus(eventId, "rejected")
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupCardFilters();
        loadAllEvents();
    }

    private void setupCardFilters() {
        CardView cardTotal = findViewById(R.id.cardTotal);
        CardView cardApproved = findViewById(R.id.cardApproved);

        // Click Total card → show pending only
        cardTotal.setOnClickListener(v -> {
            currentCardFilter = "pending";
            applyCardFilter();
            // Visual feedback
            tvTotalCount.setAlpha(1f);
            tvApprovedCount.setAlpha(0.5f);
        });

        // Click Approved card → show approved only
        cardApproved.setOnClickListener(v -> {
            currentCardFilter = "approved";
            applyCardFilter();
            // Visual feedback
            tvApprovedCount.setAlpha(1f);
            tvTotalCount.setAlpha(0.5f);
        });
    }

    private void applyCardFilter() {
        displayList.clear();
        for (Event e : allEvents) {
            if (currentCardFilter.equals("approved")
                    && "active".equals(e.getStatus())) {
                displayList.add(e);
            } else if (currentCardFilter.equals("pending")
                    && "pending_approval".equals(e.getStatus())) {
                displayList.add(e);
            } else if (currentCardFilter.equals("all")) {
                displayList.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadAllEvents() {
        // FIX: Use whereIn to only fetch pending + active events.
        // Previously the query fetched ALL events (no status filter), so "rejected" events
        // remained visible in the list even after declining them — the snapshot listener
        // re-populated allEvents with rejected docs and applyCardFilter() showed them in
        // "all" mode. Now rejected events are excluded at the query level.
        db.collection("events")
                .whereIn("status", java.util.Arrays.asList("pending_approval", "active"))
                .orderBy("status")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    allEvents.clear();
                    int approved = 0;
                    int total = 0;
                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            allEvents.add(event);
                            total++;
                            if ("active".equals(event.getStatus())) approved++;
                        }
                    }
                    applyCardFilter();
                    tvTotalCount.setText(String.valueOf(total));
                    tvApprovedCount.setText(String.valueOf(approved));
                });
    }

    private void updateStatus(String eventId, String status) {
        db.collection("events").document(eventId)
                .update("status", status)
                .addOnSuccessListener(v ->
                        Toast.makeText(this,
                                status.equals("active") ? "✅ Approved!" : "❌ Declined",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}