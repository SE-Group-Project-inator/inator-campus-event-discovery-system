package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
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
import java.util.Date;
import java.util.Locale;

/**
 * Activity responsible for allowing event managers to create and submit new events
 * US-02 (Create & Publish Event)
 * US-03 (Set Event Capacity)
 */
public class CreateEventActivity extends AppCompatActivity {
    // Firebase instances for DB read/write and user authentication
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI components for user input
    private EditText etTitle, etDescription, etDate, etTime, etCapacity;
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
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etCapacity = findViewById(R.id.etCapacity);
        spVenue = findViewById(R.id.spVenue);
        spCategory = findViewById(R.id.spCategory);

        // Spinner setup
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

        // Back button setup
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Submit button setup
        findViewById(R.id.btnSubmit).setOnClickListener(v -> saveEvent());
    }

    // Gather data from inputs, validate it and save it to firestore
    private void saveEvent() {

        // Get the current user's detail (creator of event)
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();
        String name = mAuth.getCurrentUser().getDisplayName();

        // Retrieve text and selected items from input fields and spinners
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String venue = spVenue.getSelectedItem().toString();
        String category = spCategory.getSelectedItem().toString();

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

        // Validate to ensure fields are not empty
        if (title.isEmpty() || description.isEmpty() || dateStr.isEmpty() || time.isEmpty()
                || capacityStr.isEmpty() || venue.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Instantiate new event model
        Event newEvent = new Event();
        newEvent.setTitle(title);
        newEvent.setDescription(description);
        newEvent.setDate(date);
        newEvent.setTime(time);
        newEvent.setCapacity(capacity);
        newEvent.setVenue(venue);
        newEvent.setCategory(category);

        // Set status to pending so admin can review it
        newEvent.setStatus("pending_approval");
        newEvent.setCreatedBy(uid);
        newEvent.setSubmittedByEmail(email);

        // Push the event object to events collection in firestore
        db.collection("events").add(newEvent)
                .addOnSuccessListener(dr -> {
                    showSubmitSuccessDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Popup dialog confirming event submission
    private void showSubmitSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Event submitted for evaluation!")
                .setMessage("Your event has been sent to the admins!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }
}
