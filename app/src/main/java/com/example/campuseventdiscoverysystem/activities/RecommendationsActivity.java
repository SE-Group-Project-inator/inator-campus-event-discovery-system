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
import com.example.campuseventdiscoverysystem.adapters.RecommendationAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.recommendations.RecommendationEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * RecommendationsActivity (US-22: Personalized Recommendations)
 * ============================================================
 *
 * PURPOSE:
 * Displays AI-driven personalized event recommendations for students.
 *
 * ENTRY POINT:
 * - Accessed from "Picked for You" section in StudentHomeActivity
 *
 * FEATURES:
 * - Uses RecommendationEngine (core logic layer)
 * - Shows reason for recommendation (e.g. interests, follows, trending)
 * - Handles empty state UI
 * - Real-time Firestore-based recommendation generation
 *
 * USER ROLE:
 * Student
 */
public class RecommendationsActivity extends AppCompatActivity {

    /**
     * Maximum number of recommendations displayed on screen.
     */
    private static final int RECS_LIMIT = 10;

    // RecyclerView for recommendation cards
    private RecyclerView rv;

    // Text showing why recommendations were generated
    private TextView tvReason;

    // Empty state message (when no recommendations exist)
    private TextView tvEmpty;

    // Adapter binding Event data to RecyclerView
    private RecommendationAdapter adapter;

    // Local dataset for recommendations
    private final List<Event> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        // ---------------- UI BINDING ----------------
        rv = findViewById(R.id.rvRecommendations);
        tvReason = findViewById(R.id.tvReason);
        tvEmpty = findViewById(R.id.tvEmpty);

        // Back button closes activity
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // RecyclerView setup
        adapter = new RecommendationAdapter(events);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Load recommendations from engine
        loadRecommendations();
    }

    /**
     * ============================================================
     * LOAD RECOMMENDATIONS
     * ============================================================
     *
     * Flow:
     * 1. Check if user is logged in
     * 2. Initialize RecommendationEngine
     * 3. Fetch personalized recommendations
     * 4. Update UI accordingly
     */
    private void loadRecommendations() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // If user not logged in, show message and stop execution
        if (user == null) {
            tvReason.setText("Please sign in to see recommendations.");
            return;
        }

        // Initialize recommendation engine with Firestore + limit
        RecommendationEngine engine = new RecommendationEngine(
                FirebaseFirestore.getInstance(), RECS_LIMIT);

        // Request recommendations asynchronously
        engine.getRecommendations(user.getUid(),
                new RecommendationEngine.Callback() {

                    /**
                     * Called when recommendations are successfully generated.
                     */
                    @Override
                    public void onRecommendations(List<Event> recs, String reason) {

                        // Show explanation of recommendation source
                        tvReason.setText(reason);

                        // Update dataset
                        events.clear();
                        events.addAll(recs);

                        // Refresh RecyclerView
                        adapter.notifyDataSetChanged();

                        // Handle empty state UI
                        boolean empty = recs.isEmpty();

                        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }

                    /**
                     * Called when recommendation engine fails.
                     */
                    @Override
                    public void onError(Exception e) {

                        // Show fallback message
                        tvReason.setText("Couldn't load recommendations.");

                        // Debug error message (toast)
                        Toast.makeText(RecommendationsActivity.this,
                                e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}