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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudentHomeActivity extends AppCompatActivity {

    private TextView tvEventsThisWeek, tvRegistered, tvSaved, tvGreeting;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private LinearLayout upcomingEventsList;

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

        loadGreeting();
        loadSavedCount();

        // Load everything that depends on the user's RSVPs first
        loadRsvpsThenContent();

        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

        navHome.setOnClickListener(v -> { /* already here */ });
        navSearch.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        navTickets.setOnClickListener(v -> startActivity(new Intent(this, TicketsActivity.class)));
        navProfile.setOnClickListener(v -> startActivity(new Intent(this, StudentProfileActivity.class)));

        TextView tvSeeAllTrending = findViewById(R.id.tvSeeAllTrending);
        tvSeeAllTrending.setOnClickListener(v ->
                startActivity(new Intent(this, TrendingEventsActivity.class))
        );
    }

    private void loadGreeting() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) {
                            tvGreeting.setText("Hello, " + name.split(" ")[0] + "!");
                        }
                    }
                });
    }

    private void loadSavedCount() {
        tvSaved.setText("0 Saved");
    }

    /**
     * Fetches all confirmed RSVPs for the current user, then uses the resulting
     * set of event IDs to drive every other piece of UI on this screen:
     * - "X Registered" stat
     * - "X Events" stat (future events NOT in RSVPs)
     * - Upcoming events list (future events NOT in RSVPs)
     * - Two trending preview cards (most-registered future events NOT in RSVPs)
     */
    private void loadRsvpsThenContent() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvRegistered.setText("0 Registered");
            tvEventsThisWeek.setText("0 Events");
            loadUpcomingEvents(new HashSet<>());
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

                    // "X Registered" = total confirmed RSVPs (all, not just future)
                    tvRegistered.setText(rsvpQuery.size() + " Registered");

                    // Load upcoming and trending filtered by this set
                    loadUpcomingEvents(rsvpedIds);
                })
                .addOnFailureListener(e -> {
                    tvRegistered.setText("0 Registered");
                    loadUpcomingEvents(new HashSet<>());
                });
    }

    private void loadUpcomingEvents(Set<String> rsvpedIds) {
        upcomingEventsList.removeAllViews();

        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {
                    Date now = new Date();

                    // Collect all future, non-RSVPd events
                    List<QueryDocumentSnapshot> eligible = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        if (rsvpedIds.contains(doc.getId())) continue;
                        Timestamp ts = doc.getTimestamp("date");
                        if (ts == null || ts.toDate().before(now)) continue;
                        eligible.add(doc);
                    }

                    // Sort by date ascending (soonest first)
                    eligible.sort((a, b) -> {
                        Timestamp ta = a.getTimestamp("date");
                        Timestamp tb = b.getTimestamp("date");
                        if (ta == null || tb == null) return 0;
                        return ta.compareTo(tb);
                    });

                    // "X Events" stat = how many future non-RSVPd events exist
                    tvEventsThisWeek.setText(eligible.size() + " Events");

//                    // Populate trending preview cards (top 2 by registeredCount)
//                    List<QueryDocumentSnapshot> trending = new ArrayList<>(eligible);
//                    trending.sort((a, b) -> {
//                        long ra = a.getLong("registeredCount") != null ? a.getLong("registeredCount") : 0;
//                        long rb = b.getLong("registeredCount") != null ? b.getLong("registeredCount") : 0;
//                        return Long.compare(rb, ra);
//                    });
//                    populateTrendingCard(R.id.trendingCard1, trending.size() > 0 ? trending.get(0) : null);
//                    populateTrendingCard(R.id.trendingCard2, trending.size() > 1 ? trending.get(1) : null);

                    // Populate upcoming list
                    if (eligible.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No new events available!");
                        empty.setTextColor(getResources().getColor(R.color.text_dark));
                        empty.setPadding(0, 16, 0, 16);
                        upcomingEventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : eligible) {
                        String title    = doc.getString("title");
                        String venue    = doc.getString("venue");
                        Timestamp date  = doc.getTimestamp("date");

                        View itemView = LayoutInflater.from(this)
                                .inflate(R.layout.item_upcoming, upcomingEventsList, false);

                        TextView tvTitle = itemView.findViewById(R.id.tvTitle);
                        if (tvTitle != null && title != null) tvTitle.setText(title);

                        TextView tvLocation = itemView.findViewById(R.id.tvLocation);
                        if (tvLocation != null && venue != null) tvLocation.setText("📍 " + venue);

                        if (date != null) {
                            Date d = date.toDate();
                            TextView tvDay = itemView.findViewById(R.id.tvDay);
                            if (tvDay != null)
                                tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));
                            TextView tvMonth = itemView.findViewById(R.id.tvMonth);
                            if (tvMonth != null)
                                tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
                        }

                        String eventIdFinal = doc.getId();
                        String titleFinal   = title;
                        String venueFinal   = venue;
                        String descFinal    = doc.getString("description");
                        int capFinal        = doc.getLong("capacity") != null ? doc.getLong("capacity").intValue() : 0;
                        int regFinal        = doc.getLong("registeredCount") != null ? doc.getLong("registeredCount").intValue() : 0;
                        long dateMillis     = date != null ? date.toDate().getTime() : 0;

                        itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, EventDetailActivity.class);
                            intent.putExtra("eventId", eventIdFinal);
                            intent.putExtra("eventTitle", titleFinal);
                            intent.putExtra("eventVenue", venueFinal);
                            intent.putExtra("eventDescription", descFinal);
                            intent.putExtra("eventCapacity", capFinal);
                            intent.putExtra("eventRegistered", regFinal);
                            intent.putExtra("eventDateMillis", dateMillis);
                            startActivity(intent);
                        });

                        upcomingEventsList.addView(itemView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }

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

        // Click → EventDetailActivity
        String eventId    = doc.getId();
        String titleF     = title;
        String venueF     = venue;
        String descF      = doc.getString("description");
        int capF          = cap;
        int regF          = reg;
        long dateMillisF  = date != null ? date.toDate().getTime() : 0;

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", titleF);
            intent.putExtra("eventVenue", venueF);
            intent.putExtra("eventDescription", descF);
            intent.putExtra("eventCapacity", capF);
            intent.putExtra("eventRegistered", regF);
            intent.putExtra("eventDateMillis", dateMillisF);
            startActivity(intent);
        });
    }
}