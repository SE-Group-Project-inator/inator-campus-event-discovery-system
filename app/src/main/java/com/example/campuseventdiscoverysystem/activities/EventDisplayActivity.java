package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ============================================================
 * EventDisplayActivity
 * ============================================================
 *
 * PURPOSE:
 * Read-only event details screen used for:
 * - Viewing past events (EventHistoryActivity)
 * - Viewing event details (EventManagerDashboard)
 *
 * FEATURES:
 * - No RSVP / no actions (display-only)
 * - Supports Student + Manager view modes
 * - Loads event data from:
 *      1. Intent (student path)
 *      2. Firestore (manager path)
 * - Displays capacity, date, time, organizer info
 * - Collapsible "About" section
 *
 * FIRESTORE:
 * - events/{eventId}
 *
 * MODES:
 * - Student mode → Intent-based data
 * - Manager mode → Firestore live fetch
 */
public class EventDisplayActivity extends AppCompatActivity {

    // Controls expand/collapse state of description section
    private boolean aboutExpanded = true;

    // Event ID used in manager mode (Firestore fetch)
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_display);

        // Setup UI interactions (back button + toggle)
        setupButtons();

        // Get event ID and user role from Intent
        eventID = getIntent().getStringExtra("EVENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");

        // ---------------- ROLE-BASED FLOW ----------------
        if ("manager".equals(userRole) && eventID != null) {

            // Manager view: apply admin theme color
            LinearLayout heroHeader = findViewById(R.id.heroHeader);
            heroHeader.setBackgroundColor(getColor(R.color.btn_eventmgr));

            // Fetch latest event data from Firestore
            fetchEventFromDatabase();

        } else {
            // Student / default view: use Intent data
            populateDetails();
        }
    }

    /**
     * ============================================================
     * MANAGER PATH
     * ============================================================
     *
     * Fetches event directly from Firestore for admin view.
     */
    private void fetchEventFromDatabase() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventID).get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        // ---------------- BASIC INFO ----------------
                        ((TextView) findViewById(R.id.tvTitle))
                                .setText(doc.getString("title"));

                        ((TextView) findViewById(R.id.tvDescription))
                                .setText(doc.getString("description"));

                        // ---------------- VENUE ----------------
                        String venue = doc.getString("venue");
                        ((TextView) findViewById(R.id.tvHeaderVenue))
                                .setText(venue != null ? venue : "TBD");

                        ((TextView) findViewById(R.id.tvVenue))
                                .setText(venue != null ? venue : "TBD");

                        // ---------------- ORGANIZER ----------------
                        String society = doc.getString("societyName");

                        ((TextView) findViewById(R.id.tvOrganizer))
                                .setText(society != null ? society : "Unknown");

                        ((TextView) findViewById(R.id.tvOrganizerEmail))
                                .setText(doc.getString("submittedByEmail"));

                        // ---------------- TIME ----------------
                        String startTime = doc.getString("startTime");
                        String endTime   = doc.getString("endTime");

                        if (startTime != null && endTime != null) {
                            ((TextView) findViewById(R.id.tvTime))
                                    .setText(startTime + " - " + endTime);
                        } else {
                            ((TextView) findViewById(R.id.tvTime))
                                    .setText("Time TBD");
                        }

                        // ---------------- DATE ----------------
                        Timestamp ts = doc.getTimestamp("date");

                        if (ts != null) {
                            String dateStr = new SimpleDateFormat(
                                    "EEEE, d MMMM yyyy", Locale.US
                            ).format(ts.toDate());

                            ((TextView) findViewById(R.id.tvHeaderDate))
                                    .setText(dateStr);

                            ((TextView) findViewById(R.id.tvDate))
                                    .setText(dateStr);
                        } else {
                            ((TextView) findViewById(R.id.tvHeaderDate))
                                    .setText("Date TBD");

                            ((TextView) findViewById(R.id.tvDate))
                                    .setText("Date TBD");
                        }

                        // ---------------- CAPACITY ----------------
                        Long capacityLong = doc.getLong("capacity");

                        if (capacityLong != null) {

                            int capacity = capacityLong.intValue();

                            // Temporary placeholder until RSVP system is fully linked
                            int registered = 0;

                            ((TextView) findViewById(R.id.tvCapacity))
                                    .setText(registered + " / " + capacity + " spots taken");

                            if (capacity > 0) {

                                int pct = (int) ((registered / (float) capacity) * 100);

                                ((ProgressBar) findViewById(R.id.progressAvailability))
                                        .setProgress(pct);

                                ((TextView) findViewById(R.id.tvAvailabilityLabel))
                                        .setText(pct + "% Full · " + (capacity - registered) + " spots left");

                                // Highlight if event is filling fast
                                if (pct >= 75) {
                                    findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load event data",
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * ============================================================
     * STUDENT PATH
     * ============================================================
     *
     * Populates UI using Intent extras (offline/local data).
     */
    private void populateDetails() {

        Intent in = getIntent();

        // Event data passed from previous screen
        String title       = in.getStringExtra("eventTitle");
        String description = in.getStringExtra("eventDescription");
        String venue       = in.getStringExtra("eventVenue");
        String organizer   = in.getStringExtra("eventOrganizerName");
        String orgEmail    = in.getStringExtra("eventOrganizerEmail");
        int    capacity    = in.getIntExtra("eventCapacity", 0);
        int    registered  = in.getIntExtra("eventRegistered", 0);
        long   dateMillis  = in.getLongExtra("eventDateMillis", 0);

        // ---------------- BASIC INFO ----------------
        ((TextView) findViewById(R.id.tvTitle))
                .setText(title != null ? title : "Event");

        ((TextView) findViewById(R.id.tvHeaderVenue))
                .setText(venue != null ? venue : "TBD");

        ((TextView) findViewById(R.id.tvOrganizer))
                .setText(organizer != null ? organizer : "Unknown");

        // Organizer email (optional field)
        TextView tvOrgEmail = findViewById(R.id.tvOrganizerEmail);
        if (orgEmail != null && !orgEmail.isEmpty()) {
            tvOrgEmail.setText(orgEmail);
        }

        ((TextView) findViewById(R.id.tvVenue))
                .setText(venue != null ? venue : "TBD");

        // ---------------- DATE & TIME ----------------
        if (dateMillis > 0) {

            Date d = new Date(dateMillis);

            String dateStr = new SimpleDateFormat(
                    "EEEE, d MMMM yyyy", Locale.US
            ).format(d);

            String timeStr = new SimpleDateFormat(
                    "h:mm a", Locale.US
            ).format(d);

            ((TextView) findViewById(R.id.tvHeaderDate))
                    .setText(dateStr);

            ((TextView) findViewById(R.id.tvDate))
                    .setText(dateStr);

            ((TextView) findViewById(R.id.tvTime))
                    .setText(timeStr);

        } else {
            ((TextView) findViewById(R.id.tvHeaderDate))
                    .setText("Date TBD");

            ((TextView) findViewById(R.id.tvDate))
                    .setText("TBD");

            ((TextView) findViewById(R.id.tvTime))
                    .setText("TBD");
        }

        // ---------------- CAPACITY ----------------
        if (capacity > 0) {

            ((TextView) findViewById(R.id.tvCapacity))
                    .setText(registered + " / " + capacity + " spots taken");

            int pct = (int) ((registered / (float) capacity) * 100);

            ((ProgressBar) findViewById(R.id.progressAvailability))
                    .setProgress(pct);

            ((TextView) findViewById(R.id.tvAvailabilityLabel))
                    .setText(pct + "% Full · " + (capacity - registered) + " spots left");

            if (pct >= 75) {
                findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
            }

        } else {
            ((TextView) findViewById(R.id.tvCapacity))
                    .setText(registered + " registered");
        }

        // ---------------- DESCRIPTION ----------------
        ((TextView) findViewById(R.id.tvDescription))
                .setText((description != null && !description.isEmpty())
                        ? description : "No description provided.");
    }

    /**
     * Handles UI interactions:
     * - Back navigation
     * - Expand/collapse description section
     */
    private void setupButtons() {

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Toggle About section
        LinearLayout aboutToggle = findViewById(R.id.layoutAboutToggle);

        TextView tvDesc  = findViewById(R.id.tvDescription);
        TextView tvArrow = findViewById(R.id.tvAboutArrow);

        aboutToggle.setOnClickListener(v -> {

            if (aboutExpanded) {
                tvDesc.setVisibility(View.GONE);
                tvArrow.setText("▼");
                aboutExpanded = false;
            } else {
                tvDesc.setVisibility(View.VISIBLE);
                tvArrow.setText("▲");
                aboutExpanded = true;
            }
        });
    }
}