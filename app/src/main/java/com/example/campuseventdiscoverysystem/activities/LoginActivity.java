package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp); // Add this TextView to your layout

        btnBack.setOnClickListener(v -> finish());

        // ── FIX 1: Forgot Password ──
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // ── FIX 2: Sign Up navigation ──
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(this, SignupActivity.class);
                intent.putExtra("role", selectedRole);
                startActivity(intent);
            });
        }

        // ── FIX 3: Login button (was missing signup/student/event_manager routing) ──
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                etEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
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
                        // Show friendlier error messages
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("no user record")) {
                            msg = "No account found with this email.";
                        } else if (msg != null && msg.contains("password is invalid")) {
                            msg = "Incorrect password. Please try again.";
                        }
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                    });
        });
    }

    // ── FIX 1: Forgot Password dialog ──
    private void showForgotPasswordDialog() {
        EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        emailInput.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("We'll send a password reset link to your email.")
                .setView(emailInput)
                .setPositiveButton("Send Link", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(unused ->
                                    new AlertDialog.Builder(this)
                                            .setTitle("Email Sent!")
                                            .setMessage("A password reset link has been sent to " + email + ".\n\nOpen the link in your email, set a new password, then come back and log in.")
                                            .setPositiveButton("Got it", null)
                                            .show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── FIX 3: Complete role routing ──
    private void verifyRoleAndNavigate(String uid, Button btnLogin) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "Account not found in database. Please sign up first.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    String dbRole = doc.getString("role");

                    if (dbRole == null || !dbRole.equals(selectedRole)) {
                        Toast.makeText(this,
                                "Wrong role selected! Your account is registered as: " + dbRole,
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
                        case "event_manager":
                            // TODO: Replace with EventManagerDashboardActivity when your teammate builds it
                            intent = new Intent(this, EventsListActivity.class);
                            intent.putExtra("role", "event_manager");
                            break;
                        case "student":
                            // TODO: Replace with StudentDashboardActivity when your teammate builds it
                            intent = new Intent(this, EventsListActivity.class);
                            intent.putExtra("role", "student");
                            break;
                        default:
                            Toast.makeText(this, "Unknown role. Contact admin.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Login");
                            return;
                    }

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                });
    }
}
