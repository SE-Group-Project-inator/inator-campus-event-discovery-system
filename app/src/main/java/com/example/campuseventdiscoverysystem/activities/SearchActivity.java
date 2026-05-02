package com.example.campuseventdiscoverysystem.activities;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.auth.FirebaseUser;
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
    private LinearLayout searchResultsList;
    private TextView tvResultCount;
    private TextView tvSort;
    private TextView tvDateValue;
    private TextView tvPriceValue;
    private TextView chipAll, chipSports, chipAcademic, chipCultural;
    private LinearLayout btnDateRange, btnPriceRange, btnSort;
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    private FirebaseFirestore db;
    private String selectedCategory = "All";
    private String sortOrder = "Latest";
    private Timestamp filterDateStart = null;
    private Timestamp filterDateEnd = null;
    private double filterMinPrice = -1;
    private double filterMaxPrice = -1;
    private List<QueryDocumentSnapshot> allEvents = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_events);

        db = FirebaseFirestore.getInstance();

        etSearch          = findViewById(R.id.etSearch);
        btnSearch         = findViewById(R.id.btnSearch);
        searchResultsList = findViewById(R.id.searchResultsList);
        tvResultCount     = findViewById(R.id.tvResultCount);
        tvSort            = findViewById(R.id.tvSort);
        tvDateValue       = findViewById(R.id.tvDateValue);
        tvPriceValue      = findViewById(R.id.tvPriceValue);
        chipAll           = findViewById(R.id.chipAll);
        chipSports        = findViewById(R.id.chipSports);
        chipAcademic      = findViewById(R.id.chipAcademic);
        chipCultural      = findViewById(R.id.chipCultural);
        btnDateRange      = findViewById(R.id.btnDateRange);
        btnPriceRange     = findViewById(R.id.btnPriceRange);
        btnSort           = findViewById(R.id.btnSort);
        navHome           = findViewById(R.id.navHome);
        navSearch         = findViewById(R.id.navSearch);
        navTickets        = findViewById(R.id.navTickets);
        navProfile        = findViewById(R.id.navProfile);

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

        // FIX: tap once to pick a range; tap again while a range is active to clear it
        btnDateRange.setOnClickListener(v -> {
            if (filterDateStart != null || filterDateEnd != null) {
                clearDateFilter();
            } else {
                showDateRangePicker();
            }
        });

        btnPriceRange.setOnClickListener(v -> {
            if (filterMinPrice >= 0 || filterMaxPrice >= 0) {
                // Clear price filter
                filterMinPrice = -1;
                filterMaxPrice = -1;
                tvPriceValue.setText("Any Price");
                filterAndDisplay(etSearch.getText().toString().trim());
            } else {
                showPriceFilterDialog();
            }
        });

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
                startActivity(new Intent(this, MyPaymentsActivity.class))
        );
        navProfile.setOnClickListener(v ->
                startActivity(new Intent(this, StudentProfileActivity.class))
        );
    }

    /**
     * Loads all active events then removes any the current user has already RSVPd to.
     */
    private void loadAllEvents() {
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            fetchEventsExcluding(new java.util.HashSet<>());
            return;
        }

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    java.util.Set<String> rsvpdIds = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot rsvp : rsvpQuery.getDocuments()) {
                        String eid = rsvp.getString("eventId");
                        if (eid != null) rsvpdIds.add(eid);
                    }
                    fetchEventsExcluding(rsvpdIds);
                })
                .addOnFailureListener(e -> fetchEventsExcluding(new java.util.HashSet<>()));
    }

    private void fetchEventsExcluding(java.util.Set<String> rsvpdIds) {
        // Only fetch events from now onwards — same as StudentHomeActivity
        Timestamp now = new Timestamp(new java.util.Date());

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", now)
                .get()
                .addOnSuccessListener(query -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        if (!rsvpdIds.contains(doc.getId())) {
                            allEvents.add(doc);
                        }
                    }
                    filterAndDisplay("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load events", Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Resets the date filter back to "Any Date" and refreshes results.
     */
    private void clearDateFilter() {
        filterDateStart = null;
        filterDateEnd   = null;
        tvDateValue.setText("Any Date");
        filterAndDisplay(etSearch.getText().toString().trim());
    }

    private void filterAndDisplay(String query) {
        searchResultsList.removeAllViews();

        List<QueryDocumentSnapshot> filtered = new ArrayList<>();

        for (QueryDocumentSnapshot doc : allEvents) {
            String title    = doc.getString("title");
            String venue    = doc.getString("venue");
            Timestamp date  = doc.getTimestamp("date");

            // Text search — title, venue, description
            if (!query.isEmpty()) {
                String desc = doc.getString("description");
                boolean matchesTitle = title != null &&
                        title.toLowerCase().contains(query.toLowerCase());
                boolean matchesVenue = venue != null &&
                        venue.toLowerCase().contains(query.toLowerCase());
                boolean matchesDesc  = desc != null &&
                        desc.toLowerCase().contains(query.toLowerCase());
                if (!matchesTitle && !matchesVenue && !matchesDesc) continue;
            }

            // Category filter
            if (!selectedCategory.equals("All")) {
                String category = doc.getString("category");
                if (category == null || !category.equalsIgnoreCase(selectedCategory)) continue;
            }

            // Date range filter
            if (filterDateStart != null && date != null) {
                if (date.compareTo(filterDateStart) < 0) continue;
            }
            if (filterDateEnd != null && date != null) {
                if (date.compareTo(filterDateEnd) > 0) continue;
            }

            // Price filter
            if (filterMinPrice >= 0 || filterMaxPrice >= 0) {
                double price = 0.0;
                if (doc.getDouble("price") != null) price = doc.getDouble("price");
                else if (doc.getDouble("ticketPrice") != null) price = doc.getDouble("ticketPrice");
                if (filterMinPrice >= 0 && price < filterMinPrice) continue;
                if (filterMaxPrice >= 0 && price > filterMaxPrice) continue;
            }

            filtered.add(doc);
        }

        // Sort
        filtered.sort((a, b) -> {
            Timestamp dateA = a.getTimestamp("date");
            Timestamp dateB = b.getTimestamp("date");
            if (dateA == null || dateB == null) return 0;
            return sortOrder.equals("Latest")
                    ? dateB.compareTo(dateA)
                    : dateA.compareTo(dateB);
        });

        tvResultCount.setText(filtered.size() + " events found");

        if (filtered.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No events found");
            searchResultsList.addView(empty);
            return;
        }

        // FIX: populate each card exactly like StudentHomeActivity does
        for (QueryDocumentSnapshot doc : filtered) {
            String title       = doc.getString("title");
            String venue       = doc.getString("venue");
            String description = doc.getString("description");
            String orgName     = doc.getString("submittedByName");
            String orgEmail    = doc.getString("submittedByEmail");
            Timestamp date     = doc.getTimestamp("date");
            int capacity       = doc.getLong("capacity") != null
                    ? doc.getLong("capacity").intValue() : 0;
            int registered     = doc.getLong("registeredCount") != null
                    ? doc.getLong("registeredCount").intValue() : 0;
            long dateMillis    = date != null ? date.toDate().getTime() : 0;
            double price       = 0.0;
            if (doc.getDouble("price") != null) price = doc.getDouble("price");
            else if (doc.getDouble("ticketPrice") != null) price = doc.getDouble("ticketPrice");
            final double priceFinal = price;

            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_search_result, searchResultsList, false);

            // Title
            TextView tvTitle = itemView.findViewById(R.id.tvEventTitle);
            if (tvTitle != null && title != null)
                tvTitle.setText(title);

            // Venue
            TextView tvVenue = itemView.findViewById(R.id.tvEventVenue);
            if (tvVenue != null && venue != null)
                tvVenue.setText("📍 " + venue);

            // Date: day + month
            if (date != null) {
                Date d = date.toDate();

                TextView tvDay = itemView.findViewById(R.id.tvDateDay);
                if (tvDay != null)
                    tvDay.setText(new SimpleDateFormat("dd", Locale.getDefault()).format(d));

                TextView tvMonth = itemView.findViewById(R.id.tvDateMonth);
                if (tvMonth != null)
                    tvMonth.setText(
                            new SimpleDateFormat("MMM", Locale.getDefault())
                                    .format(d).toUpperCase());
            }

            // Availability badge
            TextView tvAvailability = itemView.findViewById(R.id.tvAvailability);
            if (tvAvailability != null) {
                if (capacity > 0 && registered >= capacity) {
                    tvAvailability.setText("Full");
                } else {
                    tvAvailability.setText("Available");
                }
            }

            // Price
            TextView tvPriceResult = itemView.findViewById(R.id.tvPrice);
            if (tvPriceResult != null) {
                tvPriceResult.setText(priceFinal > 0 ? "Rs. " + (int) priceFinal : "FREE");
            }

            // Click → EventDetailActivity
            String eventIdFinal   = doc.getId();
            String titleFinal     = title;
            String venueFinal     = venue;
            String descFinal      = description;
            String orgNameFinal   = orgName;
            String orgEmailFinal  = orgEmail;
            int    capFinal       = capacity;
            int    regFinal       = registered;
            long   dateMillisFinal = dateMillis;

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventDetailActivity.class);
                intent.putExtra("eventId",             eventIdFinal);
                intent.putExtra("eventTitle",           titleFinal);
                intent.putExtra("eventVenue",           venueFinal);
                intent.putExtra("eventDescription",     descFinal);
                intent.putExtra("eventCapacity",        capFinal);
                intent.putExtra("eventRegistered",      regFinal);
                intent.putExtra("eventDateMillis",      dateMillisFinal);
                intent.putExtra("eventOrganizerName",   orgNameFinal);
                intent.putExtra("eventOrganizerEmail",  orgEmailFinal);
                intent.putExtra("eventTicketPrice",     priceFinal);
                startActivity(intent);
            });

            searchResultsList.addView(itemView);
        }
    }

    /**
     * Two-step DatePickerDialog: pick start date, then end date.
     * After both are chosen the filter is applied and tvDateValue is updated.
     * FIX: end date dialog now actually sets filterDateEnd and refreshes results.
     */
    private void showDateRangePicker() {
        Calendar cal = Calendar.getInstance();
        showPickerDialog("Select Start Date", cal, (startYear, startMonth, startDay) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(startYear, startMonth, startDay, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            filterDateStart = new Timestamp(startCal.getTime());

            showPickerDialog("Select End Date", cal, (endYear, endMonth, endDay) -> {
                Calendar endCal = Calendar.getInstance();
                endCal.set(endYear, endMonth, endDay, 23, 59, 59);
                endCal.set(Calendar.MILLISECOND, 999);
                filterDateEnd = new Timestamp(endCal.getTime());

                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                tvDateValue.setText(
                        sdf.format(startCal.getTime()) + " – " + sdf.format(endCal.getTime())
                );
                filterAndDisplay(etSearch.getText().toString().trim());
            });
        });
    }

    /**
     * Shows a custom date picker dialog with OK, Cancel, and Clear Date Range buttons.
     * OK/Cancel are on the same line; Clear Date Range is centered below them.
     */
    private void showPickerDialog(String title, Calendar initial, OnDatePickedListener listener) {
        // Build the layout programmatically
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(0, 0, 0, 0);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(48, 40, 48, 8);
        tvTitle.setTextColor(0xFF1A1A2E);
        root.addView(tvTitle);

        // DatePicker (spinner mode to keep it compact)
        android.widget.DatePicker datePicker = new android.widget.DatePicker(this);
        datePicker.setCalendarViewShown(false); // spinner style only
        datePicker.init(
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH),
                null
        );
        root.addView(datePicker);

        // OK + Cancel row
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);
        btnRow.setPadding(24, 8, 24, 0);

        android.widget.LinearLayout.LayoutParams halfParams =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        halfParams.setMargins(8, 0, 8, 0);

        Button btnOk = new Button(this);
        btnOk.setText("OK");
        btnOk.setLayoutParams(halfParams);

        Button btnCancel = new Button(this);
        btnCancel.setText("Cancel");
        btnCancel.setLayoutParams(halfParams);

        btnRow.addView(btnCancel);
        btnRow.addView(btnOk);
        root.addView(btnRow);

        // Clear Date Range row — centered below OK/Cancel
        android.widget.LinearLayout clearRow = new android.widget.LinearLayout(this);
        clearRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        clearRow.setGravity(android.view.Gravity.CENTER);
        clearRow.setPadding(24, 4, 24, 24);

        Button btnClear = new Button(this);
        btnClear.setText("Clear Date Range");
        android.widget.LinearLayout.LayoutParams clearParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
        btnClear.setLayoutParams(clearParams);

        clearRow.addView(btnClear);
        root.addView(clearRow);

        // Build and show dialog
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(root)
                .setCancelable(true)
                .create();

        btnOk.setOnClickListener(v -> {
            dialog.dismiss();
            listener.onDatePicked(
                    datePicker.getYear(),
                    datePicker.getMonth(),
                    datePicker.getDayOfMonth()
            );
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnClear.setOnClickListener(v -> {
            dialog.dismiss();
            clearDateFilter(); // resets both dates and goes back to showing all events
        });

        dialog.show();
    }

    /** Shows a simple price range filter dialog. */
    private void showPriceFilterDialog() {
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setPadding(48, 32, 48, 16);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Filter by Price (Rs.)");
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        root.addView(tvTitle);

        final android.widget.EditText etMin = new android.widget.EditText(this);
        etMin.setHint("Min price (0 for free)");
        etMin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(etMin);

        final android.widget.EditText etMax = new android.widget.EditText(this);
        etMax.setHint("Max price (leave blank for no limit)");
        etMax.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        root.addView(etMax);

        // Quick preset buttons
        android.widget.LinearLayout presets = new android.widget.LinearLayout(this);
        presets.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        presets.setPadding(0, 16, 0, 8);
        String[] labels = {"Free only", "< Rs. 500", "< Rs. 1000"};
        int[] mins      = {0,   0,   0};
        int[] maxs      = {0, 499, 999};
        for (int i = 0; i < labels.length; i++) {
            Button btn = new Button(this);
            btn.setText(labels[i]);
            btn.setTextSize(11f);
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(0,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(lp);
            final int min = mins[i], max = maxs[i];
            btn.setOnClickListener(v -> {
                etMin.setText(String.valueOf(min));
                etMax.setText(String.valueOf(max));
            });
            presets.addView(btn);
        }
        root.addView(presets);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(root)
                .setPositiveButton("Apply", (d, w) -> {
                    try {
                        String minStr = etMin.getText().toString().trim();
                        String maxStr = etMax.getText().toString().trim();
                        filterMinPrice = minStr.isEmpty() ? -1 : Double.parseDouble(minStr);
                        filterMaxPrice = maxStr.isEmpty() ? -1 : Double.parseDouble(maxStr);
                        if (filterMinPrice >= 0 && filterMaxPrice >= 0) {
                            tvPriceValue.setText("Rs." + (int)filterMinPrice + "–" + (int)filterMaxPrice);
                        } else if (filterMinPrice == 0 && filterMaxPrice == 0) {
                            tvPriceValue.setText("Free only");
                        } else if (filterMaxPrice >= 0) {
                            tvPriceValue.setText("< Rs." + (int)filterMaxPrice);
                        } else {
                            tvPriceValue.setText("≥ Rs." + (int)filterMinPrice);
                        }
                        filterAndDisplay(etSearch.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    /** Callback interface for the custom date picker. */
    interface OnDatePickedListener {
        void onDatePicked(int year, int month, int day);
    }

    private void updateChipSelection(TextView selected) {
        TextView[] chips = {chipAll, chipSports, chipAcademic, chipCultural};
        for (TextView chip : chips) {
            chip.setBackgroundResource(chip == selected
                    ? R.drawable.chip_active
                    : R.drawable.chip_inactive);
        }
    }
}