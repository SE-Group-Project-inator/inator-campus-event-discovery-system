package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.AttendeeAdapter;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.firebase.Timestamp;

/**
 * ============================================================
 * AttendeeListActivity
 * ============================================================
 *
 * PURPOSE:
 * Displays list of attendees (registrations) for a specific event.
 *
 * FEATURES:
 * - Shows confirmed attendees for an event (Firestore "rsvps")
 * - Supports search/filter by name or email
 * - Student mode (readOnly = true):
 *      -> Can reserve a spot (opens RsvpActivity)
 * - Event Manager mode (readOnly = false):
 *      -> Can confirm registrations
 *
 * USER STORIES:
 * US-27: Attendee List viewing for event participants
 * US-32: Registration confirmation by event manager
 *
 * DATA FLOW:
 * rsvps (event registrations) → users (profile info enrichment)
 *
 * PRIVACY HANDLING:
 * - isNameVisible / isRollNoVisible controls what is shown
 * - Otherwise fallback to "Anonymous Attendee"
 */
public class AttendeeListActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    // Full dataset from Firestore
    private final List<Registration> allRegistrations = new ArrayList<>();

    // Filtered dataset shown in RecyclerView
    private final List<Registration> displayList = new ArrayList<>();

    private AttendeeAdapter adapter;
    private TextView tvEmpty;
    private String eventId;

    // Determines UI mode:
    // true  -> student view (read-only)
    // false -> event manager view (can confirm attendees)
    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_list);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();

        // Accept eventId from multiple sources for flexibility
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) eventId = getIntent().getStringExtra("EVENT_ID");

        // Determine screen mode (student vs manager)
        readOnly = getIntent().getBooleanExtra("readOnly", false);

        // Empty state UI
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup core UI components
        setupRecyclerView();
        setupSearch();
        setupNavigation();

        // Load attendee data from Firestore
        loadAttendees();
    }

    /**
     * Initializes RecyclerView and adapter.
     * Adapter behavior changes based on readOnly mode.
     */
    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvAttendees);

        adapter = new AttendeeAdapter(
                displayList,
                (registration, position) -> confirmRegistration(registration, position),
                readOnly
        );

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    /**
     * Sets up search bar to filter attendees dynamically.
     */
    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                // Filter list in real-time as user types
                filterList(s.toString().trim());
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Filters attendee list based on name or email match.
     */
    private void filterList(String query) {
        displayList.clear();

        if (query.isEmpty()) {
            displayList.addAll(allRegistrations);
        } else {
            String lower = query.toLowerCase();

            for (Registration r : allRegistrations) {
                boolean matchesEmail = r.getUserEmail() != null
                        && r.getUserEmail().toLowerCase().contains(lower);

                boolean matchesName = r.getUserName() != null
                        && r.getUserName().toLowerCase().contains(lower);

                if (matchesEmail || matchesName) {
                    displayList.add(r);
                }
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /**
     * Handles navigation buttons and role-based UI actions.
     */
    private void setupNavigation() {

        // Back button closes activity
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButton btnReserve = findViewById(R.id.btnReserve);

        if (readOnly) {
            // Student mode: allow RSVP
            btnReserve.setVisibility(View.VISIBLE);

            btnReserve.setOnClickListener(v -> {
                Intent intent = new Intent(this, RsvpActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                intent.putExtra("EVENT_TITLE", getIntent().getStringExtra("eventTitle"));
                startActivity(intent);
            });
        } else {
            // Manager mode: hide reservation button
            btnReserve.setVisibility(View.GONE);
        }
    }

    /**
     * Loads attendees from Firestore:
     * - Fetches confirmed RSVPs
     * - Enriches with user profile data
     * - Applies privacy settings
     */
    private void loadAttendees() {

        if (eventId == null || eventId.isEmpty()) return;

        db.collection("rsvps")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("status", "confirmed")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null || snapshots == null) return;

                    List<DocumentSnapshot> rsvpDocs = snapshots.getDocuments();

                    if (rsvpDocs.isEmpty()) {
                        allRegistrations.clear();
                        displayList.clear();
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                        return;
                    }

                    List<Registration> built = new ArrayList<>();
                    AtomicInteger remaining = new AtomicInteger(rsvpDocs.size());

                    for (DocumentSnapshot rsvp : rsvpDocs) {

                        String userId = rsvp.getString("userId");
                        Boolean nameVisible = rsvp.getBoolean("isNameVisible");
                        Boolean rollVisible = rsvp.getBoolean("isRollNoVisible");
                        Timestamp createdAt = rsvp.getTimestamp("createdAt");

                        if (userId == null) {
                            if (remaining.decrementAndGet() == 0) publishAttendees(built);
                            continue;
                        }

                        // Fetch user profile data
                        db.collection("users").document(userId).get()
                                .addOnCompleteListener(task -> {

                                    Registration reg = new Registration();
                                    reg.setId(rsvp.getId());
                                    reg.setEventId(eventId);
                                    reg.setUserId(userId);
                                    reg.setRegisteredAt(createdAt);
                                    reg.setConfirmed(true);

                                    String name = null, email = null;

                                    if (task.isSuccessful() && task.getResult() != null
                                            && task.getResult().exists()) {
                                        name  = task.getResult().getString("name");
                                        email = task.getResult().getString("email");
                                    }

                                    // Apply privacy settings
                                    boolean showName = !Boolean.FALSE.equals(nameVisible);
                                    boolean showRoll = !Boolean.FALSE.equals(rollVisible);

                                    reg.setUserName(showName ? name : "Anonymous Attendee");
                                    reg.setUserEmail(showRoll ? email : null);

                                    synchronized (built) {
                                        built.add(reg);
                                    }

                                    if (remaining.decrementAndGet() == 0) {
                                        publishAttendees(built);
                                    }
                                });
                    }
                });
    }

    /**
     * Publishes final attendee list to UI and updates event stats.
     */
    private void publishAttendees(List<Registration> regs) {

        allRegistrations.clear();
        allRegistrations.addAll(regs);

        displayList.clear();
        displayList.addAll(regs);

        adapter.notifyDataSetChanged();
        updateEmptyState();

        // Sync confirmed count back to event document (self-healing update)
        if (eventId != null && !eventId.isEmpty()) {
            int trueCount = regs.size();

            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(evDoc -> {

                        if (!evDoc.exists()) return;

                        Long cap = evDoc.getLong("capacity");

                        int clamped = cap != null && cap > 0
                                ? Math.min(trueCount, cap.intValue())
                                : trueCount;

                        db.collection("events").document(eventId)
                                .update("registeredCount", Math.max(0, clamped));
                    });
        }
    }

    /**
     * Shows/hides empty state UI based on list content.
     */
    private void updateEmptyState() {

        RecyclerView rv = findViewById(R.id.rvAttendees);

        if (displayList.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * ============================================================
     * US-32: Confirm Registration
     * ============================================================
     *
     * Marks a registration as confirmed in Firestore.
     * Only accessible in event manager mode.
     */
    private void confirmRegistration(Registration registration, int position) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("confirmed", true);

        db.collection("rsvps")
                .document(registration.getId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    registration.setConfirmed(true);
                    adapter.updateItem(position);

                    Toast.makeText(this,
                            "Registration confirmed for " + registration.getUserName(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}