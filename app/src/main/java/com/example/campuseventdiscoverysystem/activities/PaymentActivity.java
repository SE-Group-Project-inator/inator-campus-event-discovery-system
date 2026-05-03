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

public class PaymentActivity extends AppCompatActivity {

    public static final String KEY_EVENT_ID     = "eventId";
    public static final String KEY_EVENT_TITLE  = "eventTitle";
    public static final String KEY_EVENT_DATE   = "eventDate";
    public static final String KEY_TICKET_PRICE = "ticketPrice";

    private static final String JAZZCASH_NUMBER  = "0300-1234567";
    private static final String EASYPAISA_NUMBER = "0301-7654321";

    private TextView tvEventName, tvEventDate, tvTicketPrice;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCash, rbJazzCash, rbEasypaisa;
    private CardView cardCashInfo, cardOnlineInstructions, cardScreenshot;
    private TextView tvAccountNumber, tvInstructionAmount, tvPaymentMethodName;
    private ImageView ivScreenshotPreview;
    private TextView tvUploadHint;
    private MaterialButton btnUploadScreenshot, btnSubmit;
    private ProgressBar progressBar;

    private String eventId, eventTitle, eventDate;
    private double ticketPrice;
    private Uri selectedImageUri;
    private String selectedMethod = Payment.METHOD_CASH;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private boolean isNameVisible, isRollNoVisible, optInWaitlist;

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

        db          = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        readIntentExtras();
        bindViews();
        populateEventSummary();
        setupPaymentMethodSelector();
        setupUploadButton();
        setupSubmitButton();
        setupBackButton();
    }

    private void readIntentExtras() {
        Intent in   = getIntent();
        eventId     = in.getStringExtra(KEY_EVENT_ID);
        eventTitle  = in.getStringExtra(KEY_EVENT_TITLE);
        eventDate   = in.getStringExtra(KEY_EVENT_DATE);
        ticketPrice = in.getDoubleExtra(KEY_TICKET_PRICE, 0.0);

        // Read the consent checkboxes
        isNameVisible   = in.getBooleanExtra("isNameVisible", false);
        isRollNoVisible = in.getBooleanExtra("isRollNoVisible", false);
        optInWaitlist   = in.getBooleanExtra("optInWaitlist", false);
    }

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

    private void populateEventSummary() {
        if (tvEventName  != null && eventTitle != null) tvEventName.setText(eventTitle);
        if (tvEventDate  != null && eventDate  != null) tvEventDate.setText("📅 " + eventDate);
        if (tvTicketPrice != null) {
            tvTicketPrice.setText(ticketPrice > 0
                    ? "PKR " + NumberFormat.getInstance().format((long) ticketPrice)
                    : "Free");
        }
    }

    private void setupPaymentMethodSelector() {
        showCashSection();
        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if      (checkedId == R.id.rbCash)       { selectedMethod = Payment.METHOD_CASH;      showCashSection(); }
            else if (checkedId == R.id.rbJazzCash)   { selectedMethod = Payment.METHOD_JAZZCASH;  showOnlineSection(JAZZCASH_NUMBER,  "📱 JazzCash");  }
            else if (checkedId == R.id.rbEasypaisa)  { selectedMethod = Payment.METHOD_EASYPAISA; showOnlineSection(EASYPAISA_NUMBER, "💚 Easypaisa"); }
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

    private void setupSubmitButton() {
        if (btnSubmit == null) return;
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void validateAndSubmit() {
        if (!selectedMethod.equals(Payment.METHOD_CASH) && selectedImageUri == null) {
            Toast.makeText(this, "Please upload your payment screenshot before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);

        // Check Capacity before doing anything else
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long capacity = documentSnapshot.getLong("capacity");
                    Long registeredCount = documentSnapshot.getLong("registeredCount");

                    if (capacity == null) capacity = 0L;
                    if (registeredCount == null) registeredCount = 0L;

                    boolean isEventFull = (capacity > 0 && registeredCount >= capacity);

                    // If event is full and they didn't check the waitlist box, abort!
                    if (isEventFull && !optInWaitlist) {
                        Toast.makeText(this, "Sorry, this event just filled up!", Toast.LENGTH_LONG).show();
                        setLoading(false);
                        return;
                    }

                    boolean isWaitlist = isEventFull && optInWaitlist;

                    // Proceed to image processing / document creation
                    if (selectedImageUri != null) {
                        encodeImageAndSubmit(isWaitlist);
                    } else {
                        createPaymentDocument(null, isWaitlist);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to verify event capacity.", Toast.LENGTH_SHORT).show();
                });
    }

    private void encodeImageAndSubmit(boolean isWaitlist) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            assert inputStream != null;
            while ((bytesRead = inputStream.read(buffer)) != -1) baos.write(buffer, 0, bytesRead);
            inputStream.close();

            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length > 700_000) {
                Toast.makeText(this, "Image is too large. Please choose a smaller screenshot (< 700 KB).", Toast.LENGTH_LONG).show();
                setLoading(false);
                return;
            }
            String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.DEFAULT);
            createPaymentDocument(base64Image, isWaitlist);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to read image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            setLoading(false);
        }
    }

    private void createPaymentDocument(String screenshotData, boolean isWaitlist) {
        // Cash = immediately registered; online = pending manager verification
        String status = selectedMethod.equals(Payment.METHOD_CASH)
                ? Payment.STATUS_REGISTERED
                : Payment.STATUS_VERIFICATION_PENDING;

        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("studentId",     currentUser.getUid());
        paymentData.put("studentEmail",  currentUser.getEmail());
        paymentData.put("eventId",       eventId != null ? eventId : "");
        paymentData.put("eventName",     eventTitle != null ? eventTitle : "");
        paymentData.put("paymentMethod", selectedMethod);
        paymentData.put("amount",        ticketPrice);
        paymentData.put("status",        status);
        paymentData.put("timestamp",     FieldValue.serverTimestamp());
        paymentData.put("assignedTo",    "eventManager");

        if (screenshotData != null) {
            paymentData.put("screenshotUrl", screenshotData);
        }

        // Fetch student name, then write everything
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(userDoc -> {
                    String name = userDoc.exists() ? userDoc.getString("name") : null;
                    String rollNo = userDoc.exists() ? userDoc.getString("rollNo") : null;
                    paymentData.put("studentName", name != null ? name
                            : (currentUser.getEmail() != null ? currentUser.getEmail().split("@")[0] : "Student"));
                    if (rollNo != null) paymentData.put("studentRollNo", rollNo);
                    writePaymentAndRegister(paymentData, status, isWaitlist);
                })
                .addOnFailureListener(e -> {
                    paymentData.put("studentName", "Student");
                    writePaymentAndRegister(paymentData, status, isWaitlist);
                });
    }

    private void writePaymentAndRegister(Map<String, Object> paymentData, String status, boolean isWaitlist) {
        db.collection("payments")
                .add(paymentData)
                .addOnSuccessListener(docRef -> {
                    String paymentId = docRef.getId();
                    docRef.update("paymentId", paymentId);

                    if (Payment.STATUS_REGISTERED.equals(status)) {
                        // Cash: directly register student
                        registerStudentForEvent(paymentId, status, isWaitlist);
                    } else {
                        // Online: just record the payment, manager verifies later
                        updateRsvpRecord(paymentId, status, isWaitlist);
                        String msg = isWaitlist
                                ? "Your payment is submitted. You are on the waitlist pending verification."
                                : "Your payment has been submitted. Awaiting verification.";
                        sendNotificationToStudent("Payment Submitted ✅", msg);
                        setLoading(false);
                        navigateToStatus(paymentId, status);
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to submit payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * For CASH payments: directly confirm the RSVP, add to event_attendees,
     * increment count, send notification, navigate to success.
     */
    private void registerStudentForEvent(String paymentId, String status, boolean isWaitlist) {
        String rsvpDocId = currentUser.getUid() + "_" + eventId;
        String finalStatus = isWaitlist ? "waitlisted" : "confirmed";

        Map<String, Object> rsvp = new HashMap<>();
        rsvp.put("userId",         currentUser.getUid());
        rsvp.put("eventId",        eventId);
        rsvp.put("eventName",      eventTitle != null ? eventTitle : "");
        rsvp.put("status",         finalStatus); // Set to waitlisted or confirmed
        rsvp.put("paymentId",      paymentId);
        rsvp.put("paymentStatus",  status);
        rsvp.put("paymentMethod",  selectedMethod);
        rsvp.put("createdAt",      FieldValue.serverTimestamp());

        // Save Consents
        rsvp.put("isNameVisible",   isNameVisible);
        rsvp.put("isRollNoVisible", isRollNoVisible);
        rsvp.put("optInWaitlist",   optInWaitlist);

        // Use set() with merge to avoid permission errors on missing doc
        db.collection("rsvps").document(rsvpDocId)
                .set(rsvp, SetOptions.merge())
                .addOnSuccessListener(v -> {

                    if (!isWaitlist) {
                        // Only increment count and add to roster if they secured a spot
                        // Recalculate from actual rsvps to stay accurate
                        db.collection("rsvps")
                                .whereEqualTo("eventId", eventId)
                                .whereEqualTo("status", "confirmed")
                                .get()
                                .addOnSuccessListener(snap -> {
                                    int trueCount = snap.size();
                                    // Clamp to [0, capacity]
                                    db.collection("events").document(eventId).get()
                                            .addOnSuccessListener(evDoc -> {
                                                Long cap = evDoc.getLong("capacity");
                                                int clamped = cap != null && cap > 0
                                                        ? Math.min(trueCount, cap.intValue()) : trueCount;
                                                db.collection("events").document(eventId)
                                                        .update("registeredCount", Math.max(0, clamped));
                                            });
                                });

                        // Write to event_attendees
                        Map<String, Object> attendee = new HashMap<>();
                        attendee.put("userId",       currentUser.getUid());
                        attendee.put("studentEmail", currentUser.getEmail());
                        attendee.put("joinedAt",     FieldValue.serverTimestamp());
                        db.collection("event_attendees")
                                .document(eventId)
                                .collection("attendees")
                                .document(currentUser.getUid())
                                .set(attendee, SetOptions.merge());

                        // Send success notification
                        sendNotificationToStudent("🎉 Registration Confirmed!",
                                "You are registered for \"" + eventTitle + "\". Pay cash at the entrance. See you there!");
                    } else {
                        // Notification for waitlist
                        sendNotificationToStudent("⏳ Waitlisted",
                                "You are on the waitlist for \"" + eventTitle + "\". You will be notified if a spot opens up.");
                    }

                    setLoading(false);
                    navigateToStatus(paymentId, status);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * For online payments
     */
    private void updateRsvpRecord(String paymentId, String paymentStatus, boolean isWaitlist) {
        if (eventId == null || currentUser == null) return;
        String rsvpDocId = currentUser.getUid() + "_" + eventId;
        String finalStatus = isWaitlist ? "waitlisted" : "payment_pending";

        Map<String, Object> rsvp = new HashMap<>();
        rsvp.put("userId",        currentUser.getUid());
        rsvp.put("eventId",       eventId);
        rsvp.put("eventName",     eventTitle != null ? eventTitle : "");
        rsvp.put("status",        finalStatus); // Set to waitlisted or payment_pending
        rsvp.put("paymentId",     paymentId);
        rsvp.put("paymentStatus", paymentStatus);
        rsvp.put("paymentMethod", selectedMethod);
        rsvp.put("createdAt",     FieldValue.serverTimestamp());

        // Save Consents
        rsvp.put("isNameVisible",   isNameVisible);
        rsvp.put("isRollNoVisible", isRollNoVisible);
        rsvp.put("optInWaitlist",   optInWaitlist);

        db.collection("rsvps").document(rsvpDocId).set(rsvp, SetOptions.merge());
    }

    /**
     * Sends an in-app notification to the student's notifications sub-collection.
     */
    private void sendNotificationToStudent(String title, String message) {
        if (currentUser == null) return;
        Map<String, Object> notif = new HashMap<>();
        notif.put("title",     title);
        notif.put("message",   message);
        notif.put("type",      "payment");
        notif.put("eventId",   eventId != null ? eventId : "");
        notif.put("eventName", eventTitle != null ? eventTitle : "");
        notif.put("read",      false);
        notif.put("timestamp", FieldValue.serverTimestamp());

        db.collection("users").document(currentUser.getUid())
                .collection("notifications")
                .add(notif);
    }

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

    private String getMethodLabel() {
        switch (selectedMethod) {
            case Payment.METHOD_JAZZCASH:  return "JazzCash";
            case Payment.METHOD_EASYPAISA: return "Easypaisa";
            default: return "Cash";
        }
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnSubmit   != null) btnSubmit.setEnabled(!loading);
        if (btnSubmit   != null) btnSubmit.setText(loading ? "Processing…" : "Submit Payment");
    }
}
