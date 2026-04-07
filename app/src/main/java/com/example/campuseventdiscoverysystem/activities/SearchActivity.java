package com.example.campuseventdiscoverysystem.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
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

/**
 * Activity that allows students to search, filter, and browse campus events.
 *
 * <p>
 * Features include:
 * <ul>
 *     <li>Text search (title/venue)</li>
 *     <li>Category filtering (All, Sports, Academic, Cultural)</li>
 *     <li>Date range filtering</li>
 *     <li>Sorting (Latest / Oldest)</li>
 *     <li>Dynamic event loading from Firestore</li>
 *     <li>Navigation to event details, home, profile, etc.</li>
 * </ul>
 * </p>
 */
public class SearchActivity extends AppCompatActivity {

    /** Search input field */
    private EditText etSearch;

    /** Search button icon */
    private ImageView btnSearch;

    /** Container holding all search result cards */
    private LinearLayout searchResultsList;

    /** Displays number of results found */
    private TextView tvResultCount;

    /** Displays current sort order */
    private TextView tvSort;

    /** Displays selected date filter */
    private TextView tvDateValue;

    /** Displays price filter (not yet implemented) */
    private TextView tvPriceValue;

    /** Category filter chips */
    private TextView chipAll, chipSports, chipAcademic, chipCultural;

    /** Filter and sort buttons */
    private CardView btnDateRange, btnPriceRange, btnSort;

    /** Bottom navigation containers */
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    /** Firestore database reference */
    private FirebaseFirestore db;

    /** Currently selected category filter */
    private String selectedCategory = "All";

    /** Current sort order (Latest / Oldest) */
    private String sortOrder = "Latest";

    /** Start date filter */
    private Timestamp filterDateStart = null;

    /** End date filter */
    private Timestamp filterDateEnd = null;

    /** Cached list of all events from Firestore */
    private List<QueryDocumentSnapshot> allEvents = new ArrayList<>();

    /**
     * Called when activity is created.
     * Initializes UI, loads events, and sets up listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_events);

        db = FirebaseFirestore.getInstance();

        etSearch        = findViewById(R.id.etSearch);
        btnSearch       = findViewById(R.id.btnSearch);
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
            if (filterDateStart != null || filterDateEnd != null) {
                filterDateStart = null;
                filterDateEnd = null;
                tvDateValue.setText("Any Date");
                filterAndDisplay(etSearch.getText().toString().trim());
            } else {
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

    /**
     * Loads all active events from Firestore database.
     */
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

    /**
     * Filters events based on search query, category, date range, and sort order,
     * then displays them in the UI.
     */
    private void filterAndDisplay(String query) {
        searchResultsList.removeAllViews();

        List<QueryDocumentSnapshot> filtered = new ArrayList<>();

        for (QueryDocumentSnapshot doc : allEvents) {
            String title = doc.getString("title");
            String venue = doc.getString("venue");
            Timestamp date = doc.getTimestamp("date");

            if (!query.isEmpty()) {
                boolean matchesTitle = title != null &&
                        title.toLowerCase().contains(query.toLowerCase());
                boolean matchesVenue = venue != null &&
                        venue.toLowerCase().contains(query.toLowerCase());
                if (!matchesTitle && !matchesVenue) continue;
            }

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

            return sortOrder.equals("Latest")
                    ? dateB.compareTo(dateA)
                    : dateA.compareTo(dateB);
        });

        tvResultCount.setText(filtered.size() + " events found");

        View scrollContent = null;
        if (searchResultsList.getParent() instanceof View) {
            scrollContent = (View) searchResultsList.getParent();
        }

        if (filtered.isEmpty()) {
            if (scrollContent != null) {
                scrollContent.setBackgroundColor(
                        ContextCompat.getColor(this, android.R.color.transparent));
            }

            TextView empty = new TextView(this);
            empty.setText("No events found");
            searchResultsList.addView(empty);

        } else {
            if (scrollContent != null) {
                scrollContent.setBackgroundColor(
                        ContextCompat.getColor(this, R.color.bg_light_blue));
            }

            for (QueryDocumentSnapshot doc : filtered) {
                View itemView = LayoutInflater.from(this)
                        .inflate(R.layout.item_search_result, searchResultsList, false);

                searchResultsList.addView(itemView);
            }
        }
    }

    /**
     * Opens a date range picker dialog.
     */
    private void showDateRangePicker() {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {

            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, day, 0, 0, 0);
            filterDateStart = new Timestamp(startCal.getTime());

            new DatePickerDialog(this, (v2, y2, m2, d2) -> {

                Calendar endCal = Calendar.getInstance();
                endCal.set(y2, m2, d2, 23, 59, 59);
                filterDateEnd = new Timestamp(endCal.getTime());

            }, cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();

        }, cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * Updates UI chip selection for categories.
     */
    private void updateChipSelection(TextView selected) {
        TextView[] chips = {chipAll, chipSports, chipAcademic, chipCultural};

        for (TextView chip : chips) {
            if (chip == selected) {
                chip.setBackgroundResource(R.drawable.chip_active);
            } else {
                chip.setBackgroundResource(R.drawable.chip_inactive);
            }
        }
    }
}