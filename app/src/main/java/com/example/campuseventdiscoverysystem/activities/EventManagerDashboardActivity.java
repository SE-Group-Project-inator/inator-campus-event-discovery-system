package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

// WILL EDIT THIS CLASS

/**
 * Main dashboard for the Event Manager.
 * Features navigation, event history, and routing to event creation/editing.
 */
public class EventManagerDashboardActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnNotifications;
    private FloatingActionButton fabCreate;
    private LinearLayout navHome, navEvents, navProfile;

    // RecyclerView Components
    private RecyclerView rvManagerEvents;
    private ManagerEventAdapter adapter;
    private List<Event> myEventsList;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_manager_dashboard);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Views
        btnNotifications = findViewById(R.id.btnNotifications);
        fabCreate = findViewById(R.id.fabCreate);
        navHome = findViewById(R.id.navHome);
        navEvents = findViewById(R.id.navEvents);
        navProfile = findViewById(R.id.navProfile);
        rvManagerEvents = findViewById(R.id.rvManagerEvents);

        setupRecyclerView();
        setupClickListeners();
        loadMyEvents();
    }

    /**
     * Initializes the RecyclerView and links it to the ManagerEventAdapter.
     * Also sets up the click listener to open the Edit screen.
     */
    private void setupRecyclerView() {
        myEventsList = new ArrayList<>();

        // Pass the list and the click action to the adapter
        adapter = new ManagerEventAdapter(myEventsList, eventId -> {
            // When an event is clicked, open EditEventActivity and pass the event ID
            Intent intent = new Intent(EventManagerDashboardActivity.this, EditEventActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        rvManagerEvents.setLayoutManager(new LinearLayoutManager(this));
        rvManagerEvents.setAdapter(adapter);
    }

    /**
     * Fetches events from Firestore that were created by the currently logged-in user.
     */
    private void loadMyEvents() {
        if (mAuth.getCurrentUser() == null) return;

        String currentUserId = mAuth.getCurrentUser().getUid();

        // Use addSnapshotListener to get real-time updates
        db.collection("events")
                .whereEqualTo("createdBy", currentUserId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        myEventsList.clear(); // Clear old list
                        for (DocumentSnapshot doc : snapshots) {
                            Event event = doc.toObject(Event.class);
                            if (event != null) {
                                event.setId(doc.getId());
                                myEventsList.add(event);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void setupClickListeners() {
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications section coming soon!", Toast.LENGTH_SHORT).show();
        });

        fabCreate.setOnClickListener(v -> {
            startActivity(new Intent(EventManagerDashboardActivity.this, CreateEventActivity.class));
        });

        navHome.setOnClickListener(v -> {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show();
        });

        navEvents.setOnClickListener(v -> {
            Toast.makeText(this, "Events section coming soon!", Toast.LENGTH_SHORT).show();
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(EventManagerDashboardActivity.this, EventManagerProfileActivity.class));
        });
    }
}