package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EditEventActivity
 * Activity responsible for allowing event managers to edit or delete existing events.
 */
public class EditEventActivity extends AppCompatActivity {

    // Firebase instance for DB operations
    private FirebaseFirestore db;

    // UI components for user input
    private EditText etTitle, etDescription, etDate, etStartTime, etEndTime, etCapacity;
    private Spinner spVenue, spCategory;

    // Adapters required to find index positions when populating existing data
    private ArrayAdapter<CharSequence> venueAdapter;
    private ArrayAdapter<CharSequence> categoryAdapter;

    // ID of the current event being edited
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind activity to its corresponding XML layout file
        setContentView(R.layout.activity_edit_event);

        // Initialize firebase instances
        db = FirebaseFirestore.getInstance();

        // Retrieve eventID passed from the previous screen
        eventID = getIntent().getStringExtra("EVENT_ID");
        if (eventID == null) {
            Toast.makeText(this, "Failed to retrieve eventID!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link variables to XML views
        bindViews();

        // Set components of the screen
        setupSpinners();
        setupPickers();
        setupNavigation();

        // Fetch existing data to populate the form
        loadEventData();
    }

    /**
     * Maps all XML components to their corresponding Java variables
     */
    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        etCapacity = findViewById(R.id.etCapacity);
        spVenue = findViewById(R.id.spVenue);
        spCategory = findViewById(R.id.spCategory);
    }

    /**
     * Initializes the Spinners with data
     */
    private void setupSpinners() {

        // Setup Venue Spinner
        venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, android.R.layout.simple_spinner_item);
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVenue.setAdapter(venueAdapter);

        // Setup Category Spinner
        categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
    }

    /**
     * Sets up the popup dialogues for selecting Dates and Times
     */
    private void setupPickers() {

        // Calendar Picker for Event Date
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, R.style.PurplePickerTheme, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Clock Picker for Start Time
        etStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        // Clock Picker for End Time
        etEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    /**
     * Fetches the current event's data from Firestore and populates the fields
     */
    private void loadEventData() {
        db.collection("events").document(eventID).get().addOnSuccessListener(d -> {
            if (d.exists()) {

                // Populate text fields
                etTitle.setText(d.getString("title"));
                etDescription.setText(d.getString("description"));
                etStartTime.setText(d.getString("startTime") != null ? d.getString("startTime") : "");
                etEndTime.setText(d.getString("endTime") != null ? d.getString("endTime") : "");
                Timestamp ts = d.getTimestamp("date");
                if (ts != null) {
                    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    etDate.setText(df.format(ts.toDate()));
                }
                etCapacity.setText(String.valueOf(d.getLong("capacity")));
                String venue = d.getString("venue");
                if (venue != null) {
                    int venuePos = venueAdapter.getPosition(venue);
                    spVenue.setSelection(venuePos);
                }
                String category = d.getString("category");
                if (category != null) {
                    int categoryPos = categoryAdapter.getPosition(category);
                    spCategory.setSelection(categoryPos);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load event data!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Performs validation and updates the database
     */
    private void updateEvent() {

        // Retrieve text and selected items from input fields and spinners
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String venue = spVenue.getSelectedItem().toString();
        String category = spCategory.getSelectedItem().toString();

        // Validate to ensure fields are not empty
        if (title.isEmpty() || description.isEmpty() || dateStr.isEmpty() || startTime.isEmpty()
                || endTime.isEmpty() || capacityStr.isEmpty() || venue.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert variables to required formats from string
        int capacity = Integer.parseInt(capacityStr);
        Timestamp date = null;
        try {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date parsedDate = df.parse(dateStr);
            date = new Timestamp(parsedDate);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format! Use DD/MM/YYYY", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map of updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("date", date);
        updates.put("startTime", startTime);
        updates.put("endTime", endTime);
        updates.put("capacity", capacity);
        updates.put("venue", venue);
        updates.put("category", category);

        // Push the updates to events collection
        db.collection("events").document(eventID).update(updates)
                .addOnSuccessListener(dr -> showUpdateSuccessDialog())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update event!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Deletes the event document entirely from the Firestore database
     */
    private void deleteEvent() {
        db.collection("events").document(eventID).delete()
                .addOnSuccessListener(v -> showDeleteSuccessDialog())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete event!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Popup dialog asking event deletion confirmation
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this event?")
                .setMessage("Registered students will be notified of event cancellation!")
                .setPositiveButton("Yes, Delete!", ((dialog, which) -> deleteEvent()))
                .setNegativeButton("No, Cancel!", null)
                .show();
    }

    /**
     * Popup dialog confirming event deletion
     */
    private void showDeleteSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Event deleted successfully!")
                .setMessage("Registered students will be notified!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }

    /**
     * Popup dialog confirming event updates
     */
    private void showUpdateSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Changes saved successfully!")
                .setMessage("Registered students will be notified of the updates!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();

        // Will implement notification sending feature
    }

    /**
     * Handles routing for the back, delete and save changes button
     */
    private void setupNavigation() {

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });

        // Save Changes Button
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> {
            updateEvent();
        });

        // Delete Button
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            showDeleteConfirmDialog();
        });
    }
}