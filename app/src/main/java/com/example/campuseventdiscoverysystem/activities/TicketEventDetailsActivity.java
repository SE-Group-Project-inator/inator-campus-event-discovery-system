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
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Event detail screen for a ticket (future RSVP).
 * Same details view as EventDisplayActivity but with a Cancel RSVP button at the bottom.
 */
public class TicketEventDetailsActivity extends AppCompatActivity {

    private boolean aboutExpanded = true;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_event_details);

        db = FirebaseFirestore.getInstance();

        populateDetails();
        setupButtons();
    }

    private void populateDetails() {
        Intent in = getIntent();

        String title      = in.getStringExtra("eventTitle");
        String description= in.getStringExtra("eventDescription");
        String venue      = in.getStringExtra("eventVenue");
        int    capacity   = in.getIntExtra("eventCapacity", 0);
        int    registered = in.getIntExtra("eventRegistered", 0);
        long   dateMillis = in.getLongExtra("eventDateMillis", 0);

        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "Event");
        ((TextView) findViewById(R.id.tvHeaderVenue)).setText(venue != null ? venue : "TBD");
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
        Intent in        = getIntent();
        String rsvpId    = in.getStringExtra("rsvpId");
        String eventId   = in.getStringExtra("eventId");
        String title     = in.getStringExtra("eventTitle");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Collapsible About
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

        // Cancel RSVP
        findViewById(R.id.btnCancelRsvp).setOnClickListener(v -> {
            if (rsvpId == null || eventId == null) {
                Toast.makeText(this, "Could not find RSVP", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get the current student's ID so we can remove them from the roster
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Delete the RSVP document
            db.collection("rsvps").document(rsvpId)
                    .delete()
                    .addOnSuccessListener(unused -> {

                        // Remove the student from the manager's attendee roster
                        db.collection("event_attendees").document(eventId)
                                .collection("attendees").document(currentUserId)
                                .delete();

                        // Check for waitlisted students (Auto-Promotion Logic)
                        db.collection("rsvps")
                                .whereEqualTo("eventId", eventId)
                                .whereEqualTo("status", "waitlisted")
                                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                                .limit(1) // Get the oldest waitlist entry
                                .get()
                                .addOnSuccessListener(querySnapshots -> {

                                    if (!querySnapshots.isEmpty()) {
                                        // A spot opened up and someone is on the waitlist! Promote them.
                                        com.google.firebase.firestore.DocumentSnapshot waitlistDoc = querySnapshots.getDocuments().get(0);
                                        String promotedRsvpId = waitlistDoc.getId();
                                        String promotedUserId = waitlistDoc.getString("userId");

                                        // Update their status from waitlisted to confirmed
                                        db.collection("rsvps").document(promotedRsvpId).update("status", "confirmed");

                                        // Add them to the attendees roster
                                        java.util.Map<String, Object> attendee = new java.util.HashMap<>();
                                        attendee.put("userId", promotedUserId);
                                        attendee.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                        db.collection("event_attendees").document(eventId)
                                                .collection("attendees").document(promotedUserId).set(attendee);

                                        // Send the promoted student a notification
                                        java.util.Map<String, Object> notif = new java.util.HashMap<>();
                                        notif.put("title", "Waitlist Update \uD83C\uDF89"); // 🎉 Emoji
                                        notif.put("message", "A spot opened up! You are now confirmed for \"" + title + "\".");
                                        notif.put("read", false);
                                        notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                        db.collection("users").document(promotedUserId).collection("notifications").add(notif);

                                    } else {
                                        // No one is on the waitlist. Decrement the capacity safely.
                                        db.collection("events").document(eventId)
                                                .update("registeredCount", com.google.firebase.firestore.FieldValue.increment(-1));
                                    }

                                    // Finish up and return to the tickets screen
                                    Toast.makeText(this, "RSVP cancelled successfully", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, TicketsActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to cancel RSVP", Toast.LENGTH_SHORT).show()
                    );
        });
    }
}