package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketsActivity extends AppCompatActivity {

    private LinearLayout ticketsList;
    private TextView tvTicketCount;
    private LinearLayout navHome, navSearch, navTickets, navProfile;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tickets);

        db = FirebaseFirestore.getInstance();

        ticketsList   = findViewById(R.id.ticketsList);
        tvTicketCount = findViewById(R.id.tvTicketCount);
        navHome       = findViewById(R.id.navHome);
        navSearch     = findViewById(R.id.navSearch);
        navTickets    = findViewById(R.id.navTickets);
        navProfile    = findViewById(R.id.navProfile);

        loadTickets();

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });
        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );
        navTickets.setOnClickListener(v -> { /* already here */ });
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );
    }

    private void loadTickets() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Date now = new Date();

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    ticketsList.removeAllViews();

                    List<DocumentSnapshot> rsvpDocs = rsvpQuery.getDocuments();
                    if (rsvpDocs.isEmpty()) {
                        showEmpty();
                        tvTicketCount.setText("0 Upcoming");
                        return;
                    }

                    AtomicInteger remaining = new AtomicInteger(rsvpDocs.size());
                    AtomicInteger shown     = new AtomicInteger(0);

                    for (DocumentSnapshot rsvpDoc : rsvpDocs) {
                        String rsvpId  = rsvpDoc.getId();
                        String eventId = rsvpDoc.getString("eventId");

                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0) finalise(shown);
                            continue;
                        }

                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    Timestamp ts = eventDoc.getTimestamp("date");

                                    // Only future events shown here
                                    if (ts != null && ts.toDate().after(now)) {

                                        String title = eventDoc.getString("title");
                                        String venue = eventDoc.getString("venue");
                                        String desc  = eventDoc.getString("description");
                                        int cap = eventDoc.getLong("capacity") != null
                                                ? eventDoc.getLong("capacity").intValue() : 0;
                                        int reg = eventDoc.getLong("registeredCount") != null
                                                ? eventDoc.getLong("registeredCount").intValue() : 0;
                                        long millis = ts.toDate().getTime();

                                        if (title == null) title = "Unknown Event";
                                        if (venue == null) venue = "";

                                        Date d = ts.toDate();
                                        String day   = new SimpleDateFormat("dd",  Locale.getDefault()).format(d);
                                        String month = new SimpleDateFormat("MMM", Locale.getDefault())
                                                .format(d).toUpperCase();

                                        // Inflate item_search_result — same card used in SearchActivity
                                        View card = LayoutInflater.from(this)
                                                .inflate(R.layout.item_search_result,
                                                        ticketsList, false);

                                        // Populate using the same IDs as SearchActivity does
                                        TextView tvTitle = card.findViewById(R.id.tvEventTitle);
                                        if (tvTitle != null) tvTitle.setText(title);

                                        TextView tvVenue = card.findViewById(R.id.tvEventVenue);
                                        if (tvVenue != null) tvVenue.setText("📍 " + venue);

                                        TextView tvDay = card.findViewById(R.id.tvDateDay);
                                        if (tvDay != null) tvDay.setText(day);

                                        TextView tvMonth = card.findViewById(R.id.tvDateMonth);
                                        if (tvMonth != null) tvMonth.setText(month);

                                        // Availability badge — show "Registered" for tickets
                                        TextView tvAvailability = card.findViewById(R.id.tvAvailability);
                                        if (tvAvailability != null) {
                                            tvAvailability.setText("Registered");
                                        }

                                        String finalTitle   = title;
                                        String finalVenue   = venue;
                                        String finalDesc    = desc;
                                        int    finalCap     = cap;
                                        int    finalReg     = reg;
                                        long   finalMillis  = millis;
                                        String finalRsvpId  = rsvpId;
                                        String finalEventId = eventId;

                                        card.setOnClickListener(v -> {
                                            Intent intent = new Intent(this,
                                                    TicketEventDetailsActivity.class);
                                            intent.putExtra("eventId",          finalEventId);
                                            intent.putExtra("rsvpId",           finalRsvpId);
                                            intent.putExtra("eventTitle",        finalTitle);
                                            intent.putExtra("eventVenue",        finalVenue);
                                            intent.putExtra("eventDescription",  finalDesc);
                                            intent.putExtra("eventCapacity",     finalCap);
                                            intent.putExtra("eventRegistered",   finalReg);
                                            intent.putExtra("eventDateMillis",   finalMillis);
                                            startActivity(intent);
                                        });

                                        ticketsList.addView(card);
                                        shown.incrementAndGet();
                                    }

                                    if (remaining.decrementAndGet() == 0) finalise(shown);
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0) finalise(shown);
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load tickets",
                                Toast.LENGTH_SHORT).show()
                );
    }

    /** Called once all async event fetches are done. */
    private void finalise(AtomicInteger shown) {
        int count = shown.get();
        tvTicketCount.setText(count + " Upcoming");
        if (count == 0) showEmpty();
    }

    private void showEmpty() {
        TextView tv = new TextView(this);
        tv.setText("No upcoming tickets!");
        tv.setTextColor(getResources().getColor(R.color.text_dark));
        tv.setTextSize(14f);
        tv.setPadding(0, 32, 0, 0);
        ticketsList.addView(tv);
    }
}