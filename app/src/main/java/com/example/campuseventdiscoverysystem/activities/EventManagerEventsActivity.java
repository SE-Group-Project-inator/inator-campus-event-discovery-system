package com.example.campuseventdiscoverysystem.activities;

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
import android.widget.EditText;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Event Manager Events Activity
 * Displays a history of all events managed by the current user.
 * Features dynamic filtering, a Management Bottom Sheet, and the Profile Bottom Sheet.
 */
public class EventManagerEventsActivity extends BaseSessionActivity {

    // Firebase instances for database operations
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI - Main List & State
    private RecyclerView rvMyEvents;
    private View emptyState;

    // UI - Statistics Cards
    private TextView tvTotalCount, tvPendingCount;

    // UI - Filter Chips
    private TextView filterAll, filterPending, filterApproved, filterRejected;

    // UI - Profile Bottom Sheet
    private View managerProfileSheet, profileSheetScrim;
    private boolean sheetVisible = false;
    private ImageView imgSheetAvatar;
    private TextView tvSheetManagerName, tvSheetEmail;
    private EditText etSheetSocietyName;

    // Data lists and Adapter
    private ManagerEventAdapter adapter;
    private final List<Event> allEventsList = new ArrayList<>();       // Holds ALL fetched events
    private final List<Event> displayedEventsList = new ArrayList<>(); // Holds currently filtered events
    private String currentFilter = "all"; // Default filter state

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
        setContentView(R.layout.activity_event_manager_events);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views using their IDs
        bindViews();
        setupFilters();
        setupRecyclerView();
        setupProfileSheet();
        setupNavigation();

        // Fetch data from Firestore and calculate statistics
        loadMyEventsAndStats();
        loadManagerProfile();
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {

        rvMyEvents = findViewById(R.id.rvMyEvents);
        emptyState = findViewById(R.id.emptyState);

        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvPendingCount = findViewById(R.id.tvPendingCount);

        filterAll = findViewById(R.id.filterAll);
        filterPending = findViewById(R.id.filterPending);
        filterApproved = findViewById(R.id.filterApproved);
        filterRejected = findViewById(R.id.filterRejected);

        // Profile Sheet Views
        profileSheetScrim = findViewById(R.id.profileSheetScrim);
        managerProfileSheet = findViewById(R.id.managerProfileSheet);
        imgSheetAvatar = findViewById(R.id.imgSheetAvatar);
        tvSheetManagerName = findViewById(R.id.tvSheetManagerName);
        tvSheetEmail = findViewById(R.id.tvSheetEmail);
        etSheetSocietyName = findViewById(R.id.etSheetSocietyName);
    }

    /**
     * Sets up click listeners for the filter chips.
     */
    private void setupFilters() {
        filterAll.setOnClickListener(v -> {
            currentFilter = "all";
            updateChipUI("all");
            applyFilter();
        });

        filterPending.setOnClickListener(v -> {
            currentFilter = "pending_approval";
            updateChipUI("pending_approval");
            applyFilter();
        });

        filterApproved.setOnClickListener(v -> {
            currentFilter = "active";
            updateChipUI("active");
            applyFilter();
        });

        filterRejected.setOnClickListener(v -> {
            currentFilter = "rejected";
            updateChipUI("rejected");
            applyFilter();
        });
    }

    private void updateChipUI(String activeFilter) {
        setChipState(filterAll, "all".equals(activeFilter));
        setChipState(filterPending, "pending_approval".equals(activeFilter));
        setChipState(filterApproved, "active".equals(activeFilter));
        setChipState(filterRejected, "rejected".equals(activeFilter));
    }

    private void setChipState(TextView chip, boolean isActive) {
        if (chip == null) return;
        if (isActive) {
            chip.setBackgroundResource(R.drawable.bg_chip_active);
            chip.setTextColor(getResources().getColor(R.color.white));
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.btn_eventmgr)));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_inactive);
            chip.setTextColor(getResources().getColor(R.color.text_grey));
            chip.setBackgroundTintList(null);
        }
    }

    private void applyFilter() {
        displayedEventsList.clear();
        for (Event event : allEventsList) {
            if ("all".equals(currentFilter)) {
                displayedEventsList.add(event);
            } else if (currentFilter.equals(event.getStatus())) {
                displayedEventsList.add(event);
            }
        }
        adapter.notifyDataSetChanged();

        if (displayedEventsList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            rvMyEvents.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvMyEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Initializes the RecyclerView and its adapter
     * Defines what happens when an individual event card is clicked
     */
    private void setupRecyclerView() {
        adapter = new ManagerEventAdapter(displayedEventsList, eventId -> {
            String eventTitle = "Manage Event";
            for (Event e : displayedEventsList) {
                if (e.getId().equals(eventId)) {
                    eventTitle = e.getTitle();
                    break;
                }
            }
            showManagementSheet(eventId, eventTitle);
        });

        adapter.setShowStatusBadge(true);
        rvMyEvents.setLayoutManager(new LinearLayoutManager(this));
        rvMyEvents.setAdapter(adapter);
    }

    private void showManagementSheet(String eventId, String title) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.TransparentBottomSheetDialog);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_event_manage_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        tvTitle.setText(title);

        // Find the event to check its status
        Event selectedEvent = null;
        for (Event e : displayedEventsList) {
            if (e.getId().equals(eventId)) {
                selectedEvent = e;
                break;
            }
        }

        // Hide Attendees and Analytics if not approved
        if (selectedEvent != null) {
            String status = selectedEvent.getStatus();
            if ("pending_approval".equals(status) || "rejected".equals(status)) {
                sheetView.findViewById(R.id.btnManageAttendees).setVisibility(View.GONE);
                sheetView.findViewById(R.id.btnViewAnalytics).setVisibility(View.GONE);
            } else {
                sheetView.findViewById(R.id.btnManageAttendees).setVisibility(View.VISIBLE);
                sheetView.findViewById(R.id.btnViewAnalytics).setVisibility(View.VISIBLE);
            }
        }

        sheetView.findViewById(R.id.btnEditEvent).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, ManageEventActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        sheetView.findViewById(R.id.btnManageAttendees).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, AttendeesRosterActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        sheetView.findViewById(R.id.btnViewAnalytics).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, EventAnalyticsActivity.class);
            intent.putExtra("EVENT_ID", eventId);
            startActivity(intent);
        });

        bottomSheetDialog.show();
    }

    /**
     * Fetches the manager's events from Firestore
     * Calculates the "Total Managed" and "This Month" statistics
     */
    private void loadMyEventsAndStats() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Resolve societyId first, then query events by society so all co-managers see same list
        db.collection("users").document(uid).get().addOnSuccessListener(userDoc -> {
            String societyId = userDoc.getString("societyId");
            String filterField = (societyId != null && !societyId.isEmpty()) ? "societyId" : "createdBy";
            String filterValue = (societyId != null && !societyId.isEmpty()) ? societyId : uid;

            db.collection("events")
                    .whereEqualTo(filterField, filterValue)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) { Toast.makeText(this, "Failed to load events", Toast.LENGTH_SHORT).show(); return; }
                        if (snapshots == null) return;

                        allEventsList.clear();
                        int totalCount = 0, pendingCount = 0;

                        for (DocumentSnapshot doc : snapshots) {
                            Event event = doc.toObject(Event.class);
                            if (event == null) continue;
                            event.setId(doc.getId());
                            allEventsList.add(event);
                            totalCount++;
                            if ("pending_approval".equals(event.getStatus())) pendingCount++;
                        }

                        tvTotalCount.setText(String.valueOf(totalCount));
                        tvPendingCount.setText(String.valueOf(pendingCount));
                        applyFilter();
                    });
        });
    }

    /**
     * Loads the manager's profile data (name, email, society, avatar) for the bottom sheet.
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
     * Sets up profile sheet UI behaviors (editable fields, avatar clicks, animations).
     */
    private void setupProfileSheet() {
        if (mAuth.getCurrentUser() == null) return;
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

    /**
     * Handles routing for the bottom navigation bar
     */
    private void setupNavigation() {

        // Home Navigation Tab
        findViewById(R.id.navHome).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerDashboardActivity.class));
            finish();
        });

        // Trigger the profile sheet instead of routing back to the dashboard
        findViewById(R.id.navProfile).setOnClickListener(v -> toggleProfileSheet());
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