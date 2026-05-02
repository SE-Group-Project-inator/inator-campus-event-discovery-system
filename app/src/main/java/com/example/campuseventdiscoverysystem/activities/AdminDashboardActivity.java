package com.example.campuseventdiscoverysystem.activities;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.PendingEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * AdminDashboardActivity
 *
 * This activity handles the admin dashboard functionality for the Campus Event Discovery System.
 * It allows admins to:
 * - View pending events
 * - Approve or reject events
 * - View event statistics
 * - Filter events (all, urgent, newest)
 * - Manage admin profile UI
 * - Listen to real-time Firestore updates
 */
public class AdminDashboardActivity extends BaseSessionActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvPendingCount, tvApprovedCount, tvTotalCount;
    private TextView tvWelcome, tvInsight;
    private TextView tvAvatarInitial, tvSheetAdminName, tvSheetEmail, tvSheetAvatar;

    private View adminProfileSheet, profileSheetScrim;
    private boolean sheetVisible = false;

    private final List<Event> pendingList    = new ArrayList<>();
    private final List<Event> allPendingList = new ArrayList<>();
    private PendingEventAdapter adapter;
    private String currentFilter = "all";

    private ListenerRegistration pendingListener;
    private ListenerRegistration statsListenerApproved;

    /**
     * Called when activity is created.
     * Initializes Firebase, UI, and listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupWelcomeMessage();
        setupRecyclerView();
        setupNavigation();
        setupFilters();
        setupProfileSheet();
        listenToStats();
        listenToPendingEvents();
        loadAdminProfile();
    }

    /**
     * Binds all UI components from XML to Java variables.
     */
    private void bindViews() {
        tvPendingCount  = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);
        tvTotalCount    = findViewById(R.id.tvTotalCount);
        tvWelcome       = findViewById(R.id.tvWelcome);
        tvInsight       = findViewById(R.id.tvInsight);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        tvSheetAdminName= findViewById(R.id.tvSheetAdminName);
        tvSheetEmail    = findViewById(R.id.tvSheetEmail);
        tvSheetAvatar   = findViewById(R.id.tvSheetAvatar);
        adminProfileSheet = findViewById(R.id.adminProfileSheet);
        profileSheetScrim = findViewById(R.id.profileSheetScrim);
    }

    /**
     * Sets greeting message based on current time of day.
     */
    private void setupWelcomeMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good morning, Admin 👋";
        else if (hour < 17) greeting = "Good afternoon, Admin 👋";
        else                greeting = "Good evening, Admin 👋";
        tvWelcome.setText(greeting);
    }

    /**
     * Loads admin profile information from Firebase Authentication and Firestore.
     */
    private void loadAdminProfile() {
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            if (email != null) {
                String initial = email.substring(0, 1).toUpperCase();
                tvAvatarInitial.setText(initial);
                tvSheetAvatar.setText(initial);
                tvSheetEmail.setText(email);
            }

            // Fetch admin name from Firestore
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                tvSheetAdminName.setText(name);
                                String initial = name.substring(0, 1).toUpperCase();
                                tvAvatarInitial.setText(initial);
                                tvSheetAvatar.setText(initial);
                            }
                        }
                    });
        }
    }

    /**
     * Sets up RecyclerView and adapter for pending events list.
     */
    private void setupRecyclerView() {
        RecyclerView rv = findViewById(R.id.rvPendingEvents);
        adapter = new PendingEventAdapter(
                pendingList,
                eventId -> confirmAction(eventId, "active",    "Approve this event?",
                        "The event will go live and students can RSVP."),
                eventId -> confirmAction(eventId, "rejected",  "Decline this event?",
                        "The event manager will be notified.")
        );
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
    }

    /**
     * Sets up navigation buttons and quick actions.
     */
    private void setupNavigation() {
        findViewById(R.id.btnAdminAvatar).setOnClickListener(v -> toggleProfileSheet());

        findViewById(R.id.navHome).setOnClickListener(v -> { /* already here */ });
        findViewById(R.id.navEvents).setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));
        findViewById(R.id.navProfile).setOnClickListener(v -> toggleProfileSheet());

        View qaEvents = findViewById(R.id.quickActionEvents);
        if (qaEvents != null) qaEvents.setOnClickListener(v -> {
            Intent i = new Intent(this, EventsListActivity.class);
            i.putExtra("filter", "all");
            startActivity(i);
        });

        View qaPending = findViewById(R.id.quickActionPending);
        if (qaPending != null) qaPending.setOnClickListener(v -> {
            Intent i = new Intent(this, EventsListActivity.class);
            i.putExtra("filter", "pending");
            startActivity(i);
        });

        View qaApproved = findViewById(R.id.quickActionApproved);
        if (qaApproved != null) qaApproved.setOnClickListener(v -> {
            Intent i = new Intent(this, EventsListActivity.class);
            i.putExtra("filter", "approved");
            startActivity(i);
        });

        TextView tvSeeAll = findViewById(R.id.tvSeeAll);
        if (tvSeeAll != null) tvSeeAll.setOnClickListener(v ->
                startActivity(new Intent(this, EventsListActivity.class)));
    }

    /**
     * Sets up filter chips for event filtering.
     */
    private void setupFilters() {
        TextView chipAll    = findViewById(R.id.chipAll);
        TextView chipUrgent = findViewById(R.id.chipUrgent);
        TextView chipNewest = findViewById(R.id.chipNewest);

        if (chipAll != null) chipAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateChipUI("all");
            applyFilter();
        });
        if (chipUrgent != null) chipUrgent.setOnClickListener(v -> {
            currentFilter = "urgent";
            updateChipUI("urgent");
            applyFilter();
        });
        if (chipNewest != null) chipNewest.setOnClickListener(v -> {
            currentFilter = "newest";
            updateChipUI("newest");
            applyFilter();
        });
    }

    /**
     * Updates UI state of filter chips.
     */
    private void updateChipUI(String active) {
        setChipState(findViewById(R.id.chipAll),    "all".equals(active));
        setChipState(findViewById(R.id.chipUrgent), "urgent".equals(active));
        setChipState(findViewById(R.id.chipNewest), "newest".equals(active));
    }

    /**
     * Sets visual state of a chip (active/inactive).
     */
    private void setChipState(TextView chip, boolean isActive) {
        if (chip == null) return;
        chip.setBackgroundResource(isActive ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chip.setTextColor(getColor(isActive ? R.color.white : R.color.admin_text_secondary));
    }

    /**
     * Sets up profile bottom sheet UI.
     */
    private void setupProfileSheet() {
        profileSheetScrim.setOnClickListener(v -> hideProfileSheet());

        View btnSheetLogout = findViewById(R.id.btnSheetLogout);
        if (btnSheetLogout != null) btnSheetLogout.setOnClickListener(v -> {
            hideProfileSheet();
            showLogoutDialog();
        });
    }

    /**
     * Toggles profile sheet visibility.
     */
    private void toggleProfileSheet() {
        if (sheetVisible) hideProfileSheet();
        else              showProfileSheet();
    }

    /**
     * Shows profile bottom sheet with animation.
     */
    private void showProfileSheet() {
        sheetVisible = true;
        profileSheetScrim.setVisibility(View.VISIBLE);
        profileSheetScrim.setAlpha(0f);
        profileSheetScrim.animate().alpha(1f).setDuration(200).start();

        adminProfileSheet.setVisibility(View.VISIBLE);
        adminProfileSheet.post(() -> {
            float startY = adminProfileSheet.getHeight();
            adminProfileSheet.setTranslationY(startY);
            adminProfileSheet.animate()
                    .translationY(0f)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        });
    }

    /**
     * Hides profile bottom sheet with animation.
     */
    private void hideProfileSheet() {
        sheetVisible = false;
        profileSheetScrim.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> profileSheetScrim.setVisibility(View.GONE))
                .start();

        float endY = adminProfileSheet.getHeight();
        adminProfileSheet.animate()
                .translationY(endY)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> adminProfileSheet.setVisibility(View.GONE))
                .start();
    }

    /**
     * Applies selected filter to event list.
     */
    private void applyFilter() {
        List<Event> filtered = new ArrayList<>();
        Date today = new Date();

        if ("urgent".equals(currentFilter)) {
            for (Event e : allPendingList) {
                if (e.getDate() != null) {
                    long diffMs   = e.getDate().toDate().getTime() - today.getTime();
                    long diffDays = diffMs / (1000L * 60 * 60 * 24);
                    if (diffDays >= 0 && diffDays <= 7) filtered.add(e);
                }
            }
        } else if ("newest".equals(currentFilter)) {
            filtered.addAll(allPendingList);
            filtered.sort((a, b) -> {
                if (a.getDate() == null && b.getDate() == null) return 0;
                if (a.getDate() == null) return 1;
                if (b.getDate() == null) return -1;
                return b.getDate().compareTo(a.getDate());
            });
        } else {
            filtered.addAll(allPendingList);
        }

        pendingList.clear();
        pendingList.addAll(filtered);
        adapter.notifyDataSetChanged();

        View emptyState = findViewById(R.id.emptyState);
        RecyclerView rv = findViewById(R.id.rvPendingEvents);

        if (emptyState != null && rv != null) {
            if (pendingList.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
                rv.setVisibility(View.GONE);
            } else {
                emptyState.setVisibility(View.GONE);
                rv.setVisibility(View.VISIBLE);
            }
        }

        if (tvInsight != null) {
            int count = allPendingList.size();
            if (count == 0) tvInsight.setText("All events reviewed ✅");
            else tvInsight.setText(count + " event" + (count > 1 ? "s" : "") + " pending review");
        }
    }

    /**
     * Listens to Firestore stats in real-time.
     */
    private void listenToStats() {
        db.collection("events")
                .whereEqualTo("status", "pending_approval")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        int count = snap.size();
                        animateCounter(tvPendingCount, count);
                    }
                });

        statsListenerApproved = db.collection("events")
                .whereEqualTo("status", "active")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) animateCounter(tvApprovedCount, snap.size());
                });

        db.collection("events")
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) animateCounter(tvTotalCount, snap.size());
                });
    }

    /**
     * Animates counter updates in dashboard.
     */
    private void animateCounter(TextView tv, int target) {
        if (tv == null) return;

        try {
            int current = Integer.parseInt(tv.getText().toString());

            android.animation.ValueAnimator anim =
                    android.animation.ValueAnimator.ofInt(current, target);

            anim.setDuration(600);

            anim.addUpdateListener(a ->
                    tv.setText(String.valueOf((int) a.getAnimatedValue()))
            );

            anim.start();

        } catch (NumberFormatException e) {
            tv.setText(String.valueOf(target));
        }
    }

    /**
     * Listens for pending events in real-time.
     */
    private void listenToPendingEvents() {
        pendingListener = db.collection("events")
                .whereEqualTo("status", "pending_approval")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots == null) return;

                    allPendingList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            allPendingList.add(event);
                        }
                    }
                    applyFilter();
                });
    }

    /**
     * Shows confirmation dialog before approving/rejecting event.
     */
    private void confirmAction(String eventId, String newStatus, String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Confirm", (d, w) -> updateEventStatus(eventId, newStatus))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Updates event status in Firestore.
     */
    private void updateEventStatus(String eventId, String status) {
        db.collection("events").document(eventId)
                .update("status", status)
                .addOnSuccessListener(v -> {
                    String msg = "active".equals(status) ? "✅ Event approved!" : "Event declined";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Handles back press (closes sheet first if open).
     */
    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (sheetVisible) {
            hideProfileSheet();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Cleans up Firestore listeners to avoid memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingListener != null)       pendingListener.remove();
        if (statsListenerApproved != null) statsListenerApproved.remove();
    }
}