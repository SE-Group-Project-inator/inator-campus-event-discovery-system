package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * EventAnalyticsActivity
 * Activity responsible for displaying real-time metrics and analytics for a specific event.
 * It tracks total capacity, registered attendees, waitlisted users, and the live check-in rate.
 */
public class EventAnalyticsActivity extends AppCompatActivity {

    private static final String TAG = "EventAnalyticsActivity";

    // Firebase instance for database operations
    private FirebaseFirestore db;

    // ID of the event whose analytics are being viewed
    private String eventID;

    // UI Components
    private TextView tvAnalyticsTitle, tvTotalCapacity, tvTotalRegistered;
    private TextView tvTotalWaitlist, tvCheckInRate;
    private ProgressBar progressCapacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind activity to its corresponding XML layout file
        setContentView(R.layout.activity_event_analytics);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();

        // Retrieve eventID passed from the previous screen
        eventID = getIntent().getStringExtra("EVENT_ID");
        if (eventID == null || eventID.isEmpty()) {
            Toast.makeText(this, "Error: Event ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link variables to XML views
        bindViews();

        // Setup routing and event listeners
        setupNavigation();

        // Fetch data and calculate metrics
        loadAnalytics();
    }

    /**
     * Maps all the XML UI components to Java variables.
     */
    private void bindViews() {
        tvAnalyticsTitle = findViewById(R.id.tvAnalyticsTitle);
        tvTotalCapacity = findViewById(R.id.tvTotalCapacity);
        tvTotalRegistered = findViewById(R.id.tvTotalRegistered);
        tvTotalWaitlist = findViewById(R.id.tvTotalWaitlist);
        tvCheckInRate = findViewById(R.id.tvCheckInRate);
        progressCapacity = findViewById(R.id.progressCapacity);
    }

    /**
     * Handles routing for the top navigation bar (Back button).
     */
    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Fetches event capacity and RSVP data from Firestore to calculate and display
     * real-time metrics (Capacity Utilization and Check-In Rate).
     */
    private void loadAnalytics() {
        // Step 1: Fetch the base event document to get the title and maximum capacity
        db.collection("events").document(eventID).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Set Header Title
                        String eventTitle = doc.getString("title");
                        tvAnalyticsTitle.setText(eventTitle + " Analytics");

                        // Extract Capacity safely
                        Long cap = doc.getLong("capacity");
                        int capacity = cap != null ? cap.intValue() : 0;
                        tvTotalCapacity.setText(String.valueOf(capacity));

                        // Step 2: Fetch all RSVPs for this event to calculate attendance metrics
                        fetchRsvpMetrics(capacity);
                    } else {
                        Toast.makeText(this, "Event not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch event base details", e);
                    Toast.makeText(this, "Error loading event details.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Queries the top-level RSVPs collection, tallies statuses, and updates the UI percentages.
     *
     * @param capacity The maximum capacity of the event (used for utilization math).
     */
    private void fetchRsvpMetrics(int capacity) {
        db.collection("rsvps")
                .whereEqualTo("eventId", eventID)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int registered = 0;
                    int waitlisted = 0;
                    int checkedIn = 0;

                    // Tally up the data from each RSVP document
                    for (QueryDocumentSnapshot regDoc : snapshots) {
                        String status = regDoc.getString("status");
                        Boolean isCheckedIn = regDoc.getBoolean("isCheckedIn");

                        if ("waitlisted".equals(status)) {
                            waitlisted++;
                        } else if ("confirmed".equals(status)) {
                            registered++;

                            // Count actual physical check-ins if the flag exists and is true
                            if (isCheckedIn != null && isCheckedIn) {
                                checkedIn++;
                            }
                        }
                    }

                    // Write true count back to fix any drift from the event doc field
                    int finalRegistered = registered;
                    db.collection("events").document(eventID).get()
                            .addOnSuccessListener(evDoc -> {
                                Long cap = evDoc.getLong("capacity");
                                int clamped = cap != null && cap > 0
                                        ? Math.min(finalRegistered, cap.intValue()) : finalRegistered;
                                db.collection("events").document(eventID)
                                        .update("registeredCount", Math.max(0, clamped));
                            });
                    // Update Raw Number UIs
                    tvTotalRegistered.setText(String.valueOf(registered));
                    tvTotalWaitlist.setText(String.valueOf(waitlisted));

                    // Calculate Capacity Utilization Percentage
                    if (capacity > 0) {
                        int capPct = (int) (((float) registered / capacity) * 100);
                        progressCapacity.setProgress(capPct);
                    }

                    // Calculate Check-In Rate Percentage (Checked In / Total Confirmed)
                    if (registered > 0) {
                        int checkInPct = (int) (((float) checkedIn / registered) * 100);
                        tvCheckInRate.setText(checkInPct + "%");
                    } else {
                        tvCheckInRate.setText("0%");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch RSVP metrics", e);
                    Toast.makeText(this, "Error calculating analytics.", Toast.LENGTH_SHORT).show();
                });
    }
}