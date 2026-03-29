package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class PrivacySettingsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String uid;

    private Switch switchShowProfile;
    private Switch switchShareAttendance;
    private Switch switchEmailNotifs;
    private Switch switchRecommendations;
    private Switch switchLocation;

    private boolean isLoading = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_settings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        // Bind views
        switchShowProfile = findViewById(R.id.switchShowProfile);
        switchShareAttendance = findViewById(R.id.switchShareAttendance);
        switchEmailNotifs = findViewById(R.id.switchEmailNotifs);
        switchRecommendations = findViewById(R.id.switchRecommendations);
        switchLocation = findViewById(R.id.switchLocation);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPrivacySettings();
        setupToggleListeners();
    }

    /**
     * Loads existing privacy settings from Firestore
     * and sets toggles accordingly.
     */
    private void loadPrivacySettings() {
        if (uid == null) return;

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Default to true if field doesn't exist yet
                        switchShowProfile.setChecked(
                                getBool(doc.getBoolean("privacyShowProfile"), true));
                        switchShareAttendance.setChecked(
                                getBool(doc.getBoolean("privacyShareAttendance"), false));
                        switchEmailNotifs.setChecked(
                                getBool(doc.getBoolean("privacyEmailNotifs"), true));
                        switchRecommendations.setChecked(
                                getBool(doc.getBoolean("privacyRecommendations"), true));
                        switchLocation.setChecked(
                                getBool(doc.getBoolean("privacyLocation"), false));
                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Toast.makeText(this,
                            "Failed to load settings",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Sets up listeners on all toggles.
     * Each toggle saves immediately to Firestore when changed.
     */
    private void setupToggleListeners() {
        CompoundButton.OnCheckedChangeListener listener =
                (buttonView, isChecked) -> {
                    if (isLoading) return; // don't save while loading
                    savePrivacySettings();
                };

        switchShowProfile.setOnCheckedChangeListener(listener);
        switchShareAttendance.setOnCheckedChangeListener(listener);
        switchEmailNotifs.setOnCheckedChangeListener(listener);
        switchRecommendations.setOnCheckedChangeListener(listener);
        switchLocation.setOnCheckedChangeListener(listener);
    }

    /**
     * Saves all toggle states to Firestore instantly.
     * Uses update() so other user fields are not overwritten.
     */
    private void savePrivacySettings() {
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("privacyShowProfile", switchShowProfile.isChecked());
        updates.put("privacyShareAttendance", switchShareAttendance.isChecked());
        updates.put("privacyEmailNotifs", switchEmailNotifs.isChecked());
        updates.put("privacyRecommendations", switchRecommendations.isChecked());
        updates.put("privacyLocation", switchLocation.isChecked());

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "✅ Saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error saving: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private boolean getBool(Boolean value, boolean defaultVal) {
        return value != null ? value : defaultVal;
    }
}