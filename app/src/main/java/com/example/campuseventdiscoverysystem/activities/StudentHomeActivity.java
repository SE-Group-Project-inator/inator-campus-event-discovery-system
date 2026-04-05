package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.Calendar;

public class StudentHomeActivity extends AppCompatActivity {

    // UI elements
    private TextView tvEventsThisWeek, tvRegistered, tvSaved;
    private TextView tvGreeting;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        tvEventsThisWeek  = findViewById(R.id.tvEventsThisWeek);
        tvRegistered      = findViewById(R.id.tvRegistered);
        tvSaved           = findViewById(R.id.tvSaved);
        btnNotification   = findViewById(R.id.btnNotification);
        navHome           = findViewById(R.id.navHome);
        navSearch         = findViewById(R.id.navSearch);
        navTickets        = findViewById(R.id.navTickets);
        navProfile        = findViewById(R.id.navProfile);
        upcomingEventsList = findViewById(R.id.upcomingEventsList);

        // Load data
        loadGreeting();
        loadEventsThisWeek();
        loadRegisteredCount();
        loadSavedCount();

        // Notification bell
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this,
                        "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

        // Stay Up To Date arrow → navigate to Search
        findViewById(R.id.blueSection).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            // already here, do nothing
        });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        navTickets.setOnClickListener(v ->
                Toast.makeText(this,
                        "Tickets coming soon!", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentProfileActivity.class));
        });
    }

    // ── Load student name for greeting from /users/{uid} ──
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
                            // Get first name only
                            String firstName = name.split(" ")[0];
                            TextView tvHello = findViewById(R.id.tvGreeting);
                            if (tvHello != null) {
                                tvHello.setText("Hello, " + firstName + "!");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // keep default "Hello, Areeba!" text
                });
    }

    // ── Count events happening this week from /events ──
    private void loadEventsThisWeek() {
        // Get start and end of current week
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.DAY_OF_WEEK, startCal.getFirstDayOfWeek());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);

        Calendar endCal = (Calendar) startCal.clone();
        endCal.add(Calendar.DAY_OF_WEEK, 7);

        Timestamp weekStart = new Timestamp(startCal.getTime());
        Timestamp weekEnd   = new Timestamp(endCal.getTime());

        db.collection("events")
                .whereGreaterThanOrEqualTo("date", weekStart)
                .whereLessThan("date", weekEnd)
                .get()
                .addOnSuccessListener(query ->
                        tvEventsThisWeek.setText(query.size() + " Events this week")
                )
                .addOnFailureListener(e ->
                        tvEventsThisWeek.setText("0 Events this week")
                );
    }

    // ── Count confirmed RSVPs for this student ──
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

    // ── Saved count ──
    // No saved collection in DB yet — set to 0 for now
    private void loadSavedCount() {
        // TODO: update when saved/bookmarks collection is added to Firestore
        tvSaved.setText("0 Saved");
    }
}