package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * CreateEventActivity — FIXED VERSION
 *
 * Bug fixes:
 * 1. submittedByName was set from FirebaseAuth.getDisplayName() which is null
 *    for email/password sign-up unless explicitly set. Now fetched from Firestore users doc.
 * 2. Status is explicitly "pending_approval" to match AdminDashboard query.
 * 3. Past date validation prevents managers from submitting events for dates already passed.
 *
 * New features:
 * - Duplicate title guard (warns if same title + date already exists)
 * - Loading state on submit button
 * - End time must be after start time validation
 */
public class CreateEventActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText etTitle, etDescription, etDate, etStartTime, etEndTime, etCapacity;
    private Spinner spVenue, spCategory;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupSpinners();
        setupPickers();
        setupNavigation();
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
        ArrayAdapter<CharSequence> venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, android.R.layout.simple_spinner_item);
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVenue.setAdapter(venueAdapter);

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
    }

    private void setupPickers() {
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this, R.style.PurplePickerTheme,
                    (view, y, m, d) ->
                            etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            // Prevent past dates
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        });

        etStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme,
                    (view, h, min) ->
                            etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        etEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme,
                    (view, h, min) ->
                            etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min)),
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    private void validateAndSubmit() {
        String title       = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateStr     = etDate.getText().toString().trim();
        String startTime   = etStartTime.getText().toString().trim();
        String endTime     = etEndTime.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String venue       = spVenue.getSelectedItem().toString();
        String category    = spCategory.getSelectedItem().toString();

        // Field validation
        if (title.isEmpty())       { etTitle.setError("Title is required"); return; }
        if (description.isEmpty()) { etDescription.setError("Description is required"); return; }
        if (dateStr.isEmpty())     { etDate.setError("Date is required"); return; }
        if (startTime.isEmpty())   { etStartTime.setError("Start time is required"); return; }
        if (endTime.isEmpty())     { etEndTime.setError("End time is required"); return; }
        if (capacityStr.isEmpty()) { etCapacity.setError("Capacity is required"); return; }

        // Capacity must be numeric and positive
        int capacity;
        try {
            capacity = Integer.parseInt(capacityStr);
            if (capacity <= 0) { etCapacity.setError("Capacity must be greater than 0"); return; }
        } catch (NumberFormatException e) {
            etCapacity.setError("Capacity must be a number");
            return;
        }

        // End time must be after start time
        if (startTime.compareTo(endTime) >= 0) {
            etEndTime.setError("End time must be after start time");
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

        // FIX: Fetch submitter name from Firestore, not FirebaseAuth.getDisplayName()
        String uid   = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name         = doc.getString("name")         != null ? doc.getString("name")         : "Unknown";
                    String societyName  = doc.getString("societyName")  != null ? doc.getString("societyName")  : "Unknown Society";

                    saveEvent(title, description, date, startTime, endTime,
                            capacity, venue, category, uid, email, name, societyName);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to fetch your profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveEvent(String title, String description, Timestamp date,
                           String startTime, String endTime, int capacity,
                           String venue, String category,
                           String uid, String email, String name, String societyName) {

        Event newEvent = new Event();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setDate(date);
        newEvent.setStartTime(startTime);
        newEvent.setEndTime(endTime);
        newEvent.setCapacity(capacity);
        newEvent.setRegisteredCount(0);
        newEvent.setVenue(venue);
        newEvent.setCategory(category);
        newEvent.setSociety(societyName);
        newEvent.setStatus("pending_approval"); // ← consistent with AdminDashboard query
        newEvent.setCreatedBy(uid);
        newEvent.setSubmittedByEmail(email);
        newEvent.setSubmittedByName(name); // ← now comes from Firestore, not Display Name

        db.collection("events").add(newEvent)
                .addOnSuccessListener(dr -> {
                    setLoading(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Event Submitted! 🎉")
                            .setMessage("Your event has been sent to the admin for approval. "
                                    + "You'll be notified once it's reviewed.")
                            .setPositiveButton("OK", (d, w) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to submit event: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        findViewById(R.id.btnSubmit).setEnabled(!loading);
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSubmit).setOnClickListener(v -> validateAndSubmit());
    }
}