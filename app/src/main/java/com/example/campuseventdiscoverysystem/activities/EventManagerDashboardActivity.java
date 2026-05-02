package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.ManagerEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventManagerDashboardActivity
 * Serves as the main home screen for the Event Manager
 * Features a calendar view allowing the user to select specific dates,
 * which in turn fetches and displays all active/approved campus events for that day
 */
public class EventManagerDashboardActivity extends BaseSessionActivity {

    // Firebase instances for database operations
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Components
    private CalendarView calendarView;
    private TextView tvSelectedDateHeader, tvGreeting;
    private RecyclerView rvDateEvents;
    private ImageButton btnNotifications;

    // Adapter and data source for populating the daily events list
    private ManagerEventAdapter adapter;
    private List<Event> dateEventsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to the XML layout
        setContentView(R.layout.activity_event_manager_dashboard);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views
        bindViews();

        // Setup components of the screen
        setupHeader();
        setupRecyclerView();
        setupNavigation();

        // Automatically load events for the current day when the dashboard is first opened
        loadEventsForDate(new Date());

        // Listen for user interactions with the CalendarView
        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            // Construct a Calendar object from the selected date parameters
            Calendar clickedDate = Calendar.getInstance();
            clickedDate.set(year, month, day);

            // Fetch events for the newly selected date
            loadEventsForDate(clickedDate.getTime());
        });
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDateHeader = findViewById(R.id.tvSelectedDateHeader);
        rvDateEvents = findViewById(R.id.rvDateEvents);
        tvGreeting = findViewById(R.id.tvGreeting);
        btnNotifications = findViewById(R.id.btnNotifications);
    }

    /**
     * Configures the personalized greeting and the notification icon listener
     */
    private void setupHeader() {

        // Setup greeting by extracting the user's display name
        if (mAuth.getCurrentUser() != null) {
            String fullName = mAuth.getCurrentUser().getDisplayName();
            if (fullName != null && !fullName.isEmpty()) {
                tvGreeting.setText("Hello, " + fullName + "!");
            }
        }

        // Will implement this notification screen routing later
        btnNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Will set to notifications screen!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Initializes the RecyclerView and its adapter
     * Defines what happens when an individual event card is clicked
     */
    private void setupRecyclerView() {

        // Route to the AttendeeListActivity
        adapter = new ManagerEventAdapter(dateEventsList, eventId -> {
            Intent intent = new Intent(this, AttendeeListActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        // Hide status badge of events for the dashboard
        adapter.setShowStatusBadge(false);

        // Use a vertical scrolling list and attach the adapter
        rvDateEvents.setLayoutManager(new LinearLayoutManager(this));
        rvDateEvents.setAdapter(adapter);
    }

    /**
     * Queries Firestore to fetch all "active" events for the
     * currently selected date on the calendar
     * @param selectedDate The date selected by the user
     */
    private void loadEventsForDate(Date selectedDate) {

        // Update the header text to reflect the selected date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvSelectedDateHeader.setText("Events on " + sdf.format(selectedDate));

        // Define the start of the selected day
        Calendar start = Calendar.getInstance();
        start.setTime(selectedDate);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        // Define the end of the selected day
        Calendar end = Calendar.getInstance();
        end.setTime(selectedDate);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        // Query against the "events" collection in database
        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", start.getTime())
                .whereLessThanOrEqualTo("date", end.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Clear the last day's events
                    dateEventsList.clear();

                    // Iterate through the fetched documents and add events to the list
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            dateEventsList.add(event);
                        }
                    }

                    // Notify the adapter
                    adapter.notifyDataSetChanged();
                });
    }

    /**
     * Handles routing for the bottom navigation bar and floating create button
     */
    private void setupNavigation() {

        // Events Navigation Tab
        findViewById(R.id.navEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerEventsActivity.class));
            finish();
        });

        // Profile Navigation Tab
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerProfileActivity.class));
            finish();
        });

        // Floating Create Button
        findViewById(R.id.fabCreate).setOnClickListener(v -> {
            startActivity(new Intent(this, CreateEventActivity.class));
        });
    }

    // Top-right logout button hook (added to XML)
    // Called from setupNavigation after base class is initialised
    private void wireLogout() {
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutDialog());
    }
}