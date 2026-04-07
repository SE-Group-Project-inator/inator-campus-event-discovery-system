package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
 * CreateEventActivity
 * Activity responsible for allowing event managers to create and submit new events
 */
public class CreateEventActivity extends AppCompatActivity {

    // Firebase instances for DB read/write and user authentication
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI components for user input
    private EditText etTitle, etDescription, etDate, etStartTime, etEndTime, etCapacity;
    private Spinner spVenue, spCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind activity to its corresponding XML layout file
        setContentView(R.layout.activity_create_event);

        // Initialize firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link variables to XML views
        bindViews();

        // Set components of the screen
        setupSpinners();
        setupPickers();
        setupNavigation();
    }

    /**
     * Maps all the XML UI components to Java variables
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
        ArrayAdapter<CharSequence> venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, android.R.layout.simple_spinner_item);
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVenue.setAdapter(venueAdapter);

        // Setup Category Spinner
        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);
    }

    /**
     * Sets up the popup dialogues for selecting Dates and Times
     */
    private void setupPickers() {
        // Date Picker
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, R.style.PurplePickerTheme, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Start Time Picker
        etStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        // End Time Picker
        etEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    /**
     * Validates input and fetches society name
     */
    private void validateFields() {

        // Get the current user's detail (creator of event)
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();
        String name = mAuth.getCurrentUser().getDisplayName();

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

        final Timestamp finalDate = date;

        // Fetch society name
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String societyName = "Unknown Society";
                    if (doc.exists() && doc.getString("societyName") != null) {
                        societyName = doc.getString("societyName");
                    }
                    saveEvent(title, description, finalDate, startTime, endTime,
                            capacity, venue, category, uid, email, name, societyName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch society name!", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Creates an event object and pushes it to the database
     */
    private void saveEvent(String title, String description, Timestamp date, String startTime, String endTime,
                           int capacity, String venue, String category, String uid, String email, String name, String societyName) {

        // Instantiate new event model
        Event newEvent = new Event();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setDate(date);
        newEvent.setStartTime(startTime);
        newEvent.setEndTime(endTime);
        newEvent.setCapacity(capacity);
        newEvent.setVenue(venue);
        newEvent.setCategory(category);
        newEvent.setSociety(societyName);

        // Set status to pending so admin can review it
        newEvent.setStatus("pending_approval");
        newEvent.setCreatedBy(uid);
        newEvent.setSubmittedByEmail(email);
        newEvent.setSubmittedByName(name);

        // Push the event object to events collection in firestore
        db.collection("events").add(newEvent)
                .addOnSuccessListener(dr -> {
                    showSubmitSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to push event to database!", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Popup dialog confirming event submission
     */
    private void showSubmitSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Event submitted for evaluation!")
                .setMessage("Your event has been sent to the admins!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }

    /**
     * Handles routing for the back and submit button
     */
    private void setupNavigation() {

        // Back button setup
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
        });

        // Submit button setup
        findViewById(R.id.btnSubmit).setOnClickListener(v -> {
            validateFields();
        });
    }
}
