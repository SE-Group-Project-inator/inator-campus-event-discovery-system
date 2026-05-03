package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * LoginActivity
 *
 * This activity handles user authentication for the Campus Event Discovery System.
 *
 * Responsibilities:
 * - Email/password login using Firebase Authentication
 * - Email verification enforcement
 * - Role-based access control (student, admin, event manager)
 * - Password reset flow
 * - Resend verification email flow
 * - Session initialization after successful login
 *
 * Security Features:
 * - Prevents login if email is not verified (except test accounts)
 * - Validates role from Firestore before granting access
 * - Clears back stack on successful login
 *
 * UI Features:
 * - Role-based theming (colors, labels, UI text)
 * - Loading state with ProgressBar
 * - Error feedback via Toast and dialogs
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private String            selectedRole;

    // UI references for login screen components
    private Button            btnLogin;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private ProgressBar       progressBar;

    /**
     * Predefined test accounts that bypass email verification requirement.
     * These are used for development/testing purposes only.
     */
    private static final String[] TEST_ACCOUNTS = {
            "student@lums.edu.pk",
            "admin@lums.edu.pk",
            "teststudent@lums.edu.pk",
            "testadmin@lums.edu.pk",
            "manager@lums.edu.pk"
    };

    // ─────────────────────────────────────────────────────────────────────────────
    // Lifecycle Methods
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Called when the activity is first created.
     *
     * Initializes Firebase, binds UI components, applies role-based UI theming,
     * and sets up click listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth        = FirebaseAuth.getInstance();
        db           = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");

        bindViews();
        applyRoleTheme();
        setupClickListeners();

        // Show session expiration message if redirected from SessionManager
        if (getIntent().getBooleanExtra("session_expired", false)) {
            Toast.makeText(this,
                    "Your session expired due to inactivity. Please log in again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // View Binding
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Binds XML layout views to Java variables.
     */
    private void bindViews() {
        btnLogin     = findViewById(R.id.btnLogin);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        progressBar  = findViewById(R.id.progressBar); // optional
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Role-based UI customization
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Applies UI theme based on selected role (student/admin/event manager).
     * Changes colors, labels, and messages dynamically.
     */
    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRole       = findViewById(R.id.tvRoleTitle);
        TextView tvSubtitle   = findViewById(R.id.tvSubtitle);
        TextView tvSignIn     = findViewById(R.id.tvSignInLabel);
        ScrollView scrollView = findViewById(R.id.scrollView);
        androidx.cardview.widget.CardView loginCard = findViewById(R.id.loginCard);

        if (selectedRole == null) return;

        switch (selectedRole) {
            case "admin":
                topBar.setBackgroundColor(getColor(R.color.btn_admin));
                tvRole.setText("Admin");
                scrollView.setBackgroundColor(getColor(R.color.bg_admin_login));
                loginCard.setCardBackgroundColor(getColor(R.color.card_admin_login));
                tvSubtitle.setText("Monitor and approve campus events.");
                tvSignIn.setText("Sign in with your admin account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_admin));
                break;

            case "event_manager":
                topBar.setBackgroundColor(getColor(R.color.btn_eventmgr));
                tvRole.setText("Event Manager");
                scrollView.setBackgroundColor(getColor(R.color.bg_event));
                loginCard.setCardBackgroundColor(getColor(R.color.card_event));
                tvSubtitle.setText("Create and manage your own events!");
                tvSignIn.setText("Sign in with your university account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_eventmgr));
                break;

            case "student":
            default:
                topBar.setBackgroundColor(getColor(R.color.btn_student));
                tvRole.setText("Student");
                scrollView.setBackgroundColor(getColor(R.color.bg_student));
                loginCard.setCardBackgroundColor(getColor(R.color.card_student));
                tvSubtitle.setText("Browse and register for your favourite events!");
                tvSignIn.setText("Sign in with your university account");
                btnLogin.setBackgroundTintList(getColorStateList(R.color.btn_student));
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Click Listeners
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Sets up all UI click listeners (login, signup, forgot password, back).
     */
    private void setupClickListeners() {
        ImageButton btnBack       = findViewById(R.id.btnBack);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvSignUp         = findViewById(R.id.tvSignUp);

        btnBack.setOnClickListener(v -> finish());

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        if (tvSignUp != null) {
            tvSignUp.setOnClickListener(v -> {
                Intent intent = new Intent(this, SignupActivity.class);
                intent.putExtra("role", selectedRole);
                startActivity(intent);
            });
        }

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Authentication Flow
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Attempts login using Firebase Authentication.
     *
     * Flow:
     * 1. Validate input
     * 2. Sign in with Firebase
     * 3. Reload user to ensure fresh emailVerified state
     * 4. Check email verification (unless test account)
     * 5. Validate role from Firestore
     */
    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();

                    if (user == null) {
                        setLoading(false);
                        showError("Unexpected error. Please try again.");
                        return;
                    }

                    // Force refresh user to get latest email verification state
                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser fresh = mAuth.getCurrentUser();

                        if (fresh == null) {
                            setLoading(false);
                            showError("Session error. Please try again.");
                            return;
                        }

                        String finalEmail = fresh.getEmail();

                        if (!fresh.isEmailVerified() && !isTestAccount(finalEmail)) {
                            mAuth.signOut();
                            setLoading(false);
                            showUnverifiedEmailDialog(email, password);
                        } else {
                            verifyRoleAndNavigate(fresh.getUid());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                    Log.e(TAG, "Login failed: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Unverified Email Handling
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Shows dialog when user email is not verified.
     * Allows resending verification email.
     */
    private void showUnverifiedEmailDialog(String email, String password) {
        new AlertDialog.Builder(this)
                .setTitle("📧 Email Not Verified")
                .setMessage("Please verify your email before logging in.")
                .setPositiveButton("Resend Verification Email", (d, w) ->
                        resendVerificationEmail(email, password))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Resends verification email after temporarily signing in.
     */
    private void resendVerificationEmail(String email, String password) {
        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();

                    if (user == null) return;

                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                mAuth.signOut();
                                setLoading(false);
                                Toast.makeText(this,
                                        "Verification email sent.",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                mAuth.signOut();
                                setLoading(false);
                                showError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Role Verification
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Verifies user role from Firestore before granting access.
     * Navigates to respective dashboard if valid.
     */
    private void verifyRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);

                    if (!doc.exists()) {
                        showError("Account not found.");
                        mAuth.signOut();
                        return;
                    }

                    String dbRole = doc.getString("role");

                    if (dbRole == null || !dbRole.equals(selectedRole)) {
                        showError("Role mismatch detected.");
                        mAuth.signOut();
                        return;
                    }

                    SessionManager.startSession(this);

                    Intent intent;

                    switch (dbRole) {
                        case "admin":
                            intent = new Intent(this, AdminDashboardActivity.class);
                            break;
                        case "student":
                            // First-login students must complete interest onboarding
                            // before recommendations can show anything useful.
                            Boolean done = doc.getBoolean("onboardingComplete");
                            intent = new Intent(this,
                                    Boolean.TRUE.equals(done)
                                            ? StudentHomeActivity.class
                                            : StudentOnboardingActivity.class);
                            break;
                        case "event_manager":
                            intent = new Intent(this, EventManagerDashboardActivity.class);
                            break;
                        default:
                            showError("Unknown role.");
                            return;
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError(e.getMessage());
                    Log.e(TAG, "Firestore error: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Forgot Password Flow
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Displays forgot password dialog and sends reset email.
     */
    private void showForgotPasswordDialog() {
        final TextInputEditText emailInput = new TextInputEditText(this);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setView(emailInput)
                .setPositiveButton("Send Reset Link", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // UI Helpers
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Toggles loading UI state.
     */
    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : "LOGIN");

        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Displays error message to user.
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Checks if email belongs to a test account.
     */
    private boolean isTestAccount(String email) {
        if (email == null) return false;

        for (String testEmail : TEST_ACCOUNTS) {
            if (email.equalsIgnoreCase(testEmail)) {
                return true;
            }
        }
        return false;
    }
}