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

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;
    private FirebaseFirestore db;
    private String studentId = "student_123"; // Our hardcoded test user

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