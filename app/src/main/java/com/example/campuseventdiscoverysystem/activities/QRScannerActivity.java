package com.example.campuseventdiscoverysystem.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Event Manager QR scanner.
 * Reads a student's personal QR code (format: "userId_eventId"),
 * finds their RSVP in the top-level rsvps collection,
 * and marks checkedIn = true.
 */
public class QRScannerActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvStatus;
    private TextView tvLastScanned;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) return; // cancelled
                processScannedCode(result.getContents().trim());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        db            = FirebaseFirestore.getInstance();
        tvStatus      = findViewById(R.id.tvScanStatus);
        tvLastScanned = findViewById(R.id.tvLastScanned);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnScan).setOnClickListener(v -> startScan());
    }

    private void startScan() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan student's check-in QR code");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);
        scanLauncher.launch(options);
    }

    /**
     * Expected format: "userId_eventId"
     * Looks up rsvps/{userId_eventId} and sets checkedIn = true.
     */
    private void processScannedCode(String content) {
        // Validate format — must contain exactly one underscore separating two non-empty parts
        // userId itself may contain underscores so we split on the LAST underscore
        int lastUnderscore = content.lastIndexOf("_");
        if (lastUnderscore <= 0 || lastUnderscore == content.length() - 1) {
            tvStatus.setText("❌ Invalid QR code");
            tvLastScanned.setText("Scanned: " + content);
            return;
        }

        String userId  = content.substring(0, lastUnderscore);
        String eventId = content.substring(lastUnderscore + 1);
        String rsvpDocId = userId + "_" + eventId;

        tvStatus.setText("Checking...");
        tvLastScanned.setText("Scanned RSVP: " + rsvpDocId);

        db.collection("rsvps").document(rsvpDocId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        tvStatus.setText("❌ No RSVP found for this student");
                        Toast.makeText(this, "Student has no RSVP for this event",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Boolean alreadyCheckedIn = doc.getBoolean("checkedIn");
                    if (Boolean.TRUE.equals(alreadyCheckedIn)) {
                        String name = doc.getString("eventName");
                        tvStatus.setText("⚠️ Already checked in"
                                + (name != null ? " for " + name : ""));
                        Toast.makeText(this, "Already checked in!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> update = new HashMap<>();
                    update.put("checkedIn",   true);
                    update.put("checkedInAt", FieldValue.serverTimestamp());

                    db.collection("rsvps").document(rsvpDocId)
                            .update(update)
                            .addOnSuccessListener(unused -> {
                                String name = doc.getString("eventName");
                                tvStatus.setText("✅ Checked in"
                                        + (name != null ? " for " + name : ""));
                                Toast.makeText(this, "✅ Check-in successful!",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                tvStatus.setText("❌ Failed to check in");
                                Toast.makeText(this, "Error: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("❌ Could not reach server");
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}