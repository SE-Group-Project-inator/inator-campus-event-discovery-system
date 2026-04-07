package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.HistoryAdapter;
import com.example.campuseventdiscoverysystem.models.HistoryItem;

import java.util.ArrayList;
import java.util.List;

public class EventHistoryActivity extends AppCompatActivity {

    private RecyclerView rvEventHistory;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;
    private TextView tvTotalAttended, tvThisMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_history);

        // 1. Initialize Views
        rvEventHistory = findViewById(R.id.rvEventHistory);
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvTotalAttended = findViewById(R.id.tvTotalAttended);
        tvThisMonth = findViewById(R.id.tvThisMonth);

        // 2. Setup Back Button
        btnBack.setOnClickListener(v -> finish());

        // 3. Setup RecyclerView
        rvEventHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();

        // 4. Load Dummy Data (Matching your Figma design exactly!)
        loadHistoryData();

        // 5. Attach Adapter
        adapter = new HistoryAdapter(historyList);
        rvEventHistory.setAdapter(adapter);

        // 6. Update Header Stats
        updateStats();
    }

    private void loadHistoryData() {
        // 1. Get the real logged-in student ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String studentId = (currentUser != null) ? currentUser.getUid() : "unknown_student";

        // 2. Query Firestore for this student's RSVPs
        FirebaseFirestore.getInstance()
                .collectionGroup("rsvps")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear(); // Clear out the list before adding real data

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        // Grab the data we saved in RsvpActivity
                        String eventName = doc.getString("eventName");
                        String status = doc.getString("status");

                        // Fallbacks just in case data is missing
                        if (eventName == null) eventName = "Unknown Event";
                        if (status == null) status = "Registered";

                        // Grab the timestamp and format it for your UI (Day and Month)
                        String day = "--";
                        String month = "---";
                        com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");

                        if (ts != null) {
                            Date date = ts.toDate();
                            day = new SimpleDateFormat("dd", Locale.getDefault()).format(date);
                            month = new SimpleDateFormat("MMM", Locale.getDefault()).format(date).toUpperCase();
                        }

                        // Add the real Firebase data to your visual list!
                        historyList.add(new HistoryItem(eventName, day, month, status));
                    }

                    // Tell the UI to refresh with the newly downloaded data
                    adapter.notifyDataSetChanged();
                    updateStats();
                })
                .addOnFailureListener(e -> {
                    // If it fails to download, show an error (you can optionally add a Toast here)
                    e.printStackTrace();
                });
    }
    private void updateStats() {
        int attendedCount = 0;
        int marchCount = 0;

        for (HistoryItem item : historyList) {
            if (item.getStatus().equalsIgnoreCase("Attended")) {
                attendedCount++;
            }
            // Count March events for the "This Month" stat
            if (item.getMonth().equalsIgnoreCase("MAR")) {
                marchCount++;
            }
        }

        tvTotalAttended.setText(String.valueOf(attendedCount));
        tvThisMonth.setText(String.valueOf(marchCount));
    }
}