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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            // Fallback just in case the login drops
            studentId = "unknown_student";
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rsvp);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Layouts
        layoutRsvpForm = findViewById(R.id.layoutRsvpForm);
        layoutRsvpSuccess = findViewById(R.id.layoutRsvpSuccess);

        // 2. Initialize Header TextViews
        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventHeaderDate = findViewById(R.id.tvEventHeaderDate);
        tvEventHeaderVenue = findViewById(R.id.tvEventHeaderVenue);

        // 3. Initialize Card TextViews
        tvCardEventTitle = findViewById(R.id.tvCardEventTitle);
        tvCardDate = findViewById(R.id.tvCardDate);
        tvCardTime = findViewById(R.id.tvCardTime);
        tvCardVenue = findViewById(R.id.tvCardVenue);
        tvCancellationDeadline = findViewById(R.id.tvCancellationDeadline);

        // 4. Initialize Form Elements
        cbVisibleName = findViewById(R.id.cbVisibleName);
        cbVisibleRollNo = findViewById(R.id.cbVisibleRollNo);
        cbWaitlist = findViewById(R.id.cbWaitlist);

        // 5. Initialize Buttons
        btnConfirmRsvp = findViewById(R.id.btnConfirmRsvp);
        btnDone = findViewById(R.id.btnDone);
        btnCancelRsvp = findViewById(R.id.btnCancelRsvp);
// 5. Initialize Buttons
        btnConfirmRsvp = findViewById(R.id.btnConfirmRsvp);
        btnDone = findViewById(R.id.btnDone);
        btnCancelRsvp = findViewById(R.id.btnCancelRsvp);

        // Add your new Calendar button link here:
        MaterialButton btnAddToCalendar = findViewById(R.id.btnAddToCalendar); // Use whatever ID is in your XML
        if (btnAddToCalendar != null) {
            btnAddToCalendar.setOnClickListener(v -> exportToCalendar());
        }
        // 6. Unpack Intent Data & Add Fallback for Testing
        Intent intent = getIntent();
        eventId = intent.getStringExtra("EVENT_ID");

        String title = intent.getStringExtra("EVENT_TITLE");
        String date = intent.getStringExtra("EVENT_DATE");
        String time = intent.getStringExtra("EVENT_TIME");
        String venue = intent.getStringExtra("EVENT_VENUE");

        // 7. Set the UI with Dynamic Data
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

        // 8. Set Button Listeners
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

    // --- US-30: CHECK FOR CONFLICTS BEFORE SUBMITTING ---
    private void submitRsvp() {
        btnConfirmRsvp.setEnabled(false);
        btnConfirmRsvp.setText("Checking Schedule...");

        // Querying all RSVP subcollections to find if this student is already registered for something else
        db.collectionGroup("rsvps")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "Registered")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // For testing: if they have ANY other registration, we show the overlap dialog.
                        // Ideally, we'd compare dates/times here once the format is confirmed.
                        DocumentSnapshot conflictDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String conflictName = conflictDoc.getString("eventName"); // Assuming teammate saves this
                        if (conflictName == null) conflictName = "Another Campus Event";

                        showOverlapDialog(conflictName + "\n(Scheduled at a similar time)");
                    } else {
                        proceedWithRegistration();
                    }
                })
                .addOnFailureListener(e -> {
                    // If check fails, we proceed anyway to be safe
                    proceedWithRegistration();
                });
    }

    private void showOverlapDialog(String conflictDetails) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_overlapping_event, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

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

    private void proceedWithRegistration() {
        btnConfirmRsvp.setText("Processing...");

        Map<String, Object> rsvpData = new HashMap<>();
        rsvpData.put("studentId", studentId);
        rsvpData.put("eventName", tvEventTitle.getText().toString()); // Added to help conflict checks
        rsvpData.put("isNameVisible", cbVisibleName != null && cbVisibleName.isChecked());
        rsvpData.put("isRollNoVisible", cbVisibleRollNo != null && cbVisibleRollNo.isChecked());
        rsvpData.put("optInWaitlist", cbWaitlist != null && cbWaitlist.isChecked());
        rsvpData.put("status", "Registered");
        rsvpData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("rsvps").document(studentId)
                .set(rsvpData)
                .addOnSuccessListener(aVoid -> {
                    if (layoutRsvpForm != null && layoutRsvpSuccess != null) {
                        layoutRsvpForm.setVisibility(View.GONE);
                        layoutRsvpSuccess.setVisibility(View.VISIBLE);
                    }

                    String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Campus Event";
                    String dateStr = tvCardDate != null ? tvCardDate.getText().toString() : "";
                    String timeStr = tvCardTime != null ? tvCardTime.getText().toString() : "";

                    if (!dateStr.isEmpty() && !timeStr.isEmpty()) {
                        scheduleReminders(title, dateStr, timeStr);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnConfirmRsvp.setEnabled(true);
                    btnConfirmRsvp.setText("✓ Confirm RSVP");
                });
    }

    private void showCancelRsvpQuestion() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.cancel_rsvp_question, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnDontCancel = dialogView.findViewById(R.id.btnDontCancel);
        MaterialButton btnYesCancel = dialogView.findViewById(R.id.btnYesCancel);
        android.widget.ProgressBar progressBar = dialogView.findViewById(R.id.progressBarCancel);
        LinearLayout layoutCancelButtons = dialogView.findViewById(R.id.layoutCancelButtons);

        btnDontCancel.setOnClickListener(v -> dialog.dismiss());

        btnYesCancel.setOnClickListener(v -> {
            layoutCancelButtons.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);

            db.collection("events").document(eventId)
                    .collection("rsvps").document(studentId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        if (layoutRsvpSuccess != null && layoutRsvpForm != null) {
                            layoutRsvpSuccess.setVisibility(View.GONE);
                            layoutRsvpForm.setVisibility(View.VISIBLE);
                            btnConfirmRsvp.setText("✓ Confirm RSVP");
                            btnConfirmRsvp.setEnabled(true);
                        }
                        showCancelSuccessDialog();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        layoutCancelButtons.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                    });
        });

        dialog.show();
    }

    private void showCancelSuccessDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.cancel_confirmation_dialog, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        ImageButton btnDialogBack = dialogView.findViewById(R.id.btnDialogBack);
        btnDialogBack.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

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
                Data data24 = new Data.Builder()
                        .putString("EVENT_NAME", title)
                        .putString("MESSAGE", "Reminder: " + title + " is happening tomorrow!")
                        .build();

                OneTimeWorkRequest work24 = new OneTimeWorkRequest.Builder(com.example.campuseventdiscoverysystem.workers.ReminderWorker.class)
                        .setInitialDelay(delay24Hours, TimeUnit.MILLISECONDS)
                        .setInputData(data24)
                        .build();
                workManager.enqueue(work24);
            }

            if (delay1Hour > 0) {
                Data data1 = new Data.Builder()
                        .putString("EVENT_NAME", title)
                        .putString("MESSAGE", title + " starts in 1 hour. See you there!")
                        .build();

                OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(com.example.campuseventdiscoverysystem.workers.ReminderWorker.class)
                        .setInitialDelay(delay1Hour, TimeUnit.MILLISECONDS)
                        .setInputData(data1)
                        .build();
                workManager.enqueue(work1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void exportToCalendar() {
        String title = tvEventTitle != null ? tvEventTitle.getText().toString() : "Campus Event";
        String venue = tvCardVenue != null ? tvCardVenue.getText().toString() : "";
        String dateStr = tvCardDate != null ? tvCardDate.getText().toString() : "";
        String timeStr = tvCardTime != null ? tvCardTime.getText().toString() : "";

        long startMillis = 0;
        try {
            // Extract the start time and combine it with the date
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

        long endMillis = startMillis + (2 * 60 * 60 * 1000); // Default 2-hour duration

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
}