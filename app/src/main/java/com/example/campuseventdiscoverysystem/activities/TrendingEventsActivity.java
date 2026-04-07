package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.TrendingEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

/**
 * TrendingEventsActivity
 *
 * Displays a list of trending (most popular) events.
 * Events are fetched from Firebase Firestore and sorted
 * in descending order based on registration count.
 *
 * Features:
 * - Real-time updates using Firestore snapshot listener
 * - Navigation to EventDetailActivity
 * - Bottom navigation (Home, Sign Out)
 *
 * User Story:
 * US-10: Trending Events Ranking
 */
public class TrendingEventsActivity extends AppCompatActivity {

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Data source for RecyclerView
    private final List<Event> eventList = new ArrayList<>();

    // Adapter for displaying events
    private TrendingEventAdapter adapter;

    // Empty state TextView
    private TextView tvEmpty;

    /**
     * Called when the activity is created.
     * Initializes UI, Firebase, and loads trending events.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending_events);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Bind empty state view
        tvEmpty = findViewById(R.id.tvEmpty);

        // Setup UI components
        setupRecyclerView();
        setupNavigation();

        // Load trending events from Firestore
        loadTrendingEvents();
    }

    /**
     * Initializes the RecyclerView and its adapter.
     * Also handles click events on each event item.
     */
    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvTrendingEvents);

        adapter = new TrendingEventAdapter(eventList, event -> {
            // US-21: Open EventDetailActivity when an event is clicked
            Intent intent = new Intent(this, EventDetailActivity.class);

            // Pass event data via Intent
            intent.putExtra("eventId", event.getId());
            intent.putExtra("eventTitle", event.getTitle());
            intent.putExtra("eventDescription", event.getDescription());
            intent.putExtra("eventVenue", event.getVenue());
            intent.putExtra("eventCapacity", event.getCapacity());
            intent.putExtra("eventRegistered", event.getRegisteredCount());
            intent.putExtra("eventOrganizerName", event.getSubmittedByName());

            // Pass event date if available
            if (event.getDate() != null) {
                intent.putExtra("eventDateMillis", event.getDate().toDate().getTime());
            }

            // Navigate to detail screen
            startActivity(intent);
        });

        // Set layout manager and adapter
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    /**
     * Sets up navigation actions for bottom navigation bar.
     */
    private void setupNavigation() {

        // Sign out button → clears session and navigates to RoleSelectActivity
        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Home button → navigates back to StudentHomeActivity
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // avoids stacking
            startActivity(intent);
            finish();
        });
    }

    /**
     * Fetches active events from Firestore and updates the UI.
     * Uses a snapshot listener for real-time updates.
     */
    private void loadTrendingEvents() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, error) -> {

                    // Exit if error occurs or no data is available
                    if (error != null || snapshots == null) return;

                    // Clear existing list
                    eventList.clear();

                    // Convert Firestore documents into Event objects
                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            eventList.add(event);
                        }
                    }

                    // Sort events by popularity (highest registrations first)
                    eventList.sort((a, b) ->
                            Integer.compare(b.getRegisteredCount(), a.getRegisteredCount()));

                    // Notify adapter about data changes
                    adapter.notifyDataSetChanged();

                    // Handle empty state UI
                    RecyclerView rv = findViewById(R.id.rvTrendingEvents);
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