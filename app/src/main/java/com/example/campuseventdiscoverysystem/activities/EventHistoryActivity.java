package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.HistoryAdapter;
import com.example.campuseventdiscoverysystem.models.HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ============================================================
 * EventHistoryActivity
 * ============================================================
 *
 * PURPOSE:
 * Displays user's past attended events history.
 *
 * FEATURES:
 * - Shows all confirmed RSVP events for current user
 * - Filters only past events (event date < current date)
 * - Displays event cards in RecyclerView
 * - Shows statistics:
 *      → Total attended events
 *      → Events attended this month
 * - Opens EventDisplayActivity on click (read-only mode)
 *
 * FIRESTORE STRUCTURE:
 * - rsvps (user attendance records)
 * - events (event details lookup)
 *
 * USER ROLE:
 * Student / Attendee
 */
public class EventHistoryActivity extends AppCompatActivity {

    private RecyclerView rvEventHistory;

    // Adapter for history list RecyclerView
    private HistoryAdapter adapter;

    // Main dataset of past events
    private List<HistoryItem> historyList;

    // UI stats
    private TextView tvTotalAttended, tvThisMonth;

    // Firestore database instance
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_history);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // UI bindings
        rvEventHistory  = findViewById(R.id.rvEventHistory);
        tvTotalAttended = findViewById(R.id.tvTotalAttended);
        tvThisMonth     = findViewById(R.id.tvThisMonth);

        // Back navigation button
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // RecyclerView setup
        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);

        rvEventHistory.setAdapter(adapter);

        // ---------------- ITEM CLICK HANDLER ----------------
        // Opens event details in read-only mode
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, EventDisplayActivity.class);

            intent.putExtra("eventId",          item.getEventId());
            intent.putExtra("eventTitle",       item.getTitle());
            intent.putExtra("eventVenue",       item.getVenue());
            intent.putExtra("eventDescription", item.getDescription());
            intent.putExtra("eventCapacity",    item.getCapacity());
            intent.putExtra("eventRegistered",  item.getRegistered());
            intent.putExtra("eventDateMillis",  item.getDateMillis());

            startActivity(intent);
        });

        // Load history data from Firestore
        loadHistoryData();
    }

    /**
     * ============================================================
     * DATA LOADING LOGIC
     * ============================================================
     *
     * Steps:
     * 1. Get current user
     * 2. Fetch confirmed RSVPs
     * 3. Fetch corresponding event details
     * 4. Filter only past events
     * 5. Build HistoryItem objects
     */
    private void loadHistoryData() {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("rsvps")
                .whereEqualTo("userId", currentUser.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {

                    historyList.clear();

                    List<DocumentSnapshot> rsvpDocs = rsvpQuery.getDocuments();

                    if (rsvpDocs.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        updateStats();
                        return;
                    }

                    Date now = new Date();

                    // Tracks async Firestore calls completion
                    AtomicInteger remaining = new AtomicInteger(rsvpDocs.size());

                    for (DocumentSnapshot rsvpDoc : rsvpDocs) {

                        String eventId = rsvpDoc.getString("eventId");

                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0) {
                                adapter.notifyDataSetChanged();
                                updateStats();
                            }
                            continue;
                        }

                        // Fetch event details for each RSVP
                        db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {

                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");

                                    // ---------------- FILTER PAST EVENTS ONLY ----------------
                                    if (ts != null && ts.toDate().before(now)) {

                                        String title = eventDoc.getString("title");
                                        String venue = eventDoc.getString("venue");
                                        String desc  = eventDoc.getString("description");

                                        int cap = eventDoc.getLong("capacity") != null
                                                ? eventDoc.getLong("capacity").intValue() : 0;

                                        int reg = eventDoc.getLong("registeredCount") != null
                                                ? eventDoc.getLong("registeredCount").intValue() : 0;

                                        long millis = ts.toDate().getTime();

                                        // Fallback title if missing
                                        if (title == null) title = rsvpDoc.getString("eventName");
                                        if (title == null) title = "Unknown Event";

                                        if (venue == null) venue = "";

                                        // Format date into day/month display
                                        Date d = ts.toDate();

                                        String day   = new SimpleDateFormat("dd", Locale.getDefault())
                                                .format(d);

                                        String month = new SimpleDateFormat("MMM", Locale.getDefault())
                                                .format(d).toUpperCase();

                                        // Build history item
                                        HistoryItem item = new HistoryItem(title, day, month, "Attended");

                                        item.setEventId(eventDoc.getId());
                                        item.setVenue(venue);
                                        item.setDescription(desc);
                                        item.setCapacity(cap);
                                        item.setRegistered(reg);
                                        item.setDateMillis(millis);

                                        historyList.add(item);
                                    }

                                    // When all async calls finish, update UI
                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateStats();
                                    }
                                })
                                .addOnFailureListener(e -> {

                                    // Ensure UI still updates even if one event fails
                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateStats();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    /**
     * Updates statistics:
     * - Total attended events
     * - Events attended in current month
     */
    private void updateStats() {

        int thisMonthCount = 0;

        String currentMonth = new SimpleDateFormat("MMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime())
                .toUpperCase();

        for (HistoryItem item : historyList) {
            if (item.getMonth().equalsIgnoreCase(currentMonth)) {
                thisMonthCount++;
            }
        }

        // Total past events attended
        tvTotalAttended.setText(String.valueOf(historyList.size()));

        // Events attended this month
        tvThisMonth.setText(String.valueOf(thisMonthCount));
    }
}