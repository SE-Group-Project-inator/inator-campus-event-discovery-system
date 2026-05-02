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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * StudentHomeActivity — MERGED VERSION
 *
 * Keeps origin/main improvements:
 * - Time-of-day greeting with fallback name
 * - Notification bell navigates to NotificationsActivity
 * - Unread notification badge (bell turns red)
 * - Real-time snapshot listener for upcoming events
 * - loadEventsThisWeek() reads only current week
 * - loadRegisteredCount() reads from "registrations" collection
 * - onDestroy() removes snapshot listener
 * - onResume() refreshes counts
 *
 * Keeps your logic:
 * - Upcoming events list filters out already-RSVPd events
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
        loadSavedCount();
        loadEventsThisWeek();
        loadRegisteredCount();

        // Fetch RSVPs first, then show upcoming events with filtering
        loadRsvpsThenUpcoming();

        // Notification bell → NotificationsActivity
        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

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
        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, EventHistoryActivity.class)));
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class)));

        // Logout button
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // ─────────────────────────────────────────────────────────────
    // Greeting
    // ─────────────────────────────────────────────────────────────

    private void loadGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String firstName = "there";
                    if (doc.exists() && doc.getString("name") != null) {
                        firstName = doc.getString("name").split(" ")[0];
                    }

                    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    String timeGreet;
                    if (hour < 12)      timeGreet = "Good morning";
                    else if (hour < 17) timeGreet = "Good afternoon";
                    else                timeGreet = "Good evening";

                    tvGreeting.setText(timeGreet + ", " + firstName + "! 👋");
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Stats
    // ─────────────────────────────────────────────────────────────

    /**
     * Counts only events happening THIS week (Mon–Sun).
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
     * Reads from "registrations" collection — consistent with RsvpActivity.
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

    // ─────────────────────────────────────────────────────────────
    // Notifications badge
    // ─────────────────────────────────────────────────────────────

    private void loadUnreadNotificationCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid())
                .collection("notifications")
                .whereEqualTo("unread", true)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        btnNotification.setColorFilter(
                                getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Upcoming events — real-time listener + RSVP filtering
    // ─────────────────────────────────────────────────────────────

    /**
     * Fetches the user's confirmed RSVPs first, then starts a real-time listener
     * for upcoming events, filtering out any already-RSVPd ones.
     */
    private void loadRsvpsThenUpcoming() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            listenToUpcomingEvents(new HashSet<>());
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
                    listenToUpcomingEvents(rsvpedIds);
                })
                .addOnFailureListener(e -> listenToUpcomingEvents(new HashSet<>()));
    }

    /**
     * Real-time listener for active upcoming events, filtered by rsvpedIds.
     */
    private void listenToUpcomingEvents(Set<String> rsvpedIds) {
        if (upcomingEventsListener != null) upcomingEventsListener.remove();

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

                    // Filter out events the user already RSVPd to
                    List<QueryDocumentSnapshot> eligible = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        if (!rsvpedIds.contains(doc.getId())) {
                            eligible.add(doc);
                        }
                    }

                    upcomingEventsList.removeAllViews();

                    if (eligible.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No upcoming events — check back soon!");
                        empty.setTextColor(getResources().getColor(R.color.text_dark, getTheme()));
                        empty.setPadding(0, 16, 0, 16);
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : eligible) {
                        String title     = doc.getString("title");
                        String venue     = doc.getString("venue");
                        String desc      = doc.getString("description");
                        Timestamp date   = doc.getTimestamp("date");
                        String startTime = doc.getString("startTime");
                        int cap          = doc.getLong("capacity") != null ? doc.getLong("capacity").intValue() : 0;
                        int reg          = doc.getLong("registeredCount") != null ? doc.getLong("registeredCount").intValue() : 0;
                        long dateMillis  = date != null ? date.toDate().getTime() : 0;
                        String orgName   = doc.getString("submittedByName");
                        String orgEmail  = doc.getString("submittedByEmail");

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

                        // Tap → EventDetailActivity
                        String finalEventId  = doc.getId();
                        String finalTitle    = title;
                        String finalVenue    = venue;
                        String finalDesc     = desc;
                        String finalOrgName  = orgName;
                        String finalOrgEmail = orgEmail;
                        int finalCap         = cap;
                        int finalReg         = reg;
                        long finalDateMillis = dateMillis;

                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, EventDetailActivity.class);
                            intent.putExtra("eventId",             finalEventId);
                            intent.putExtra("eventTitle",          finalTitle);
                            intent.putExtra("eventVenue",          finalVenue);
                            intent.putExtra("eventDescription",    finalDesc);
                            intent.putExtra("eventCapacity",       finalCap);
                            intent.putExtra("eventRegistered",     finalReg);
                            intent.putExtra("eventDateMillis",     finalDateMillis);
                            intent.putExtra("eventOrganizerName",  finalOrgName);
                            intent.putExtra("eventOrganizerEmail", finalOrgEmail);
                            startActivity(intent);
                        });

                        upcomingEventsList.addView(itemView);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Trending preview cards
    // ─────────────────────────────────────────────────────────────

    /** Populates one of the two trending preview cards on the homepage. */
    private void populateTrendingCard(int cardViewId, QueryDocumentSnapshot doc) {
        View card = findViewById(cardViewId);
        if (card == null) return;

        if (doc == null) {
            card.setVisibility(View.INVISIBLE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        String title    = doc.getString("title");
        String category = doc.getString("category");
        Timestamp date  = doc.getTimestamp("date");
        int cap         = doc.getLong("capacity") != null ? doc.getLong("capacity").intValue() : 0;
        int reg         = doc.getLong("registeredCount") != null ? doc.getLong("registeredCount").intValue() : 0;
        String venue    = doc.getString("venue");

        TextView tvCategory = card.findViewById(R.id.tvCategory);
        if (tvCategory != null) tvCategory.setText(category != null ? category : "");

        TextView tvTitle = card.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(title != null ? title : "");

        TextView tvDate = card.findViewById(R.id.tvDate);
        if (tvDate != null && date != null) {
            String dateStr = new SimpleDateFormat("d MMM", Locale.getDefault()).format(date.toDate());
            tvDate.setText(dateStr + (venue != null ? " · " + venue : ""));
        }

        TextView tvAvail = card.findViewById(R.id.textView);
        if (tvAvail != null) {
            if (cap > 0) {
                int left = cap - reg;
                tvAvail.setText(left > 0 ? left + " spots left" : "Full");
            } else {
                tvAvail.setText("Open");
            }
        }

        String finalEventId   = doc.getId();
        String finalTitle     = title;
        String finalVenue     = venue;
        String finalDesc      = doc.getString("description");
        int finalCap          = cap;
        int finalReg          = reg;
        long finalDateMillis  = date != null ? date.toDate().getTime() : 0;

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId",          finalEventId);
            intent.putExtra("eventTitle",        finalTitle);
            intent.putExtra("eventVenue",        finalVenue);
            intent.putExtra("eventDescription",  finalDesc);
            intent.putExtra("eventCapacity",     finalCap);
            intent.putExtra("eventRegistered",   finalReg);
            intent.putExtra("eventDateMillis",   finalDateMillis);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume(); // BaseSessionActivity checks session — auto-logout if expired
        loadRegisteredCount();
        loadUnreadNotificationCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (upcomingEventsListener != null) upcomingEventsListener.remove();
    }
}