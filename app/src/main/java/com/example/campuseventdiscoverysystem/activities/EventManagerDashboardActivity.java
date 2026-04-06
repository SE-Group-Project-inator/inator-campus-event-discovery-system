package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.ManagerEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for event managers after login.
 * Shows all events created by the logged-in event manager.
 * Tapping "Attendees" on any event opens AttendeeListActivity (US-27).
 */
public class EventManagerDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<Event> eventList = new ArrayList<>();
    private ManagerEventAdapter adapter;
    private TextView tvMyEventsCount, tvActiveCount, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvMyEventsCount = findViewById(R.id.tvMyEventsCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvEmpty = findViewById(R.id.tvEmpty);

        setupRecyclerView();
        setupNavigation();
        loadMyEvents();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvMyEvents);
        adapter = new ManagerEventAdapter(eventList, event -> {
            // US-27: open attendee list for this event
            Intent intent = new Intent(this, AttendeeListActivity.class);
            intent.putExtra("eventId", event.getId());
            intent.putExtra("eventTitle", event.getTitle());
            intent.putExtra("eventVenue", event.getVenue());
            intent.putExtra("eventCapacity", event.getCapacity());
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupNavigation() {
        // navHome is the current screen
        findViewById(R.id.navHome).setOnClickListener(v -> { /* already here */ });

        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadMyEvents() {
        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    eventList.clear();
                    int activeCount = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            eventList.add(event);
                            if ("active".equals(event.getStatus())) {
                                activeCount++;
                            }
                        }
                    }

                    // Sort by date descending (newest first)
                    eventList.sort((a, b) -> {
                        if (a.getDate() == null || b.getDate() == null) return 0;
                        return b.getDate().compareTo(a.getDate());
                    });

                    adapter.notifyDataSetChanged();

                    tvMyEventsCount.setText(String.valueOf(eventList.size()));
                    tvActiveCount.setText(String.valueOf(activeCount));

                    RecyclerView rv = findViewById(R.id.rvMyEvents);
                    if (eventList.isEmpty()) {
                        rv.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rv.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                });
    }
}