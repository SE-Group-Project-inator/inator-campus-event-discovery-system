package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * First-login interest capture for students. Drives cold-start recommendations
 * before the student has any RSVP history.
 *
 * Saves to users/{uid}.preferences.categories and sets onboardingComplete = true.
 * "Skip" still flips the flag so the user is not prompted again.
 */
public class StudentOnboardingActivity extends AppCompatActivity {

    private ChipGroup chipGroup;
    private Button btnContinue;
    private TextView tvSkip;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_onboarding);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        chipGroup = findViewById(R.id.chipGroupInterests);
        btnContinue = findViewById(R.id.btnContinue);
        tvSkip = findViewById(R.id.tvSkip);

        populateChips();

        btnContinue.setOnClickListener(v -> savePreferencesAndProceed(false));
        tvSkip.setOnClickListener(v -> savePreferencesAndProceed(true));
    }

    private void populateChips() {
        String[] categories = getResources().getStringArray(R.array.category_array);
        for (String cat : categories) {
            Chip chip = new Chip(this);
            chip.setText(cat);
            chip.setCheckable(true);
            chip.setClickable(true);
            chipGroup.addView(chip);
        }
    }

    private List<String> selectedCategories() {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.isChecked()) out.add(chip.getText().toString());
        }
        return out;
    }

    private void savePreferencesAndProceed(boolean skip) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToHome();
            return;
        }

        List<String> picked = selectedCategories();
        if (!skip && picked.isEmpty()) {
            Toast.makeText(this, "Pick at least one interest, or tap Skip.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        btnContinue.setEnabled(false);
        tvSkip.setEnabled(false);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("categories", picked);

        Map<String, Object> update = new HashMap<>();
        update.put("preferences", prefs);
        update.put("onboardingComplete", true);

        db.collection("users").document(user.getUid())
                .set(update, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> goToHome())
                .addOnFailureListener(e -> {
                    btnContinue.setEnabled(true);
                    tvSkip.setEnabled(true);
                    Toast.makeText(this, "Couldn't save: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void goToHome() {
        Intent i = new Intent(this, StudentHomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
