package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Event Manager Events Activity
 * Screen showing the history of events managed by the current manager
 * Features an "Edit Mode" toggle that allows the user to switch
 * between viewing event analytics and editing event details
 */
public class EventManagerEventsActivity extends AppCompatActivity {

    // Firebase instances for database operations
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Components for displaying the list and counters
    private RecyclerView rvMyEvents;
    private TextView tvTotalManaged, tvThisMonth;

    // Adapter and data list for the RecyclerView
    private ManagerEventAdapter adapter;
    private List<Event> myEventsList = new ArrayList<>();

    // Variables for handling the "Edit Mode" functionality
    private boolean isEditMode = false;
    private TextView tvHeaderTitle, btnToggleEdit;
    private View topBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to the XML layout
        setContentView(R.layout.activity_event_manager_events);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views using their IDs
        bindViews();

        // Setup components of the screen
        setupEditModeToggle();
        setupRecyclerView();
        setupNavigation();

        // Fetch data from Firestore and calculate statistics
        loadMyEventsAndStats();
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {

        rvMyEvents = findViewById(R.id.rvMyEvents);
        tvTotalManaged = findViewById(R.id.tvTotalManaged);
        tvThisMonth = findViewById(R.id.tvThisMonth);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        btnToggleEdit = findViewById(R.id.btnToggleEdit);
        topBar = findViewById(R.id.topBar);
    }

    /**
     * Configures the "Edit" button in the top bar
     * Handles the visual changes and state when toggling Edit Mode
     */
    private void setupEditModeToggle() {

        btnToggleEdit.setOnClickListener(v -> {
            isEditMode = !isEditMode;

            // Notify the adapter about the state change
            if (adapter != null) {
                adapter.setEditMode(isEditMode);
            }

            // Update the visuals based on the current mode
            if (isEditMode) {
                // Edit Mode: Change text and set background to grey color
                btnToggleEdit.setText("Done");
                tvHeaderTitle.setText("Tap Event to Edit");
                topBar.setBackgroundResource(R.color.text_grey);
            } else {
                // Normal Mode: Revert to default text and primary theme color
                btnToggleEdit.setText("Edit");
                tvHeaderTitle.setText("My Events");
                topBar.setBackgroundResource(R.color.btn_eventmgr);
            }
        });
    }

    /**
     * Initializes the RecyclerView and its adapter
     * Defines what happens when an individual event card is clicked
     */
    private void setupRecyclerView() {

        // The click listener behavior changes on the 'isEditMode' flag
        adapter = new ManagerEventAdapter(myEventsList, eventId -> {
            if (isEditMode) {
                // Edit Mode: Route the user to the EditEventActivity
                Intent intent = new Intent(this, EditEventActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            } else {
                // Normal Mode: Route the user to the AttendeeListActivity
                Intent intent = new Intent(this, AttendeeListActivity.class);
                intent.putExtra("EVENT_ID", eventId);
                startActivity(intent);
            }
        });

        // Use a vertical scrolling list and attach the adapter
        rvMyEvents.setLayoutManager(new LinearLayoutManager(this));
        rvMyEvents.setAdapter(adapter);
    }

    /**
     * Fetches the manager's events from Firestore
     * Calculates the "Total Managed" and "This Month" statistics
     */
    private void loadMyEventsAndStats() {

        // Ensure user is logged in
        if (mAuth.getCurrentUser() == null)
            return;
        String uid = mAuth.getCurrentUser().getUid();

        // Setup calendar instances to determine if an event falls in the current month
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
        Calendar eventCal = Calendar.getInstance();

        // Query database for all events created by this manager, ordered by newest first
        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots == null)
                        return;

                    // Clear the old list
                    myEventsList.clear();

                    // Counters for statistics
                    int activeCount = 0;
                    int thisMonthCount = 0;

                    // Iterate through the fetched documents
                    for (DocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event == null)
                            continue;

                        // Set the document ID to the event object
                        event.setId(doc.getId());
                        myEventsList.add(event);

                        // Count approved events for the total managed
                        if ("active".equals(event.getStatus())) {
                            activeCount++;

                            // Check if the event's date falls within the current month
                            if (event.getDate() != null) {
                                eventCal.setTime(event.getDate().toDate());
                                if (eventCal.get(Calendar.MONTH) == currentMonth &&
                                        eventCal.get(Calendar.YEAR) == currentYear) {
                                    thisMonthCount++;
                                }
                            }
                        }
                    }

                    // Notify the adapter
                    adapter.notifyDataSetChanged();

                    // Update the counters
                    tvTotalManaged.setText(String.valueOf(activeCount));
                    tvThisMonth.setText(String.valueOf(thisMonthCount));
                });
    }

    /**
     * Handles routing for the bottom navigation bar
     */
    private void setupNavigation() {

        // Home Navigation Tab
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerDashboardActivity.class));
            finish();
        });

        // Profile Navigation Tab
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerProfileActivity.class));
            finish();
        });
    }
}