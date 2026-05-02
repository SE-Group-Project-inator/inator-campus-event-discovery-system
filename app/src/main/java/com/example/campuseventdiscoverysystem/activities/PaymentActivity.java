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
 * PaymentActivity — Step 2 of the student event registration flow.
 *
 * Responsibilities:
 *  • Show event summary (name, date, ticket price)
 *  • Let student pick: Cash / JazzCash / Easypaisa
 *  • For online methods: show payment instructions + screenshot upload
 *  • Submit payment request to Firestore payments collection
 *  • Navigate to PaymentStatusActivity on success
 */
public class PaymentActivity extends AppCompatActivity {

    // ── Intent keys (callers must pass these) ─────────────────────────
    public static final String KEY_EVENT_ID    = "eventId";
    public static final String KEY_EVENT_TITLE = "eventTitle";
    public static final String KEY_EVENT_DATE  = "eventDate";
    public static final String KEY_TICKET_PRICE = "ticketPrice";

    // ── Account numbers shown in payment instructions ──────────────────
    private static final String JAZZCASH_NUMBER   = "0300-1234567";
    private static final String EASYPAISA_NUMBER  = "0301-7654321";

    // ── UI ─────────────────────────────────────────────────────────────
    private TextView tvEventName, tvEventDate, tvTicketPrice;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbJazzCash, rbEasypaisa;
    private CardView cardCashInfo, cardOnlineInstructions, cardScreenshot;
    private TextView tvAccountNumber, tvInstructionAmount, tvPaymentMethodName;
    private ImageView ivScreenshotPreview;
    private TextView tvUploadHint;
    private MaterialButton btnUploadScreenshot, btnSubmit;
    private ProgressBar progressBar;

    // ── State ──────────────────────────────────────────────────────────
    private String eventId, eventTitle, eventDate;
    private double ticketPrice;
    private Uri selectedImageUri;
    private String selectedMethod = Payment.METHOD_CASH;

    // ── Firebase ───────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // ── Image picker ───────────────────────────────────────────────────
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivScreenshotPreview.setImageURI(selectedImageUri);
                    ivScreenshotPreview.setVisibility(View.VISIBLE);
                    tvUploadHint.setText("Screenshot selected ✓");
                    tvUploadHint.setTextColor(getResources().getColor(R.color.green_accept, getTheme()));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        readIntentExtras();
        bindViews();
        populateEventSummary();
        setupPaymentMethodSelector();
        setupUploadButton();
        setupSubmitButton();
        setupBackButton();
    }

    // ── Initialisation ─────────────────────────────────────────────────

    private void readIntentExtras() {
        Intent in = getIntent();
        eventId     = in.getStringExtra(KEY_EVENT_ID);
        eventTitle  = in.getStringExtra(KEY_EVENT_TITLE);
        eventDate   = in.getStringExtra(KEY_EVENT_DATE);
        ticketPrice = in.getDoubleExtra(KEY_TICKET_PRICE, 0.0);
    }

    private void bindViews() {
        tvEventName          = findViewById(R.id.tvPaymentEventName);
        tvEventDate          = findViewById(R.id.tvPaymentEventDate);
        tvTicketPrice        = findViewById(R.id.tvPaymentTicketPrice);
        rgPaymentMethod      = findViewById(R.id.rgPaymentMethod);
        rbCash               = findViewById(R.id.rbCash);
        rbJazzCash           = findViewById(R.id.rbJazzCash);
        rbEasypaisa          = findViewById(R.id.rbEasypaisa);
        cardCashInfo         = findViewById(R.id.cardCashInfo);
        cardOnlineInstructions = findViewById(R.id.cardOnlineInstructions);
        cardScreenshot       = findViewById(R.id.cardScreenshot);
        tvAccountNumber      = findViewById(R.id.tvAccountNumber);
        tvInstructionAmount  = findViewById(R.id.tvInstructionAmount);
        tvPaymentMethodName  = findViewById(R.id.tvPaymentMethodName);
        ivScreenshotPreview  = findViewById(R.id.ivScreenshotPreview);
        tvUploadHint         = findViewById(R.id.tvUploadHint);
        btnUploadScreenshot  = findViewById(R.id.btnUploadScreenshot);
        btnSubmit            = findViewById(R.id.btnSubmitPayment);
        progressBar          = findViewById(R.id.progressBarPayment);
    }

    private void populateEventSummary() {
        if (tvEventName != null && eventTitle != null) tvEventName.setText(eventTitle);
        if (tvEventDate != null && eventDate != null) tvEventDate.setText(eventDate);
        if (tvTicketPrice != null) {
            if (ticketPrice > 0) {
                tvTicketPrice.setText("PKR " + NumberFormat.getInstance().format((long) ticketPrice));
            } else {
                tvTicketPrice.setText("Free");
            }
        }
    }

    private void setupPaymentMethodSelector() {
        // Default: Cash selected
        showCashSection();

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbCash) {
                selectedMethod = Payment.METHOD_CASH;
                showCashSection();
            } else if (checkedId == R.id.rbJazzCash) {
                selectedMethod = Payment.METHOD_JAZZCASH;
                showOnlineSection(JAZZCASH_NUMBER, "JazzCash");
            } else if (checkedId == R.id.rbEasypaisa) {
                selectedMethod = Payment.METHOD_EASYPAISA;
                showOnlineSection(EASYPAISA_NUMBER, "Easypaisa");
            }
        });
    }

    private void showCashSection() {
        cardCashInfo.setVisibility(View.VISIBLE);
        cardOnlineInstructions.setVisibility(View.GONE);
        cardScreenshot.setVisibility(View.GONE);
        selectedImageUri = null;
    }

    private void showOnlineSection(String accountNumber, String methodName) {
        cardCashInfo.setVisibility(View.GONE);
        cardOnlineInstructions.setVisibility(View.VISIBLE);
        cardScreenshot.setVisibility(View.VISIBLE);

        tvAccountNumber.setText(accountNumber);
        tvPaymentMethodName.setText(methodName);
        tvInstructionAmount.setText("PKR " + NumberFormat.getInstance().format((long) ticketPrice));
    }

    private void setupUploadButton() {
        if (btnUploadScreenshot == null) return;
        btnUploadScreenshot.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pick.setType("image/*");
            imagePickerLauncher.launch(pick);
        });
    }

    private void setupBackButton() {
        View btnBack = findViewById(R.id.btnPaymentBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    // ── Submission ─────────────────────────────────────────────────────

    private void setupSubmitButton() {
        if (btnSubmit == null) return;
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        // Online payment requires screenshot
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

        if (selectedImageUri != null) {
            // Encode image as base64 and save directly in Firestore (no Firebase Storage required)
            encodeImageAndSubmit();
        } else {
            // Cash payment — no image needed
            createPaymentDocument(null);
        }
    }

    /**
     * Reads image bytes, base64-encodes them, and calls createPaymentDocument.
     * This avoids requiring Firebase Storage (which needs billing setup),
     * storing a compact base64 string in Firestore instead.
     * For production, replace with Firebase Storage upload.
     */
    private void encodeImageAndSubmit() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            assert inputStream != null;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            inputStream.close();

            byte[] imageBytes = baos.toByteArray();
            // Limit to ~700KB to stay within Firestore doc size limit
            if (imageBytes.length > 700_000) {
                Toast.makeText(this,
                        "Image is too large. Please choose a smaller screenshot (< 700 KB).",
                        Toast.LENGTH_LONG).show();
                setLoading(false);
                return;
            }

            String base64Image = "data:image/jpeg;base64," +
                    Base64.encodeToString(imageBytes, Base64.DEFAULT);

            createPaymentDocument(base64Image);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to read image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setLoading(false);
        }
    }

    /**
     * Writes the payment request to Firestore.
     * Collection: payments
     * Document ID: auto-generated
     */
    private void createPaymentDocument(String screenshotDataOrUrl) {
        // Determine status based on payment method
        String status = selectedMethod.equals(Payment.METHOD_CASH)
                ? Payment.STATUS_PENDING_CASH
                : Payment.STATUS_VERIFICATION_PENDING;

        // Build the payment map
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("studentId",     currentUser.getUid());
        paymentData.put("studentEmail",  currentUser.getEmail());
        paymentData.put("eventId",       eventId);
        paymentData.put("eventName",     eventTitle != null ? eventTitle : "");
        paymentData.put("paymentMethod", selectedMethod);
        paymentData.put("amount",        ticketPrice);
        paymentData.put("status",        status);
        paymentData.put("timestamp",     FieldValue.serverTimestamp());
        paymentData.put("assignedTo",    "eventManager");

        if (screenshotDataOrUrl != null) {
            paymentData.put("screenshotUrl", screenshotDataOrUrl);
        }

        // Fetch student name from users collection, then write payment
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists() && userDoc.getString("name") != null) {
                        paymentData.put("studentName", userDoc.getString("name"));
                    } else {
                        paymentData.put("studentName",
                                currentUser.getEmail() != null
                                        ? currentUser.getEmail().split("@")[0]
                                        : "Student");
                    }
                    writePayment(paymentData, status);
                })
                .addOnFailureListener(e -> {
                    paymentData.put("studentName", "Student");
                    writePayment(paymentData, status);
                });
    }

    private void writePayment(Map<String, Object> paymentData, String status) {
        db.collection("payments")
                .add(paymentData)
                .addOnSuccessListener(docRef -> {
                    // Update the document with its own ID
                    docRef.update("paymentId", docRef.getId());

                    // Write to event_attendees so "Who is Attending" works
                    writeEventAttendee();

                    // Also update rsvp status to reflect payment submitted
                    updateRsvpPaymentStatus(docRef.getId(), status);

                    setLoading(false);
                    navigateToStatus(docRef.getId(), status);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to submit payment: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Writes userId: true into event_attendees/{eventId}/{userId}
     * This powers the "Who is Attending" feature.
     */
    private void writeEventAttendee() {
        if (eventId == null || currentUser == null) return;
        Map<String, Object> attendeeData = new HashMap<>();
        attendeeData.put("userId", currentUser.getUid());
        attendeeData.put("joinedAt", FieldValue.serverTimestamp());
        db.collection("event_attendees")
                .document(eventId)
                .collection("attendees")
                .document(currentUser.getUid())
                .set(attendeeData);
    }

    /**
     * Updates the corresponding RSVP document with payment status so
     * EventHistoryActivity can show consistent state.
     */
    private void updateRsvpPaymentStatus(String paymentId, String paymentStatus) {
        String rsvpDocId = currentUser.getUid() + "_" + eventId;
        db.collection("rsvps").document(rsvpDocId)
                .update(
                        "paymentId",     paymentId,
                        "paymentStatus", paymentStatus,
                        "paymentMethod", selectedMethod
                )
                .addOnFailureListener(e -> {
                    // If RSVP doc doesn't exist yet, create a minimal record
                    Map<String, Object> rsvp = new HashMap<>();
                    rsvp.put("userId",        currentUser.getUid());
                    rsvp.put("eventId",       eventId);
                    rsvp.put("eventName",     eventTitle);
                    rsvp.put("status",        "confirmed");
                    rsvp.put("paymentId",     paymentId);
                    rsvp.put("paymentStatus", paymentStatus);
                    rsvp.put("paymentMethod", selectedMethod);
                    rsvp.put("createdAt",     FieldValue.serverTimestamp());
                    db.collection("rsvps").document(rsvpDocId).set(rsvp);
                    // Increment registered count
                    db.collection("events").document(eventId)
                            .update("registeredCount", FieldValue.increment(1));
                });
    }

    // ── Navigation ─────────────────────────────────────────────────────

    private void navigateToStatus(String paymentId, String status) {
        Intent intent = new Intent(this, PaymentStatusActivity.class);
        intent.putExtra(PaymentStatusActivity.KEY_PAYMENT_ID, paymentId);
        intent.putExtra(PaymentStatusActivity.KEY_STATUS,     status);
        intent.putExtra(PaymentStatusActivity.KEY_METHOD,     selectedMethod);
        intent.putExtra(PaymentStatusActivity.KEY_EVENT_NAME, eventTitle);
        intent.putExtra(PaymentStatusActivity.KEY_AMOUNT,     ticketPrice);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnSubmit   != null) btnSubmit.setEnabled(!loading);
        if (loading && btnSubmit != null) btnSubmit.setText("Submitting…");
        else if (btnSubmit != null)       btnSubmit.setText("Submit Payment");
    }
}