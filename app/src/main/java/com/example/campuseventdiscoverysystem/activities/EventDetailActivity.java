package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.activities.PaymentActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ============================================================
 * EventDetailActivity
 * ============================================================
 *
 * PURPOSE:
 * Displays full details of a selected event including:
 * - Title, description, venue
 * - Organizer information
 * - Capacity & registration status
 * - Date & time formatting
 * - Ticket pricing (paid/free events)
 *
 * FEATURES:
 * - Expand/collapse event description ("About" section)
 * - Live attendee count from Firestore
 * - See attendees (read-only mode)
 * - RSVP / Payment flow integration
 * - Waitlist support when event is full
 *
 * FIRESTORE:
 * - events collection (event metadata)
 * - rsvps collection (attendance tracking)
 *
 * USER FLOW:
 * EventDetail → RSVP/Payment → AttendeeList
 */
public class EventDetailActivity extends AppCompatActivity {

    /**
     * Loads number of confirmed attendees for this event.
     * Reads from rsvps collection where status = "confirmed".
     */
    private void loadAttendeeCount() {
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) return;

        FirebaseFirestore.getInstance()
                .collection("rsvps")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(snap -> {
                    int count = snap.size();

                    // Updates UI with live attendee count
                    TextView tvAttendeeCount = findViewById(R.id.tvAttendeeCount);
                    if (tvAttendeeCount != null) {
                        tvAttendeeCount.setText(count + " attending");
                    }
                });
    }

    // Tracks whether "About" section is expanded or collapsed
    private boolean aboutExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Populate UI with event data passed via Intent
        populateDetails();

        // Setup all button click actions (RSVP, attendees, back, etc.)
        setupButtons();

        // Load live attendee count from Firestore
        loadAttendeeCount();
    }

    /**
     * Reads event data from Intent and binds it to UI components.
     * Handles null safety and fallback values.
     */
    private void populateDetails() {

        Intent in = getIntent();

        // Event metadata from previous screen
        String title       = in.getStringExtra("eventTitle");
        String description = in.getStringExtra("eventDescription");
        String venue       = in.getStringExtra("eventVenue");
        String organizer   = in.getStringExtra("eventOrganizerName");
        String orgEmail    = in.getStringExtra("eventOrganizerEmail");
        int    capacity    = in.getIntExtra("eventCapacity", 0);
        int    registered  = in.getIntExtra("eventRegistered", 0);
        long   dateMillis  = in.getLongExtra("eventDateMillis", 0);

        // ---------------- HEADER SECTION ----------------
        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "Event");
        ((TextView) findViewById(R.id.tvHeaderVenue)).setText(venue != null ? venue : "TBD");

        // ---------------- ORGANIZER SECTION ----------------
        ((TextView) findViewById(R.id.tvOrganizer)).setText(
                organizer != null ? organizer : "Unknown");

        if (orgEmail != null && !orgEmail.isEmpty()) {
            TextView tvOrgEmail = findViewById(R.id.tvOrganizerEmail);
            tvOrgEmail.setText(orgEmail);
        }

        // ---------------- EVENT DETAILS SECTION ----------------
        ((TextView) findViewById(R.id.tvVenue)).setText(venue != null ? venue : "TBD");

        // Format event date & time if available
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

        // ---------------- CAPACITY & PROGRESS ----------------
        if (capacity > 0) {
            ((TextView) findViewById(R.id.tvCapacity))
                    .setText(registered + " / " + capacity + " spots taken");

            // Calculate event fill percentage
            int pct = (int) ((registered / (float) capacity) * 100);

            ((ProgressBar) findViewById(R.id.progressAvailability)).setProgress(pct);

            String spotsLeft = (capacity - registered) + " spots left";

            ((TextView) findViewById(R.id.tvAvailabilityLabel))
                    .setText(pct + "% Full · " + spotsLeft);

            // Show warning when event is mostly full
            if (pct >= 75) {
                findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
            }
        } else {
            ((TextView) findViewById(R.id.tvCapacity)).setText(registered + " registered");
        }

        // ---------------- PRICE DISPLAY ----------------
        double ticketPrice = in.getDoubleExtra("eventTicketPrice", 0.0);
        TextView tvPrice = findViewById(R.id.tvTicketPrice);

        if (tvPrice != null) {
            tvPrice.setText(ticketPrice > 0 ? "Rs. " + (int) ticketPrice : "FREE");

            // Color coding for paid vs free events
            tvPrice.setTextColor(ticketPrice > 0
                    ? getResources().getColor(android.R.color.holo_orange_dark, getTheme())
                    : getResources().getColor(R.color.green_accept, getTheme()));
        }

        // Event description fallback
        ((TextView) findViewById(R.id.tvDescription)).setText(
                (description != null && !description.isEmpty())
                        ? description : "No description provided.");
    }

    /**
     * Handles all button actions:
     * - Back navigation
     * - Expand/collapse description
     * - View attendees
     * - RSVP / Payment flow
     */
    private void setupButtons() {

        Intent in = getIntent();

        String eventId    = in.getStringExtra("eventId");
        String eventTitle = in.getStringExtra("eventTitle");
        String eventVenue = in.getStringExtra("eventVenue");
        String eventDate  = in.getStringExtra("eventDate");
        String eventTime  = in.getStringExtra("eventTime");
        long   dateMillisBtn = in.getLongExtra("eventDateMillis", 0);

        // Format date if not already provided
        if ((eventDate == null || eventDate.isEmpty()) && dateMillisBtn > 0) {
            eventDate = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(new java.util.Date(dateMillisBtn));
        }

        // ---------------- BACK BUTTON ----------------
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ---------------- ABOUT SECTION TOGGLE ----------------
        LinearLayout aboutToggle = findViewById(R.id.layoutAboutToggle);
        TextView tvDesc = findViewById(R.id.tvDescription);
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

        // ---------------- SEE ATTENDEES ----------------
        findViewById(R.id.btnSeeAttendees).setOnClickListener(v -> {
            Intent attendeesIntent = new Intent(this, AttendeeListActivity.class);
            attendeesIntent.putExtra("eventId", eventId);
            attendeesIntent.putExtra("eventTitle", eventTitle);
            attendeesIntent.putExtra("readOnly", true);
            startActivity(attendeesIntent);
        });

        // ---------------- RSVP / PAYMENT FLOW ----------------
        double ticketPrice = getIntent().getDoubleExtra("eventTicketPrice", 0.0);
        String formattedDate = eventDate != null ? eventDate : "";

        int capacity = getIntent().getIntExtra("eventCapacity", 0);
        int registered = getIntent().getIntExtra("eventRegistered", 0);

        android.widget.Button btnConfirmRsvp = findViewById(R.id.btnConfirmRsvp);
        android.widget.CheckBox cbWaitlist = findViewById(R.id.cbWaitlist);

        // Check if event is full
        boolean isFull = capacity > 0 && registered >= capacity;

        if (isFull) {

            // Disable RSVP when full
            btnConfirmRsvp.setText("Event Full");
            btnConfirmRsvp.setEnabled(false);

            btnConfirmRsvp.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(android.graphics.Color.GRAY));

            if (cbWaitlist != null) cbWaitlist.setVisibility(android.view.View.GONE);

            // Check Firestore if waitlist is enabled
            if (eventId != null) {
                FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                        .addOnSuccessListener(doc -> {

                            Boolean waitlistEnabled = doc.getBoolean("waitlistEnabled");

                            if (Boolean.TRUE.equals(waitlistEnabled) && cbWaitlist != null) {

                                cbWaitlist.setVisibility(android.view.View.VISIBLE);

                                cbWaitlist.setOnCheckedChangeListener((buttonView, isChecked) -> {

                                    if (isChecked) {
                                        btnConfirmRsvp.setText("Join Waitlist");
                                        btnConfirmRsvp.setEnabled(true);
                                    } else {
                                        btnConfirmRsvp.setText("Event Full");
                                        btnConfirmRsvp.setEnabled(false);
                                    }
                                });
                            }
                        });
            }

        } else {

            // Event still has space
            if (cbWaitlist != null) cbWaitlist.setVisibility(android.view.View.GONE);

            if (eventId != null) {
                FirebaseFirestore.getInstance().collection("events").document(eventId).get()
                        .addOnSuccessListener(doc -> {
                            Boolean waitlistEnabled = doc.getBoolean("waitlistEnabled");
                            // Reserved for future UI logic
                        });
            }
        }

        // ---------------- CONFIRM RSVP BUTTON ----------------
        findViewById(R.id.btnConfirmRsvp).setOnClickListener(v -> {

            CheckBox cbName = findViewById(R.id.cbVisibleName);
            CheckBox cbRoll = findViewById(R.id.cbVisibleRollNo);
            CheckBox cbWait = findViewById(R.id.cbWaitlist);

            Intent payIntent = new Intent(this, PaymentActivity.class);

            payIntent.putExtra(PaymentActivity.KEY_EVENT_ID,     eventId);
            payIntent.putExtra(PaymentActivity.KEY_EVENT_TITLE,  eventTitle);
            payIntent.putExtra(PaymentActivity.KEY_EVENT_DATE,   formattedDate);
            payIntent.putExtra(PaymentActivity.KEY_TICKET_PRICE, ticketPrice);

            // Pass privacy preferences to payment system
            payIntent.putExtra("isNameVisible", cbName != null && cbName.isChecked());
            payIntent.putExtra("isRollNoVisible", cbRoll != null && cbRoll.isChecked());
            payIntent.putExtra("optInWaitlist", cbWait != null && cbWait.isChecked());

            startActivity(payIntent);
        });
    }
}