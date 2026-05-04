package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.models.Payment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.*;

/**
 * =============================================================================
 * PaymentActivity
 * =============================================================================
 * Handles event payment processing including:
 * - Cash payments
 * - Online payments (JazzCash / Easypaisa)
 * - Screenshot upload & encoding
 * - RSVP creation and event registration
 * - Capacity and waitlist handling
 * - Firestore integration for payments & notifications
 */
public class PaymentActivity extends AppCompatActivity {

    /** Intent keys for event data transfer */
    public static final String KEY_EVENT_ID     = "eventId";
    public static final String KEY_EVENT_TITLE  = "eventTitle";
    public static final String KEY_EVENT_DATE   = "eventDate";
    public static final String KEY_TICKET_PRICE = "ticketPrice";

    /** Hardcoded payment account numbers */
    private static final String JAZZCASH_NUMBER  = "0300-1234567";
    private static final String EASYPAISA_NUMBER = "0301-7654321";

    // ========================= UI COMPONENTS =========================
    private TextView tvEventName, tvEventDate, tvTicketPrice;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbJazzCash, rbEasypaisa;
    private CardView cardCashInfo, cardOnlineInstructions, cardScreenshot;
    private TextView tvAccountNumber, tvInstructionAmount, tvPaymentMethodName;
    private ImageView ivScreenshotPreview;
    private TextView tvUploadHint;
    private MaterialButton btnUploadScreenshot, btnSubmit;
    private ProgressBar progressBar;

    // ========================= DATA =========================
    private String eventId, eventTitle, eventDate;
    private double ticketPrice;
    private Uri selectedImageUri;
    private String selectedMethod = Payment.METHOD_CASH;

    // Firebase references
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Consent flags passed from previous screen
    private boolean isNameVisible, isRollNoVisible, optInWaitlist;

    /**
     * Image picker launcher for selecting payment screenshot
     */
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                // Handle image selection result
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();

                    // Preview selected image
                    ivScreenshotPreview.setImageURI(selectedImageUri);
                    ivScreenshotPreview.setVisibility(View.VISIBLE);

                    // Update UI hint
                    tvUploadHint.setText("Screenshot selected ✓");
                    tvUploadHint.setTextColor(getResources().getColor(R.color.green_accept, getTheme()));
                }
            });

    /**
     * Called when activity is created.
     * Initializes Firebase, UI, and listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Load data and setup UI
        readIntentExtras();
        bindViews();
        populateEventSummary();
        setupPaymentMethodSelector();
        setupUploadButton();
        setupSubmitButton();
        setupBackButton();
    }

    /**
     * Reads event and consent data passed via Intent
     */
    private void readIntentExtras() {
        Intent in = getIntent();

        eventId     = in.getStringExtra(KEY_EVENT_ID);
        eventTitle  = in.getStringExtra(KEY_EVENT_TITLE);
        eventDate   = in.getStringExtra(KEY_EVENT_DATE);
        ticketPrice = in.getDoubleExtra(KEY_TICKET_PRICE, 0.0);

        // Consent flags
        isNameVisible   = in.getBooleanExtra("isNameVisible", false);
        isRollNoVisible = in.getBooleanExtra("isRollNoVisible", false);
        optInWaitlist   = in.getBooleanExtra("optInWaitlist", false);
    }

    /**
     * Binds XML views to Java variables
     */
    private void bindViews() {
        tvEventName            = findViewById(R.id.tvPaymentEventName);
        tvEventDate            = findViewById(R.id.tvPaymentEventDate);
        tvTicketPrice          = findViewById(R.id.tvPaymentTicketPrice);
        rgPaymentMethod        = findViewById(R.id.rgPaymentMethod);
        rbCash                 = findViewById(R.id.rbCash);
        rbJazzCash             = findViewById(R.id.rbJazzCash);
        rbEasypaisa            = findViewById(R.id.rbEasypaisa);

        cardCashInfo           = findViewById(R.id.cardCashInfo);
        cardOnlineInstructions = findViewById(R.id.cardOnlineInstructions);
        cardScreenshot         = findViewById(R.id.cardScreenshot);

        tvAccountNumber        = findViewById(R.id.tvAccountNumber);
        tvInstructionAmount    = findViewById(R.id.tvInstructionAmount);
        tvPaymentMethodName    = findViewById(R.id.tvPaymentMethodName);

        ivScreenshotPreview    = findViewById(R.id.ivScreenshotPreview);
        tvUploadHint           = findViewById(R.id.tvUploadHint);

        btnUploadScreenshot    = findViewById(R.id.btnUploadScreenshot);
        btnSubmit              = findViewById(R.id.btnSubmitPayment);
        progressBar            = findViewById(R.id.progressBarPayment);
    }

    /**
     * Displays event info in UI
     */
    private void populateEventSummary() {
        if (tvEventName != null) tvEventName.setText(eventTitle);
        if (tvEventDate != null) tvEventDate.setText("📅 " + eventDate);

        if (tvTicketPrice != null) {
            tvTicketPrice.setText(ticketPrice > 0
                    ? "PKR " + NumberFormat.getInstance().format((long) ticketPrice)
                    : "Free");
        }
    }

    /**
     * Handles switching between payment methods (Cash / Online)
     */
    private void setupPaymentMethodSelector() {
        showCashSection();

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {

            if (checkedId == R.id.rbCash) {
                selectedMethod = Payment.METHOD_CASH;
                showCashSection();

            } else if (checkedId == R.id.rbJazzCash) {
                selectedMethod = Payment.METHOD_JAZZCASH;
                showOnlineSection(JAZZCASH_NUMBER, "📱 JazzCash");

            } else if (checkedId == R.id.rbEasypaisa) {
                selectedMethod = Payment.METHOD_EASYPAISA;
                showOnlineSection(EASYPAISA_NUMBER, "💚 Easypaisa");
            }
        });
    }

    /**
     * Shows Cash UI section
     */
    private void showCashSection() {
        cardCashInfo.setVisibility(View.VISIBLE);
        cardOnlineInstructions.setVisibility(View.GONE);
        cardScreenshot.setVisibility(View.GONE);
        selectedImageUri = null;
    }

    /**
     * Shows Online payment UI section
     */
    private void showOnlineSection(String accountNumber, String methodName) {
        cardCashInfo.setVisibility(View.GONE);
        cardOnlineInstructions.setVisibility(View.VISIBLE);
        cardScreenshot.setVisibility(View.VISIBLE);

        tvAccountNumber.setText(accountNumber);
        tvPaymentMethodName.setText(methodName);
        tvInstructionAmount.setText("PKR " +
                NumberFormat.getInstance().format((long) ticketPrice));
    }

    /**
     * Upload button listener (opens gallery)
     */
    private void setupUploadButton() {
        btnUploadScreenshot.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.setType("image/*");
            imagePickerLauncher.launch(pick);
        });
    }

    /**
     * Back button handler
     */
    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnPaymentBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    /**
     * Submit payment button handler
     */
    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    /**
     * Validates payment input before submission
     */
    private void validateAndSubmit() {

        // Require screenshot for online payments
        if (!selectedMethod.equals(Payment.METHOD_CASH) && selectedImageUri == null) {
            Toast.makeText(this,
                    "Please upload your payment screenshot before submitting.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Check event capacity before proceeding
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {

                    Long capacity = documentSnapshot.getLong("capacity");
                    Long registeredCount = documentSnapshot.getLong("registeredCount");

                    if (capacity == null) capacity = 0L;
                    if (registeredCount == null) registeredCount = 0L;

                    boolean isEventFull = (capacity > 0 && registeredCount >= capacity);

                    // Waitlist logic
                    if (isEventFull && !optInWaitlist) {
                        Toast.makeText(this,
                                "Sorry, this event just filled up!",
                                Toast.LENGTH_LONG).show();
                        setLoading(false);
                        return;
                    }

                    boolean isWaitlist = isEventFull && optInWaitlist;

                    if (selectedImageUri != null) {
                        encodeImageAndSubmit(isWaitlist);
                    } else {
                        createPaymentDocument(null, isWaitlist);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to verify event capacity.",
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Encodes image to Base64 before storing in Firestore
     */
    private void encodeImageAndSubmit(boolean isWaitlist) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1)
                baos.write(buffer, 0, bytesRead);

            inputStream.close();

            byte[] imageBytes = baos.toByteArray();

            // File size validation
            if (imageBytes.length > 700_000) {
                Toast.makeText(this,
                        "Image is too large (< 700 KB required).",
                        Toast.LENGTH_LONG).show();
                setLoading(false);
                return;
            }

            String base64Image =
                    "data:image/jpeg;base64," +
                            Base64.encodeToString(imageBytes, Base64.DEFAULT);

            createPaymentDocument(base64Image, isWaitlist);

        } catch (Exception e) {
            Toast.makeText(this,
                    "Failed to read image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            setLoading(false);
        }
    }

    /**
     * Creates payment document in Firestore
     */
    private void createPaymentDocument(String screenshotData, boolean isWaitlist) {

        String status = selectedMethod.equals(Payment.METHOD_CASH)
                ? Payment.STATUS_REGISTERED
                : Payment.STATUS_VERIFICATION_PENDING;

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("studentId", currentUser.getUid());
        paymentData.put("studentEmail", currentUser.getEmail());
        paymentData.put("eventId", eventId);
        paymentData.put("eventName", eventTitle);
        paymentData.put("paymentMethod", selectedMethod);
        paymentData.put("amount", ticketPrice);
        paymentData.put("status", status);
        paymentData.put("timestamp", FieldValue.serverTimestamp());
        paymentData.put("assignedTo", "eventManager");

        if (screenshotData != null) {
            paymentData.put("screenshotUrl", screenshotData);
        }

        // Fetch user details before saving
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {

                    String name = userDoc.getString("name");
                    String rollNo = userDoc.getString("rollNo");

                    paymentData.put("studentName",
                            name != null ? name : "Student");

                    if (rollNo != null)
                        paymentData.put("studentRollNo", rollNo);

                    writePaymentAndRegister(paymentData, status, isWaitlist);
                })
                .addOnFailureListener(e -> {
                    paymentData.put("studentName", "Student");
                    writePaymentAndRegister(paymentData, status, isWaitlist);
                });
    }

    /**
     * Writes payment and proceeds with RSVP logic
     */
    private void writePaymentAndRegister(
            Map<String, Object> paymentData,
            String status,
            boolean isWaitlist) {

        db.collection("payments")
                .add(paymentData)
                .addOnSuccessListener(docRef -> {

                    String paymentId = docRef.getId();
                    docRef.update("paymentId", paymentId);

                    if (Payment.STATUS_REGISTERED.equals(status)) {
                        registerStudentForEvent(paymentId, status, isWaitlist);
                    } else {
                        updateRsvpRecord(paymentId, status, isWaitlist);

                        sendNotificationToStudent(
                                "Payment Submitted ✅",
                                isWaitlist
                                        ? "You are on waitlist."
                                        : "Awaiting verification.");

                        setLoading(false);
                        navigateToStatus(paymentId, status);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to submit payment: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Registers student immediately for CASH payments
     */
    private void registerStudentForEvent(
            String paymentId,
            String status,
            boolean isWaitlist) {

        String rsvpDocId = currentUser.getUid() + "_" + eventId;
        String finalStatus = isWaitlist ? "waitlisted" : "confirmed";

        Map<String, Object> rsvp = new HashMap<>();
        rsvp.put("userId", currentUser.getUid());
        rsvp.put("eventId", eventId);
        rsvp.put("eventName", eventTitle);
        rsvp.put("status", finalStatus);
        rsvp.put("paymentId", paymentId);
        rsvp.put("paymentStatus", status);
        rsvp.put("paymentMethod", selectedMethod);
        rsvp.put("createdAt", FieldValue.serverTimestamp());

        // Consent flags
        rsvp.put("isNameVisible", isNameVisible);
        rsvp.put("isRollNoVisible", isRollNoVisible);
        rsvp.put("optInWaitlist", optInWaitlist);

        db.collection("rsvps").document(rsvpDocId)
                .set(rsvp, SetOptions.merge())
                .addOnSuccessListener(v -> {

                    sendNotificationToStudent(
                            isWaitlist ? "⏳ Waitlisted" : "🎉 Confirmed",
                            isWaitlist
                                    ? "You are waitlisted."
                                    : "You are registered!");

                    setLoading(false);
                    navigateToStatus(paymentId, status);
                });
    }

    /**
     * Updates RSVP for online payments
     */
    private void updateRsvpRecord(
            String paymentId,
            String paymentStatus,
            boolean isWaitlist) {

        String rsvpDocId = currentUser.getUid() + "_" + eventId;

        Map<String, Object> rsvp = new HashMap<>();
        rsvp.put("userId", currentUser.getUid());
        rsvp.put("eventId", eventId);
        rsvp.put("eventName", eventTitle);
        rsvp.put("status", isWaitlist ? "waitlisted" : "payment_pending");
        rsvp.put("paymentId", paymentId);
        rsvp.put("paymentStatus", paymentStatus);
        rsvp.put("paymentMethod", selectedMethod);
        rsvp.put("createdAt", FieldValue.serverTimestamp());

        rsvp.put("isNameVisible", isNameVisible);
        rsvp.put("isRollNoVisible", isRollNoVisible);
        rsvp.put("optInWaitlist", optInWaitlist);

        db.collection("rsvps").document(rsvpDocId)
                .set(rsvp, SetOptions.merge());
    }

    /**
     * Sends notification to user
     */
    private void sendNotificationToStudent(String title, String message) {
        Map<String, Object> notif = new HashMap<>();
        notif.put("title", title);
        notif.put("message", message);
        notif.put("type", "payment");
        notif.put("eventId", eventId);
        notif.put("eventName", eventTitle);
        notif.put("read", false);
        notif.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users")
                .document(currentUser.getUid())
                .collection("notifications")
                .add(notif);
    }

    /**
     * Navigates to payment status screen
     */
    private void navigateToStatus(String paymentId, String status) {
        Intent intent = new Intent(this, PaymentStatusActivity.class);
        intent.putExtra(PaymentStatusActivity.KEY_PAYMENT_ID, paymentId);
        intent.putExtra(PaymentStatusActivity.KEY_STATUS, status);
        intent.putExtra(PaymentStatusActivity.KEY_METHOD, selectedMethod);
        intent.putExtra(PaymentStatusActivity.KEY_EVENT_NAME, eventTitle);
        intent.putExtra(PaymentStatusActivity.KEY_AMOUNT, ticketPrice);
        startActivity(intent);
        finish();
    }

    /**
     * Controls loading UI state
     */
    private void setLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (btnSubmit != null) {
            btnSubmit.setEnabled(!loading);
            btnSubmit.setText(loading ? "Processing…" : "Submit Payment");
        }
    }
}