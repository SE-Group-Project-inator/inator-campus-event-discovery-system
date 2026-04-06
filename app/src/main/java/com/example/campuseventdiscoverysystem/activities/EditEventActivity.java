package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity responsible for allowing event managers to edit or delete existing events
 * US-17 (Update Event)
 * US-31 (Event Deletion)
 */
public class EditEventActivity extends AppCompatActivity {
    // Firebase instances for DB read/write
    private FirebaseFirestore db;

    // UI components for user input
    private EditText etTitle, etDescription, etDate, etTime, etCapacity;
    private Spinner spVenue, spCategory;

    // Keep track of adapters
    private ArrayAdapter<CharSequence> venueAdapter;
    private ArrayAdapter<CharSequence> categoryAdapter;

    // Id of the current event
    private String eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind activity to its corresponding XML layout file
        setContentView(R.layout.activity_edit_event);

        // Initialize firebase instances
        db = FirebaseFirestore.getInstance();

        // Retrieve eventID passed
        eventID = getIntent().getStringExtra("EVENT_ID");
        if (eventID == null) {
            return;
        }

        // Link variables to XML views
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etCapacity = findViewById(R.id.etCapacity);
        spVenue = findViewById(R.id.spVenue);
        spCategory = findViewById(R.id.spCategory);

        // Setup venue spinner
        venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, android.R.layout.simple_spinner_item);
        venueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spVenue.setAdapter(venueAdapter);

        // Setup category spinner
        categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(categoryAdapter);

        // Query the events collection from database
        db.collection("events").document(eventID).get().addOnSuccessListener(d -> {
            if (d.exists()) {
                // Populate fields with database values
                etTitle.setText(d.getString("title"));
                etDescription.setText(d.getString("description"));
                etTime.setText(d.getString("time"));
                Timestamp ts = d.getTimestamp("date");
                SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDate.setText(df.format(ts.toDate()));
                etCapacity.setText(String.valueOf(d.getLong("capacity")));
                String venue = d.getString("venue");
                int venuePos = venueAdapter.getPosition(venue);
                spVenue.setSelection(venuePos);
                String category = d.getString("category");
                int categoryPos = venueAdapter.getPosition(category);
                spCategory.setSelection(categoryPos);
            }
        });

        // Back button setup
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // Save Changes button setup
        findViewById(R.id.btnSaveChanges).setOnClickListener(v -> updateEvent());
        // Delete button setup
        findViewById(R.id.btnDelete).setOnClickListener(v -> showDeleteConfirmDialog());
    }

    // Send the updated field to firestore
    private void updateEvent() {

        // Create a map of updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", etTitle.getText().toString());
        updates.put("description", etDescription.getText().toString());
        updates.put("time", etTime.getText().toString());
        Date parsedDate = null;
        try {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            parsedDate = df.parse(etDate.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format! Use DD/MM/YYYY", Toast.LENGTH_SHORT).show();
            return;
        }
        updates.put("date", new Timestamp(parsedDate));
        updates.put("capacity", Integer.parseInt(etCapacity.getText().toString().trim()));
        updates.put("venue", spVenue.getSelectedItem().toString());
        updates.put("category", spCategory.getSelectedItem().toString());

        // Push the updates to events collection in firestore
        db.collection("events").document(eventID).update(updates)
                .addOnSuccessListener(dr -> {
                    showUpdateSuccessDialog();
                });
    }

    // Delete the event from firestore database
    private void deleteEvent() {
        db.collection("events").document(eventID).delete()
                .addOnSuccessListener(v -> {
                    showDeleteSuccessDialog();
                });
    }

    // Popup dialog asking event deletion confirmation
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this event?")
                .setMessage("Registered students will be notified of event cancellation!")
                .setPositiveButton("Yes, Delete!", ((dialog, which) -> deleteEvent()))
                .setNegativeButton("No, Don't!", null)
                .show();
    }

    // Popup dialog confirming event deletion
    private void showDeleteSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Event deleted successfully!")
                .setMessage("Registered students have been notified!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }

    // Popup dialog confirming event updates
    private void showUpdateSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Changes saved successfully!")
                .setMessage("Registered students have been notified!")
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }
}
