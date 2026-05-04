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

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        // Apply admin theme if launched from admin dashboard
        String role = getIntent().getStringExtra("role");
        if ("admin".equals(role)) {
            View headerLayout = findViewById(R.id.headerLayout);
            if (headerLayout != null) {
                headerLayout.setBackgroundColor(getColor(R.color.admin_primary));
            }
            View rootLayout = findViewById(R.id.notificationsRoot);
            if (rootLayout != null) {
                rootLayout.setBackgroundColor(getColor(R.color.admin_bg));
            }
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty         = findViewById(R.id.tvEmptyNotifications);
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        listenForNotifications();
    }

    private void listenForNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to see notifications.", Toast.LENGTH_SHORT).show();
            return;
        }

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
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            NotificationItem item = doc.toObject(NotificationItem.class);
                            if (item != null) {
                                item.setId(doc.getId());
                                notificationList.add(item);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // Mark all as read
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Boolean read = doc.getBoolean("read");
                            if (read == null || !read) {
                                doc.getReference().update("read", true);
                            }
                        }

                        if (tvEmpty != null) {
                            tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}