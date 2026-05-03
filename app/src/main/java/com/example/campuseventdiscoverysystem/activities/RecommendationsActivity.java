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
 * US-22 Personalised Recommendations — full-list screen.
 *
 * Reachable from the "Picked for You" entry on StudentHomeActivity.
 */
public class RecommendationsActivity extends AppCompatActivity {

    private static final int RECS_LIMIT = 10;

    private RecyclerView rv;
    private TextView tvReason, tvEmpty;
    private RecommendationAdapter adapter;
    private final List<Event> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        rv = findViewById(R.id.rvRecommendations);
        tvReason = findViewById(R.id.tvReason);
        tvEmpty = findViewById(R.id.tvEmpty);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        adapter = new RecommendationAdapter(events);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadRecommendations();
    }

    private void loadRecommendations() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvReason.setText("Please sign in to see recommendations.");
            return;
        }

        RecommendationEngine engine = new RecommendationEngine(
                FirebaseFirestore.getInstance(), RECS_LIMIT);

        engine.getRecommendations(user.getUid(), new RecommendationEngine.Callback() {
            @Override
            public void onRecommendations(List<Event> recs, String reason) {
                tvReason.setText(reason);
                events.clear();
                events.addAll(recs);
                adapter.notifyDataSetChanged();

                boolean empty = recs.isEmpty();
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                tvReason.setText("Couldn't load recommendations.");
                Toast.makeText(RecommendationsActivity.this,
                        e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
