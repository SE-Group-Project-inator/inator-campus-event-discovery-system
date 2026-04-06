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

public class StudentHomeActivity extends AppCompatActivity {

    // UI elements
    private TextView tvEventsThisWeek, tvRegistered, tvSaved, tvGreeting;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_homepage);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        // Load data
        loadGreeting();
        loadEventsThisWeek();
        loadRegisteredCount();
        loadSavedCount();
        loadUpcomingEvents();

        // Notification bell
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this,
                        "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

//        // ✅ FIXED: Stay Updated card click → go to placeholder screen
//        findViewById(R.id.stayUpdatedCard).setOnClickListener(v ->
//                startActivity(new Intent(this, PersonalizedRecommendationsActivity.class))
//        );

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            // already here
        });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        navTickets.setOnClickListener(v ->
                Toast.makeText(this,
                        "Tickets coming soon!", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );
    }

    // ── Greeting ──
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

    // ── Count approved events ──
    private void loadEventsThisWeek() {
        db.collection("events")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(query ->
                        tvEventsThisWeek.setText(query.size() + " Events")
                )
                .addOnFailureListener(e ->
                        tvEventsThisWeek.setText("0 Events")
                );
    }

    // ── Registered count ──
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
    private void loadSavedCount() {
        tvSaved.setText("0 Saved");
    }

    // ── Load ALL approved events ──
    private void loadUpcomingEvents() {

        upcomingEventsList.removeAllViews();

        db.collection("events")
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(query -> {

                    if (query.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No events available!");
                        empty.setTextColor(getResources().getColor(R.color.text_dark));
                        empty.setPadding(0, 16, 0, 16);
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : query) {

                        String title = doc.getString("title");
                        String venue = doc.getString("venue");
                        Timestamp date = doc.getTimestamp("date");

                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_upcoming,
                                        upcomingEventsList, false);

                        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
                        if (tvTitle != null && title != null)
                            tvTitle.setText(title);

                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        if (tvLocation != null && venue != null)
                            tvLocation.setText("📍 " + venue);

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

                        upcomingEventsList.addView(itemView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }
}