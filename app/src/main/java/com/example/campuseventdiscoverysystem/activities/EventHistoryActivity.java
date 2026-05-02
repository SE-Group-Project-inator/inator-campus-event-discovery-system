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

public class EventHistoryActivity extends AppCompatActivity {

    private RecyclerView rvEventHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private TextView tvTotalAttended, tvThisMonth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_history);

        db = FirebaseFirestore.getInstance();

        rvEventHistory  = findViewById(R.id.rvEventHistory);
        tvTotalAttended = findViewById(R.id.tvTotalAttended);
        tvThisMonth     = findViewById(R.id.tvThisMonth);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        rvEventHistory.setAdapter(adapter);

        // Tap on a history card → open EventDisplayActivity (read-only details)
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, EventDisplayActivity.class);
            intent.putExtra("eventId",          item.getEventId());
            intent.putExtra("eventTitle",        item.getTitle());
            intent.putExtra("eventVenue",        item.getVenue());
            intent.putExtra("eventDescription",  item.getDescription());
            intent.putExtra("eventCapacity",     item.getCapacity());
            intent.putExtra("eventRegistered",   item.getRegistered());
            intent.putExtra("eventDateMillis",   item.getDateMillis());
            startActivity(intent);
        });

        loadHistoryData();
    }

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

                        db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {

                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");

                                    // Only include events whose date has already passed
                                    if (ts != null && ts.toDate().before(now)) {

                                        String title = eventDoc.getString("title");
                                        String venue = eventDoc.getString("venue");
                                        String desc  = eventDoc.getString("description");
                                        int cap = eventDoc.getLong("capacity") != null
                                                ? eventDoc.getLong("capacity").intValue() : 0;
                                        int reg = eventDoc.getLong("registeredCount") != null
                                                ? eventDoc.getLong("registeredCount").intValue() : 0;
                                        long millis = ts.toDate().getTime();

                                        if (title == null) title = rsvpDoc.getString("eventName");
                                        if (title == null) title = "Unknown Event";
                                        if (venue == null) venue = "";

                                        Date d = ts.toDate();
                                        String day   = new SimpleDateFormat("dd",  Locale.getDefault()).format(d);
                                        String month = new SimpleDateFormat("MMM", Locale.getDefault())
                                                .format(d).toUpperCase();

                                        HistoryItem item = new HistoryItem(title, day, month, "Attended");
                                        item.setEventId(eventDoc.getId());
                                        item.setVenue(venue);
                                        item.setDescription(desc);
                                        item.setCapacity(cap);
                                        item.setRegistered(reg);
                                        item.setDateMillis(millis);

                                        historyList.add(item);
                                    }

                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateStats();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0) {
                                        adapter.notifyDataSetChanged();
                                        updateStats();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void updateStats() {
        int thisMonthCount = 0;

        String currentMonth = new SimpleDateFormat("MMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime()).toUpperCase();

        for (HistoryItem item : historyList) {
            if (item.getMonth().equalsIgnoreCase(currentMonth)) {
                thisMonthCount++;
            }
        }

        // Every item in the list is a past attended event
        tvTotalAttended.setText(String.valueOf(historyList.size()));
        tvThisMonth.setText(String.valueOf(thisMonthCount));
    }
}