package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.NotificationAdapter;
import com.example.campuseventdiscoverysystem.models.NotificationItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * NotificationsActivity
 * ============================================================
 *
 * PURPOSE:
 * Displays real-time notifications for the logged-in user.
 *
 * FEATURES:
 * - Shows notifications from Firestore subcollection
 * - Supports both student and admin UI themes
 * - Live updates using snapshot listener
 * - Auto-mark notifications as "read"
 * - Handles empty state UI
 *
 * FIRESTORE STRUCTURE:
 * users/{userId}/notifications/{notificationId}
 *
 * NOTIFICATION TYPES:
 * - Event approval/rejection
 * - System updates
 * - Event-related alerts
 *
 * USER ROLE:
 * Student / Admin (UI themed)
 */
public class NotificationsActivity extends AppCompatActivity {

    // RecyclerView for notification list
    private RecyclerView rvNotifications;

    // Adapter for binding notifications
    private NotificationAdapter adapter;

    // Local dataset for UI
    private List<NotificationItem> notificationList;

    // Empty state view
    private TextView tvEmpty;

    // Firestore instance
    private FirebaseFirestore db;

    // Listener reference for cleanup
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // ---------------- ROLE-BASED UI THEME ----------------
        String role = getIntent().getStringExtra("role");

        if ("admin".equals(role)) {

            // Apply admin color scheme to header
            View headerLayout = findViewById(R.id.headerLayout);
            if (headerLayout != null) {
                headerLayout.setBackgroundColor(getColor(R.color.admin_primary));
            }

            // Apply admin background theme
            View rootLayout = findViewById(R.id.notificationsRoot);
            if (rootLayout != null) {
                rootLayout.setBackgroundColor(getColor(R.color.admin_bg));
            }
        }

        // UI bindings
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty         = findViewById(R.id.tvEmptyNotifications);

        // Back button closes activity
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // RecyclerView setup
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);

        rvNotifications.setAdapter(adapter);

        // Start listening to notifications
        listenForNotifications();
    }

    /**
     * ============================================================
     * FIRESTORE LISTENER
     * ============================================================
     *
     * Loads notifications in real time for current user.
     *
     * FLOW:
     * 1. Get logged-in user
     * 2. Listen to notifications subcollection
     * 3. Order by latest timestamp
     * 4. Update UI dynamically
     * 5. Mark unread notifications as read
     */
    private void listenForNotifications() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // If user not logged in, stop execution
        if (user == null) {
            Toast.makeText(this,
                    "Please log in to see notifications.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Attach real-time listener
        listener = db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)

                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        // show empty state instead of error on first load
                        if (notificationList.isEmpty() && tvEmpty != null) tvEmpty.setVisibility(android.view.View.VISIBLE);
                        return;
                    }

                    if (value != null) {

                        // Clear old data before reload
                        notificationList.clear();

                        // Convert Firestore docs into model objects
                        for (DocumentSnapshot doc : value.getDocuments()) {

                            NotificationItem item = doc.toObject(NotificationItem.class);

                            if (item != null) {
                                item.setId(doc.getId());
                                notificationList.add(item);
                            }
                        }

                        // Refresh UI
                        adapter.notifyDataSetChanged();

                        // ---------------- AUTO MARK AS READ ----------------
                        for (DocumentSnapshot doc : value.getDocuments()) {

                            Boolean read = doc.getBoolean("read");

                            if (read == null || !read) {
                                doc.getReference().update("read", true);
                            }
                        }

                        // Handle empty state UI
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(
                                    notificationList.isEmpty()
                                            ? View.VISIBLE
                                            : View.GONE
                            );
                        }
                    }
                });
    }

    /**
     * Cleanup Firestore listener to prevent memory leaks
     * when activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}