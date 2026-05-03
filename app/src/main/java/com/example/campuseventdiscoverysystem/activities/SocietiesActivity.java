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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shows all societies. Each card has a Follow/Unfollow button.
 * Tapping the card body opens SocietyDetailActivity.
 * followOnly=true mode is used by MySocietiesActivity to show only followed ones.
 */
public class SocietiesActivity extends AppCompatActivity {

    // Hardcoded society list — add more here later
    public static final List<Society> ALL_SOCIETIES = Arrays.asList(
            new Society("spades",   "SPADES",                        "SP", "Student Programming And Dev Society"),
            new Society("lrs",      "LUMS Religious Society",        "LR", "Spiritual growth & interfaith dialogue"),
            new Society("lwic",     "LUMS Women In Computing",       "LW", "Empowering women in tech at LUMS")
    );

    private LinearLayout societiesList;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private Set<String> followedIds = new HashSet<>();
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

        loadFollowsThenDisplay();
    }

    private void loadFollowsThenDisplay() {
        if (currentUser == null) {
            displaySocieties();
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
                    displaySocieties();
                })
                .addOnFailureListener(e -> displaySocieties());
    }

    private void displaySocieties() {
        societiesList.removeAllViews();

        for (Society society : ALL_SOCIETIES) {
            boolean isFollowed = followedIds.contains(society.id);

            // In MySocietiesActivity mode, skip unfollowed ones
            if (followOnly && !isFollowed) continue;

            View card = LayoutInflater.from(this)
                    .inflate(R.layout.item_society, societiesList, false);

            ((TextView) card.findViewById(R.id.tvSocietyInitials)).setText(society.initials);
            ((TextView) card.findViewById(R.id.tvSocietyName)).setText(society.name);
            ((TextView) card.findViewById(R.id.tvSocietyTagline)).setText(society.tagline);

            MaterialButton btnFollow = card.findViewById(R.id.btnFollow);
            updateFollowButton(btnFollow, isFollowed);

            btnFollow.setOnClickListener(v -> {
                if (currentUser == null) {
                    Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean currentlyFollowed = followedIds.contains(society.id);
                toggleFollow(society, btnFollow, currentlyFollowed);
            });

            // Tap the card body → SocietyDetailActivity
            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, SocietyDetailActivity.class);
                intent.putExtra("societyId",   society.id);
                intent.putExtra("societyName", society.name);
                intent.putExtra("societyTagline", society.tagline);
                intent.putExtra("societyInitials", society.initials);
                startActivity(intent);
            });

            societiesList.addView(card);
        }

        // Show empty state in MySocieties mode if nothing followed
        if (followOnly && societiesList.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText("You haven't followed any societies yet.\nHead to Societies to find some!");
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
                        // In MySocieties mode, remove the card from view
                        if (followOnly) displaySocieties();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to unfollow", Toast.LENGTH_SHORT).show());
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("userId",    currentUser.getUid());
            data.put("societyId", society.id);
            db.collection("societyFollows").document(docId)
                    .set(data)
                    .addOnSuccessListener(unused -> {
                        followedIds.add(society.id);
                        updateFollowButton(btn, true);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to follow", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateFollowButton(MaterialButton btn, boolean followed) {
        if (followed) {
            btn.setText("Following");
            btn.setBackgroundTintList(
                    getColorStateList(R.color.green_accept));
        } else {
            btn.setText("Follow");
            btn.setBackgroundTintList(
                    getColorStateList(R.color.btn_student));
        }
    }

    // Simple data class for a society
    public static class Society {
        public final String id, name, initials, tagline;
        public Society(String id, String name, String initials, String tagline) {
            this.id = id; this.name = name;
            this.initials = initials; this.tagline = tagline;
        }
    }
}