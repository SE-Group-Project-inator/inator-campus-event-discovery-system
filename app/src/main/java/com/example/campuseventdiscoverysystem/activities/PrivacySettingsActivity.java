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

/**
 * PrivacySettingsActivity
 *
 * Allows users to manage their privacy preferences.
 * Settings are stored in Firebase Firestore under the user's document.
 *
 * Features:
 * - Toggle-based privacy controls
 * - Real-time saving of settings
 * - Firestore integration for persistent user preferences
 */
public class PrivacySettingsActivity extends AppCompatActivity {

    // Firebase instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String uid;

    // UI switches for privacy settings
    private Switch switchShowProfile;
    private Switch switchShareAttendance;
    private Switch switchEmailNotifs;
    private Switch switchRecommendations;
    private Switch switchLocation;

    // Flag to prevent saving while initial data is loading
    private boolean isLoading = true;

    /**
     * Called when the activity is created.
     * Initializes Firebase, binds UI elements, and loads saved settings.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_settings);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get current user ID
        uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : null;

        // Bind views
        switchShowProfile = findViewById(R.id.switchShowProfile);
        switchShareAttendance = findViewById(R.id.switchShareAttendance);
        switchEmailNotifs = findViewById(R.id.switchEmailNotifs);
        switchRecommendations = findViewById(R.id.switchRecommendations);
        switchLocation = findViewById(R.id.switchLocation);

        // Back button closes activity
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Load saved settings from Firestore
        loadPrivacySettings();

        // Setup toggle listeners for real-time saving
        setupToggleListeners();
    }

    /**
     * Loads existing privacy settings from Firestore
     * and applies them to the UI switches.
     */
    private void loadPrivacySettings() {
        if (uid == null) return;

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Apply saved values or defaults if null
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

                    // Loading complete → allow updates
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
     * Attaches listeners to all switches.
     * Any change triggers an immediate save to Firestore.
     */
    private void setupToggleListeners() {
        CompoundButton.OnCheckedChangeListener listener =
                (buttonView, isChecked) -> {
                    if (isLoading) return; // prevent saving during initialization
                    savePrivacySettings();
                };

        switchShowProfile.setOnCheckedChangeListener(listener);
        switchShareAttendance.setOnCheckedChangeListener(listener);
        switchEmailNotifs.setOnCheckedChangeListener(listener);
        switchRecommendations.setOnCheckedChangeListener(listener);
        switchLocation.setOnCheckedChangeListener(listener);
    }

    /**
     * Saves all privacy settings to Firestore.
     * Uses update() to avoid overwriting other user fields.
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

    /**
     * Utility method to safely extract boolean values from Firestore.
     * Returns default value if the field is null.
     */
    private boolean getBool(Boolean value, boolean defaultVal) {
        return value != null ? value : defaultVal;
    }
}