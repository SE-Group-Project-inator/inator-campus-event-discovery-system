package com.example.campuseventdiscoverysystem.activities;

import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import com.example.campuseventdiscoverysystem.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.provider.CalendarContract;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity responsible for handling RSVP (registration, cancellation, and calendar/reminder setup)
 * for campus events. It manages event details display, RSVP submission, conflict detection,
 * cancellation flow, and reminder scheduling.
 */
public class RsvpActivity extends AppCompatActivity {

    private LinearLayout layoutRsvpForm, layoutRsvpSuccess;
    private CheckBox cbVisibleName, cbVisibleRollNo, cbWaitlist;
    private MaterialButton btnConfirmRsvp, btnDone, btnCancelRsvp;

    private TextView tvEventTitle, tvEventHeaderDate, tvEventHeaderVenue;
    private TextView tvCardEventTitle, tvCardDate, tvCardTime, tvCardVenue;
    private TextView tvCancellationDeadline;

    private FirebaseFirestore db;

    private String eventId;
    private String studentId;

    /**
     * Called when the activity is first created.
     * Initializes UI components, retrieves intent data, and sets up event listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            studentId = "unknown_student";
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsvp);

        db = FirebaseFirestore.getInstance();

        layoutRsvpForm = findViewById(R.id.layoutRsvpForm);
        layoutRsvpSuccess = findViewById(R.id.layoutRsvpSuccess);

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventHeaderDate = findViewById(R.id.tvEventHeaderDate);
        tvEventHeaderVenue = findViewById(R.id.tvEventHeaderVenue);

        tvCardEventTitle = findViewById(R.id.tvCardEventTitle);
        tvCardDate = findViewById(R.id.tvCardDate);
        tvCardTime = findViewById(R.id.tvCardTime);
        tvCardVenue = findViewById(R.id.tvCardVenue);
        tvCancellationDeadline = findViewById(R.id.tvCancellationDeadline);

        cbVisibleName = findViewById(R.id.cbVisibleName);
        cbVisibleRollNo = findViewById(R.id.cbVisibleRollNo);
        cbWaitlist = findViewById(R.id.cbWaitlist);

        btnConfirmRsvp = findViewById(R.id.btnConfirmRsvp);
        btnDone = findViewById(R.id.btnDone);
        btnCancelRsvp = findViewById(R.id.btnCancelRsvp);

        btnConfirmRsvp = findViewById(R.id.btnConfirmRsvp);
        btnDone = findViewById(R.id.btnDone);
        btnCancelRsvp = findViewById(R.id.btnCancelRsvp);

        MaterialButton btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        if (btnAddToCalendar != null) {
            btnAddToCalendar.setOnClickListener(v -> exportToCalendar());
        }

        Intent intent = getIntent();
        eventId = intent.getStringExtra("EVENT_ID");

        String title = intent.getStringExtra("EVENT_TITLE");
        String date = intent.getStringExtra("EVENT_DATE");
        String time = intent.getStringExtra("EVENT_TIME");
        String venue = intent.getStringExtra("EVENT_VENUE");

        if (title != null && tvEventTitle != null && tvCardEventTitle != null) {
            tvEventTitle.setText(title);
            tvCardEventTitle.setText(title);
        }
        if (date != null && tvEventHeaderDate != null && tvCardDate != null) {
            tvEventHeaderDate.setText("📅 " + date);
            tvCardDate.setText(date);
            calculateDeadline(date);
        }
        if (time != null && tvCardTime != null) {
            tvCardTime.setText(time);
        }
        if (venue != null && tvEventHeaderVenue != null && tvCardVenue != null) {
            tvEventHeaderVenue.setText("📍 " + venue);
            tvCardVenue.setText(venue);
        }

        if (btnConfirmRsvp != null) {
            btnConfirmRsvp.setOnClickListener(v -> submitRsvp());
        }
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> finish());
        }
        if (btnCancelRsvp != null) {
            btnCancelRsvp.setOnClickListener(v -> showCancelRsvpQuestion());
        }
    }

    /**
     * Calculates and displays RSVP cancellation deadline (1 day before event).
     */
    private void calculateDeadline(String eventDateString) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault());
            Date eventDate = sdf.parse(eventDateString);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(eventDate);
            calendar.add(Calendar.DAY_OF_MONTH, -1);

            SimpleDateFormat deadlineFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            String deadlineString = deadlineFormat.format(calendar.getTime());

            if (tvCancellationDeadline != null) {
                tvCancellationDeadline.setText("Cancellation deadline: " + deadlineString);
            }

        } catch (Exception e) {
            if (tvCancellationDeadline != null) {
                tvCancellationDeadline.setText("Cancellation deadline: 1 day before event");
            }
        }
    }

    /**
     * Handles RSVP submission after checking for schedule conflicts.
     */
    private void submitRsvp() {
        btnConfirmRsvp.setEnabled(false);
        btnConfirmRsvp.setText("Checking Schedule...");

        db.collectionGroup("rsvps")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "Registered")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot conflictDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String conflictName = conflictDoc.getString("eventName");
                        if (conflictName == null) conflictName = "Another Campus Event";

                        showOverlapDialog(conflictName + "\n(Scheduled at a similar time)");
                    } else {
                        proceedWithRegistration();
                    }
                })
                .addOnFailureListener(e -> {
                    proceedWithRegistration();
                });
    }

    /**
     * Displays a dialog when a scheduling conflict is detected.
     */
    private void showOverlapDialog(String conflictDetails) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overlapping_event, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        TextView tvDetails = dialogView.findViewById(R.id.tvConflictingEventDetails);
        tvDetails.setText(conflictDetails);

        MaterialButton btnNoOverlap = dialogView.findViewById(R.id.btnNoOverlap);
        MaterialButton btnYesOverlap = dialogView.findViewById(R.id.btnYesOverlap);

        btnNoOverlap.setOnClickListener(v -> {
            dialog.dismiss();
            btnConfirmRsvp.setEnabled(true);
            btnConfirmRsvp.setText("✓ Confirm RSVP");
        });

        btnYesOverlap.setOnClickListener(v -> {
            dialog.dismiss();
            proceedWithRegistration();
        });

        dialog.show();
    }

    /**
     * Stores RSVP data in Firestore and updates UI on success.
     */
    /**
     * Stores RSVP data in Firestore and updates registeredCount ONLY if not on waitlist.
     */
    private void proceedWithRegistration() {
        btnConfirmRsvp.setText("Processing...");

        boolean isWaitlist = cbWaitlist != null && cbWaitlist.isChecked();

        Map<String, Object> rsvpData = new HashMap<>();
        rsvpData.put("userId",          studentId);
        rsvpData.put("eventId",         eventId);
        rsvpData.put("eventName",       tvEventTitle.getText().toString());
        rsvpData.put("isNameVisible",   cbVisibleName   != null && cbVisibleName.isChecked());
        rsvpData.put("isRollNoVisible", cbVisibleRollNo != null && cbVisibleRollNo.isChecked());
        rsvpData.put("optInWaitlist",   isWaitlist);
        rsvpData.put("status",          "confirmed");
        rsvpData.put("createdAt",       FieldValue.serverTimestamp());

        db.collection("rsvps")
                .document(studentId + "_" + eventId)
                .set(rsvpData)
                .addOnSuccessListener(aVoid -> {

                    // Only increment capacity count if NOT on waitlist
                    if (!isWaitlist) {
                        db.collection("events").document(eventId)
                                .update("registeredCount", FieldValue.increment(1));
                    }

                    // If free event, go straight to PaymentStatusActivity
                    double ticketPrice = getIntent().getDoubleExtra("EVENT_PRICE", 0.0);
                    if (ticketPrice <= 0) {
                        // Create a minimal payment doc so PaymentStatusActivity can listen
                        java.util.Map<String, Object> freePayment = new java.util.HashMap<>();
                        freePayment.put("userId", studentId);
                        freePayment.put("eventId", eventId);
                        freePayment.put("eventName", tvEventTitle.getText().toString());
                        freePayment.put("method", "free");
                        freePayment.put("amount", 0.0);
                        freePayment.put("status", "registered");
                        freePayment.put("createdAt", FieldValue.serverTimestamp());
                        db.collection("payments").add(freePayment).addOnSuccessListener(ref -> {
                            android.content.Intent si = new android.content.Intent(this, PaymentStatusActivity.class);
                            si.putExtra(PaymentStatusActivity.KEY_PAYMENT_ID, ref.getId());
                            si.putExtra(PaymentStatusActivity.KEY_STATUS, "registered");
                            si.putExtra(PaymentStatusActivity.KEY_METHOD, "free");
                            si.putExtra(PaymentStatusActivity.KEY_EVENT_NAME, tvEventTitle.getText().toString());
                            si.putExtra(PaymentStatusActivity.KEY_AMOUNT, 0.0);
                            startActivity(si);
                            finish();
                        }).addOnFailureListener(e2 -> {
                            // fallback: show normal success screen
                        });
                        return;
                    }

                    // Populate success layout
                    TextView tvDateSuccess  = findViewById(R.id.tvCardDateSuccess);
                    TextView tvTimeSuccess  = findViewById(R.id.tvCardTimeSuccess);
                    TextView tvVenueSuccess = findViewById(R.id.tvCardVenueSuccess);
                    TextView tvTitleSuccess = findViewById(R.id.tvCardEventTitleSuccess);

                    if (tvTitleSuccess != null) tvTitleSuccess.setText(tvEventTitle.getText());
                    if (tvDateSuccess  != null) tvDateSuccess.setText(tvCardDate.getText());
                    if (tvTimeSuccess  != null) tvTimeSuccess.setText(tvCardTime.getText());
                    if (tvVenueSuccess != null) tvVenueSuccess.setText(tvCardVenue.getText());

                    if (layoutRsvpForm    != null) layoutRsvpForm.setVisibility(View.GONE);
                    if (layoutRsvpSuccess != null) layoutRsvpSuccess.setVisibility(View.VISIBLE);

                    String title   = tvEventTitle.getText().toString();
                    String dateStr = tvCardDate.getText().toString();
                    String timeStr = tvCardTime.getText().toString();
                    scheduleReminders(title, dateStr, timeStr);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirmRsvp.setEnabled(true);
                    btnConfirmRsvp.setText("✓ Confirm RSVP");
                });
    }

    /**
     * Displays RSVP cancellation confirmation dialog.
     * Deletes from the correct top-level rsvps collection and decrements
     * registeredCount ONLY if the RSVP was not a waitlist entry.
     */
    private void showCancelRsvpQuestion() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.cancel_rsvp_question, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        MaterialButton btnDontCancel    = dialogView.findViewById(R.id.btnDontCancel);
        MaterialButton btnYesCancel     = dialogView.findViewById(R.id.btnYesCancel);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBarCancel);
        LinearLayout layoutCancelButtons = dialogView.findViewById(R.id.layoutCancelButtons);

        btnDontCancel.setOnClickListener(v -> dialog.dismiss());

        btnYesCancel.setOnClickListener(v -> {
            layoutCancelButtons.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            String rsvpDocId = studentId + "_" + eventId;

            // First read the RSVP to check if it was a waitlist entry
            db.collection("rsvps").document(rsvpDocId)
                    .get()
                    .addOnSuccessListener(rsvpDoc -> {
                        // Use the actual RSVP status, not the optInWaitlist preference flag.
                        // A student may have ticked the waitlist box but still got a confirmed spot.
                        boolean wasWaitlisted = "waitlisted".equals(rsvpDoc.getString("status"));

                        // Delete from the correct top-level rsvps collection
                        db.collection("rsvps").document(rsvpDocId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (!wasWaitlisted) {
                                        // Confirmed spot cancelled — check if someone is waiting
                                        db.collection("rsvps")
                                                .whereEqualTo("eventId", eventId)
                                                .whereEqualTo("status", "waitlisted")
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener(waitlistSnap -> {
                                                    if (!waitlistSnap.isEmpty()) {
                                                        // Promote first waitlisted user, count stays same
                                                        com.google.firebase.firestore.DocumentSnapshot wDoc =
                                                                waitlistSnap.getDocuments().get(0);
                                                        String promotedId = wDoc.getId();
                                                        String promotedUserId = wDoc.getString("userId");
                                                        db.collection("rsvps").document(promotedId)
                                                                .update("status", "confirmed");
                                                        // Notify promoted user
                                                        if (promotedUserId != null) {
                                                            java.util.Map<String, Object> notif = new java.util.HashMap<>();
                                                            notif.put("title", "Waitlist Update 🎉");
                                                            notif.put("message", "A spot opened up! You are now confirmed.");
                                                            notif.put("read", false);
                                                            notif.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                                                            db.collection("users").document(promotedUserId)
                                                                    .collection("notifications").add(notif);
                                                        }
                                                    } else {
                                                        // No waitlisted users — recalculate from actual rsvps
                                                        recalculateRegisteredCount(eventId);
                                                    }
                                                })
                                                .addOnFailureListener(e2 -> {
                                                    // Query failed — still recalculate
                                                    recalculateRegisteredCount(eventId);
                                                });
                                    }
                                    // wasWaitlisted=true: no count change needed
                                    dialog.dismiss();
                                    layoutRsvpSuccess.setVisibility(View.GONE);
                                    layoutRsvpForm.setVisibility(View.VISIBLE);
                                    showCancelSuccessDialog();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    layoutCancelButtons.setVisibility(View.VISIBLE);
                                    progressBar.setVisibility(View.GONE);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to cancel: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        layoutCancelButtons.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    });
        });

        dialog.show();
    }


    /**
     * Shows success dialog after cancellation.
     */
    private void showCancelSuccessDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.cancel_confirmation_dialog, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        ImageButton btnDialogBack = dialogView.findViewById(R.id.btnDialogBack);
        btnDialogBack.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    /**
     * Schedules push/local reminders using WorkManager.
     */
    private void scheduleReminders(String title, String dateStr, String timeStr) {
        try {
            String startTimeStr = timeStr;
            if (timeStr != null && timeStr.contains("-")) {
                startTimeStr = timeStr.split("-")[0].trim();
            }

            String dateTimeStr = dateStr + " " + startTimeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy hh:mm a", Locale.getDefault());
            Date eventDate = sdf.parse(dateTimeStr);

            if (eventDate == null) return;

            long currentTime = System.currentTimeMillis();
            long eventTime = eventDate.getTime();

            long timeUntilEvent = eventTime - currentTime;
            long delay24Hours = timeUntilEvent - TimeUnit.HOURS.toMillis(24);
            long delay1Hour = timeUntilEvent - TimeUnit.HOURS.toMillis(1);

            WorkManager workManager = WorkManager.getInstance(this);

            if (delay24Hours > 0) {
                OneTimeWorkRequest work24 =
                        new OneTimeWorkRequest.Builder(com.example.campuseventdiscoverysystem.workers.ReminderWorker.class)
                                .setInitialDelay(delay24Hours, TimeUnit.MILLISECONDS)
                                .setInputData(new Data.Builder()
                                        .putString("EVENT_NAME", title)
                                        .putString("MESSAGE", "Reminder: " + title + " is tomorrow!")
                                        .build())
                                .build();

                workManager.enqueue(work24);
            }

            if (delay1Hour > 0) {
                OneTimeWorkRequest work1 =
                        new OneTimeWorkRequest.Builder(com.example.campuseventdiscoverysystem.workers.ReminderWorker.class)
                                .setInitialDelay(delay1Hour, TimeUnit.MILLISECONDS)
                                .setInputData(new Data.Builder()
                                        .putString("EVENT_NAME", title)
                                        .putString("MESSAGE", title + " starts in 1 hour!")
                                        .build())
                                .build();

                workManager.enqueue(work1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds event to device calendar using an intent.
     */
    private void exportToCalendar() {
        String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Campus Event";
        String venue = tvCardVenue != null ? tvCardVenue.getText().toString() : "";
        String dateStr = tvCardDate != null ? tvCardDate.getText().toString() : "";
        String timeStr = tvCardTime != null ? tvCardTime.getText().toString() : "";

        long startMillis = 0;

        try {
            String startTimeStr = timeStr;
            if (timeStr != null && timeStr.contains("-")) {
                startTimeStr = timeStr.split("-")[0].trim();
            }

            String dateTimeStr = dateStr + " " + startTimeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy hh:mm a", Locale.getDefault());
            Date eventDate = sdf.parse(dateTimeStr);

            if (eventDate != null) {
                startMillis = eventDate.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (startMillis <= 0) {
            Toast.makeText(this, "Event date not available for calendar export.", Toast.LENGTH_SHORT).show();
            return;
        }

        long endMillis = startMillis + (2 * 60 * 60 * 1000);

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, venue)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No calendar app found on this device.", Toast.LENGTH_SHORT).show();
        }
    }
    /** Recalculates registeredCount from actual confirmed rsvps, clamped to [0, capacity]. */
    private void recalculateRegisteredCount(String eventId) {
        if (eventId == null) return;
        com.google.firebase.firestore.FirebaseFirestore db2 =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();
        db2.collection("rsvps")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(snap -> {
                    int trueCount = snap.size();
                    db2.collection("events").document(eventId).get()
                            .addOnSuccessListener(evDoc -> {
                                Long cap = evDoc.getLong("capacity");
                                int clamped = cap != null && cap > 0
                                        ? Math.min(trueCount, cap.intValue()) : trueCount;
                                db2.collection("events").document(eventId)
                                        .update("registeredCount", Math.max(0, clamped));
                            });
                });
    }

}