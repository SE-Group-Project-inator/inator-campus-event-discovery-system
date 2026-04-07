package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class StudentProfileActivity extends AppCompatActivity {

    // UI elements
    private TextView tvStudentName, tvStudentEmail;
    private TextView tvEventsAttended, tvThisMonth, tvFollowing;
    private CardView btnAttendanceHistory, btnMySocieties;
    private CardView btnQRCheckIn, btnSignOut, btnPrivacySettings;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Link UI elements
        tvStudentName        = findViewById(R.id.tvStudentName);
        tvStudentEmail       = findViewById(R.id.tvStudentEmail);
        tvEventsAttended     = findViewById(R.id.tvEventsAttended);
        tvThisMonth          = findViewById(R.id.tvThisMonth);
        tvFollowing          = findViewById(R.id.tvFollowing);
        btnNotification      = findViewById(R.id.btnNotification);
        btnAttendanceHistory = findViewById(R.id.btnAttendanceHistory);
        btnMySocieties       = findViewById(R.id.btnMySocieties);
        btnQRCheckIn         = findViewById(R.id.btnQRCheckIn);
        btnSignOut           = findViewById(R.id.btnSignOut);
        btnPrivacySettings   = findViewById(R.id.btnPrivacySettings);
        navHome              = findViewById(R.id.navHome);
        navSearch            = findViewById(R.id.navSearch);
        navTickets           = findViewById(R.id.navTickets);
        navProfile           = findViewById(R.id.navProfile);

        // Load all data
        loadStudentProfile();
        loadAttendedCount();
        loadThisMonthCount();
        loadFollowingCount();

        // Notification bell
        btnNotification.setOnClickListener(v ->
                Toast.makeText(this, "Notifications coming soon!", Toast.LENGTH_SHORT).show()
        );

//         Attendance History
//        btnAttendanceHistory.setOnClickListener(v ->
//                startActivity(new Intent(this, AttendanceHistoryActivity.class))
//        );
//
//         My Societies
//        btnMySocieties.setOnClickListener(v ->
//                startActivity(new Intent(this, SocietiesActivity.class))
//        );
//
//         QR Check In
//        btnQRCheckIn.setOnClickListener(v ->
//                startActivity(new Intent(this, QRActivity.class))
//        );

        // Privacy Settings
        btnPrivacySettings.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacySettingsActivity.class))
        );

        // Sign Out
        btnSignOut.setOnClickListener(v -> signOut());

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        navTickets.setOnClickListener(v ->
                Toast.makeText(this, "Tickets coming soon!", Toast.LENGTH_SHORT).show()
        );

        navProfile.setOnClickListener(v -> {
            // already on profile, do nothing
        });
    }

    // ── Load name and email from /users/{uid} ──
    private void loadStudentProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Set email from Auth immediately as fallback
        tvStudentEmail.setText(user.getEmail());

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("name");
                        String email = doc.getString("email");
                        if (name  != null) tvStudentName.setText(name);
                        if (email != null) tvStudentEmail.setText(email);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Could not load profile", Toast.LENGTH_SHORT).show()
                );
    }


    // Fields used: userId, status
    private void loadAttendedCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(query ->
                        tvEventsAttended.setText(String.valueOf(query.size()))
                )
                .addOnFailureListener(e ->
                        tvEventsAttended.setText("0")
                );
    }


    // Fields used: userId, status, createdAt
    private void loadThisMonthCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Get first day of current month as Timestamp
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startOfMonth = new Timestamp(cal.getTime());

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .whereGreaterThanOrEqualTo("createdAt", startOfMonth)
                .get()
                .addOnSuccessListener(query ->
                        tvThisMonth.setText(String.valueOf(query.size()))
                )
                .addOnFailureListener(e ->
                        tvThisMonth.setText("0")
                );
    }

    // ── Following count ──
    // No following collection in DB yet — set to 0 for now
    private void loadFollowingCount() {
        // TODO: update when following collection is added to Firestore
        tvFollowing.setText("0");
    }

    // ── Sign out ──
    private void signOut() {
        mAuth.signOut();
        Intent intent = new Intent(this, RoleSelectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}