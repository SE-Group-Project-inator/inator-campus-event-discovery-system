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
 * US-10: Trending Events Ranking
 * Displays approved events ranked by registeredCount (descending).
 */
public class TrendingEventsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<Event> eventList = new ArrayList<>();
    private TrendingEventAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending_events);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvEmpty = findViewById(R.id.tvEmpty);

        setupRecyclerView();
        setupNavigation();
        loadTrendingEvents();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvTrendingEvents);
        adapter = new TrendingEventAdapter(eventList, event -> {
            // US-21: open event detail for calendar export
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", event.getId());
            intent.putExtra("eventTitle", event.getTitle());
            intent.putExtra("eventDescription", event.getDescription());
            intent.putExtra("eventVenue", event.getVenue());
            intent.putExtra("eventCapacity", event.getCapacity());
            intent.putExtra("eventRegistered", event.getRegisteredCount());
            intent.putExtra("eventOrganizerName", event.getSubmittedByName());
            if (event.getDate() != null) {
                intent.putExtra("eventDateMillis", event.getDate().toDate().getTime());
            }
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    private void setupNavigation() {

        findViewById(R.id.navSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, RoleSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // navHome is the current screen — no-op
        findViewById(R.id.navHome).setOnClickListener(v -> { /* already here */ });
    }

    private void loadTrendingEvents() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    eventList.clear();
                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            eventList.add(event);
                        }
                    }

                    // Sort by registeredCount descending (trending = most popular first)
                    eventList.sort((a, b) ->
                            Integer.compare(b.getRegisteredCount(), a.getRegisteredCount()));

                    adapter.notifyDataSetChanged();

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