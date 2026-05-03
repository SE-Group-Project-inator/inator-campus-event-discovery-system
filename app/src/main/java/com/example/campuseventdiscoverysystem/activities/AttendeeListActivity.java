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
 * US-27: Attendee List — shows who is attending an event.
 * US-32: Seat/Registration Confirmation — event manager can confirm registrations.
 * readOnly=true → student view (Reserve A Spot button visible, confirm buttons hidden)
 * readOnly=false → event manager view (confirm buttons visible per row)
 */
public class AttendeeListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<Registration> allRegistrations = new ArrayList<>();
    private final List<Registration> displayList = new ArrayList<>();
    private AttendeeAdapter adapter;
    private TextView tvEmpty;
    private String eventId;
    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_list);

        db = FirebaseFirestore.getInstance();

        // Accept both "eventId" (from EventDetailActivity) and "EVENT_ID" (from EventManagerDashboard)
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) eventId = getIntent().getStringExtra("EVENT_ID");

        readOnly = getIntent().getBooleanExtra("readOnly", false);

        tvEmpty = findViewById(R.id.tvEmpty);

        setupRecyclerView();
        setupSearch();
        setupNavigation();
        loadAttendees();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvAttendees);
        adapter = new AttendeeAdapter(displayList,
                (registration, position) -> confirmRegistration(registration, position),
                readOnly);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterList(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

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
                if (matchesEmail || matchesName) displayList.add(r);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        MaterialButton btnReserve = findViewById(R.id.btnReserve);
        if (readOnly) {
            // Student view — "Reserve A Spot!" opens RsvpActivity
            btnReserve.setVisibility(View.VISIBLE);
            btnReserve.setOnClickListener(v -> {
                Intent intent = new Intent(this, RsvpActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                intent.putExtra("EVENT_TITLE", getIntent().getStringExtra("eventTitle"));
                startActivity(intent);
            });
        } else {
            // Event manager view — bottom button not needed
            btnReserve.setVisibility(View.GONE);
        }
    }

    private void loadAttendees() {
        if (eventId == null || eventId.isEmpty()) return;

        // Reads from the same `rsvps` collection RsvpActivity / PaymentActivity write to.
        // Each rsvp doc only stores userId — for display we fan out to users/{uid} to
        // pick up name + email, then apply the privacy flags (isNameVisible / isRollNoVisible)
        // captured at RSVP time.
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

                                    boolean showName = !Boolean.FALSE.equals(nameVisible);
                                    boolean showRoll = !Boolean.FALSE.equals(rollVisible);
                                    reg.setUserName(showName ? name : "Anonymous Attendee");
                                    reg.setUserEmail(showRoll ? email : null);

                                    synchronized (built) { built.add(reg); }
                                    if (remaining.decrementAndGet() == 0) publishAttendees(built);
                                });
                    }
                });
    }

    private void publishAttendees(List<Registration> regs) {
        allRegistrations.clear();
        allRegistrations.addAll(regs);
        displayList.clear();
        displayList.addAll(regs);
        adapter.notifyDataSetChanged();
        updateEmptyState();
        // Self-heal: write the true confirmed count back to the event document
        if (eventId != null && !eventId.isEmpty()) {
            int trueCount = regs.size();
            db.collection("events").document(eventId).get()
                    .addOnSuccessListener(evDoc -> {
                        if (!evDoc.exists()) return;
                        Long cap = evDoc.getLong("capacity");
                        int clamped = cap != null && cap > 0
                                ? Math.min(trueCount, cap.intValue()) : trueCount;
                        db.collection("events").document(eventId)
                                .update("registeredCount", Math.max(0, clamped));
                    });
        }
    }

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
     * US-32: Confirms a student's registration (sets confirmed = true in Firestore).
     * Only reachable from event manager view (readOnly=false).
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
