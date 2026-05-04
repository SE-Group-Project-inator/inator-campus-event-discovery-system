package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class StudentProfileActivity extends BaseSessionActivity {

    private TextView tvStudentName, tvStudentEmail;
    private TextView tvEventsAttended, tvThisMonth, tvFollowing;
    private CardView btnAttendanceHistory, btnMySocieties, btnMyPayments;
    private CardView btnSignOut;
    private ImageButton btnNotification;
    private LinearLayout navHome, navSearch, navTickets, navProfile;

    private FirebaseAuth mAuth;
    private ImageView imgAvatar;
    private ActivityResultLauncher<String> pickerLauncher;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        mAuth = FirebaseAuth.getInstance();
        pickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    uploadProfilePic(uri);
                });
        db    = FirebaseFirestore.getInstance();

        tvStudentName        = findViewById(R.id.tvStudentName);
        tvStudentEmail       = findViewById(R.id.tvStudentEmail);
        tvEventsAttended     = findViewById(R.id.tvEventsAttended);
        tvThisMonth          = findViewById(R.id.tvThisMonth);
        tvFollowing          = findViewById(R.id.tvFollowing);
        btnNotification      = findViewById(R.id.btnNotification);
        btnAttendanceHistory = findViewById(R.id.btnAttendanceHistory);
        btnMySocieties       = findViewById(R.id.btnMySocieties);
        btnMyPayments        = findViewById(R.id.btnMyPayments);
        btnSignOut           = findViewById(R.id.btnSignOut);
        navHome              = findViewById(R.id.navHome);
        navSearch            = findViewById(R.id.navSearch);
        navTickets           = findViewById(R.id.navTickets);
        navProfile           = findViewById(R.id.navProfile);

        imgAvatar = findViewById(R.id.imgAvatar);
        if (imgAvatar != null) imgAvatar.setOnClickListener(v -> pickerLauncher.launch("image/*"));
        loadStudentProfile();
        loadAttendedCount();
        loadThisMonthCount();
        loadFollowingCount();

        btnNotification.setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class))
        );

        // Attendance History → EventHistoryActivity
        // (the button is labelled "Attendance History" in the layout,
        //  EventHistoryActivity is the class that implements it)
        btnAttendanceHistory.setOnClickListener(v ->
                startActivity(new Intent(this, EventHistoryActivity.class))
        );

        btnMySocieties.setOnClickListener(v ->
                startActivity(new Intent(this, MySocietiesActivity.class))
        );

        // My Payments → MyPaymentsActivity
        if (btnMyPayments != null) {
            btnMyPayments.setOnClickListener(v ->
                    startActivity(new Intent(this, MyPaymentsActivity.class))
            );
        }

        btnSignOut.setOnClickListener(v -> showLogoutDialog());

        // Bottom Navigation
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(this, StudentHomeActivity.class));
            finish();
        });

        navSearch.setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class))
        );

        // navTickets → TicketsActivity (not Payments — Payments is in profile buttons above)
        navTickets.setOnClickListener(v ->
                startActivity(new Intent(this, TicketsActivity.class))
        );

        navProfile.setOnClickListener(v -> { /* already here */ });
    }

    private void loadStudentProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

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
                        String pic = doc.getString("profilePicture");
                        if (pic != null && pic.startsWith("data:image") && imgAvatar != null) {
                            try {
                                String clean = pic.substring(pic.indexOf(",") + 1);
                                byte[] decoded = Base64.decode(clean, Base64.DEFAULT);
                                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                                imgAvatar.setImageBitmap(bmp);
                            } catch (Exception ignored) {}
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load profile",
                                Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Counts confirmed RSVPs where the event date has already passed.
     * Matches EventHistoryActivity's definition of "attended" exactly.
     */
    private void uploadProfilePic(Uri uri) {
        com.google.firebase.auth.FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            is.close();
            byte[] bytes = baos.toByteArray();
            if (bytes.length > 800_000) { Toast.makeText(this, "Image too large", Toast.LENGTH_SHORT).show(); return; }
            String b64 = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
            db.collection("users").document(user.getUid()).update("profilePicture", b64)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        if (imgAvatar != null) imgAvatar.setImageURI(uri);
                    });
        } catch (Exception e) { Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show(); }
    }

    private void loadAttendedCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    List<DocumentSnapshot> docs = rsvpQuery.getDocuments();
                    if (docs.isEmpty()) {
                        tvEventsAttended.setText("0");
                        return;
                    }

                    Date now = new Date();
                    AtomicInteger total     = new AtomicInteger(0);
                    AtomicInteger remaining = new AtomicInteger(docs.size());

                    for (DocumentSnapshot rsvp : docs) {
                        String eventId = rsvp.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0)
                                tvEventsAttended.setText(String.valueOf(total.get()));
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");
                                    if (ts != null && ts.toDate().before(now))
                                        total.incrementAndGet();
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsAttended.setText(String.valueOf(total.get()));
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0)
                                        tvEventsAttended.setText(String.valueOf(total.get()));
                                });
                    }
                })
                .addOnFailureListener(e -> tvEventsAttended.setText("0"));
    }

    /**
     * Counts past-date RSVPs whose event month matches the current calendar month.
     * Matches EventHistoryActivity's updateStats() logic exactly.
     */
    private void loadThisMonthCount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String currentMonth = new SimpleDateFormat("MMM", Locale.getDefault())
                .format(Calendar.getInstance().getTime()).toUpperCase();
        Date now = new Date();

        db.collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpQuery -> {
                    List<DocumentSnapshot> docs = rsvpQuery.getDocuments();
                    if (docs.isEmpty()) {
                        tvThisMonth.setText("0");
                        return;
                    }

                    AtomicInteger count     = new AtomicInteger(0);
                    AtomicInteger remaining = new AtomicInteger(docs.size());

                    for (DocumentSnapshot rsvp : docs) {
                        String eventId = rsvp.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0)
                                tvThisMonth.setText(String.valueOf(count.get()));
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    com.google.firebase.Timestamp ts =
                                            eventDoc.getTimestamp("date");
                                    if (ts != null && ts.toDate().before(now)) {
                                        String month = new SimpleDateFormat(
                                                "MMM", Locale.getDefault())
                                                .format(ts.toDate()).toUpperCase();
                                        if (month.equals(currentMonth))
                                            count.incrementAndGet();
                                    }
                                    if (remaining.decrementAndGet() == 0)
                                        tvThisMonth.setText(String.valueOf(count.get()));
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0)
                                        tvThisMonth.setText(String.valueOf(count.get()));
                                });
                    }
                })
                .addOnFailureListener(e -> tvThisMonth.setText("0"));
    }

    private void loadFollowingCount() {
        tvFollowing.setText("0");
    }
}