package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.view.View;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * StudentHomeActivity — IMPROVED VERSION
 *
 * Fixes:
 * 1. Notification bell now opens NotificationsActivity instead of a Toast.
 * 2. navTickets now opens EventHistoryActivity (student's registered events).
 * 3. Events this week counter is accurate (filters by current week, not all time).
 * 4. Registered count reads from "registrations" collection (consistent with RsvpActivity).
 * 5. Real-time snapshot listener for upcoming events — new events appear without refresh.
 *
 * New features:
 * - Trending Events quick-launch card
 * - Time-of-day greeting (Good morning / afternoon / evening)
 * - Unread notification badge on bell icon
 */
public class StudentHomeActivity extends BaseSessionActivity {

    private TextView tvEventsThisWeek, tvRegistered, tvSaved, tvGreeting;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration upcomingEventsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_homepage);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

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

        loadGreeting();
        loadEventsThisWeek();
        loadRegisteredCount();
        loadSavedCount();
        listenToUpcomingEvents(); // Real-time

        // FIX: Notification bell now navigates instead of showing a Toast
        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Unread notification badge
        loadUnreadNotificationCount();

        // See All Trending
        TextView tvSeeAllTrending = findViewById(R.id.tvSeeAllTrending);
        if (tvSeeAllTrending != null) {
            tvSeeAllTrending.setOnClickListener(v ->
                    startActivity(new Intent(this, TrendingEventsActivity.class)));
        }

        // Bottom nav
        navHome.setOnClickListener(v -> { /* already here */ });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        // FIX: "Tickets" tab now shows the student's registered events history
        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, EventHistoryActivity.class)));

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class)));

        // Top-right logout button (added to XML)
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void loadGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String firstName = "there";
                    if (doc.exists() && doc.getString("name") != null) {
                        String fullName = doc.getString("name");
                        firstName = fullName.split(" ")[0];
                    }

                    // Time-of-day greeting
                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    String timeGreet;
                    if (hour < 12)      timeGreet = "Good morning";
                    else if (hour < 17) timeGreet = "Good afternoon";
                    else                timeGreet = "Good evening";

                    tvGreeting.setText(timeGreet + ", " + firstName + "! 👋");
                });
    }

    /**
     * FIX: Count only events happening THIS week (Mon–Sun), not all active events.
     */
    private void loadEventsThisWeek() {
        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);

        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_WEEK, 7);

        Timestamp tsStart = new Timestamp(weekStart.getTime());
        Timestamp tsEnd   = new Timestamp(weekEnd.getTime());

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", tsStart)
                .whereLessThan("date", tsEnd)
                .get()
                .addOnSuccessListener(query ->
                        tvEventsThisWeek.setText(query.size() + " This Week"))
                .addOnFailureListener(e ->
                        tvEventsThisWeek.setText("— Events"));
    }

    /**
     * FIX: Reads from "registrations" collection — consistent with RsvpActivity which
     * writes there. The old code checked "rsvps" collection which was never written to.
     */
    private void loadRegisteredCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("registrations")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(query ->
                        tvRegistered.setText(query.size() + " Registered"))
                .addOnFailureListener(e ->
                        tvRegistered.setText("0 Registered"));
    }

    private void loadSavedCount() {
        // Placeholder — wire up when bookmarks feature is added
        tvSaved.setText("0 Saved");
    }

    /**
     * NEW: Show unread notification count as a badge on the bell icon.
     */
    private void loadUnreadNotificationCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .collection("notifications")
                .whereEqualTo("unread", true)
                .get()
                .addOnSuccessListener(snap -> {
                    // If there are unread notifications, tint the bell icon red
                    if (snap != null && !snap.isEmpty()) {
                        btnNotification.setColorFilter(
                                getResources().getColor(android.R.color.holo_red_dark,
                                        getTheme()));
                    }
                });
    }

    /**
     * FIX: Real-time listener so newly approved events appear without a screen restart.
     * Sorted by date ascending (soonest first).
     */
    private void listenToUpcomingEvents() {
        upcomingEventsListener = db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", new Timestamp(new Date()))
                .orderBy("date")
                .limit(20)
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (query == null) return;

                    upcomingEventsList.removeAllViews();

                    if (query.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No upcoming events — check back soon!");
                        empty.setTextColor(getResources().getColor(R.color.text_dark, getTheme()));
                        empty.setPadding(0, 16, 0, 16);
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : query) {
                        String title      = doc.getString("title");
                        String venue      = doc.getString("venue");
                        String desc       = doc.getString("description");
                        Timestamp date    = doc.getTimestamp("date");
                        String startTime  = doc.getString("startTime");
                        int cap           = doc.getLong("capacity") != null ? doc.getLong("capacity").intValue() : 0;
                        int reg           = doc.getLong("registeredCount") != null ? doc.getLong("registeredCount").intValue() : 0;
                        long dateMillis   = date != null ? date.toDate().getTime() : 0;
                        String orgName    = doc.getString("submittedByName");
                        String orgEmail   = doc.getString("submittedByEmail");

                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_upcoming, upcomingEventsList, false);

                        TextView tvTitle    = itemView.findViewById(R.id.tvTitle);
                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        TextView tvDay      = itemView.findViewById(R.id.tvDay);
                        TextView tvMonth    = itemView.findViewById(R.id.tvMonth);

                        if (tvTitle != null && title != null) tvTitle.setText(title);
                        if (tvLocation != null && venue != null) tvLocation.setText("📍 " + venue);

                        if (date != null) {
                            Date d = date.toDate();
                            if (tvDay != null)
                                tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));
                            if (tvMonth != null)
                                tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
                        }

                        // Tap → open EventDetailActivity
                        String finalTitle   = title;
                        String finalVenue   = venue;
                        String finalDesc    = desc;
                        String finalOrgName = orgName;
                        String finalOrgEmail = orgEmail;
                        String finalTime    = startTime;
                        String finalEventId = doc.getId();

                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, EventDetailActivity.class);
                            intent.putExtra("eventId",            finalEventId);
                            intent.putExtra("eventTitle",         finalTitle);
                            intent.putExtra("eventVenue",         finalVenue);
                            intent.putExtra("eventDescription",   finalDesc);
                            intent.putExtra("eventCapacity",      cap);
                            intent.putExtra("eventRegistered",    reg);
                            intent.putExtra("eventDateMillis",    dateMillis);
                            intent.putExtra("eventOrganizerName", finalOrgName);
                            intent.putExtra("eventOrganizerEmail",finalOrgEmail);
                            startActivity(intent);
                        });

                        upcomingEventsList.addView(itemView);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upcomingEventsListener != null) upcomingEventsListener.remove();
    }

    @Override
    protected void onResume() {
        super.onResume(); // BaseSessionActivity checks session here — auto-logout if expired
        loadRegisteredCount();
        loadUnreadNotificationCount();
    }
}