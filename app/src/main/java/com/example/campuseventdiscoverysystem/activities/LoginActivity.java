package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

/**
 * Activity responsible for authenticating users based on their selected role.
 * This class handles Firebase Email/Password authentication, password resets,
 * and performs a secondary role-validation check against Firestore to ensure
 * users access the correct dashboard.
 */
public class LoginActivity extends AppCompatActivity {

    /** Instance of Firebase Auth used for sign-in and password reset operations. */
    private FirebaseAuth mAuth;
    /** Instance of Firebase Firestore used to verify user roles. */
    private FirebaseFirestore db;
    /** The role selected by the user in the previous activity (e.g., "admin", "student", "event_manager"). */
    private String selectedRole;

    /**
     * Initializes the activity, retrieves the intent data, and triggers UI styling.
     * @param savedInstanceState If the activity is being re-initialized, this contains the most recent data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");

        Log.d("LOGIN_DEBUG", "Activity started. Selected role: " + selectedRole);

        applyRoleTheme();
        setupClickListeners();
    }

    /**
     * Dynamically updates the UI colors, text labels, and backgrounds based on
     * the {@code selectedRole}. This ensures the login screen matches the branding
     * of the specific user type (Admin, Student, or Event Manager).
     */
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

    /**
     * Sets up listeners for all interactive elements, including the login button,
     * back button, forgot password text, and sign-up navigation.
     */
    private void setupClickListeners() {
        Button btnLogin = findViewById(R.id.btnLogin);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp = findViewById(R.id.tvSignUp);

        btnBack.setOnClickListener(v -> finish());

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Sign Up navigation
        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(this, SignupActivity.class);
                intent.putExtra("role", selectedRole);
                startActivity(intent);
            });
        }

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
            Log.d("LOGIN_DEBUG", "Attempting login for: " + email);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Log.d("LOGIN_DEBUG", "Auth success. UID: " + uid);
                        verifyRoleAndNavigate(uid, btnLogin);
                    })
                    .addOnFailureListener(e -> {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        String msg = e.getMessage();
                        Log.e("LOGIN_DEBUG", "Auth failed: " + msg);
                        if (msg != null && msg.contains("no user record")) {
                            msg = "No account found with this email.";
                        } else if (msg != null && msg.contains("password is invalid")) {
                            msg = "Incorrect password. Please try again.";
                        }
                        Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                    });
        });
    }

    /**
     * Displays an AlertDialog that prompts the user for their email address
     * to send a Firebase password reset link.
     */
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
                                            .setMessage("A password reset link has been sent to " + email + ".")
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

    /**
     * Cross-references the authenticated UID with the users collection in Firestore.
     * If the role in the database does not match the role selected during login,
     * the user is signed out to prevent unauthorized access to the wrong dashboard.
     * * @param uid The unique Firebase Authentication ID of the user.
     * @param btnLogin Reference to the login button to re-enable it if verification fails.
     */
    private void verifyRoleAndNavigate(String uid, Button btnLogin) {
        Log.d("LOGIN_DEBUG", "Verifying role for UID: " + uid);
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Log.e("LOGIN_DEBUG", "No Firestore document for UID: " + uid);
                        Toast.makeText(this,
                                "Account not found in database. Please sign up first.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Login");
                        return;
                    }

                    String dbRole = doc.getString("role");
                    Log.d("LOGIN_DEBUG", "Found role in DB: " + dbRole + ". Expected: " + selectedRole);

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
                        case "student":
                            intent = new Intent(this, StudentHomeActivity.class);
                            break;
                        case "event_manager":
                            intent = new Intent(this, EventManagerDashboardActivity.class);
                            break;
                        default:
                            Log.e("LOGIN_DEBUG", "Unknown role string: " + dbRole);
                            Toast.makeText(this, "Unknown role. Contact admin.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                            btnLogin.setEnabled(true);
                            btnLogin.setText("Login");
                            return;
                    }

                    Log.d("LOGIN_DEBUG", "Navigating to: " + intent.getComponent().getClassName());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("LOGIN_DEBUG", "Firestore error: " + e.getMessage());
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Login");
                });
    }
}