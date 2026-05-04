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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Shows a society's name, abbreviation, description (all from Firestore),
 * and its upcoming events (events where societyId == this society's doc ID).
 */
public class SocietyDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout eventsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society_detail);

        db = FirebaseFirestore.getInstance();

        String societyId          = getIntent().getStringExtra("societyId");
        String societyName        = getIntent().getStringExtra("societyName");
        String societyDescription = getIntent().getStringExtra("societyDescription");
        String societyInitials    = getIntent().getStringExtra("societyInitials");

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Populate header from intent data (already loaded by SocietiesActivity)
        TextView tvName     = findViewById(R.id.tvSocietyName);
        TextView tvTagline  = findViewById(R.id.tvSocietyTagline);
        TextView tvInitials = findViewById(R.id.tvSocietyInitials);
        TextView tvDesc     = findViewById(R.id.tvSocietyDescription);

        if (tvName     != null) tvName.setText(societyName     != null ? societyName     : "");
        if (tvTagline  != null) tvTagline.setText(societyInitials != null ? societyInitials : "");
        if (tvInitials != null) tvInitials.setText(societyInitials != null ? societyInitials : "");
        if (tvDesc     != null) tvDesc.setText(
                societyDescription != null && !societyDescription.isEmpty()
                        ? societyDescription
                        : "No description available.");

        // Always fetch from Firestore to get latest data including category
        if (societyId != null) {
            db.collection("societies").document(societyId).get()
                    .addOnSuccessListener(doc -> {
                        String name = doc.getString("name");
                        String desc = doc.getString("description");
                        String cat  = doc.getString("category");
                        if (tvName != null && name != null) tvName.setText(name);
                        // Description falls back to category if empty
                        String displayDesc = (desc != null && !desc.isEmpty()) ? desc
                                : cat != null ? cat : "";
                        if (tvDesc != null) tvDesc.setText(displayDesc.isEmpty()
                                ? "No description available." : displayDesc);
                        // Category badge under the title
                        if (tvTagline != null && cat != null) tvTagline.setText(cat);
                    });
        }

        eventsList = findViewById(R.id.societyEventsList);
        if (societyId != null) loadSocietyEvents(societyId);
    }

    /**
     * Fetches active future events where the "societyId" field matches this society's doc ID.
     * Event managers tag events with societyId when creating them.
     */
    private void loadSocietyEvents(String societyId) {
        Timestamp now = new Timestamp(new java.util.Date());
        String societyName = getIntent().getStringExtra("societyName");

        // Query by societyId first (new events), then also by society name (old events).
        // Merging both ensures events created before societyId was added still appear.
        db.collection("events")
                .whereEqualTo("societyId", societyId)
                .get()
                .addOnSuccessListener(q1 -> {
                    java.util.Map<String, QueryDocumentSnapshot> merged = new java.util.LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : q1) merged.put(doc.getId(), doc);

                    if (societyName != null && !societyName.isEmpty()) {
                        db.collection("events")
                                .whereEqualTo("society", societyName)
                                .get()
                                .addOnSuccessListener(q2 -> {
                                    for (QueryDocumentSnapshot doc : q2) merged.put(doc.getId(), doc);
                                    renderSocietyEvents(merged.values(), now);
                                })
                                .addOnFailureListener(e -> renderSocietyEvents(merged.values(), now));
                    } else {
                        renderSocietyEvents(merged.values(), now);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show());
    }

    private void renderSocietyEvents(java.util.Collection<QueryDocumentSnapshot> docs, Timestamp now) {
        if (eventsList == null) return;
        eventsList.removeAllViews();

        java.util.List<QueryDocumentSnapshot> upcoming = new java.util.ArrayList<>();
        for (QueryDocumentSnapshot doc : docs) {
            String status = doc.getString("status");
            if (!"active".equals(status)) continue;
            Timestamp date = doc.getTimestamp("date");
            if (date != null && date.compareTo(now) >= 0) upcoming.add(doc);
        }

        if (upcoming.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No upcoming events from this society.");
            empty.setTextColor(getResources().getColor(R.color.text_grey));
            empty.setTextSize(13f);
            empty.setPadding(0, 16, 0, 0);
            eventsList.addView(empty);
            return;
        }

        for (QueryDocumentSnapshot doc : upcoming) {
            String title    = doc.getString("title");
            String venue    = doc.getString("venue");
            String desc     = doc.getString("description");
            String orgName  = doc.getString("submittedByName");
            String orgEmail = doc.getString("submittedByEmail");
            Timestamp date  = doc.getTimestamp("date");

            int cap = doc.getLong("capacity") != null
                    ? doc.getLong("capacity").intValue() : 0;
            int reg = doc.getLong("registeredCount") != null
                    ? doc.getLong("registeredCount").intValue() : 0;
            long millis = date != null ? date.toDate().getTime() : 0;
            double price = doc.getDouble("price") != null ? doc.getDouble("price")
                    : doc.getDouble("ticketPrice") != null ? doc.getDouble("ticketPrice") : 0.0;

            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_search_result, eventsList, false);

            TextView tvTitle = card.findViewById(R.id.tvEventTitle);
            TextView tvVenue = card.findViewById(R.id.tvEventVenue);
            TextView tvDay   = card.findViewById(R.id.tvDateDay);
            TextView tvMonth = card.findViewById(R.id.tvDateMonth);
            TextView tvAvail = card.findViewById(R.id.tvAvailability);
            TextView tvPrice = card.findViewById(R.id.tvPrice);

            if (tvTitle != null) tvTitle.setText(title);
            if (tvVenue != null && venue != null) tvVenue.setText("📍 " + venue);
            if (tvPrice != null) tvPrice.setText(price > 0 ? "Rs. " + (int) price : "FREE");
            if (tvAvail != null) tvAvail.setText(cap > 0 && reg >= cap ? "Full" : "Available");

            if (date != null) {
                Date d = date.toDate();
                if (tvDay   != null) tvDay.setText(new SimpleDateFormat("dd",  Locale.getDefault()).format(d));
                if (tvMonth != null) tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault()).format(d).toUpperCase());
            }

            String fId       = doc.getId();
            String fTitle    = title;
            String fVenue    = venue;
            String fDesc     = desc;
            String fOrgName  = orgName;
            String fOrgEmail = orgEmail;
            int    fCap      = cap;
            int    fReg      = reg;
            long   fMillis   = millis;
            double fPrice    = price;

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra("eventId",            fId);
                intent.putExtra("eventTitle",          fTitle);
                intent.putExtra("eventVenue",          fVenue);
                intent.putExtra("eventDescription",    fDesc);
                intent.putExtra("eventCapacity",       fCap);
                intent.putExtra("eventRegistered",     fReg);
                intent.putExtra("eventDateMillis",     fMillis);
                intent.putExtra("eventOrganizerName",  fOrgName);
                intent.putExtra("eventOrganizerEmail", fOrgEmail);
                intent.putExtra("eventTicketPrice",    fPrice);
                startActivity(intent);
            });

            eventsList.addView(card);
        }
    }
}