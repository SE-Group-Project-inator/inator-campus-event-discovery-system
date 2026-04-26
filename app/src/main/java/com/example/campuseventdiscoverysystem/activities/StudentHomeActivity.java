package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * StudentHomeActivity
 *
 * Main dashboard screen for students.
 * Displays greeting, event statistics, and a list of upcoming events.
 * Integrates with Firebase Authentication and Firestore.
 */
public class StudentHomeActivity extends AppCompatActivity {

    // UI elements
    private TextView tvEventsThisWeek, tvRegistered, tvSaved, tvGreeting;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    /**
     * Called when activity is created.
     * Initializes UI components, Firebase, and loads data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_homepage);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind UI elements
        tvEventsThisWeek   = findViewById(R.id.tvEventsThisWeek);
        tvRegistered       = findViewById(R.id.tvRegistered);
        tvSaved            = findViewById(R.id.tvSaved);
        tvGreeting         = findViewById(R.id.tvGreeting);
        btnNotification    = findViewById(R.id.btnNotification);
        navHome            = findViewById(R.id.navHome);
        navSearch          = findViewById(R.id.navSearch);
        navTickets         = findViewById(R.id.navTickets);
        navProfile         = findViewById(R.id.navProfile);
        upcomingEventsList = findViewById(R.id.upcomingEventsList);

        // Load dashboard data
        loadGreeting();
        loadEventsThisWeek();
        loadRegisteredCount();
        loadSavedCount();
        loadUpcomingEvents();

        // Notification button (placeholder)
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this,
                        "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            // Already on home screen
        });

        // Navigate to Search screen
        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        // Tickets (not implemented yet)
        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, TicketsActivity.class))
        );

        // Navigate to Profile screen
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );

        // "See All" → opens Trending Events screen
        TextView tvSeeAllTrending = findViewById(R.id.tvSeeAllTrending);
        tvSeeAllTrending.setOnClickListener(v ->
                startActivity(new Intent(this, TrendingEventsActivity.class))
        );
    }

    /**
     * Loads and displays the user's greeting using their first name.
     */
    private void loadGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) {
                            String firstName = name.split(" ")[0];
                            tvGreeting.setText("Hello, " + firstName + "!");
                        }
                    }
                });
    }

    /**
     * Loads the total number of active events.
     */
    private void loadEventsThisWeek() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query ->
                        tvEventsThisWeek.setText(query.size() + " Events")
                )
                .addOnFailureListener(e ->
                        tvEventsThisWeek.setText("0 Events")
                );
    }

    /**
     * Loads the count of events the user has registered for.
     */
    private void loadRegisteredCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(query ->
                        tvRegistered.setText(query.size() + " Registered")
                )
                .addOnFailureListener(e ->
                        tvRegistered.setText("0 Registered")
                );
    }

    /**
     * Loads saved events count.
     * Currently hardcoded (feature not implemented yet).
     */
    private void loadSavedCount() {
        tvSaved.setText("0 Saved");
    }

    /**
     * Loads all active events and dynamically displays them
     * in the upcoming events list.
     */
    private void loadUpcomingEvents() {

        // Clear previous views
        upcomingEventsList.removeAllViews();

        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {

                    // Show empty state if no events
                    if (query.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No events available!");
                        empty.setTextColor(getResources().getColor(R.color.text_dark));
                        empty.setPadding(0, 16, 0, 16);
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    // Loop through each event
                    for (QueryDocumentSnapshot doc : query) {

                        String title = doc.getString("title");
                        String venue = doc.getString("venue");
                        Timestamp date = doc.getTimestamp("date");

                        // Inflate event card layout
                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_upcoming,
                                        upcomingEventsList, false);

                        // Set event title
                        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
                        if (tvTitle != null && title != null)
                            tvTitle.setText(title);

                        // Set event location
                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        if (tvLocation != null && venue != null)
                            tvLocation.setText("📍 " + venue);

                        // Format and display event date
                        if (date != null) {
                            Date d = date.toDate();

                            TextView tvDay = itemView.findViewById(R.id.tvDay);
                            if (tvDay != null)
                                tvDay.setText(
                                        new SimpleDateFormat("dd", Locale.getDefault())
                                                .format(d));

                            TextView tvMonth = itemView.findViewById(R.id.tvMonth);
                            if (tvMonth != null)
                                tvMonth.setText(
                                        new SimpleDateFormat("MMM", Locale.getDefault())
                                                .format(d).toUpperCase());
                        }

                        // Prepare event data for detail screen
                        String eventIdFinal   = doc.getId();
                        String titleFinal     = title;
                        String venueFinal     = venue;
                        String descFinal      = doc.getString("description");
                        int    capFinal       = doc.getLong("capacity") != null
                                ? doc.getLong("capacity").intValue() : 0;
                        int    regFinal       = doc.getLong("registeredCount") != null
                                ? doc.getLong("registeredCount").intValue() : 0;
                        long   dateMillis     = date != null ? date.toDate().getTime() : 0;

                        // Navigate to EventDetailActivity on click
                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, EventDetailActivity.class);
                            intent.putExtra("eventId", eventIdFinal);
                            intent.putExtra("eventTitle", titleFinal);
                            intent.putExtra("eventVenue", venueFinal);
                            intent.putExtra("eventDescription", descFinal);
                            intent.putExtra("eventCapacity", capFinal);
                            intent.putExtra("eventRegistered", regFinal);
                            intent.putExtra("eventDateMillis", dateMillis);
                            startActivity(intent);
                        });

                        // Add event card to layout
                        upcomingEventsList.addView(itemView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }
}