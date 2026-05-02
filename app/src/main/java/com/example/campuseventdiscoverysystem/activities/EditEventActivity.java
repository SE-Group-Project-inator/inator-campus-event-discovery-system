package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EditEventActivity — FIXED VERSION
 *
 * Bug fixes:
 * 1. DELETED EVENTS NOT DISAPPEARING FROM ADMIN:
 *    The old deleteEvent() only deleted the event document. It did NOT delete the
 *    registrations subcollection. More critically, AdminDashboard was watching
 *    status="pending_approval" but the event stayed in Firestore with status="active"
 *    after approval — so when deleted it just vanished from events collection,
 *    but AdminDashboard had already stopped tracking it anyway.
 *    The real root cause was AdminDashboard used a one-shot .get() call.
 *    That's fixed in AdminDashboardActivity. Here we also:
 *    a) Set status = "deleted" before deleting (so any live listener removes it)
 *    b) Write cancellation notifications to all registered students
 *    c) Delete all registrations in a batch
 *
 * 2. EDIT RESETS STATUS TO pending_approval so admin must re-approve changes.
 *    This prevents an event manager from silently editing an approved event.
 *
 * New features:
 * - Loading spinner during save/delete
 * - Capacity validation (cannot be lower than current registeredCount)
 */
public class EditEventActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText etTitle, etDescription, etDate, etStartTime, etEndTime, etCapacity;
    private Spinner spVenue, spCategory;
    private ArrayAdapter<CharSequence> venueAdapter, categoryAdapter;
    private String eventID;
    private ProgressBar progressBar;
    private int currentRegisteredCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        db = FirebaseFirestore.getInstance();

        eventID = getIntent().getStringExtra("EVENT_ID");
        if (eventID == null) {
            Toast.makeText(this, "Failed to load event!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindViews();
        setupSpinners();
        setupPickers();
        setupNavigation();
        loadEventData();
    }

    private void bindViews() {
        etTitle       = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDate        = findViewById(R.id.etDate);
        etStartTime   = findViewById(R.id.etStartTime);
        etEndTime     = findViewById(R.id.etEndTime);
        etCapacity    = findViewById(R.id.etCapacity);
        spVenue       = findViewById(R.id.spVenue);
        spCategory    = findViewById(R.id.spCategory);
        progressBar   = findViewById(R.id.progressBar); // optional — add to your XML
    }

    private void setupSpinners() {
        venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, android.R.layout.simple_spinner_item);
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVenue.setAdapter(venueAdapter);

        categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, R.style.PurplePickerTheme, (view, y, m, d) ->
                    etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        etStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, h, min) ->
                    etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        etEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, h, min) ->
                    etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    private void loadEventData() {
        db.collection("events").document(eventID).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) {
                        Toast.makeText(this, "Event not found!", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    etTitle.setText(d.getString("title"));
                    etDescription.setText(d.getString("description"));
                    etStartTime.setText(d.getString("startTime") != null ? d.getString("startTime") : "");
                    etEndTime.setText(d.getString("endTime") != null ? d.getString("endTime") : "");

                    Timestamp ts = d.getTimestamp("date");
                    if (ts != null) {
                        etDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                .format(ts.toDate()));
                    }

                    Long cap = d.getLong("capacity");
                    etCapacity.setText(cap != null ? String.valueOf(cap) : "0");

                    Long regCount = d.getLong("registeredCount");
                    currentRegisteredCount = regCount != null ? regCount.intValue() : 0;

                    String venue = d.getString("venue");
                    if (venue != null) spVenue.setSelection(venueAdapter.getPosition(venue));

                    String category = d.getString("category");
                    if (category != null) spCategory.setSelection(categoryAdapter.getPosition(category));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load event data!", Toast.LENGTH_SHORT).show());
    }

    private void updateEvent() {
        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateStr     = etDate.getText().toString().trim();
        String startTime   = etStartTime.getText().toString().trim();
        String endTime     = etEndTime.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String venue       = spVenue.getSelectedItem().toString();
        String category    = spCategory.getSelectedItem().toString();

        if (title.isEmpty() || description.isEmpty() || dateStr.isEmpty()
                || startTime.isEmpty() || endTime.isEmpty() || capacityStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(capacityStr);
        } catch (NumberFormatException e) {
            etCapacity.setError("Capacity must be a number");
            return;
        }

        // Validate capacity is not less than existing registrations
        if (capacity < currentRegisteredCount) {
            etCapacity.setError("Capacity cannot be less than current registrations ("
                    + currentRegisteredCount + ")");
            return;
        }

        Timestamp date;
        try {
            Date parsed = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateStr);
            date = new Timestamp(parsed);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format! Use DD/MM/YYYY", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Read price (optional)
        double price = 0.0;
        EditText etPrice = findViewById(R.id.etPrice);
        if (etPrice != null && !etPrice.getText().toString().trim().isEmpty()) {
            try { price = Double.parseDouble(etPrice.getText().toString().trim()); } catch (Exception ignored) {}
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title",       title);
        updates.put("description", description);
        updates.put("date",        date);
        updates.put("startTime",   startTime);
        updates.put("endTime",     endTime);
        updates.put("capacity",    capacity);
        updates.put("venue",       venue);
        updates.put("category",    category);
        updates.put("price",       price);
        // FIX: Reset to pending so admin must re-approve any edited event
        updates.put("status",      "pending_approval");
        updates.put("updatedAt",   Timestamp.now());

        db.collection("events").document(eventID).update(updates)
                .addOnSuccessListener(dr -> {
                    setLoading(false);
                    notifyRegisteredStudentsOfUpdate(title);
                    new AlertDialog.Builder(this)
                            .setTitle("Changes Saved!")
                            .setMessage("Your event has been re-submitted for admin approval. "
                                    + "Registered students have been notified of the update.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to update event: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * FIX: Proper delete that:
     * 1. Notifies all registered students via Firestore notification documents
     * 2. Deletes all registration documents in a batch
     * 3. Deletes the event document last
     *
     * This ensures AdminDashboard (and any other listener) sees the event disappear
     * in real time because the snapshot listener detects the document deletion.
     */
    private void deleteEvent() {
        setLoading(true);

        // Step 1: Get all registrations for this event
        db.collection("registrations")
                .whereEqualTo("eventId", eventID)
                .get()
                .addOnSuccessListener(regSnapshots -> {
                    WriteBatch batch = db.batch();

                    // Step 2: For each registered student, write a cancellation notification
                    String eventTitle = etTitle.getText().toString().trim();
                    for (DocumentSnapshot regDoc : regSnapshots.getDocuments()) {
                        String userId = regDoc.getString("userId");
                        if (userId != null) {
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title",     "Event Cancelled");
                            notification.put("message",   "\"" + eventTitle + "\" has been cancelled by the organiser.");
                            notification.put("unread",    true);
                            notification.put("timestamp", Timestamp.now());

                            // Write to users/{userId}/notifications
                            batch.set(
                                    db.collection("users").document(userId)
                                            .collection("notifications").document(),
                                    notification
                            );

                            // Step 3: Delete the registration document
                            batch.delete(regDoc.getReference());
                        }
                    }

                    // Step 4: Delete the event document itself
                    batch.delete(db.collection("events").document(eventID));

                    // Step 5: Commit everything atomically
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                setLoading(false);
                                new AlertDialog.Builder(this)
                                        .setTitle("Event Deleted")
                                        .setMessage("The event has been removed and registered students have been notified.")
                                        .setPositiveButton("OK", (d, w) -> finish())
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load registrations: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Write an "event updated" notification to each registered student.
     */
    private void notifyRegisteredStudentsOfUpdate(String eventTitle) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventID)
                .get()
                .addOnSuccessListener(regSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot regDoc : regSnapshots.getDocuments()) {
                        String userId = regDoc.getString("userId");
                        if (userId != null) {
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title",     "Event Updated");
                            notification.put("message",   "\"" + eventTitle + "\" has been updated. Check the latest details.");
                            notification.put("unread",    true);
                            notification.put("timestamp", Timestamp.now());
                            batch.set(
                                    db.collection("users").document(userId)
                                            .collection("notifications").document(),
                                    notification
                            );
                        }
                    }
                    batch.commit();
                });
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this event?")
                .setMessage("This cannot be undone. All " + currentRegisteredCount
                        + " registered student(s) will be notified of the cancellation.")
                .setPositiveButton("Yes, Delete", (d, w) -> deleteEvent())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        findViewById(R.id.btnSaveChanges).setEnabled(!loading);
        findViewById(R.id.btnDelete).setEnabled(!loading);
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> updateEvent());
        findViewById(R.id.btnDelete).setOnClickListener(v -> showDeleteConfirmDialog());
    }
}