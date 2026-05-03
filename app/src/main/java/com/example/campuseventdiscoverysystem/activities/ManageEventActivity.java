package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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
 * ManageEventActivity
 * Unified Activity responsible for allowing event managers to create, edit, or delete events.
 */
public class ManageEventActivity extends AppCompatActivity {

    // Firebase instances for DB read/write and user authentication
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI components for user input
    private EditText etTitle, etDescription, etDate, etStartTime, etEndTime, etCapacity, etPrice;
    private Spinner spVenue, spCategory;
    private ProgressBar progressBar;
    private Button btnSubmit;
    private ImageButton btnDelete;

    // Adapters required to find index positions when populating existing data
    private ArrayAdapter<CharSequence> venueAdapter;
    private ArrayAdapter<CharSequence> categoryAdapter;

    // State tracking variables
    private String eventID;
    private boolean isEditMode = false;
    private int currentRegisteredCount = 0;
    private String originalStatus = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_event);

        // Initialize firebase instances
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Check if an EVENT_ID was passed to determine Create vs Edit mode
        eventID = getIntent().getStringExtra("EVENT_ID");
        isEditMode = (eventID != null);

        // Link variables to XML views
        bindViews();

        // Set components of the screen
        setupSpinners();
        setupPickers();
        setupNavigation();
        setupMode();
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
        etPrice = findViewById(R.id.etPrice);
        spVenue = findViewById(R.id.spVenue);
        spCategory = findViewById(R.id.spCategory);
        progressBar = findViewById(R.id.progressBar);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnDelete = findViewById(R.id.btnDelete);
    }

    /**
     * Configures the UI based on whether the user is Creating or Editing
     */
    private void setupMode() {
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);

        if (isEditMode) {
            tvToolbarTitle.setText("Edit Event");
            btnSubmit.setText("Save Changes");
            btnDelete.setVisibility(View.VISIBLE);
            // Subtitle text is cleared here and set dynamically in loadEventData()
            tvSubtitle.setText("Loading details...");
            loadEventData();
        } else {
            tvToolbarTitle.setText("Create New Event");
            tvSubtitle.setText("Fill in the core information for your event.");
            btnSubmit.setText("Submit Event");
            btnDelete.setVisibility(View.GONE);
        }
    }

    /**
     * Initializes the Spinners with data
     */
    private void setupSpinners() {
        // Use custom layouts so selected text is black on white, dropdown also black on white
        venueAdapter = ArrayAdapter.createFromResource(this,
                R.array.venue_array, R.layout.item_spinner_selected);
        venueAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spVenue.setAdapter(venueAdapter);
        // Force dark text on selected item regardless of theme
        spVenue.post(() -> {
            android.widget.TextView tv = (android.widget.TextView) spVenue.getSelectedView();
            if (tv != null) tv.setTextColor(android.graphics.Color.BLACK);
        });

        categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.category_array, R.layout.item_spinner_selected);
        categoryAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spCategory.setAdapter(categoryAdapter);
        spCategory.post(() -> {
            android.widget.TextView tv = (android.widget.TextView) spCategory.getSelectedView();
            if (tv != null) tv.setTextColor(android.graphics.Color.BLACK);
        });
        // Also set listeners to keep text dark after selection changes
        spVenue.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                if (v instanceof android.widget.TextView) ((android.widget.TextView) v).setTextColor(android.graphics.Color.BLACK);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        spCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                if (v instanceof android.widget.TextView) ((android.widget.TextView) v).setTextColor(android.graphics.Color.BLACK);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
    }

    /**
     * Sets up the popup dialogues for selecting Dates and Times
     */
    private void setupPickers() {
        // Calendar Picker for Event Date
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dpd = new DatePickerDialog(this, R.style.PurplePickerTheme, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year));
                etDate.setError(null);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

            // Prevent selecting past dates
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dpd.show();
        });

        // Clock Picker for Start Time
        etStartTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                etStartTime.setError(null);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });

        // Clock Picker for End Time
        etEndTime.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, R.style.PurplePickerTheme, (view, hourOfDay, minute) -> {
                etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                etEndTime.setError(null);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        });
    }

    /**
     * Fetches the current event's data from Firestore and populates the fields (Edit Mode Only)
     */
    private void loadEventData() {
        db.collection("events").document(eventID).get().addOnSuccessListener(d -> {
            if (d.exists()) {

                originalStatus = d.getString("status") != null ? d.getString("status") : "";

                // Dynamically update the subtitle based on the status
                TextView tvSubtitle = findViewById(R.id.tvSubtitle);
                if ("approved".equals(originalStatus) || "active".equals(originalStatus)) {
                    tvSubtitle.setText("Registered students will be notified of changes.");
                } else if ("rejected".equals(originalStatus) || "declined".equals(originalStatus)) {
                    tvSubtitle.setText("Update the details to resubmit for admin approval.");
                } else {
                    tvSubtitle.setText("Update your event details for admin review.");
                }

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

                Double price = d.getDouble("price");
                if (price != null && price > 0 && etPrice != null) {
                    etPrice.setText(String.valueOf(price));
                }

                Long regCount = d.getLong("registeredCount");
                currentRegisteredCount = regCount != null ? regCount.intValue() : 0;

                String venue = d.getString("venue");
                if (venue != null) {
                    spVenue.setSelection(venueAdapter.getPosition(venue));
                }
                String category = d.getString("category");
                if (category != null) {
                    spCategory.setSelection(categoryAdapter.getPosition(category));
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to load event data!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Performs validation on user inputs before saving
     */
    private void validateAndSave() {
        // Retrieve text and selected items from input fields and spinners
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dateStr = etDate.getText().toString().trim();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();
        String capacityStr = etCapacity.getText().toString().trim();
        String priceStr = etPrice != null ? etPrice.getText().toString().trim() : "0";
        String venue = spVenue.getSelectedItem().toString();
        String category = spCategory.getSelectedItem().toString();
        boolean hasError = false;

        // Validate to ensure fields are not empty
        if (title.isEmpty()) { etTitle.setError("Title is required"); hasError = true; }
        if (description.isEmpty()) { etDescription.setError("Description is required"); hasError = true; }
        if (dateStr.isEmpty()) { etDate.setError("Date is required"); hasError = true; }
        if (startTime.isEmpty()) { etStartTime.setError("Start time is required"); hasError = true; }
        if (endTime.isEmpty()) { etEndTime.setError("End time is required"); hasError = true; }

        // Time logic check
        if (!startTime.isEmpty() && !endTime.isEmpty() && startTime.compareTo(endTime) >= 0) {
            etEndTime.setError("End time must be after start time");
            hasError = true;
        }

        // Convert variables to required formats from string
        int capacity = 0;
        if (capacityStr.isEmpty()) {
            etCapacity.setError("Capacity is required");
            hasError = true;
        } else {
            try {
                capacity = Integer.parseInt(capacityStr);
                if (capacity <= 0 && !isEditMode) {
                    etCapacity.setError("Capacity must be greater than 0");
                    hasError = true;
                } else if (capacity < currentRegisteredCount) {
                    etCapacity.setError("Capacity cannot be less than current registrations (" + currentRegisteredCount + ")");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                etCapacity.setError("Capacity must be a valid number");
                hasError = true;
            }
        }

        if (hasError) return;

        Timestamp date = null;
        try {
            SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date parsedDate = df.parse(dateStr);
            date = new Timestamp(parsedDate);
        } catch (Exception e) {
            Toast.makeText(this, "Invalid date format! Use DD/MM/YYYY", Toast.LENGTH_SHORT).show();
            return;
        }

        // Read optional price field dynamically
        double price = 0.0;
        if (!priceStr.isEmpty()) {
            try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
        }

        setLoading(true);

        // Before pushing to the database, check if the venue is already booked
        checkVenueAvailability(title, description, date, startTime, endTime, capacity, venue, category, price);
    }

    /**
     * Queries Firestore to ensure the venue is not double-booked by an approved event.
     */
    private void checkVenueAvailability(String title, String description, Timestamp date, String startTime, String endTime, int capacity, String venue, String category, double price) {
        // Calculate the start and end of the chosen day to narrow down the database query
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.setTime(date.toDate());
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);

        Calendar endOfDay = Calendar.getInstance();
        endOfDay.setTime(date.toDate());
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);

        // Query events occurring on the same day
        db.collection("events")
                .whereGreaterThanOrEqualTo("date", new Timestamp(startOfDay.getTime()))
                .whereLessThanOrEqualTo("date", new Timestamp(endOfDay.getTime()))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isDoubleBooked = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Check if venue matches and if the event is an approved event
                        if (venue.equals(doc.getString("venue")) && "approved".equals(doc.getString("status"))) {

                            // Ignore the current event being edited
                            if (isEditMode && doc.getId().equals(eventID)) {
                                continue;
                            }

                            String existingStart = doc.getString("startTime");
                            String existingEnd = doc.getString("endTime");

                            if (existingStart != null && existingEnd != null) {
                                if (isTimeOverlapping(startTime, endTime, existingStart, existingEnd)) {
                                    isDoubleBooked = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (isDoubleBooked) {
                        setLoading(false);
                        new AlertDialog.Builder(this)
                                .setTitle("Venue Unavailable")
                                .setMessage("The venue '" + venue + "' is already booked by an approved event during this time. Please choose another venue or time.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        // Safe to proceed! Route to Create or Edit execution
                        if (isEditMode) {
                            executeUpdate(title, description, date, startTime, endTime, capacity, venue, category, price);
                        } else {
                            executeCreate(title, description, date, startTime, endTime, capacity, venue, category, price);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to check venue availability: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Checks if two time windows mathematically overlap.
     */
    private boolean isTimeOverlapping(String newStartStr, String newEndStr, String existingStartStr, String existingEndStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date nStart = sdf.parse(newStartStr);
            Date nEnd = sdf.parse(newEndStr);
            Date eStart = sdf.parse(existingStartStr);
            Date eEnd = sdf.parse(existingEndStr);

            return nStart.before(eEnd) && nEnd.after(eStart);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    /**
     * Executes the Create Event flow (fetching user data, building object, pushing to DB)
     */
    private void executeCreate(String title, String description, Timestamp date, String startTime, String endTime, int capacity, String venue, String category, double price) {
        String uid = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();

        // Fetch society name and creator name
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String societyName = "Unknown Society";
                    String societyId   = "";
                    String name = "Unknown";
                    if (doc.exists()) {
                        if (doc.getString("societyName") != null) societyName = doc.getString("societyName");
                        if (doc.getString("societyId")   != null) societyId   = doc.getString("societyId");
                        if (doc.getString("name")        != null) name        = doc.getString("name");
                    }

                    // Instantiate new event model
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
                    newEvent.setPrice(price);

                    // Set status to pending so admin can review it
                    newEvent.setStatus("pending_approval");
                    newEvent.setCreatedBy(uid);
                    newEvent.setSubmittedByEmail(email);
                    newEvent.setSubmittedByName(name);

                    // Build the Firestore map so we can add societyId (not in Event model)
                    Map<String, Object> eventMap = new HashMap<>();
                    eventMap.put("title",            newEvent.getTitle());
                    eventMap.put("description",      newEvent.getDescription());
                    eventMap.put("date",             newEvent.getDate());
                    eventMap.put("startTime",        newEvent.getStartTime());
                    eventMap.put("endTime",          newEvent.getEndTime());
                    eventMap.put("capacity",         newEvent.getCapacity());
                    eventMap.put("registeredCount",  0);
                    eventMap.put("venue",            newEvent.getVenue());
                    eventMap.put("category",         newEvent.getCategory());
                    eventMap.put("society",          newEvent.getSociety());
                    eventMap.put("societyId",        societyId);
                    eventMap.put("price",            newEvent.getPrice());
                    eventMap.put("status",           "pending_approval");
                    eventMap.put("createdBy",        uid);
                    eventMap.put("submittedByEmail", email);
                    eventMap.put("submittedByName",  name);

                    // Push the event map to events collection in firestore
                    db.collection("events").add(eventMap)
                            .addOnSuccessListener(dr -> {
                                setLoading(false);
                                showSuccessDialog("Event submitted for evaluation!", "Your event has been sent to the admins.");
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Failed to push event to database!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Executes the Edit Event flow (building updates map, pushing to DB, notifying students)
     */
    private void executeUpdate(String title, String description, Timestamp date, String startTime, String endTime, int capacity, String venue, String category, double price) {
        // Create a map of updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("date", date);
        updates.put("startTime", startTime);
        updates.put("endTime", endTime);
        updates.put("capacity", capacity);
        // Save waitlist enabled state
        androidx.appcompat.widget.SwitchCompat switchWaitlist = findViewById(R.id.switchWaitlist);
        boolean waitlistEnabled = switchWaitlist != null && switchWaitlist.isChecked();
        updates.put("waitlistEnabled", waitlistEnabled);
        updates.put("venue", venue);
        updates.put("category", category);
        updates.put("price", price);

        // Reset status to force admin re-approval on edited events
        updates.put("status", "pending_approval");
        updates.put("updatedAt", Timestamp.now());

        // Push the updates to events collection
        db.collection("events").document(eventID).update(updates)
                .addOnSuccessListener(dr -> {
                    setLoading(false);

                    // Dynamic Success Dialog based on originalStatus
                    String successMsg;
                    if ("approved".equals(originalStatus) || "active".equals(originalStatus)) {
                        notifyRegisteredStudentsOfUpdate(title);
                        successMsg = "The event is now pending re-approval. Registered students have been notified.";
                    } else {
                        successMsg = "Changes saved successfully! The event is now pending admin approval.";
                    }

                    showSuccessDialog("Changes saved successfully!", successMsg);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to update event!", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes the event document entirely from the Firestore database
     */
    private void deleteEvent() {
        setLoading(true);

        // Fetch all registrations for this event
        db.collection("registrations")
                .whereEqualTo("eventId", eventID)
                .get()
                .addOnSuccessListener(regSnapshots -> {
                    WriteBatch batch = db.batch();
                    String eventTitle = etTitle.getText().toString().trim();

                    // Loop through and create deletion notifications + delete the registration documents
                    for (DocumentSnapshot regDoc : regSnapshots.getDocuments()) {
                        String userId = regDoc.getString("userId");
                        if (userId != null) {
                            Map<String, Object> notification = new HashMap<>();
                            notification.put("title", "Event Cancelled");
                            notification.put("message", "\"" + eventTitle + "\" has been cancelled by the organiser.");
                            notification.put("unread", true);
                            notification.put("timestamp", Timestamp.now());

                            batch.set(
                                    db.collection("users").document(userId)
                                            .collection("notifications").document(),
                                    notification
                            );

                            batch.delete(regDoc.getReference());
                        }
                    }

                    // Delete the event itself
                    batch.delete(db.collection("events").document(eventID));

                    // Commit batch
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                setLoading(false);

                                // Dynamic Delete Success Message based on originalStatus
                                String successMsg = ("approved".equals(originalStatus) || "active".equals(originalStatus))
                                        ? "Registered students will be notified."
                                        : "The event has been removed from your history.";

                                showSuccessDialog("Event deleted successfully!", successMsg);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Failed to delete event!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Could not load registrations to delete event!", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Writes an update notification to all registered students
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
                            notification.put("title", "Event Updated");
                            notification.put("message", "\"" + eventTitle + "\" has been updated. Check the latest details.");
                            notification.put("unread", true);
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

    /**
     * Reusable popup dialog for confirming successful actions
     */
    private void showSuccessDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", ((dialog, which) -> finish()))
                .show();
    }

    /**
     * Popup dialog asking event deletion confirmation
     */
    private void showDeleteConfirmDialog() {
        // Dynamic Delete Confirm Dialog based on originalStatus
        String message;
        if ("approved".equals(originalStatus) || "active".equals(originalStatus)) {
            message = "This cannot be undone. All " + currentRegisteredCount + " registered student(s) will be notified of event cancellation!";
        } else {
            message = "This cannot be undone. Are you sure you want to delete this event?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete this event?")
                .setMessage(message)
                .setPositiveButton("Yes, Delete!", ((dialog, which) -> deleteEvent()))
                .setNegativeButton("No, Cancel!", null)
                .show();
    }

    /**
     * Handles routing for the back, delete and save changes button
     */
    private void setupNavigation() {
        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Save Changes / Create Button
        btnSubmit.setOnClickListener(v -> validateAndSave());

        // Delete Button
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    /**
     * Handles UI loading state to prevent duplicate submissions
     */
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        btnSubmit.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
    }
}