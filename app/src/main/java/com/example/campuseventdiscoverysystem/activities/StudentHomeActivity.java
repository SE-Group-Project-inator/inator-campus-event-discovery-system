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
import com.example.campuseventdiscoverysystem.activities.MyPaymentsActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
        listenToUpcomingEvents();

        View cardMyPayments = findViewById(R.id.cardMyPayments);
        if (cardMyPayments != null) {
            cardMyPayments.setOnClickListener(v ->
                    startActivity(new Intent(this, MyPaymentsActivity.class)));
        }

        View cardQuickSearch = findViewById(R.id.cardQuickSearch);
        if (cardQuickSearch != null) cardQuickSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        View cardQuickTrending = findViewById(R.id.cardQuickTrending);
        if (cardQuickTrending != null) cardQuickTrending.setOnClickListener(v ->
                startActivity(new Intent(this, TrendingEventsActivity.class)));

        View cardQuickProfile = findViewById(R.id.cardQuickProfile);
        if (cardQuickProfile != null) cardQuickProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class)));

        View cardQuickSocieties = findViewById(R.id.cardQuickSocieties);
        if (cardQuickSocieties != null) cardQuickSocieties.setOnClickListener(v ->
                startActivity(new Intent(this, SocietiesActivity.class)));

        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        TextView tvSeeAllTrending = findViewById(R.id.tvSeeAllTrending);
        if (tvSeeAllTrending != null) {
            tvSeeAllTrending.setOnClickListener(v ->
                    startActivity(new Intent(this, TrendingEventsActivity.class)));
        }

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));

        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, TicketsActivity.class)));

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class)));

        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void listenToUpcomingEvents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // First get all eventIds this user has already RSVPd to
        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    // Build a set of already-RSVPd event IDs
                    java.util.Set<String> rsvpdIds = new java.util.HashSet<>();
                    for (DocumentSnapshot rsvp : rsvpQuery.getDocuments()) {
                        String eid = rsvp.getString("eventId");
                        if (eid != null) rsvpdIds.add(eid);
                    }
                    attachUpcomingListener(rsvpdIds);
                })
                .addOnFailureListener(e -> attachUpcomingListener(new java.util.HashSet<>()));
    }

    private void attachUpcomingListener(java.util.Set<String> rsvpdIds) {
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

                    // Filter out events already RSVPd to
                    java.util.List<QueryDocumentSnapshot> filtered = new java.util.ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        if (!rsvpdIds.contains(doc.getId())) {
                            filtered.add(doc);
                        }
                    }

                    if (filtered.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No upcoming events — check back soon!");
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : filtered) {
                        String title     = doc.getString("title");
                        String venue     = doc.getString("venue");
                        String desc      = doc.getString("description");
                        Timestamp date   = doc.getTimestamp("date");

                        int cap = doc.getLong("capacity")       != null ? doc.getLong("capacity").intValue()       : 0;
                        int reg = doc.getLong("registeredCount") != null ? doc.getLong("registeredCount").intValue() : 0;

                        long dateMillis  = date != null ? date.toDate().getTime() : 0;
                        String orgName   = doc.getString("submittedByName");
                        String orgEmail  = doc.getString("submittedByEmail");

                        final double ticketPrice =
                                doc.getDouble("price")       != null ? doc.getDouble("price") :
                                        doc.getDouble("ticketPrice") != null ? doc.getDouble("ticketPrice") : 0.0;

                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_upcoming, upcomingEventsList, false);

                        TextView tvTitle    = itemView.findViewById(R.id.tvTitle);
                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        TextView tvDay      = itemView.findViewById(R.id.tvDay);
                        TextView tvMonth    = itemView.findViewById(R.id.tvMonth);
                        TextView tvPrice    = itemView.findViewById(R.id.tvPrice);

                        if (tvTitle    != null) tvTitle.setText(title);
                        if (tvLocation != null) tvLocation.setText("📍 " + venue);
                        if (tvPrice    != null) tvPrice.setText(ticketPrice > 0 ? "Rs. " + (int) ticketPrice : "FREE");

                        if (date != null) {
                            Date d = date.toDate();
                            if (tvDay   != null) tvDay.setText(new SimpleDateFormat("dd",  Locale.getDefault()).format(d));
                            if (tvMonth != null) tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
                        }

                        String finalTitle    = title;
                        String finalVenue    = venue;
                        String finalDesc     = desc;
                        String finalOrgName  = orgName;
                        String finalOrgEmail = orgEmail;
                        String finalEventId  = doc.getId();

                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, EventDetailActivity.class);
                            intent.putExtra("eventId",             finalEventId);
                            intent.putExtra("eventTitle",           finalTitle);
                            intent.putExtra("eventVenue",           finalVenue);
                            intent.putExtra("eventDescription",     finalDesc);
                            intent.putExtra("eventCapacity",        cap);
                            intent.putExtra("eventRegistered",      reg);
                            intent.putExtra("eventDateMillis",      dateMillis);
                            intent.putExtra("eventOrganizerName",   finalOrgName);
                            intent.putExtra("eventOrganizerEmail",  finalOrgEmail);
                            intent.putExtra("eventTicketPrice",     ticketPrice);
                            startActivity(intent);
                        });

                        upcomingEventsList.addView(itemView);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (upcomingEventsListener != null) upcomingEventsListener.remove();
    }

    private void loadGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) tvGreeting.setText("Hello, " + name.split(" ")[0] + "!");
                    }
                });
    }

    private void loadEventsThisWeek() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { tvEventsThisWeek.setText("0"); return; }

        Calendar startOfWeek = Calendar.getInstance();
        startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        startOfWeek.set(Calendar.MINUTE, 0);
        startOfWeek.set(Calendar.SECOND, 0);
        startOfWeek.set(Calendar.MILLISECOND, 0);
        Calendar endOfWeek = (Calendar) startOfWeek.clone();
        endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
        endOfWeek.set(Calendar.HOUR_OF_DAY, 23);
        endOfWeek.set(Calendar.MINUTE, 59);
        endOfWeek.set(Calendar.SECOND, 59);

        Timestamp weekStart = new Timestamp(startOfWeek.getTime());
        Timestamp weekEnd   = new Timestamp(endOfWeek.getTime());

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    java.util.List<DocumentSnapshot> docs = rsvpQuery.getDocuments();
                    if (docs.isEmpty()) { tvEventsThisWeek.setText("0"); return; }

                    java.util.concurrent.atomic.AtomicInteger count     = new java.util.concurrent.atomic.AtomicInteger(0);
                    java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(docs.size());

                    for (DocumentSnapshot rsvp : docs) {
                        String eventId = rsvp.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0) tvEventsThisWeek.setText(String.valueOf(count.get()));
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    Timestamp eventDate = eventDoc.getTimestamp("date");
                                    if (eventDate != null
                                            && eventDate.compareTo(weekStart) >= 0
                                            && eventDate.compareTo(weekEnd) <= 0)
                                        count.incrementAndGet();
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsThisWeek.setText(String.valueOf(count.get()));
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsThisWeek.setText(String.valueOf(count.get()));
                                });
                    }
                })
                .addOnFailureListener(e -> tvEventsThisWeek.setText("0"));
    }

    private void loadRegisteredCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { tvRegistered.setText("0"); return; }
        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(query -> tvRegistered.setText(String.valueOf(query.size())))
                .addOnFailureListener(e -> tvRegistered.setText("0"));
    }

    private void loadSavedCount() {
        tvSaved.setText("0");
    }
}