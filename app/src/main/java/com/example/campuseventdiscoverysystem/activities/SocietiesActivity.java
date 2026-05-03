package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shows all societies loaded from the Firestore "societies" collection.
 * Each document has: name, abbr (initials), description.
 * Each card has a Follow/Unfollow button. Tapping opens SocietyDetailActivity.
 * followOnly=true mode shows only followed societies (used by MySocietiesActivity).
 */
public class SocietiesActivity extends AppCompatActivity {

    // Simple data class — kept public so SocietyDetailActivity can reference the type if needed
    public static class Society {
        public final String id, name, initials, description;
        public Society(String id, String name, String initials, String description) {
            this.id = id;
            this.name = name;
            this.initials = initials;
            this.description = description;
        }
    }

    private LinearLayout societiesList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private final Set<String> followedIds = new HashSet<>();
    private final List<Society> loadedSocieties = new ArrayList<>();
    protected boolean followOnly = false; // overridden by MySocietiesActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_societies);

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        societiesList = findViewById(R.id.societiesList);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        loadFollowsThenFetchSocieties();
    }

    /** Step 1: load which societies the user already follows, then fetch all societies. */
    private void loadFollowsThenFetchSocieties() {
        if (currentUser == null) {
            fetchSocietiesFromFirestore();
            return;
        }
        db.collection("societyFollows")
                .whereEqualTo("userId", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    followedIds.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        String sid = doc.getString("societyId");
                        if (sid != null) followedIds.add(sid);
                    }
                    fetchSocietiesFromFirestore();
                })
                .addOnFailureListener(e -> fetchSocietiesFromFirestore());
    }

    /** Step 2: read the 'societies' collection and build the Society list. */
    private void fetchSocietiesFromFirestore() {
        db.collection("societies")
                .get()
                .addOnSuccessListener(query -> {
                    loadedSocieties.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String id          = doc.getId();
                        String name        = doc.getString("name");
                        String abbr        = doc.getString("abbr");        // initials e.g. "LWIC"
                        String description = doc.getString("description");

                        if (name == null) name = id;
                        if (abbr == null) abbr = name.length() >= 2
                                ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
                        if (description == null) description = "";

                        loadedSocieties.add(new Society(id, name, abbr, description));
                    }
                    displaySocieties();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load societies", Toast.LENGTH_SHORT).show();
                    displaySocieties(); // will show empty state
                });
    }

    private void displaySocieties() {
        societiesList.removeAllViews();

        for (Society society : loadedSocieties) {
            boolean isFollowed = followedIds.contains(society.id);
            if (followOnly && !isFollowed) continue;

            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_society, societiesList, false);

            ((TextView) card.findViewById(R.id.tvSocietyInitials)).setText(society.initials);
            ((TextView) card.findViewById(R.id.tvSocietyName)).setText(society.name);
            ((TextView) card.findViewById(R.id.tvSocietyTagline)).setText(society.description);

            MaterialButton btnFollow = card.findViewById(R.id.btnFollow);
            updateFollowButton(btnFollow, isFollowed);

            btnFollow.setOnClickListener(v -> {
                if (currentUser == null) {
                    Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
                    return;
                }
                toggleFollow(society, btnFollow, followedIds.contains(society.id));
            });

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, SocietyDetailActivity.class);
                intent.putExtra("societyId",          society.id);
                intent.putExtra("societyName",        society.name);
                intent.putExtra("societyDescription", society.description);
                intent.putExtra("societyInitials",    society.initials);
                startActivity(intent);
            });

            societiesList.addView(card);
        }

        if (societiesList.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(followOnly
                    ? "You haven't followed any societies yet.\nHead to Societies to find some!"
                    : "No societies found.");
            empty.setTextColor(getResources().getColor(R.color.text_grey));
            empty.setTextSize(14f);
            empty.setPadding(0, 32, 0, 0);
            empty.setGravity(android.view.Gravity.CENTER);
            societiesList.addView(empty);
        }
    }

    private void toggleFollow(Society society, MaterialButton btn, boolean currentlyFollowed) {
        String docId = currentUser.getUid() + "_" + society.id;
        if (currentlyFollowed) {
            db.collection("societyFollows").document(docId)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        followedIds.remove(society.id);
                        updateFollowButton(btn, false);
                        if (followOnly) displaySocieties();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show());
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("userId",    currentUser.getUid());
            data.put("societyId", society.id);
            data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            db.collection("societyFollows").document(docId)
                    .set(data)
                    .addOnSuccessListener(unused -> {
                        followedIds.add(society.id);
                        updateFollowButton(btn, true);
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("SocietiesActivity", "Follow failed: " + e.getMessage(), e);
                        Toast.makeText(this, "Failed to follow: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void updateFollowButton(MaterialButton btn, boolean followed) {
        if (followed) {
            btn.setText("Following");
            btn.setBackgroundTintList(getColorStateList(R.color.green_accept));
        } else {
            btn.setText("Follow");
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#0D9488")));
        }
    }
}
