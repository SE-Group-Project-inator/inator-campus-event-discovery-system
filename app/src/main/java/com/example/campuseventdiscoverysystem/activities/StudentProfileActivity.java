package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity that displays the student's profile screen.
 *
 * <p>This screen shows:
 * <ul>
 *     <li>Student name and email</li>
 *     <li>Total events attended</li>
 *     <li>Events attended this month</li>
 *     <li>Following count (future feature)</li>
 *     <li>Navigation options and settings</li>
 * </ul>
 * </p>
 */
public class StudentProfileActivity extends AppCompatActivity {

    /** Student name display */
    private TextView tvStudentName, tvStudentEmail;

    /** Event statistics display */
    private TextView tvEventsAttended, tvThisMonth, tvFollowing;

    /** Action buttons */
    private CardView btnAttendanceHistory, btnMySocieties;
    private CardView btnQRCheckIn, btnSignOut, btnPrivacySettings;

    /** Notification icon */
    private ImageButton btnNotification;

    /** Bottom navigation containers */
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    /** Firebase authentication instance */
    private FirebaseAuth mAuth;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /**
     * Called when activity is created.
     * Initializes UI, Firebase, and loads all user data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        tvStudentName        = findViewById(R.id.tvStudentName);
        tvStudentEmail       = findViewById(R.id.tvStudentEmail);
        tvEventsAttended     = findViewById(R.id.tvEventsAttended);
        tvThisMonth          = findViewById(R.id.tvThisMonth);
        tvFollowing          = findViewById(R.id.tvFollowing);
        btnNotification      = findViewById(R.id.btnNotification);
        btnAttendanceHistory = findViewById(R.id.btnAttendanceHistory);
        btnMySocieties       = findViewById(R.id.btnMySocieties);
        btnQRCheckIn         = findViewById(R.id.btnQRCheckIn);
        btnSignOut           = findViewById(R.id.btnSignOut);
        btnPrivacySettings   = findViewById(R.id.btnPrivacySettings);
        navHome              = findViewById(R.id.navHome);
        navSearch            = findViewById(R.id.navSearch);
        navTickets           = findViewById(R.id.navTickets);
        navProfile           = findViewById(R.id.navProfile);

        // Load all data
        loadStudentProfile();
        loadAttendedCount();
        loadThisMonthCount();
        loadFollowingCount();

        btnAttendanceHistory.setOnClickListener(v ->
                startActivity(new Intent(this, EventHistoryActivity.class))
        );

        // Notification button
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

        // Privacy settings navigation
        btnPrivacySettings.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacySettingsActivity.class))
        );

        // Sign out user
        btnSignOut.setOnClickListener(v -> signOut());

        // Bottom navigation
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, TicketsActivity.class))
        );

        navProfile.setOnClickListener(v -> {
            // already on profile
        });
    }

    /**
     * Loads student name and email from Firestore /users collection.
     */
    private void loadStudentProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvStudentEmail.setText(user.getEmail());

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        if (name  != null) tvStudentName.setText(name);
                        if (email != null) tvStudentEmail.setText(email);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load profile", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Loads total number of confirmed event attendances.
     */
    /**
     * Counts only RSVPs where the event date has already passed — matching
     * EventHistoryActivity's definition of "attended".
     */
    private void loadAttendedCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    Date now = new Date();
                    // We need event dates, so fetch each event and count past ones
                    List<com.google.firebase.firestore.DocumentSnapshot> docs =
                            rsvpQuery.getDocuments();
                    if (docs.isEmpty()) { tvEventsAttended.setText("0"); return; }

                    java.util.concurrent.atomic.AtomicInteger total     =
                            new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicInteger remaining =
                            new java.util.concurrent.atomic.AtomicInteger(docs.size());

                    for (com.google.firebase.firestore.DocumentSnapshot rsvp : docs) {
                        String eventId = rsvp.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0)
                                tvEventsAttended.setText(String.valueOf(total.get()));
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");
                                    if (ts != null && ts.toDate().before(now))
                                        total.incrementAndGet();
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsAttended.setText(String.valueOf(total.get()));
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsAttended.setText(String.valueOf(total.get()));
                                });
                    }
                })
                .addOnFailureListener(e -> tvEventsAttended.setText("0"));
    }

    private void loadThisMonthCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String currentMonth = new java.text.SimpleDateFormat("MMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime()).toUpperCase();
        Date now = new Date();

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    List<DocumentSnapshot> docs =
                            rsvpQuery.getDocuments();
                    if (docs.isEmpty()) { tvThisMonth.setText("0"); return; }

                    java.util.concurrent.atomic.AtomicInteger count     =
                            new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicInteger remaining =
                            new java.util.concurrent.atomic.AtomicInteger(docs.size());

                    for (com.google.firebase.firestore.DocumentSnapshot rsvp : docs) {
                        String eventId = rsvp.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0)
                                tvThisMonth.setText(String.valueOf(count.get()));
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");
                                    if (ts != null && ts.toDate().before(now)) {
                                        String month = new java.text.SimpleDateFormat(
                                                "MMM", Locale.getDefault())
                                                .format(ts.toDate()).toUpperCase();
                                        if (month.equals(currentMonth))
                                            count.incrementAndGet();
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        tvThisMonth.setText(String.valueOf(count.get()));
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0)
                                        tvThisMonth.setText(String.valueOf(count.get()));
                                });
                    }
                })
                .addOnFailureListener(e -> tvThisMonth.setText("0"));
    }

    /**
     * Loads following count (placeholder until Firestore feature is added).
     */
    private void loadFollowingCount() {
        tvFollowing.setText("0");
    }

    /**
     * Signs out the current user and clears activity stack.
     */
    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(this, RoleSelectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}