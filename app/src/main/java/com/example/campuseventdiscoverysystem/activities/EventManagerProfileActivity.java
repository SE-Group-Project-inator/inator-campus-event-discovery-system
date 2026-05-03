package com.example.campuseventdiscoverysystem.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Event Manager Profile Activity
 * Displays user info, statistics, quick action buttons, and handles navigation
 */
public class EventManagerProfileActivity extends BaseSessionActivity {

    // Firebase Instances
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Elements
    private ImageButton btnNotificationsTop;
    private ImageView imgAvatar;
    private TextView tvProfileName, tvProfileEmail;

    // Statistics Counters
    private TextView tvManagedCount, tvThisMonthCount, tvFollowersCount;

    // Input Fields
    private EditText etSocietyName;

    // Quick Access Cards
    private CardView btnQuickCreate, btnQuickHistory, btnSignOut;

    // Image Picker Launcher for Profile Picture
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
        setContentView(R.layout.activity_event_manager_profile);

        // Initialize Firebase connections
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Link Java variables to the XML Views using their IDs
        bindViews();

        // Load the data and setup listeners
        loadProfileData();
        loadStatistics();
        setupInteractions();
        setupNavigation();
    }

    /**
     * Maps all the XML UI components to Java variables
     */
    private void bindViews() {
        btnNotificationsTop = findViewById(R.id.btnNotificationsTop);
        imgAvatar = findViewById(R.id.imgAvatar);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvManagedCount = findViewById(R.id.tvManagedCount);
        tvThisMonthCount = findViewById(R.id.tvThisMonthCount);
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        etSocietyName = findViewById(R.id.etSocietyName);
        btnQuickCreate = findViewById(R.id.btnQuickCreate);
        btnQuickHistory = findViewById(R.id.btnQuickHistory);
        btnSignOut = findViewById(R.id.btnSignOut);
    }

    /**
     * Fetches the manager's name, email, society name, and avatar from the database
     */
    private void loadProfileData() {
        // Ensure user is logged in
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Fetch the details from the database
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String email = doc.getString("email");
                String society = doc.getString("societyName");
                String profilePicBase64 = doc.getString("profilePicture");

                // Update UI
                if (name != null) tvProfileName.setText(name);
                if (email != null) tvProfileEmail.setText(email);
                if (society != null) etSocietyName.setText(society);

                // Decode and display Profile Picture if it exists
                if (profilePicBase64 != null && profilePicBase64.startsWith("data:image")) {
                    try {
                        String cleanBase64 = profilePicBase64.substring(profilePicBase64.indexOf(",") + 1);
                        byte[] decodedString = Base64.decode(cleanBase64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        imgAvatar.setImageBitmap(decodedByte);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile data!", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Fetches statistics for the profile cards (Managed, This Month, Followers)
     */
    private void loadStatistics() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Get Event Statistics (Total and This Month)
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int currentYear = now.get(Calendar.YEAR);
        Calendar eventCal = Calendar.getInstance();

        // Make query for all active events by this manager
        db.collection("events")
                .whereEqualTo("createdBy", uid)
                .whereEqualTo("status", "active").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // Set total managed events
                    int totalManaged = queryDocumentSnapshots.size();
                    tvManagedCount.setText(String.valueOf(totalManaged));

                    // Count events in current month
                    int thisMonthCount = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp dateTs = doc.getTimestamp("date");
                        if (dateTs != null) {
                            eventCal.setTime(dateTs.toDate());
                            if (eventCal.get(Calendar.MONTH) == currentMonth && eventCal.get(Calendar.YEAR) == currentYear) {
                                thisMonthCount++;
                            }
                        }
                    }
                    // Set This Month count
                    tvThisMonthCount.setText(String.valueOf(thisMonthCount));
                });

        // Get Followers Count
        db.collection("users").document(uid).collection("followers").get()
                .addOnSuccessListener(snapshots -> {
                    tvFollowersCount.setText(String.valueOf(snapshots.size()));
                })
                .addOnFailureListener(e -> tvFollowersCount.setText("0"));
    }

    /**
     * Sets up the listeners for the interactive elements
     */
    private void setupInteractions() {
        String uid = mAuth.getCurrentUser().getUid();

        // Trigger Image Picker when avatar is clicked
        imgAvatar.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.setType("image/*");
            imagePickerLauncher.launch(pick);
        });

        // Save Society Name when the "Done" or "Enter" button is pressed on the keyboard
        etSocietyName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                String newSocietyName = etSocietyName.getText().toString().trim();

                db.collection("users").document(uid)
                        .update("societyName", newSocietyName)
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Society name updated successfully!", Toast.LENGTH_SHORT).show();
                            etSocietyName.clearFocus(); // Remove cursor
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to update society name.", Toast.LENGTH_SHORT).show());
                return true;
            }
            return false;
        });

        // Notification Icon
        btnNotificationsTop.setOnClickListener(v -> {
            Toast.makeText(this, "Will set to notifications screen!", Toast.LENGTH_SHORT).show();
        });

        // Quick Access Cards
        btnQuickCreate.setOnClickListener(v -> startActivity(new Intent(this, ManageEventActivity.class)));
        btnQuickHistory.setOnClickListener(v -> startActivity(new Intent(this, EventManagerEventsActivity.class)));

        // Sign Out Button
        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, RoleSelectActivity.class));
            finish();
        });
    }

    /**
     * Converts the selected image to a Base64 string and uploads it to Firestore.
     */
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

            // Basic compression check (prevent exceeding Firestore 1MB limit)
            if (imageBytes.length > 800_000) {
                Toast.makeText(this, "Image too large. Please select a smaller picture.", Toast.LENGTH_LONG).show();
                return;
            }

            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);

            db.collection("users").document(uid)
                    .update("profilePicture", base64Image)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                        // Instantly show the new image
                        imgAvatar.setImageURI(imageUri);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show());

        } catch (Exception e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Event Navigation Tab
        findViewById(R.id.navEvents).setOnClickListener(v -> {
            startActivity(new Intent(this, EventManagerEventsActivity.class));
            finish();
        });
    }
}