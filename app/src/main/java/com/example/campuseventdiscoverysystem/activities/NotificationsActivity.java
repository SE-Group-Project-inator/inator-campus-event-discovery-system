package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity responsible for displaying a list of notifications for a specific student.
 * It listens for real-time updates from a Firebase Firestore sub-collection and
 * updates the UI dynamically as new notifications arrive.
 */
public class NotificationsActivity extends AppCompatActivity {

    /** The RecyclerView used to display the notification items. */
    private RecyclerView rvNotifications;

    /** The adapter used to bind {@code NotificationItem} data to the RecyclerView. */
    private NotificationAdapter adapter;

    /** The data source list containing notification items retrieved from the database. */
    private List<NotificationItem> notificationList;

    /** Instance of Firebase Firestore for database operations. */
    private FirebaseFirestore db;

    /** The unique identifier for the student whose notifications are being retrieved. */
    private String studentId = "student_123"; // Our hardcoded test user

    /**
     * Initializes the activity, sets up the RecyclerView and adapter,
     * and triggers the database listener.
     * @param savedInstanceState If the activity is being re-initialized, this
     * contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        rvNotifications = findViewById(R.id.rvNotifications);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // 2. Setup Back Button
        btnBack.setOnClickListener(v -> finish());

        // 3. Setup RecyclerView (Empty by default now!)
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        // 4. Fetch Real Data from Firestore
        listenForNotifications();
    }

    /**
     * Establishes a real-time listener on the Firestore "notifications" sub-collection
     * for the current student. When data changes in the database, the local list is
     * cleared and repopulated, and the adapter is notified to refresh the UI.
     */
    private void listenForNotifications() {
        // We look inside: users -> student_123 -> notifications
        db.collection("users").document(studentId).collection("notifications")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        notificationList.clear(); // Clear old data

                        // Loop through everything in the database
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            if (item != null) {
                                notificationList.add(item);
                            }
                        }

                        // Tell the screen to redraw the list
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}