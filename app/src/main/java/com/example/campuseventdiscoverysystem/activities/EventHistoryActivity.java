package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        historyList.add(new HistoryItem("Tech Fest 2025", "17", "MAR", "Attended"));
        historyList.add(new HistoryItem("Tech Fest 2025", "27", "FEB", "Did Not Attend"));
        historyList.add(new HistoryItem("SPADES Gala 2025", "23", "FEB", "Recap"));
        historyList.add(new HistoryItem("Tech Fest 2025", "19", "JAN", "Attended"));
        historyList.add(new HistoryItem("Tech Fest 2025", "02", "JAN", "Attended"));
        historyList.add(new HistoryItem("Tech Fest 2025", "01", "JAN", "Attended"));
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