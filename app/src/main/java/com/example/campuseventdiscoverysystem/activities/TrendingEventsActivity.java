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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TrendingEventsActivity
 *
 * Displays future active events sorted by registration count,
 * excluding events the current user has already RSVPd to.
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

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvEmpty = findViewById(R.id.tvEmpty);

        setupRecyclerView();
        setupNavigation();
        loadTrendingEvents();
    }

    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvTrendingEvents);

        adapter = new TrendingEventAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId",          event.getId());
            intent.putExtra("eventTitle",        event.getTitle());
            intent.putExtra("eventDescription",  event.getDescription());
            intent.putExtra("eventVenue",        event.getVenue());
            intent.putExtra("eventCapacity",     event.getCapacity());
            intent.putExtra("eventRegistered",   event.getRegisteredCount());
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
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        findViewById(R.id.navSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );
        findViewById(R.id.navTickets).setOnClickListener(v ->
                startActivity(new Intent(this, TicketsActivity.class))
        );
        findViewById(R.id.navProfile).setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );
    }

    /**
     * Fetches the user's RSVPs first, then loads active future events
     * that the user has NOT already registered for, sorted by popularity.
     */
    private void loadTrendingEvents() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            fetchAndDisplayEvents(new HashSet<>());
            return;
        }

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    Set<String> rsvpedIds = new HashSet<>();
                    for (DocumentSnapshot doc : rsvpQuery.getDocuments()) {
                        String eid = doc.getString("eventId");
                        if (eid != null) rsvpedIds.add(eid);
                    }
                    fetchAndDisplayEvents(rsvpedIds);
                })
                .addOnFailureListener(e -> fetchAndDisplayEvents(new HashSet<>()));
    }

    private void fetchAndDisplayEvents(Set<String> rsvpedIds) {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshots -> {
                    Date now = new Date();
                    eventList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        // Skip past events
                        com.google.firebase.Timestamp ts = doc.getTimestamp("date");
                        if (ts == null || ts.toDate().before(now)) continue;

                        // Skip events user already RSVPd to
                        if (rsvpedIds.contains(doc.getId())) continue;

                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            eventList.add(event);
                        }
                    }

                    // Sort by popularity (highest registrations first)
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
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setVisibility(View.VISIBLE);
                    RecyclerView rv = findViewById(R.id.rvTrendingEvents);
                    rv.setVisibility(View.GONE);
                });
    }
}