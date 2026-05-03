package com.example.campuseventdiscoverysystem.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Shows the student's personal check-in QR code for a specific event.
 * The QR encodes "userId_eventId" — the event manager scans this
 * with QRScannerActivity to mark the student as checked in.
 */
public class StudentQRActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_qr);

        String userId     = getIntent().getStringExtra("userId");
        String eventId    = getIntent().getStringExtra("eventId");
        String eventTitle = getIntent().getStringExtra("eventTitle");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvEventTitle);
        if (tvTitle != null && eventTitle != null) tvTitle.setText(eventTitle);

        if (userId == null || eventId == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // QR content = "userId_eventId" — matches what QRScannerActivity expects
        String qrContent = userId + "_" + eventId;
        generateQR(qrContent);
    }

    private void generateQR(String content) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.encodeBitmap(content,
                    BarcodeFormat.QR_CODE, 600, 600);
            ((ImageView) findViewById(R.id.ivQRCode)).setImageBitmap(bitmap);
        } catch (WriterException e) {
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show();
        }
    }
}