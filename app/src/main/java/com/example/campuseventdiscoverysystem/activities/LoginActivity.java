package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");

        applyRoleTheme();
        setupClickListeners();
    }

    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRole = findViewById(R.id.tvRoleTitle);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        TextView tvSignIn = findViewById(R.id.tvSignInLabel);
        Button btnLogin = findViewById(R.id.btnLogin);
        ScrollView scrollView = findViewById(R.id.scrollView);
        androidx.cardview.widget.CardView loginCard = findViewById(R.id.loginCard);

        if (selectedRole == null) return;

        switch (selectedRole) {
            case "admin":
                topBar.setBackgroundColor(getColor(R.color.btn_admin));
                tvRole.setText("Admin");
                tvRole.setTextColor(getColor(R.color.white));
                scrollView.setBackgroundColor(getColor(R.color.bg_beige));
                loginCard.setCardBackgroundColor(getColor(R.color.card_tan));
                tvSubtitle.setText("Monitor and approve campus events.");
                tvSignIn.setText("Sign in with your admin account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_admin));

                break;
            case "event_manager":
                topBar.setBackgroundColor(getColor(R.color.btn_eventmgr));
                tvRole.setText("Event Manager");
                tvRole.setTextColor(getColor(R.color.white));
                scrollView.setBackgroundColor(getColor(R.color.bg_event));
                loginCard.setCardBackgroundColor(getColor(R.color.card_event));
                tvSubtitle.setText("Create and manage your own events!");
                tvSignIn.setText("Sign in with your university account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_eventmgr));
                break;
            case "student":
                topBar.setBackgroundColor(getColor(R.color.btn_student));
                tvRole.setText("Student");
                tvRole.setTextColor(getColor(R.color.white));
                scrollView.setBackgroundColor(getColor(R.color.bg_student));
                loginCard.setCardBackgroundColor(getColor(R.color.card_student));
                tvSubtitle.setText("Browse and register for your favourite events!");
                tvSignIn.setText("Sign in with your university account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_student));
                break;
        }
    }

    private void setupClickListeners() {
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                etPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            btnLogin.setText("Signing in...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        verifyRoleAndNavigate(uid, btnLogin);
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        Toast.makeText(this,
                                "Login failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }

    //
//    private void verifyRoleAndNavigate(String uid, Button btnLogin) {
//        db.collection("users").document(uid).get()
//                .addOnSuccessListener(doc -> {
//                    if (!doc.exists()) {
//                        Toast.makeText(this,
//                                "Account not found. Contact admin.",
//                                Toast.LENGTH_LONG).show();
//                        mAuth.signOut();
//                        btnLogin.setEnabled(true);
//                        btnLogin.setText("Login");
//                        return;
//                    }
//
//                    String dbRole = doc.getString("role");
//
//                    if (dbRole == null || !dbRole.equals(selectedRole)) {
//                        Toast.makeText(this,
//                                "Wrong role! You are registered as: " + dbRole,
//                                Toast.LENGTH_LONG).show();
//                        mAuth.signOut();
//                        btnLogin.setEnabled(true);
//                        btnLogin.setText("Login");
//                        return;
//                    }
//
//                    // Navigate based on role — teammates will replace these
//                    // with their own activities as they build them
//                    // Navigate based on role
//                    Intent intent;
//                    switch (dbRole) {
//                        case "admin":
//                            intent = new Intent(this, AdminDashboardActivity.class);
//                            break;
//                        default:
//                            // Teammates will replace with their own activities
//                            Toast.makeText(this,
//                                    "Welcome! Logged in as " + dbRole,
//                                    Toast.LENGTH_SHORT).show();
//                            btnLogin.setEnabled(true);
//                            btnLogin.setText("Login");
//                            return;
//                    }
//                    startActivity(intent);
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this,
//                            "Error: " + e.getMessage(),
//                            Toast.LENGTH_SHORT).show();
//                    btnLogin.setEnabled(true);
//                    btnLogin.setText("Login");
//                });
//    }
    private void verifyRoleAndNavigate(String uid, Button btnLogin) {
        android.util.Log.d("LOGIN", "Checking UID: " + uid);
        android.util.Log.d("LOGIN", "Selected role: " + selectedRole);

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    android.util.Log.d("LOGIN", "Doc exists: " + doc.exists());

                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Account not found. Contact admin.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    String dbRole = doc.getString("role");
                    android.util.Log.d("LOGIN", "DB role: " + dbRole);
                    android.util.Log.d("LOGIN", "Selected role: " + selectedRole);

                    if (dbRole == null || !dbRole.equals(selectedRole)) {
                        Toast.makeText(this,
                                "Wrong role! You are registered as: " + dbRole,
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    Intent intent;
                    switch (dbRole) {
                        case "admin":
                            intent = new Intent(this, AdminDashboardActivity.class);
                            break;
                        case "student":
                            intent = new Intent(this, StudentHomeActivity.class);
                            break;
                        case "event_manager":
                            intent = new Intent(this, EventManagerDashboardActivity.class);
                            break;
                        default:
                            Toast.makeText(this,
                                    "Welcome! Logged in as " + dbRole,
                                    Toast.LENGTH_SHORT).show();
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Login");
                            return;
                    }
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("LOGIN", "Error: " + e.getMessage());
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                });
    }
}
