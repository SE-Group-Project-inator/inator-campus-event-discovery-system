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
 * Read-only event detail screen. No RSVP button.
 * Used from EventHistoryActivity when tapping a past event.
 * Also used by EventManagerDashboard to view full event details.
 */
public class EventDisplayActivity extends AppCompatActivity {

    private boolean aboutExpanded = true;
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_display);

        setupButtons();

        // Check if the dashboard sent an EVENT_ID and a USER_ROLE
        eventID = getIntent().getStringExtra("EVENT_ID");
        String userRole = getIntent().getStringExtra("USER_ROLE");

        if ("manager".equals(userRole) && eventID != null) {
            // Manager Path - Change theme to purple and fetch from Firestore
            LinearLayout heroHeader = findViewById(R.id.heroHeader);
            heroHeader.setBackgroundColor(getColor(R.color.btn_eventmgr));
            fetchEventFromDatabase();
        } else {
            // Student Path - Use existing intent logic
            populateDetails();
        }
    }

    /**
     * Manager Path
     */
    private void fetchEventFromDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("events").document(eventID).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // Populate Text
                ((TextView) findViewById(R.id.tvTitle)).setText(doc.getString("title"));
                ((TextView) findViewById(R.id.tvDescription)).setText(doc.getString("description"));

                // Venue
                String venue = doc.getString("venue");
                ((TextView) findViewById(R.id.tvHeaderVenue)).setText(venue != null ? venue : "TBD");
                ((TextView) findViewById(R.id.tvVenue)).setText(venue != null ? venue : "TBD");

                // Organizer
                String society = doc.getString("societyName");
                ((TextView) findViewById(R.id.tvOrganizer)).setText(society != null ? society : "Unknown");
                ((TextView) findViewById(R.id.tvOrganizerEmail)).setText(doc.getString("submittedByEmail"));

                // Time
                String startTime = doc.getString("startTime");
                String endTime = doc.getString("endTime");
                if (startTime != null && endTime != null) {
                    ((TextView) findViewById(R.id.tvTime)).setText(startTime + " - " + endTime);
                } else {
                    ((TextView) findViewById(R.id.tvTime)).setText("Time TBD");
                }

                // Date
                Timestamp ts = doc.getTimestamp("date");
                if (ts != null) {
                    String dateStr = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(ts.toDate());
                    ((TextView) findViewById(R.id.tvHeaderDate)).setText(dateStr);
                    ((TextView) findViewById(R.id.tvDate)).setText(dateStr);
                } else {
                    ((TextView) findViewById(R.id.tvHeaderDate)).setText("Date TBD");
                    ((TextView) findViewById(R.id.tvDate)).setText("Date TBD");
                }

                // Capacity & Availability
                Long capacityLong = doc.getLong("capacity");
                if (capacityLong != null) {
                    int capacity = capacityLong.intValue();
                    // Hardcoded registered count for now until RSVP logic is built
                    int registered = 0;

                    ((TextView) findViewById(R.id.tvCapacity)).setText(registered + " / " + capacity + " spots taken");

                    if (capacity > 0) {
                        int pct = (int) ((registered / (float) capacity) * 100);
                        ((ProgressBar) findViewById(R.id.progressAvailability)).setProgress(pct);
                        ((TextView) findViewById(R.id.tvAvailabilityLabel))
                                .setText(pct + "% Full · " + (capacity - registered) + " spots left");

                        if (pct >= 75) {
                            findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load event data", Toast.LENGTH_SHORT).show());
    }

    /**
     * Student Path
     */
    private void populateDetails() {
        Intent in = getIntent();

        String title       = in.getStringExtra("eventTitle");
        String description = in.getStringExtra("eventDescription");
        String venue       = in.getStringExtra("eventVenue");
        String organizer   = in.getStringExtra("eventOrganizerName");
        String orgEmail    = in.getStringExtra("eventOrganizerEmail");
        int    capacity    = in.getIntExtra("eventCapacity", 0);
        int    registered  = in.getIntExtra("eventRegistered", 0);
        long   dateMillis  = in.getLongExtra("eventDateMillis", 0);

        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "Event");
        ((TextView) findViewById(R.id.tvHeaderVenue)).setText(venue != null ? venue : "TBD");
        ((TextView) findViewById(R.id.tvOrganizer)).setText(organizer != null ? organizer : "Unknown");

        TextView tvOrgEmail = findViewById(R.id.tvOrganizerEmail);
        if (orgEmail != null && !orgEmail.isEmpty()) tvOrgEmail.setText(orgEmail);

        ((TextView) findViewById(R.id.tvVenue)).setText(venue != null ? venue : "TBD");

        if (dateMillis > 0) {
            Date d = new Date(dateMillis);
            String dateStr = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).format(d);
            String timeStr = new SimpleDateFormat("h:mm a", Locale.US).format(d);
            ((TextView) findViewById(R.id.tvHeaderDate)).setText(dateStr);
            ((TextView) findViewById(R.id.tvDate)).setText(dateStr);
            ((TextView) findViewById(R.id.tvTime)).setText(timeStr);
        } else {
            ((TextView) findViewById(R.id.tvHeaderDate)).setText("Date TBD");
            ((TextView) findViewById(R.id.tvDate)).setText("TBD");
            ((TextView) findViewById(R.id.tvTime)).setText("TBD");
        }

        if (capacity > 0) {
            ((TextView) findViewById(R.id.tvCapacity))
                    .setText(registered + " / " + capacity + " spots taken");
            int pct = (int) ((registered / (float) capacity) * 100);
            ((ProgressBar) findViewById(R.id.progressAvailability)).setProgress(pct);
            ((TextView) findViewById(R.id.tvAvailabilityLabel))
                    .setText(pct + "% Full · " + (capacity - registered) + " spots left");
            if (pct >= 75) findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
        } else {
            ((TextView) findViewById(R.id.tvCapacity)).setText(registered + " registered");
        }

        ((TextView) findViewById(R.id.tvDescription)).setText(
                (description != null && !description.isEmpty())
                        ? description : "No description provided.");
    }

    private void setupButtons() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

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