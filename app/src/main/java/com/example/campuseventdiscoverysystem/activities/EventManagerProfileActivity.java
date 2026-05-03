package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

/**
 * Event Manager Profile Activity
 * Displays user info, statistics, quick action buttons, and handles navigation
 */
public class EventManagerProfileActivity extends BaseSessionActivity {

    // Firebase Instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private ImageButton btnNotificationsTop;
    private TextView tvProfileName, tvProfileEmail;

    // Statistics Counters
    private TextView tvManagedCount, tvThisMonthCount, tvFollowersCount;

    // Input Fields
    private EditText etSocietyName;

    // Quick Access Cards
    private CardView btnQuickCreate, btnQuickHistory, btnPrivacySettings, btnSignOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to the XML layout
        setContentView(R.layout.activity_event_manager_profile);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views using their IDs
        bindViews();

        // Load the data and setup listeners
        loadProfileData();
        loadStatistics();
        setupInteractions();
        setupNavigation();
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {

        btnNotificationsTop = findViewById(R.id.btnNotificationsTop);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvManagedCount = findViewById(R.id.tvManagedCount);
        tvThisMonthCount = findViewById(R.id.tvThisMonthCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        etSocietyName = findViewById(R.id.etSocietyName);
        btnQuickCreate = findViewById(R.id.btnQuickCreate);
        btnQuickHistory = findViewById(R.id.btnQuickHistory);
        btnPrivacySettings = findViewById(R.id.btnPrivacySettings);
        btnSignOut = findViewById(R.id.btnSignOut);
    }

    /**
     * Fetches the manager's name, email, and society name from database
     */
    private void loadProfileData() {

        // Ensure user is logged in
        if (mAuth.getCurrentUser() == null)
            return;
        String uid = mAuth.getCurrentUser().getUid();

        // Fetch the details from the database
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String society = doc.getString("societyName");

                // Update UI if the data exists in the database
                if (name != null)
                    tvProfileName.setText(name);
                if (email != null)
                    tvProfileEmail.setText(email);
                if (society != null)
                    etSocietyName.setText(society);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile data!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Fetches statistics for the profile cards
     */
    private void loadStatistics() {
        if (mAuth.getCurrentUser() == null)
            return;
        String uid = mAuth.getCurrentUser().getUid();

        // Get current month and year
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
        Calendar eventCal = Calendar.getInstance();

        // Make query for all active events by this manager
        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .whereEqualTo("status", "active").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Set total managed events
                    int totalManaged = queryDocumentSnapshots.size();
                    tvManagedCount.setText(String.valueOf(totalManaged));

                    // Count events in current month
                    int thisMonthCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp dateTs = doc.getTimestamp("date");
                        if (dateTs != null) {
                            eventCal.setTime(dateTs.toDate());
                            int eventMonth = eventCal.get(Calendar.MONTH);
                            int eventYear = eventCal.get(Calendar.YEAR);
                            if (eventMonth == currentMonth && eventYear == currentYear) {
                                thisMonthCount++;
                            }
                        }
                    }
                    // Set This Month count
                    tvThisMonthCount.setText(String.valueOf(thisMonthCount));
                });

        // Will implement the follower count feature here later
    }

    /**
     * Sets up the listeners for the interactive elements
     */
    private void setupInteractions() {

        // Will implement the society name change feature later
        etSocietyName.setOnEditorActionListener((v, actionId, event) -> {
            return false;
        });

        // Notification Icon
        btnNotificationsTop.setOnClickListener(v -> {
            Toast.makeText(this, "Will set to notifications screen!", Toast.LENGTH_SHORT).show();
        });

        // Create Event Quick Access Card
        btnQuickCreate.setOnClickListener(v -> {
            startActivity(new Intent(this, ManageEventActivity.class));
        });

        // Events History Quick Access Card
        btnQuickHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerEventsActivity.class));
        });

        // Privacy Settings Quick Access Card — navigate to shared PrivacySettingsActivity
        btnPrivacySettings.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacySettingsActivity.class))
        );

        // Sign Out Button
        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();

            // Route back to the Role Selection screen
            startActivity(new Intent(this, RoleSelectActivity.class));
            finish();
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

        // Event Navigation Tab
        findViewById(R.id.navEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerEventsActivity.class));
            finish();
        });
    }
}