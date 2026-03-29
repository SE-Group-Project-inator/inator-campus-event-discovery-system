package com.example.campuseventdiscoverysystem;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🔥 TEST Firebase connection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").get()
                .addOnSuccessListener(snap ->
                        Log.d("FIREBASE_TEST", "✅ Firebase works! Docs: " + snap.size()))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE_TEST", "❌ Error: " + e.getMessage()));
    }
}
