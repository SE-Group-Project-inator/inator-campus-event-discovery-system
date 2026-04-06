package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.ManagerEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * Main dashboard for the Event Manager.
 * Shows event history, routes to CreateEvent/EditEvent, and
 * opens AttendeeListActivity (US-27/US-32) via "View Attendees".
 */
public class EventManagerDashboardActivity extends AppCompatActivity {

    private ImageButton btnNotifications;
    private FloatingActionButton fabCreate;
    private LinearLayout navHome, navEvents, navProfile;
    private RecyclerView rvManagerEvents;
    private TextView tvEmpty;
    private ManagerEventAdapter adapter;
    private List<Event> myEventsList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager_dashboard);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnNotifications = findViewById(R.id.btnNotifications);
        fabCreate        = findViewById(R.id.fabCreate);
        navHome          = findViewById(R.id.navHome);
        navEvents        = findViewById(R.id.navEvents);
        navProfile       = findViewById(R.id.navProfile);
        rvManagerEvents  = findViewById(R.id.rvManagerEvents);
        tvEmpty          = findViewById(R.id.tvEmpty);

        setupRecyclerView();
        setupClickListeners();
        loadMyEvents();
    }

    private void setupRecyclerView() {
        myEventsList = new ArrayList<>();

        adapter = new ManagerEventAdapter(
                myEventsList,
                // Tap card → edit event
                eventId -> {
                    Intent intent = new Intent(this, EditEventActivity.class);
                    intent.putExtra("EVENT_ID", eventId);
                    startActivity(intent);
                },
                // Tap "View Attendees" → US-27/32
                event -> {
                    Intent intent = new Intent(this, AttendeeListActivity.class);
                    intent.putExtra("eventId",       event.getId());
                    intent.putExtra("eventTitle",    event.getTitle());
                    intent.putExtra("eventVenue",    event.getVenue());
                    intent.putExtra("eventCapacity", event.getCapacity());
                    startActivity(intent);
                }
        );

        rvManagerEvents.setLayoutManager(new LinearLayoutManager(this));
        rvManagerEvents.setAdapter(adapter);
    }

    private void loadMyEvents() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Failed to load events.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        myEventsList.clear();
                        for (DocumentSnapshot doc : snapshots) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setId(doc.getId());
                                myEventsList.add(event);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvEmpty.setVisibility(
                                myEventsList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void setupClickListeners() {
        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this,
                        "Notifications coming soon!", Toast.LENGTH_SHORT).show());

        fabCreate.setOnClickListener(v ->
                startActivity(new Intent(this, CreateEventActivity.class)));

        navHome.setOnClickListener(v ->
                Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show());

        navEvents.setOnClickListener(v ->
                Toast.makeText(this, "Events coming soon!", Toast.LENGTH_SHORT).show());

        navProfile.setOnClickListener(v ->
                Toast.makeText(this, "Profile coming soon!", Toast.LENGTH_SHORT).show());
    }
}
