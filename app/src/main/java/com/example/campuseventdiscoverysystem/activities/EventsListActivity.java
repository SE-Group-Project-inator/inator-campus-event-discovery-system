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

public class EventsListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<Event> allEvents   = new ArrayList<>();
    private final List<Event> displayList = new ArrayList<>();
    private PendingEventAdapter adapter;
    private TextView tvTotalCount, tvApprovedCount;
    private String currentFilter = "all";

    // Filter chip views
    private TextView filterAll, filterPending, filterApproved, filterRejected;

    private ListenerRegistration allEventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);

        db = FirebaseFirestore.getInstance();
        tvTotalCount    = findViewById(R.id.tvTotalCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);

        // Filter chips
        filterAll      = findViewById(R.id.filterAll);
        filterPending  = findViewById(R.id.filterPending);
        filterApproved = findViewById(R.id.filterApproved);
        filterRejected = findViewById(R.id.filterRejected);

        RecyclerView rv = findViewById(R.id.rvAllEvents);
        adapter = new PendingEventAdapter(
                displayList,
                eventId -> updateStatus(eventId, "active"),
                eventId -> updateStatus(eventId, "rejected")
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Read filter from intent (sent by quick-action tiles on dashboard)
        String intentFilter = getIntent().getStringExtra("filter");
        if (intentFilter != null) {
            currentFilter = intentFilter;
        }

        setupFilterChips();
        updateChipStates(currentFilter);
        listenToAllEvents();
    }

    private void setupFilterChips() {
        if (filterAll != null) filterAll.setOnClickListener(v -> setFilter("all"));
        if (filterPending != null) filterPending.setOnClickListener(v -> setFilter("pending"));
        if (filterApproved != null) filterApproved.setOnClickListener(v -> setFilter("approved"));
        if (filterRejected != null) filterRejected.setOnClickListener(v -> setFilter("rejected"));
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        applyCardFilter();
        updateChipStates(filter);
    }

    private void updateChipStates(String activeFilter) {
        resetChip(filterAll);
        resetChip(filterPending);
        resetChip(filterApproved);
        resetChip(filterRejected);
        switch (activeFilter) {
            case "all":      activateChip(filterAll);      break;
            case "pending":  activateChip(filterPending);  break;
            case "approved": activateChip(filterApproved); break;
            case "rejected": activateChip(filterRejected); break;
        }
    }

    private void resetChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_inactive);
        chip.setTextColor(getColor(R.color.admin_text_secondary));
    }

    private void activateChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_active);
        chip.setTextColor(getColor(R.color.white));
    }

    private void listenToAllEvents() {
        allEventsListener = db.collection("events")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
            switch (currentFilter) {
                case "pending":
                    if ("pending_approval".equals(e.getStatus())) displayList.add(e);
                    break;
                case "approved":
                    if ("active".equals(e.getStatus())) displayList.add(e);
                    break;
                case "rejected":
                    if ("rejected".equals(e.getStatus())) displayList.add(e);
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
                    String msg = "active".equals(status) ? "✅ Approved!" : "Declined";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (allEventsListener != null) allEventsListener.remove();
    }
}
