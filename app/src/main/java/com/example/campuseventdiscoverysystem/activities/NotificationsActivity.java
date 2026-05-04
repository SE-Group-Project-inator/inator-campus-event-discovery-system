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
 * - Supports Student, Admin, and Manager UI themes dynamically
 * - Live updates using snapshot listener
 * - Auto-mark notifications as "read"
 * - Handles empty state UI
 *
 * FIRESTORE STRUCTURE:
 * users/{userId}/notifications/{notificationId}
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
        View headerLayout = findViewById(R.id.headerLayout);
        View rootLayout = findViewById(R.id.notificationsRoot);

        if ("admin".equals(role)) {
            // Apply admin color scheme
            if (headerLayout != null) headerLayout.setBackgroundColor(getColor(R.color.admin_primary));
            if (rootLayout != null) rootLayout.setBackgroundColor(getColor(R.color.admin_bg));
        } else if ("manager".equals(role)) {
            // Apply manager color scheme
            if (headerLayout != null) headerLayout.setBackgroundColor(getColor(R.color.btn_eventmgr));
            if (rootLayout != null) rootLayout.setBackgroundColor(getColor(R.color.bg_event));
        }

        // UI bindings
        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty         = findViewById(R.id.tvEmptyNotifications); // Fallback

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
     */
    private void listenForNotifications() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // If user not logged in, stop execution
        if (user == null) {
            Toast.makeText(this, "Please log in to see notifications.", Toast.LENGTH_SHORT).show();
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
                        if (notificationList.isEmpty()) {
                            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                        }
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

                        // Handle empty state UI (Preferring the premium layout container)
                        boolean isEmpty = notificationList.isEmpty();
                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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