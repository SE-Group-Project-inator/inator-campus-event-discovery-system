package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * AttendeesRosterActivity
 * Activity responsible for displaying the list of confirmed attendees and waitlisted students
 * for a specific event. Event Managers use this to monitor capacity and event roster details.
 */
public class AttendeesRosterActivity extends AppCompatActivity {

    // Firebase instance for database operations
    private FirebaseFirestore db;

    // ID of the event whose roster is being viewed
    private String eventID;

    // UI Components
    private RecyclerView rvAttendees;
    private Button btnRoster, btnWaitlist;
    private TextView tvEmptyState;

    // State and Data Variables
    private boolean showingWaitlist = false;
    private List<DocumentSnapshot> allRegistrations = new ArrayList<>();
    private AttendeeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind activity to its corresponding XML layout file
        setContentView(R.layout.activity_attendees_roster);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();

        // Retrieve eventID passed from the previous screen
        eventID = getIntent().getStringExtra("EVENT_ID");
        if (eventID == null || eventID.isEmpty()) {
            Toast.makeText(this, "Error: Event ID missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link variables to XML views
        bindViews();

        // Setup components of the screen
        setupNavigation();
        setupRecyclerView();
        setupTabs();

        // Fetch data from Firestore
        loadAttendees();
    }

    /**
     * Maps all the XML UI components to Java variables.
     */
    private void bindViews() {
        rvAttendees = findViewById(R.id.rvAttendees);
        btnRoster = findViewById(R.id.btnRoster);
        btnWaitlist = findViewById(R.id.btnWaitlist);
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    /**
     * Handles routing for the top navigation bar (Back button).
     */
    private void setupNavigation() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    /**
     * Initializes the RecyclerView and its custom adapter.
     */
    private void setupRecyclerView() {
        rvAttendees.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendeeAdapter();
        rvAttendees.setAdapter(adapter);
    }

    /**
     * Configures the toggle buttons for switching between "Registered" and "Waitlist" views.
     */
    private void setupTabs() {
        btnRoster.setOnClickListener(v -> {
            showingWaitlist = false;
            // Visually indicate the active tab using alpha transparency
            btnRoster.setAlpha(1.0f);
            btnWaitlist.setAlpha(0.5f);
            filterList();
        });

        btnWaitlist.setOnClickListener(v -> {
            showingWaitlist = true;
            // Visually indicate the active tab using alpha transparency
            btnRoster.setAlpha(0.5f);
            btnWaitlist.setAlpha(1.0f);
            filterList();
        });
    }

    /**
     * Fetches all RSVPs for the current event from Firestore in real-time.
     * Stores them in a global list so we can filter them locally without extra database calls.
     */
    private void loadAttendees() {
        // Point to the top-level "rsvps" collection and filter by this event's ID
        db.collection("rsvps")
                .whereEqualTo("eventId", eventID)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("AttendeesRosterActivity", "Listen failed.", e);
                        Toast.makeText(this, "Failed to load roster data.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        allRegistrations = snapshots.getDocuments();
                        // Re-filter and update UI whenever data changes
                        filterList();
                    }
                });
    }

    /**
     * Filters the global list of registrations based on the currently selected tab
     * (Confirmed vs. Waitlisted) and updates the RecyclerView.
     */
    private void filterList() {
        List<DocumentSnapshot> filteredList = new ArrayList<>();

        for (DocumentSnapshot doc : allRegistrations) {
            String status = doc.getString("status");

            // Match the exact status strings used in the database saving logic
            if (showingWaitlist && "waitlisted".equals(status)) {
                filteredList.add(doc);
            } else if (!showingWaitlist && "confirmed".equals(status)) {
                filteredList.add(doc);
            }
        }

        // Toggle the empty state message if no students match the current filter
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);

        // Pass the filtered data to the adapter
        adapter.setList(filteredList);
    }

    /**
     * Inner Adapter class for populating the RecyclerView with attendee data.
     * Uses Android's default simple_list_item_2 layout for a clean, two-line list item.
     */
    private class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {

        private List<DocumentSnapshot> list = new ArrayList<>();

        /**
         * Updates the adapter's data source and refreshes the UI.
         *
         * @param list The new list of DocumentSnapshots to display.
         */
        public void setList(List<DocumentSnapshot> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = list.get(position);
            String userId = doc.getString("userId");

            // Set placeholder text while the database fetches their actual profile info
            holder.text1.setText("Loading student...");
            holder.text2.setText("...");

            // Fetch the user's real name, email, and roll number from the users collection
            if (userId != null) {
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String name = userDoc.getString("name");
                                String email = userDoc.getString("email");
                                String rollNo = userDoc.getString("rollNo");

                                // Format the display string (e.g., "John Doe (24100155)")
                                String displayName = name != null ? name : "Student";
                                if (rollNo != null && !rollNo.isEmpty()) {
                                    displayName += " (" + rollNo + ")";
                                }

                                holder.text1.setText(displayName);
                                holder.text2.setText(email != null ? email : "No email provided");
                            } else {
                                holder.text1.setText("Unknown Student");
                                holder.text2.setText("ID: " + userId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.text1.setText("Error loading student");
                            holder.text2.setText("ID: " + userId);
                        });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        /**
         * ViewHolder class for the Attendee items.
         */
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;

            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }
}