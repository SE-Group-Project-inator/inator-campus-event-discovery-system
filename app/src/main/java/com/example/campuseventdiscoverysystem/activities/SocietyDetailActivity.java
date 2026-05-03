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
 * Shows a society's description and its upcoming events.
 * Events are fetched from the events collection filtered by societyId field.
 * Tapping an event navigates to EventDetailActivity for normal RSVP flow.
 */
public class SocietyDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout eventsList;

    // Society descriptions — keyed by society id
    private static final java.util.Map<String, String> DESCRIPTIONS = new java.util.HashMap<>();
    static {
        DESCRIPTIONS.put("spades",
                "SPADES (Student Programming And Dev Society) is LUMS's premier tech society. " +
                        "We run hackathons, coding competitions, workshops on cutting-edge technologies, " +
                        "and connect students with industry professionals.");
        DESCRIPTIONS.put("lrs",
                "The LUMS Religious Society (LRS) fosters spiritual growth and interfaith dialogue " +
                        "on campus. We host discussions, talks, and events that bring together students " +
                        "from all faiths in a respectful and inclusive environment.");
        DESCRIPTIONS.put("lwic",
                "LUMS Women In Computing (LWIC) is dedicated to empowering women in tech at LUMS. " +
                        "We run mentorship programs, networking events, and skill-building workshops " +
                        "to support women pursuing careers in computing and technology.");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_society_detail);

        db = FirebaseFirestore.getInstance();

        String societyId       = getIntent().getStringExtra("societyId");
        String societyName     = getIntent().getStringExtra("societyName");
        String societyTagline  = getIntent().getStringExtra("societyTagline");
        String societyInitials = getIntent().getStringExtra("societyInitials");

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Header
        TextView tvName = findViewById(R.id.tvSocietyName);
        TextView tvTagline = findViewById(R.id.tvSocietyTagline);
        TextView tvInitials = findViewById(R.id.tvSocietyInitials);
        TextView tvDesc = findViewById(R.id.tvSocietyDescription);

        if (tvName     != null) tvName.setText(societyName);
        if (tvTagline  != null) tvTagline.setText(societyTagline);
        if (tvInitials != null) tvInitials.setText(societyInitials);
        if (tvDesc     != null) tvDesc.setText(
                DESCRIPTIONS.containsKey(societyId)
                        ? DESCRIPTIONS.get(societyId)
                        : "No description available.");

        eventsList = findViewById(R.id.societyEventsList);

        if (societyId != null) loadSocietyEvents(societyId);
    }

    /**
     * Fetches active future events where the "society" field matches societyId.
     * Event managers tag their events with a societyId when creating them.
     */
    private void loadSocietyEvents(String societyId) {
        Timestamp now = new Timestamp(new Date());

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereEqualTo("societyId", societyId)
                .whereGreaterThanOrEqualTo("date", now)
                .get()
                .addOnSuccessListener(query -> {
                    eventsList.removeAllViews();

                    if (query.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No upcoming events from this society.");
                        empty.setTextColor(getResources().getColor(R.color.text_grey));
                        empty.setTextSize(13f);
                        empty.setPadding(0, 16, 0, 0);
                        eventsList.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : query) {
                        String title = doc.getString("title");
                        String venue = doc.getString("venue");
                        String desc  = doc.getString("description");
                        String orgName  = doc.getString("submittedByName");
                        String orgEmail = doc.getString("submittedByEmail");
                        Timestamp date  = doc.getTimestamp("date");

                        int cap = doc.getLong("capacity") != null
                                ? doc.getLong("capacity").intValue() : 0;
                        int reg = doc.getLong("registeredCount") != null
                                ? doc.getLong("registeredCount").intValue() : 0;
                        long millis = date != null ? date.toDate().getTime() : 0;
                        double price = doc.getDouble("price") != null ? doc.getDouble("price") :
                                doc.getDouble("ticketPrice") != null ? doc.getDouble("ticketPrice") : 0.0;

                        // Inflate the same item_search_result card used everywhere else
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

                        // Click → normal EventDetailActivity with full RSVP flow
                        String fId      = doc.getId();
                        String fTitle   = title;
                        String fVenue   = venue;
                        String fDesc    = desc;
                        String fOrgName = orgName;
                        String fOrgEmail= orgEmail;
                        int    fCap     = cap;
                        int    fReg     = reg;
                        long   fMillis  = millis;
                        double fPrice   = price;

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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }
}