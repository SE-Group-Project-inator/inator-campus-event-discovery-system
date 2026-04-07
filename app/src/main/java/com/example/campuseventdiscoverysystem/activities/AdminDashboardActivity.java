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
 * Activity class that serves as the administrative dashboard for the Campus Event Discovery System.
 * This dashboard allows administrators to review pending event requests, view system statistics,
 * and filter events based on urgency or recency.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    /** Instance of Firestore database for data operations. */
    private FirebaseFirestore db;
    /** Instance of Firebase Auth for managing admin sessions. */
    private FirebaseAuth mAuth;
    /** UI components to display the count of pending and approved events. */
    private TextView tvPendingCount, tvApprovedCount;
    /** The list currently displayed in the RecyclerView. */
    private final List<Event> pendingList = new ArrayList<>();
    /** Adapter for the pending events RecyclerView. */
    private PendingEventAdapter adapter;
    /** Master list of all pending events retrieved from the database. */
    private final List<Event> allPendingList = new ArrayList<>();
    /** The currently selected filter mode: "all", "urgent", or "newest". */
    private String currentFilter = "all";

    /**
     * Initializes the activity, sets up Firebase instances, and triggers UI initialization.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * this contains the data it most recently supplied.
     */
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

    /**
     * Configures the RecyclerView used to display pending events.
     * Sets up the layout manager and initializes the adapter with callback logic
     * for event approval and rejection.
     */
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

    /**
     * Sets up click listeners for navigation elements, including sign out
     * and redirection to the full events list.
     */
    private void setupNavigation() {
        // Sign out
        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // See all → Events List
        findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));

        // Events nav button
        findViewById(R.id.navEvents).setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));
    }

    /**
     * Initializes the Material Chips used for filtering the pending events list.
     */
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

    /**
     * Filters and sorts the {@code allPendingList} based on the {@code currentFilter} value.
     * - "urgent": Events occurring within the next 7 days.
     * - "newest": Events sorted by date in descending order.
     * - "all": Displays all events without specific sorting/filtering.
     */
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

    /**
     * Fetches current statistics from Firestore, such as the total count of
     * pending and active events, and updates the UI accordingly.
     */
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

    /**
     * Sets up a real-time Firestore listener to monitor changes in events with "pending_approval" status.
     * Automatically updates the UI when events are added or modified.
     */
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

    /**
     * Updates the status of a specific event in Firestore.
     * @param eventId The unique document ID of the event.
     * @param status The new status to apply (e.g., "active" for approval, "rejected" for denial).
     */
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