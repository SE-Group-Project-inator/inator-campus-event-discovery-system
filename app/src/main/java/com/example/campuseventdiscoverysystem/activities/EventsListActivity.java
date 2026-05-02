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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * EventsListActivity — FIXED VERSION
 *
 * Bug fix: Changed one-shot .get() to addSnapshotListener() so the full events list
 * updates in real time when an event manager deletes an event.
 * Previously a deleted event stayed visible until the admin navigated away and back.
 */
public class EventsListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<Event> allEvents    = new ArrayList<>();
    private final List<Event> displayList  = new ArrayList<>();
    private PendingEventAdapter adapter;
    private TextView tvTotalCount, tvApprovedCount;
    private String currentCardFilter = "all";

    private ListenerRegistration allEventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);

        db = FirebaseFirestore.getInstance();
        tvTotalCount    = findViewById(R.id.tvTotalCount);
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
        listenToAllEvents(); // FIX: real-time instead of one-shot
    }

    private void setupCardFilters() {
        CardView cardTotal    = findViewById(R.id.cardTotal);
        CardView cardApproved = findViewById(R.id.cardApproved);

        cardTotal.setOnClickListener(v -> {
            currentCardFilter = "pending";
            applyCardFilter();
            tvTotalCount.setAlpha(1f);
            tvApprovedCount.setAlpha(0.5f);
        });

        cardApproved.setOnClickListener(v -> {
            currentCardFilter = "approved";
            applyCardFilter();
            tvApprovedCount.setAlpha(1f);
            tvTotalCount.setAlpha(0.5f);
        });
    }

    /**
     * FIX: addSnapshotListener fires whenever ANY document in the "events" collection
     * changes, including deletions. So when an event manager deletes an event via
     * EditEventActivity, this list updates automatically without a screen refresh.
     */
    private void listenToAllEvents() {
        allEventsListener = db.collection("events")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading events: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    allEvents.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            allEvents.add(event);
                        }
                    }

                    updateCounters();
                    applyCardFilter();
                });
    }

    private void applyCardFilter() {
        displayList.clear();
        for (Event e : allEvents) {
            switch (currentCardFilter) {
                case "pending":
                    if ("pending_approval".equals(e.getStatus())) displayList.add(e);
                    break;
                case "approved":
                    if ("active".equals(e.getStatus())) displayList.add(e);
                    break;
                default:
                    displayList.add(e);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateCounters() {
        int total    = allEvents.size();
        int approved = 0;
        for (Event e : allEvents) {
            if ("active".equals(e.getStatus())) approved++;
        }
        tvTotalCount.setText(String.valueOf(total));
        tvApprovedCount.setText(String.valueOf(approved));
    }

    private void updateStatus(String eventId, String status) {
        db.collection("events").document(eventId)
                .update("status", status)
                .addOnSuccessListener(v -> {
                    String msg = "active".equals(status) ? "✅ Approved!" : "❌ Declined";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    // Snapshot listener will automatically refresh the list
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (allEventsListener != null) allEventsListener.remove();
    }
}