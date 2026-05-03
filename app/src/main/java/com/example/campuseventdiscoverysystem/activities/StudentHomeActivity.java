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
import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.recommendations.RecommendationEngine;
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
import java.util.List;
import java.util.Locale;

public class StudentHomeActivity extends BaseSessionActivity {

    private static final int HOME_RECS_PREVIEW = 3;

    private TextView tvEventsThisWeek, tvRegistered, tvSaved, tvGreeting, tvRecsReason;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList, recsList;

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
        recsList           = findViewById(R.id.recsList);
        tvRecsReason       = findViewById(R.id.tvRecsReason);

        loadGreeting();
        loadEventsThisWeek();
        loadRegisteredCount();
        loadSavedCount();
        listenToUpcomingEvents();
        loadRecommendationsPreview();

        TextView tvSeeAllRecs = findViewById(R.id.tvSeeAllRecs);
        if (tvSeeAllRecs != null) {
            tvSeeAllRecs.setOnClickListener(v ->
                    startActivity(new Intent(this, RecommendationsActivity.class)));
        }

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

    private void loadRecommendationsPreview() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        RecommendationEngine engine = new RecommendationEngine(db, HOME_RECS_PREVIEW);
        engine.getRecommendations(user.getUid(), new RecommendationEngine.Callback() {
            @Override
            public void onRecommendations(List<Event> recs, String reason) {
                if (isFinishing() || isDestroyed()) return;
                recsList.removeAllViews();

                if (recs.isEmpty()) {
                    tvRecsReason.setText("RSVP to a few events and we'll start picking for you.");
                    return;
                }

                tvRecsReason.setText(reason);
                for (Event e : recs) recsList.addView(buildRecCard(e));
            }

            @Override
            public void onError(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                tvRecsReason.setText("Couldn't load recommendations.");
            }
        });
    }

    private View buildRecCard(Event e) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_upcoming, recsList, false);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvLocation = v.findViewById(R.id.tvLocation);
        TextView tvDay = v.findViewById(R.id.tvDay);
        TextView tvMonth = v.findViewById(R.id.tvMonth);
        TextView tvPrice = v.findViewById(R.id.tvPrice);

        if (tvTitle != null) tvTitle.setText(e.getTitle());
        if (tvLocation != null) tvLocation.setText("📍 " + (e.getVenue() != null ? e.getVenue() : ""));
        if (tvPrice != null) tvPrice.setText(e.getPriceDisplay());

        Timestamp ts = e.getDate();
        if (ts != null) {
            Date d = ts.toDate();
            if (tvDay != null)
                tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));
            if (tvMonth != null)
                tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
        }

        long dateMillis = ts != null ? ts.toDate().getTime() : 0L;
        v.setOnClickListener(view -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", e.getId());
            intent.putExtra("eventTitle", e.getTitle());
            intent.putExtra("eventVenue", e.getVenue());
            intent.putExtra("eventDescription", e.getDescription());
            intent.putExtra("eventCapacity", e.getCapacity());
            intent.putExtra("eventRegistered", e.getRegisteredCount());
            intent.putExtra("eventDateMillis", dateMillis);
            intent.putExtra("eventOrganizerName", e.getSubmittedByName());
            intent.putExtra("eventOrganizerEmail", e.getSubmittedByEmail());
            intent.putExtra("eventTicketPrice", e.getPrice());
            startActivity(intent);
        });
        return v;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (upcomingEventsListener != null) upcomingEventsListener.remove();
    }

    private void loadGreeting() {}
    private void loadEventsThisWeek() {}
    private void loadRegisteredCount() {}
    private void loadSavedCount() {}
}