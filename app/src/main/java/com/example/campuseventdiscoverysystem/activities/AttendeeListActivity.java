package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.AttendeeAdapter;
import com.example.campuseventdiscoverysystem.models.Registration;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US-27: Event Manager Attendee List — Event Analytics screen.
 * US-32: Seat/Registration Confirmation — "+ Register" → "✓ Registered".
 */
public class AttendeeListActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private final List<Registration> allRegistrations = new ArrayList<>();
    private final List<Registration> displayList = new ArrayList<>();
    private AttendeeAdapter adapter;
    private TextView tvTotalRegistered, tvCheckInRate, tvWaitlist,
            tvCapacityPct, tvEmpty;
    private ProgressBar progressCapacity;
    private String eventId;
    private boolean readOnly;
    private int eventCapacity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendee_list);

        db = FirebaseFirestore.getInstance();

        eventId       = getIntent().getStringExtra("eventId");
        readOnly      = getIntent().getBooleanExtra("readOnly", false);
        eventCapacity = getIntent().getIntExtra("eventCapacity", 0);
        String eventTitle = getIntent().getStringExtra("eventTitle");
        String eventVenue = getIntent().getStringExtra("eventVenue");

        ((TextView) findViewById(R.id.tvEventName))
                .setText(eventTitle != null ? eventTitle : "Event");
        if (eventVenue != null) {
            ((TextView) findViewById(R.id.tvEventVenue)).setText(eventVenue);
        }

        tvTotalRegistered = findViewById(R.id.tvTotalRegistered);
        tvCheckInRate     = findViewById(R.id.tvCheckInRate);
        tvWaitlist        = findViewById(R.id.tvWaitlist);
        tvCapacityPct     = findViewById(R.id.tvCapacityPct);
        progressCapacity  = findViewById(R.id.progressCapacity);
        tvEmpty           = findViewById(R.id.tvEmpty);

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
                if (matchesEmail || matchesName) {
                    displayList.add(r);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        // "View List" scrolls to the list (it's already visible, so no-op visually)
        findViewById(R.id.btnViewList).setOnClickListener(v -> {
            RecyclerView rv = findViewById(R.id.rvAttendees);
            rv.smoothScrollToPosition(0);
        });
    }

    private void loadAttendees() {
        if (eventId == null || eventId.isEmpty()) return;

        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    allRegistrations.clear();
                    int confirmedCount = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        Registration reg = doc.toObject(Registration.class);
                        if (reg != null) {
                            reg.setId(doc.getId());
                            allRegistrations.add(reg);
                            if (reg.isConfirmed()) confirmedCount++;
                        }
                    }

                    displayList.clear();
                    displayList.addAll(allRegistrations);
                    adapter.notifyDataSetChanged();

                    updateStats(confirmedCount);
                    updateEmptyState();
                });
    }

    private void updateStats(int confirmedCount) {
        int total = allRegistrations.size();
        tvTotalRegistered.setText(String.valueOf(total));

        int checkInPct = total > 0 ? (confirmedCount * 100 / total) : 0;
        tvCheckInRate.setText(checkInPct + "%");

        // Waitlist = unconfirmed registrations
        tvWaitlist.setText(String.valueOf(total - confirmedCount));

        // Capacity
        if (eventCapacity > 0) {
            int capPct = Math.min(total * 100 / eventCapacity, 100);
            tvCapacityPct.setText(capPct + "%");
            progressCapacity.setProgress(capPct);
        } else {
            tvCapacityPct.setText("–");
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
     * US-32: Confirms a student's registration (marks confirmed = true).
     */
    private void confirmRegistration(Registration registration, int position) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("confirmed", true);

        db.collection("registrations")
                .document(registration.getId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    registration.setConfirmed(true);
                    adapter.updateItem(position);
                    Toast.makeText(this,
                            "Registration confirmed for " + registration.getUserName(),
                            Toast.LENGTH_SHORT).show();
                    // Refresh stats
                    int confirmed = 0;
                    for (Registration r : allRegistrations) {
                        if (r.isConfirmed()) confirmed++;
                    }
                    updateStats(confirmed);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
