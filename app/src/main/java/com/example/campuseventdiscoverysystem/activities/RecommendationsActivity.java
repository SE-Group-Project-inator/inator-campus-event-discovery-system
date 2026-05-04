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

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Step 1: get societies the user follows
        db.collection("societyFollows")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(followSnap -> {
                    List<String> societyIds = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : followSnap.getDocuments()) {
                        String sid = doc.getString("societyId");
                        if (sid != null) societyIds.add(sid);
                    }

                    if (societyIds.isEmpty()) {
                        // Fallback to engine if not following anyone
                        loadViaEngine(user.getUid(), db);
                        return;
                    }

                    // Step 2: fetch active upcoming events from those societies
                    // whereIn supports up to 30 values
                    List<String> ids = societyIds.size() > 10 ? societyIds.subList(0, 10) : societyIds;
                    com.google.firebase.Timestamp now = new com.google.firebase.Timestamp(new java.util.Date());

                    db.collection("events")
                            .whereEqualTo("status", "active")
                            .whereIn("societyId", ids)
                            .get()
                            .addOnSuccessListener(eventsSnap -> {
                                events.clear();
                                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : eventsSnap) {
                                    com.google.firebase.Timestamp date = doc.getTimestamp("date");
                                    if (date == null || date.compareTo(now) < 0) continue;
                                    Event e = doc.toObject(Event.class);
                                    if (e != null) { e.setId(doc.getId()); events.add(e); }
                                }
                                // Sort by date
                                events.sort((a, b) -> {
                                    if (a.getDate() == null || b.getDate() == null) return 0;
                                    return a.getDate().compareTo(b.getDate());
                                });

                                tvReason.setText("From societies you follow");
                                adapter.notifyDataSetChanged();
                                boolean empty = events.isEmpty();
                                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                                rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                            })
                            .addOnFailureListener(e -> loadViaEngine(user.getUid(), db));
                })
                .addOnFailureListener(e -> loadViaEngine(user.getUid(), db));
    }

    private void loadViaEngine(String uid, FirebaseFirestore db) {
        RecommendationEngine engine = new RecommendationEngine(db, RECS_LIMIT);
        engine.getRecommendations(uid, new RecommendationEngine.Callback() {
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
                Toast.makeText(RecommendationsActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}