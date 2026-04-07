package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * US-21: Event Detail screen.
 * Shows full event details. "Confirm RSVP" button at the bottom opens RsvpActivity.
 * "SEE WHO'S ATTENDING" opens AttendeeListActivity in read-only mode.
 */
public class EventDetailActivity extends AppCompatActivity {

    private boolean aboutExpanded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        populateDetails();
        setupButtons();
    }

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

        // Header
        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "Event");
        ((TextView) findViewById(R.id.tvHeaderVenue)).setText(venue != null ? venue : "TBD");

        // Organiser card
        ((TextView) findViewById(R.id.tvOrganizer)).setText(
                organizer != null ? organizer : "Unknown");
        if (orgEmail != null && !orgEmail.isEmpty()) {
            TextView tvOrgEmail = findViewById(R.id.tvOrganizerEmail);
            tvOrgEmail.setText(orgEmail);
        }

        // Event details card
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

            // Availability bar
            int pct = (int) ((registered / (float) capacity) * 100);
            ((ProgressBar) findViewById(R.id.progressAvailability)).setProgress(pct);
            String spotsLeft = (capacity - registered) + " spots left";
            ((TextView) findViewById(R.id.tvAvailabilityLabel))
                    .setText(pct + "% Full · " + spotsLeft);
            if (pct >= 75) {
                findViewById(R.id.tvFilling).setVisibility(View.VISIBLE);
            }
        } else {
            ((TextView) findViewById(R.id.tvCapacity)).setText(registered + " registered");
        }

        ((TextView) findViewById(R.id.tvDescription)).setText(
                (description != null && !description.isEmpty())
                        ? description : "No description provided.");
    }

    private void setupButtons() {
        Intent in = getIntent();
        String eventId    = in.getStringExtra("eventId");
        String eventTitle = in.getStringExtra("eventTitle");
        String eventVenue = in.getStringExtra("eventVenue");
        String eventDate  = in.getStringExtra("eventDate");
        String eventTime  = in.getStringExtra("eventTime");

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Collapsible About section
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

        // SEE WHO'S ATTENDING → read-only attendee list
        findViewById(R.id.btnSeeAttendees).setOnClickListener(v -> {
            Intent attendeesIntent = new Intent(this, AttendeeListActivity.class);
            attendeesIntent.putExtra("eventId", eventId);
            attendeesIntent.putExtra("eventTitle", eventTitle);
            attendeesIntent.putExtra("readOnly", true);
            startActivity(attendeesIntent);
        });

        // Confirm RSVP → RsvpActivity
        findViewById(R.id.btnConfirmRsvp).setOnClickListener(v -> {
            Intent rsvpIntent = new Intent(this, RsvpActivity.class);
            rsvpIntent.putExtra("EVENT_ID", eventId);
            rsvpIntent.putExtra("EVENT_TITLE", eventTitle);
            rsvpIntent.putExtra("EVENT_VENUE", eventVenue);
            if (eventDate != null) rsvpIntent.putExtra("EVENT_DATE", eventDate);
            if (eventTime != null) rsvpIntent.putExtra("EVENT_TIME", eventTime);
            startActivity(rsvpIntent);
        });
    }
}
