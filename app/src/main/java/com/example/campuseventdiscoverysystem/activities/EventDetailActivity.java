package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * US-21: Export Event to Calendar
 * Shows full event details matching the Figma design, with calendar export via
 * the top-right share icon and the fixed "Add to Calendar" button at the bottom.
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
        long   dateMillis = in.getLongExtra("eventDateMillis", 0);
        String title      = in.getStringExtra("eventTitle");
        String venue      = in.getStringExtra("eventVenue");
        String desc       = in.getStringExtra("eventDescription");

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Top-right export icon — US-21
        findViewById(R.id.btnExportCalendar).setOnClickListener(v ->
                exportToCalendar(title, venue, desc, dateMillis));

        // Fixed bottom button — US-21
        findViewById(R.id.btnAddToCalendar).setOnClickListener(v ->
                exportToCalendar(title, venue, desc, dateMillis));

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

        // SEE WHO'S ATTENDING → show attendees (read-only for students)
        String eventId = in.getStringExtra("eventId");
        String eventTitle = in.getStringExtra("eventTitle");
        findViewById(R.id.btnSeeAttendees).setOnClickListener(v -> {
            Intent attendeesIntent = new Intent(this, AttendeeListActivity.class);
            attendeesIntent.putExtra("eventId", eventId);
            attendeesIntent.putExtra("eventTitle", eventTitle);
            attendeesIntent.putExtra("readOnly", true);
            startActivity(attendeesIntent);
        });
    }

    /**
     * US-21: Opens the system calendar app with event details pre-filled.
     */
    private void exportToCalendar(String title, String venue,
                                   String description, long startMillis) {
        if (startMillis <= 0) {
            Toast.makeText(this,
                    "Event date not available for calendar export.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        long endMillis = startMillis + (2 * 60 * 60 * 1000); // default 2-hour duration

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE,
                        title != null ? title : "Campus Event")
                .putExtra(CalendarContract.Events.EVENT_LOCATION,
                        venue != null ? venue : "")
                .putExtra(CalendarContract.Events.DESCRIPTION,
                        description != null ? description : "")
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this,
                    "No calendar app found on this device.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
