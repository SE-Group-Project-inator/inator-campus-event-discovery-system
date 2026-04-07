package com.example.campuseventdiscoverysystem.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private EditText etSearch;
    private ImageView btnSearch;
    private ImageButton btnBack;
    private LinearLayout searchResultsList;
    private TextView tvResultCount, tvSort, tvDateValue, tvPriceValue;
    private TextView chipAll, chipSports, chipAcademic, chipCultural;
    private CardView btnDateRange, btnPriceRange, btnSort;
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    private FirebaseFirestore db;

    private String selectedCategory = "All";
    private String sortOrder = "Latest";
    private Timestamp filterDateStart = null;
    private Timestamp filterDateEnd = null;

    private List<QueryDocumentSnapshot> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_events);

        db = FirebaseFirestore.getInstance();

        etSearch        = findViewById(R.id.etSearch);
        btnSearch       = findViewById(R.id.btnSearch);
        btnBack         = findViewById(R.id.btnBack);
        searchResultsList = findViewById(R.id.searchResultsList);
        tvResultCount   = findViewById(R.id.tvResultCount);
        tvSort          = findViewById(R.id.tvSort);
        tvDateValue     = findViewById(R.id.tvDateValue);
        tvPriceValue    = findViewById(R.id.tvPriceValue);
        chipAll         = findViewById(R.id.chipAll);
        chipSports      = findViewById(R.id.chipSports);
        chipAcademic    = findViewById(R.id.chipAcademic);
        chipCultural    = findViewById(R.id.chipCultural);
        btnDateRange    = findViewById(R.id.btnDateRange);
        btnPriceRange   = findViewById(R.id.btnPriceRange);
        btnSort         = findViewById(R.id.btnSort);
        navHome         = findViewById(R.id.navHome);
        navSearch       = findViewById(R.id.navSearch);
        navTickets      = findViewById(R.id.navTickets);
        navProfile      = findViewById(R.id.navProfile);

        loadAllEvents();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndDisplay(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSearch.setOnClickListener(v ->
                filterAndDisplay(etSearch.getText().toString().trim())
        );

        btnBack.setOnClickListener(v -> finish());

        chipAll.setOnClickListener(v -> {
            selectedCategory = "All";
            updateChipSelection(chipAll);
            filterAndDisplay(etSearch.getText().toString().trim());
        });

        chipSports.setOnClickListener(v -> {
            selectedCategory = "Sports";
            updateChipSelection(chipSports);
            filterAndDisplay(etSearch.getText().toString().trim());
        });

        chipAcademic.setOnClickListener(v -> {
            selectedCategory = "Academic";
            updateChipSelection(chipAcademic);
            filterAndDisplay(etSearch.getText().toString().trim());
        });

        chipCultural.setOnClickListener(v -> {
            selectedCategory = "Cultural";
            updateChipSelection(chipCultural);
            filterAndDisplay(etSearch.getText().toString().trim());
        });

        btnDateRange.setOnClickListener(v -> {
            // If already selected → clear filter
            if (filterDateStart != null || filterDateEnd != null) {
                filterDateStart = null;
                filterDateEnd = null;
                tvDateValue.setText("Any Date");

                filterAndDisplay(etSearch.getText().toString().trim());
            } else {
                // Otherwise open picker
                showDateRangePicker();
            }
        });

        btnPriceRange.setOnClickListener(v ->
                Toast.makeText(this, "Price filter coming soon!", Toast.LENGTH_SHORT).show()
        );

        btnSort.setOnClickListener(v -> {
            if (sortOrder.equals("Latest")) {
                sortOrder = "Oldest";
                tvSort.setText("⬆ Sort: Oldest");
            } else {
                sortOrder = "Latest";
                tvSort.setText("⬇ Sort: Latest");
            }
            filterAndDisplay(etSearch.getText().toString().trim());
        });

        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });

        navTickets.setOnClickListener(v ->
                Toast.makeText(this, "Tickets coming soon!", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );
    }

    private void loadAllEvents() {
        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(query -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        allEvents.add(doc);
                    }
                    filterAndDisplay("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }

    private void filterAndDisplay(String query) {
        searchResultsList.removeAllViews();

        List<QueryDocumentSnapshot> filtered = new ArrayList<>();

        for (QueryDocumentSnapshot doc : allEvents) {
            String title  = doc.getString("title");
            String venue  = doc.getString("venue");
            Timestamp date = doc.getTimestamp("date");

            if (!query.isEmpty()) {
                boolean matchesTitle = title != null &&
                        title.toLowerCase().contains(query.toLowerCase());
                boolean matchesVenue = venue != null &&
                        venue.toLowerCase().contains(query.toLowerCase());
                if (!matchesTitle && !matchesVenue) continue;
            }

            // Category chip filter — selectedCategory was tracked but never applied
            if (!selectedCategory.equals("All")) {
                String category = doc.getString("category");
                if (category == null || !category.equalsIgnoreCase(selectedCategory)) continue;
            }

            if (filterDateStart != null && date != null) {
                if (date.compareTo(filterDateStart) < 0) continue;
            }
            if (filterDateEnd != null && date != null) {
                if (date.compareTo(filterDateEnd) > 0) continue;
            }

            filtered.add(doc);
        }

        filtered.sort((a, b) -> {
            Timestamp dateA = a.getTimestamp("date");
            Timestamp dateB = b.getTimestamp("date");
            if (dateA == null || dateB == null) return 0;
            if (sortOrder.equals("Latest")) {
                return dateB.compareTo(dateA);
            } else {
                return dateA.compareTo(dateB);
            }
        });

        tvResultCount.setText(filtered.size() + " events found");

        // ✅ FIXED PART (safe parent access)
        View scrollContent = null;
        if (searchResultsList != null && searchResultsList.getParent() instanceof View) {
            scrollContent = (View) searchResultsList.getParent();
        }

        if (filtered.isEmpty()) {

            if (scrollContent != null) {
                scrollContent.setBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.transparent));
            }

            TextView empty = new TextView(this);
            empty.setText("No events found");
            empty.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            empty.setPadding(0, 24, 0, 24);
            searchResultsList.addView(empty);

        } else {

            if (scrollContent != null) {
                scrollContent.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.bg_light_blue));
            }

            for (QueryDocumentSnapshot doc : filtered) {
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_search_result,
                                searchResultsList, false);

                String title = doc.getString("title");
                String venue = doc.getString("venue");
                Timestamp date = doc.getTimestamp("date");
                Long capacity = doc.getLong("capacity");
                Long registered = doc.getLong("registeredCount");

                TextView tvTitle = itemView.findViewById(R.id.tvEventTitle);
                if (tvTitle != null && title != null)
                    tvTitle.setText(title);

                TextView tvVenue = itemView.findViewById(R.id.tvEventVenue);
                if (tvVenue != null && venue != null)
                    tvVenue.setText("📍 " + venue);

                if (date != null) {
                    Date d = date.toDate();

                    TextView tvDay = itemView.findViewById(R.id.tvDateDay);
                    if (tvDay != null)
                        tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));

                    TextView tvMonth = itemView.findViewById(R.id.tvDateMonth);
                    if (tvMonth != null)
                        tvMonth.setText(new SimpleDateFormat("MMM", Locale.getDefault())
                                .format(d).toUpperCase());
                }

                TextView tvAvail = itemView.findViewById(R.id.tvAvailability);
                if (tvAvail != null && capacity != null && registered != null) {
                    double fillPct = (double) registered / capacity * 100;

                    if (fillPct >= 100) {
                        tvAvail.setText("Full");
                        tvAvail.setTextColor(
                                ContextCompat.getColor(this, R.color.red_decline));
                        tvAvail.setBackgroundResource(R.drawable.badge_full);
                    } else if (fillPct >= 80) {
                        tvAvail.setText("Almost Full");
                        tvAvail.setTextColor(
                                ContextCompat.getColor(this, R.color.card_tan));
                        tvAvail.setBackgroundResource(R.drawable.badge_almost_full);
                    } else {
                        tvAvail.setText("Available");
                        tvAvail.setTextColor(
                                ContextCompat.getColor(this, R.color.green_accept));
                        tvAvail.setBackgroundResource(R.drawable.badge_available);
                    }
                }

                // Navigate to EventDetailActivity when tapping the card or arrow
                String eventIdFinal  = doc.getId();
                String titleFinal    = title;
                String venueFinal    = venue;
                String descFinal     = doc.getString("description");
                int    capFinal      = capacity != null ? capacity.intValue() : 0;
                int    regFinal      = registered != null ? registered.intValue() : 0;
                long   dateMillis    = date != null ? date.toDate().getTime() : 0;

                View.OnClickListener openDetail = v -> {
                    Intent intent = new Intent(this, EventDetailActivity.class);
                    intent.putExtra("eventId", eventIdFinal);
                    intent.putExtra("eventTitle", titleFinal);
                    intent.putExtra("eventVenue", venueFinal);
                    intent.putExtra("eventDescription", descFinal);
                    intent.putExtra("eventCapacity", capFinal);
                    intent.putExtra("eventRegistered", regFinal);
                    intent.putExtra("eventDateMillis", dateMillis);
                    startActivity(intent);
                };

                itemView.setOnClickListener(openDetail);
                CardView btnArrow = itemView.findViewById(R.id.btnArrow);
                if (btnArrow != null) btnArrow.setOnClickListener(openDetail);

                searchResultsList.addView(itemView);
            }
        }
    }

    private void showDateRangePicker() {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, day, 0, 0, 0);
            filterDateStart = new Timestamp(startCal.getTime());

            new DatePickerDialog(this, (view2, year2, month2, day2) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(year2, month2, day2, 23, 59, 59);
                filterDateEnd = new Timestamp(endCal.getTime());

                String label = day + "/" + (month + 1) +
                        " – " + day2 + "/" + (month2 + 1);
                tvDateValue.setText(label);

                filterAndDisplay(etSearch.getText().toString().trim());

            }, cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();

        }, cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateChipSelection(TextView selected) {
        TextView[] chips = {chipAll, chipSports, chipAcademic, chipCultural};
        for (TextView chip : chips) {
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.chip_active);
                chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                chip.setBackgroundResource(R.drawable.chip_inactive);
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_dark));
            }
        }
    }
}