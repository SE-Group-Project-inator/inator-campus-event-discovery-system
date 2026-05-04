package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Event detail screen for a ticket (future RSVP).
 * Shows full event details, a "Show My QR" button, and a Cancel RSVP button.
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

        applyRoleBasedUI();
    }

    private void populateDetails() {
        Intent in = getIntent();

        String title       = in.getStringExtra("eventTitle");
        String description = in.getStringExtra("eventDescription");
        String venue       = in.getStringExtra("eventVenue");
        String organizer = in.getStringExtra("eventOrganizer");
        int    capacity    = in.getIntExtra("eventCapacity", 0);
        int    registered  = in.getIntExtra("eventRegistered", 0);
        long   dateMillis  = in.getLongExtra("eventDateMillis", 0);

        ((TextView) findViewById(R.id.tvTitle))
                .setText(title != null ? title : "Event");
        ((TextView) findViewById(R.id.tvHeaderVenue))
                .setText(venue != null ? venue : "TBD");
        ((TextView) findViewById(R.id.tvVenue))
                .setText(venue != null ? venue : "TBD");

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

        // Set Organizer Name
        TextView tvOrganizer = findViewById(R.id.tvOrganizer);
        if (tvOrganizer != null) {
            tvOrganizer.setText(organizer != null && !organizer.isEmpty() ? organizer : "Campus Society");
        }
    }

    private void setupButtons() {
        Intent in = getIntent();
        String rsvpId = in.getStringExtra("rsvpId");
        String eventId = in.getStringExtra("eventId");
        String title = in.getStringExtra("eventTitle");

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

        // Show My QR
        findViewById(R.id.btnShowQR).setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent qrIntent = new Intent(this, StudentQRActivity.class);
            qrIntent.putExtra("userId", user.getUid());
            qrIntent.putExtra("eventId", eventId);
            qrIntent.putExtra("eventTitle", title);
            startActivity(qrIntent);
        });

        // Cancel RSVP
        View btnCancel = findViewById(R.id.btnCancelRsvp);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                if (rsvpId == null || eventId == null) {
                    Toast.makeText(this, "Could not find RSVP", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prevent double-clicking
                v.setEnabled(false);
                Toast.makeText(this, "Processing cancellation...", Toast.LENGTH_SHORT).show();

                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                db.collection("rsvps").document(rsvpId).get().addOnSuccessListener(rsvpDoc -> {
                    if (!rsvpDoc.exists()) {
                        finishFlow();
                        return;
                    }

                    // Check status case-insensitively just to be safe
                    String currentStatus = rsvpDoc.getString("status");
                    boolean wasWaitlisted = currentStatus != null && currentStatus.toLowerCase().equals("waitlisted");

                    // 1. Delete the RSVP
                    db.collection("rsvps").document(rsvpId).delete().addOnSuccessListener(unused -> {

                        // 2. Remove user from attendee roster
                        db.collection("event_attendees").document(eventId)
                                .collection("attendees").document(currentUserId).delete();

                        if (!wasWaitlisted) {
                            // 3. Promote Waitlist (Because a confirmed spot opened up!)
                            db.collection("rsvps")
                                    .whereEqualTo("eventId", eventId)
                                    .whereEqualTo("status", "waitlisted")
                                    .limit(1) // Grab the next person in line
                                    .get()
                                    .addOnSuccessListener(querySnapshots -> {
                                        if (!querySnapshots.isEmpty()) {
                                            // SOMEONE IS ON WAITLIST -> PROMOTE THEM
                                            var waitlistDoc = querySnapshots.getDocuments().get(0);
                                            String promotedRsvpId = waitlistDoc.getId();
                                            String promotedUserId = waitlistDoc.getString("userId");

                                            // Update their RSVP status
                                            db.collection("rsvps").document(promotedRsvpId).update("status", "confirmed");

                                            // Add them to attendee roster
                                            java.util.Map<String, Object> attendee = new java.util.HashMap<>();
                                            attendee.put("userId", promotedUserId);
                                            attendee.put("joinedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                            db.collection("event_attendees").document(eventId)
                                                    .collection("attendees").document(promotedUserId).set(attendee);

                                            // Send them a notification!
                                            java.util.Map<String, Object> notif = new java.util.HashMap<>();
                                            notif.put("title", "Waitlist Update 🎉");
                                            notif.put("message", "A spot opened up! You are now confirmed for \"" + title + "\".");
                                            notif.put("read", false);
                                            notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                                            db.collection("users").document(promotedUserId).collection("notifications").add(notif);

                                            // Finish flow immediately. Total count doesn't change (-1 cancel +1 promote = 0)
                                            finishFlow();

                                        } else {
                                            // NO ONE ON WAITLIST -> Drop the capacity count safely
                                            recalculateRegisteredCount(eventId);
                                            finishFlow();
                                        }
                                    }).addOnFailureListener(e -> {
                                        recalculateRegisteredCount(eventId);
                                        finishFlow();
                                    });
                        } else {
                            // The user cancelling was already on the waitlist, so no capacity change needed.
                            finishFlow();
                        }
                    }).addOnFailureListener(e -> {
                        v.setEnabled(true);
                        Toast.makeText(this, "Error cancelling RSVP.", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        }
    }

        private void finishFlow () {
            Toast.makeText(this, "RSVP cancelled successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TicketsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }

        /** Recalculates registeredCount from actual confirmed rsvps, clamped to [0, capacity]. */
        private void recalculateRegisteredCount (String eventId){
            if (eventId == null) return;
            db.collection("rsvps")
                    .whereEqualTo("eventId", eventId)
                    .whereEqualTo("status", "confirmed")
                    .get()
                    .addOnSuccessListener(snap -> {
                        int trueCount = snap.size();
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(evDoc -> {
                                    Long cap = evDoc.getLong("capacity");
                                    int clamped = cap != null && cap > 0
                                            ? Math.min(trueCount, cap.intValue()) : trueCount;
                                    db.collection("events").document(eventId)
                                            .update("registeredCount", Math.max(0, clamped));
                                });
                    });
        }

        /**
         * Dynamically themes the screen based on who is viewing it.
         * Manager = Purple theme + hidden student controls.
         * Student = Teal theme + visible student controls.
         */
        private void applyRoleBasedUI () {
            String userRole = getIntent().getStringExtra("USER_ROLE");

            int primaryColor;
            int bgColor;

            if ("manager".equals(userRole)) {
                // == MANAGER VIEW (Dynamic Theme) ==
                primaryColor = getResources().getColor(R.color.btn_eventmgr, getTheme());
                bgColor = getResources().getColor(R.color.bg_event, getTheme());

                // Hide student-specific controls
                int[] viewsToHide = {
                        R.id.btnShowQR, R.id.btnCancelRsvp, R.id.btnConfirmRsvp,
                        R.id.cardConsent, R.id.cbWaitlist, R.id.cbVisibleName, R.id.cbVisibleRollNo
                };
                for (int id : viewsToHide) {
                    View v = findViewById(id);
                    if (v != null) v.setVisibility(View.GONE);
                }
            } else {
                // == STUDENT VIEW (Teal Theme) ==
                primaryColor = android.graphics.Color.parseColor("#0D9488"); // Teal
                bgColor = android.graphics.Color.parseColor("#EBF8F5");      // Light Teal BG
            }

            // 1. Apply Background Colors
            View rootLayout = findViewById(R.id.rootLayout);
            View headerLayout = findViewById(R.id.headerLayout);
            if (rootLayout != null) rootLayout.setBackgroundColor(bgColor);
            if (headerLayout != null) headerLayout.setBackgroundColor(primaryColor);

            // 2. Apply Back Button Color
            ImageButton btnBack = findViewById(R.id.btnBack);
            if (btnBack != null)
                btnBack.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));

            // 3. Apply Text Colors to Section Headers
            int[] textViewsToTint = {
                    R.id.tvLabelEventDetails, R.id.tvAvailabilityLabel, R.id.tvLabelAbout
            };
            for (int id : textViewsToTint) {
                TextView tv = findViewById(id);
                if (tv != null) tv.setTextColor(primaryColor);
            }

            // 4. Apply Progress Bar Color
            ProgressBar pb = findViewById(R.id.progressAvailability);
            if (pb != null)
                pb.setProgressTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        }
}