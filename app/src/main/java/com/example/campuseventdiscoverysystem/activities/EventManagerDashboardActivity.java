package com.example.campuseventdiscoverysystem.activities;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.ManagerEventAdapter;
import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * EventManagerDashboardActivity
 * Serves as the main home screen for the Event Manager.
 * Features a calendar view, real-time statistics, quick action links,
 * and a custom-animated profile bottom sheet.
 */
public class EventManagerDashboardActivity extends BaseSessionActivity {

    // Firebase instances for database operations
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI - Top Bar & Welcome
    private ImageButton btnNotifications;
    private TextView tvGreeting;

    // UI - Statistics
    private TextView tvTotalManaged, tvThisMonth, tvPendingCount;

    // UI - Main Feed (Calendar)
    private CalendarView calendarView;
    private TextView tvSelectedDateHeader, tvEmpty;
    private RecyclerView rvDateEvents;

    // UI - Profile Bottom Sheet
    private View managerProfileSheet, profileSheetScrim;
    private boolean sheetVisible = false;
    private ImageView imgSheetAvatar;
    private TextView tvSheetManagerName, tvSheetEmail;
    private EditText etSheetSocietyName;

    // Adapter Data
    private ManagerEventAdapter adapter;
    private List<Event> dateEventsList = new ArrayList<>();

    // Image Picker for Profile Pic
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    uploadProfilePicture(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bind to the XML layout
        setContentView(R.layout.activity_event_manager_dashboard);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views
        bindViews();
        setupWelcomeMessage();
        setupStatistics();
        setupQuickActions();
        setupProfileSheet();
        setupRecyclerView();
        setupNavigation();
        loadManagerProfile();

        // Load events for today by default
        loadEventsForDate(new Date());

        // Listen for Calendar interactions
        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            // Construct a Calendar object from the selected date parameters
            Calendar clickedDate = Calendar.getInstance();
            clickedDate.set(year, month, day);

            // Fetch events for the newly selected date
            loadEventsForDate(clickedDate.getTime());
        });
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {
        btnNotifications = findViewById(R.id.btnNotifications);
        tvGreeting = findViewById(R.id.tvGreeting);

        tvTotalManaged = findViewById(R.id.tvTotalManaged);
        tvThisMonth = findViewById(R.id.tvThisMonth);
        tvPendingCount = findViewById(R.id.tvPendingCount);

        calendarView = findViewById(R.id.calendarView);
        tvSelectedDateHeader = findViewById(R.id.tvSelectedDateHeader);
        rvDateEvents = findViewById(R.id.rvDateEvents);
        tvEmpty = findViewById(R.id.tvEmpty);

        profileSheetScrim = findViewById(R.id.profileSheetScrim);
        managerProfileSheet = findViewById(R.id.managerProfileSheet);
        imgSheetAvatar = findViewById(R.id.imgSheetAvatar);
        tvSheetManagerName = findViewById(R.id.tvSheetManagerName);
        tvSheetEmail = findViewById(R.id.tvSheetEmail);
        etSheetSocietyName = findViewById(R.id.etSheetSocietyName);
    }

    /**
     * Sets a time-appropriate greeting message.
     */
    private void setupWelcomeMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good morning, Manager 👋";
        else if (hour < 17) greeting = "Good afternoon, Manager 👋";
        else greeting = "Good evening, Manager 👋";

        tvGreeting.setText(greeting);

        btnNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Routing to Notifications...", Toast.LENGTH_SHORT).show());
    }

    /**
     * Loads the manager's profile data (name, email, society, avatar).
     */
    private void loadManagerProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvSheetManagerName.setText(doc.getString("name"));
                tvSheetEmail.setText(doc.getString("email"));
                etSheetSocietyName.setText(doc.getString("societyName"));

                String profilePicBase64 = doc.getString("profilePicture");
                if (profilePicBase64 != null && profilePicBase64.startsWith("data:image")) {
                    try {
                        String cleanBase64 = profilePicBase64.substring(profilePicBase64.indexOf(",") + 1);
                        byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imgSheetAvatar.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * Calculates the "Total", "This Month", and "Pending" event counters.
     */
    private void setupStatistics() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
        Calendar eventCal = Calendar.getInstance();

        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .addSnapshotListener((snapshots, error) -> {
                    if (snapshots == null) return;

                    int totalActive = 0;
                    int thisMonth = 0;
                    int pending = 0;

                    for (DocumentSnapshot doc : snapshots) {
                        String status = doc.getString("status");

                        if ("pending_approval".equals(status)) {
                            pending++;
                        } else if ("active".equals(status)) {
                            totalActive++;
                            Timestamp dateTs = doc.getTimestamp("date");
                            if (dateTs != null) {
                                eventCal.setTime(dateTs.toDate());
                                if (eventCal.get(Calendar.MONTH) == currentMonth && eventCal.get(Calendar.YEAR) == currentYear) {
                                    thisMonth++;
                                }
                            }
                        }
                    }

                    animateCounter(tvTotalManaged, totalActive);
                    animateCounter(tvThisMonth, thisMonth);
                    animateCounter(tvPendingCount, pending);
                });
    }

    /**
     * Animates counter numbers from current to target value.
     */
    private void animateCounter(TextView tv, int target) {
        if (tv == null) return;
        try {
            int current = Integer.parseInt(tv.getText().toString());
            ValueAnimator anim = ValueAnimator.ofInt(current, target);
            anim.setDuration(600);
            anim.addUpdateListener(a -> tv.setText(String.valueOf((int) a.getAnimatedValue())));
            anim.start();
        } catch (NumberFormatException e) {
            tv.setText(String.valueOf(target));
        }
    }

    /**
     * Sets up intents for the colored Quick Action grid.
     */
    private void setupQuickActions() {
        findViewById(R.id.btnQuickCreate).setOnClickListener(v ->
                startActivity(new Intent(this, ManageEventActivity.class)));

        findViewById(R.id.btnQuickPayments).setOnClickListener(v ->
                startActivity(new Intent(this, PaymentVerificationActivity.class)));

        findViewById(R.id.btnQuickScanQR).setOnClickListener(v ->
                Toast.makeText(this, "QR Scanner coming soon!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Sets up the RecyclerView for calendar events.
     */
    private void setupRecyclerView() {

        // Route to the EventDisplayActivity
        adapter = new ManagerEventAdapter(dateEventsList, eventId -> {
            Intent intent = new Intent(this, EventDisplayActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            intent.putExtra("USER_ROLE", "manager");
            startActivity(intent);
        });

        // Hide status badge of events for the dashboard
        adapter.setShowStatusBadge(false);

        // Use a vertical scrolling list and attach the adapter
        rvDateEvents.setLayoutManager(new LinearLayoutManager(this));
        rvDateEvents.setAdapter(adapter);
    }

    /**
     * Fetches events for the selected date on the calendar.
     */
    private void loadEventsForDate(Date selectedDate) {

        // Update the header text to reflect the selected date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvSelectedDateHeader.setText("Events on " + sdf.format(selectedDate));

        // Define the start of the selected day
        Calendar start = Calendar.getInstance();
        start.setTime(selectedDate);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        // Define the end of the selected day
        Calendar end = Calendar.getInstance();
        end.setTime(selectedDate);
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        // Query against the "events" collection in database
        db.collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", start.getTime())
                .whereLessThanOrEqualTo("date", end.getTime())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Clear the last day's events
                    dateEventsList.clear();

                    // Iterate through the fetched documents and add events to the list
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = doc.toObject(Event.class);
                        if (event != null) {
                            event.setId(doc.getId());
                            dateEventsList.add(event);
                        }
                    }

                    // Notify the adapter
                    adapter.notifyDataSetChanged();

                    if (tvEmpty != null) {
                        if (dateEventsList.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            rvDateEvents.setVisibility(View.GONE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            rvDateEvents.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    /**
     * Sets up bottom navigation bar intents and profile sheet toggle.
     */
    private void setupNavigation() {

        // Events Navigation Tab
        findViewById(R.id.navEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerEventsActivity.class));
            finish();
        });

        findViewById(R.id.navProfile).setOnClickListener(v -> toggleProfileSheet());
    }

    /**
     * Sets up profile sheet UI behaviors (editable fields, avatar clicks, animations).
     */
    private void setupProfileSheet() {
        String uid = mAuth.getCurrentUser().getUid();

        profileSheetScrim.setOnClickListener(v -> hideProfileSheet());

        findViewById(R.id.btnSheetLogout).setOnClickListener(v -> {
            hideProfileSheet();
            showLogoutDialog();
        });

        // Edit Profile Picture
        imgSheetAvatar.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.setType("image/*");
            imagePickerLauncher.launch(pick);
        });

        // Edit Society Name
        etSheetSocietyName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                String newSocietyName = etSheetSocietyName.getText().toString().trim();
                db.collection("users").document(uid).update("societyName", newSocietyName)
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Society Name Updated!", Toast.LENGTH_SHORT).show();
                            etSheetSocietyName.clearFocus();
                        });
                return true;
            }
            return false;
        });
    }

    private void toggleProfileSheet() {
        if (sheetVisible) hideProfileSheet();
        else showProfileSheet();
    }

    private void showProfileSheet() {
        sheetVisible = true;
        profileSheetScrim.setVisibility(View.VISIBLE);
        profileSheetScrim.setAlpha(0f);
        profileSheetScrim.animate().alpha(1f).setDuration(200).start();

        managerProfileSheet.setVisibility(View.VISIBLE);
        managerProfileSheet.post(() -> {
            float startY = managerProfileSheet.getHeight();
            managerProfileSheet.setTranslationY(startY);
            managerProfileSheet.animate()
                    .translationY(0f)
                    .setDuration(320)
                    .setInterpolator(new DecelerateInterpolator(2f))
                    .start();
        });
    }

    private void hideProfileSheet() {
        sheetVisible = false;
        profileSheetScrim.animate().alpha(0f).setDuration(200)
                .withEndAction(() -> profileSheetScrim.setVisibility(View.GONE))
                .start();

        float endY = managerProfileSheet.getHeight();
        managerProfileSheet.animate()
                .translationY(endY)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> managerProfileSheet.setVisibility(View.GONE))
                .start();
    }

    private void uploadProfilePicture(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) baos.write(buffer, 0, bytesRead);
            inputStream.close();

            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length > 800_000) {
                Toast.makeText(this, "Image too large.", Toast.LENGTH_LONG).show();
                return;
            }

            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);

            db.collection("users").document(uid).update("profilePicture", base64Image)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        imgSheetAvatar.setImageURI(imageUri);
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        if (sheetVisible) {
            hideProfileSheet();
        } else {
            super.onBackPressed();
        }
    }
}