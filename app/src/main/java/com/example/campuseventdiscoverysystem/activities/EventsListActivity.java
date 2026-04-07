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

/**
 * Activity class that displays a comprehensive list of all events in the system.
 * It provides administrative controls to filter events by their approval status
 * (Approved vs. Pending) using interactive CardView statistics.
 */
public class EventsListActivity extends AppCompatActivity {

    /** The Firestore database instance for retrieving and updating event data. */
    private FirebaseFirestore db;
    /** The master list containing every event retrieved from the database. */
    private final List<Event> allEvents = new ArrayList<>();
    /** The subset of {@code allEvents} currently being displayed in the UI based on active filters. */
    private final List<Event> displayList = new ArrayList<>();
    /** The adapter used to manage and display event items in the RecyclerView. */
    private PendingEventAdapter adapter;
    /** TextViews used to display the numeric count of total and approved events. */
    private TextView tvTotalCount, tvApprovedCount;
    /** Tracks the current UI filter state. Possible values: "all", "approved", "pending". */
    private String currentCardFilter = "all"; // "all", "approved", "pending"

    /**
     * Initializes the activity, sets up the RecyclerView, and attaches event listeners
     * to navigation and filter components.
     * @param savedInstanceState A mapping from String keys to various Parcelable values
     * if the activity is re-initialized.
     */
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

    /**
     * Configures click listeners for the dashboard-style CardViews.
     * Clicking these cards filters the event list to show only pending or only approved events,
     * while providing visual feedback via alpha (transparency) changes.
     */
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

    /**
     * Logic to filter the {@code allEvents} master list into the {@code displayList}.
     * This method is called whenever the user changes a filter or the underlying data updates.
     */
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

    /**
     * Establishes a real-time SnapshotListener on the Firestore "events" collection.
     * The list is ordered chronologically by date. This method also recalculates
     * the total and approved counts for the stat header.
     */
    private void loadAllEvents() {
        db.collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
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

    /**
     * Updates the status of a specific event document in Firestore.
     * @param eventId The unique identifier of the event document to update.
     * @param status The new status string to apply (e.g., "active" or "rejected").
     */
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