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
 * LoginActivity — Production-ready version
 *
 * Fixes vs. original:
 *   ✅ Email verification enforced — user cannot log in without verifying email
 *   ✅ Resend verification email button shown when email is unverified
 *   ✅ Loading spinner during network calls (replaces plain button text change)
 *   ✅ Proper Firebase user reload before checking emailVerified flag
 *   ✅ SessionManager.startSession() called after every successful login
 *   ✅ Forgot password dialog fixed (null-safe, non-dismissing until send succeeds)
 *   ✅ Role mismatch signs the user out cleanly before showing the error toast
 *   ✅ Back stack cleared correctly on navigation (FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK)
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;
    private String            selectedRole;

    // UI references
    private Button            btnLogin;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private ProgressBar       progressBar;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

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

        // Show "session expired" banner if we were redirected here by SessionManager
        if (getIntent().getBooleanExtra("session_expired", false)) {
            Toast.makeText(this,
                    "Your session expired due to inactivity. Please log in again.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ─── View binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        btnLogin     = findViewById(R.id.btnLogin);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);

        // ProgressBar is optional — only present if you added it to activity_login.xml.
        // If it doesn't exist yet, the spinner logic gracefully degrades.
        progressBar  = findViewById(R.id.progressBar);
    }

    // ─── Role theming ─────────────────────────────────────────────────────────

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
                scrollView.setBackgroundColor(getColor(R.color.bg_beige));
                loginCard.setCardBackgroundColor(getColor(R.color.card_tan));
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

    // ─── Click listeners ──────────────────────────────────────────────────────

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

    // ─── Login flow ───────────────────────────────────────────────────────────

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // ── Input validation ──────────────────────────────────────────────────
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
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

                    // ── CRITICAL: Reload the user to get the latest emailVerified flag.
                    // Without reload(), Firebase might return a cached (false) value
                    // even after the user clicked the verification link.
                    user.reload().addOnCompleteListener(reloadTask -> {
                        FirebaseUser fresh = mAuth.getCurrentUser();
                        if (fresh == null) {
                            setLoading(false);
                            showError("Session error. Please try again.");
                            return;
                        }

                        if (!fresh.isEmailVerified()) {
                            // ── Block login and prompt re-verification ───────────
                            mAuth.signOut();
                            setLoading(false);
                            showUnverifiedEmailDialog(email, password);
                        } else {
                            // ── Email verified — check role in Firestore ─────────
                            verifyRoleAndNavigate(fresh.getUid());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    String msg;
                    if (e instanceof FirebaseAuthInvalidUserException) {
                        msg = "No account found with this email. Please sign up first.";
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        msg = "Incorrect password. Please try again.";
                    } else {
                        msg = e.getMessage();
                    }
                    showError(msg);
                    Log.e(TAG, "Login failed: " + e.getMessage());
                });
    }

    /**
     * Shown when Firebase confirms the credentials are correct but the email
     * has not been verified yet.
     *
     * Gives the user two choices:
     *   1. Resend verification email (in case the first one expired/got lost)
     *   2. Dismiss and wait
     */
    private void showUnverifiedEmailDialog(String email, String password) {
        new AlertDialog.Builder(this)
                .setTitle("📧 Email Not Verified")
                .setMessage("Your email address has not been verified yet.\n\n"
                        + "Please check your inbox (and spam folder) for a verification link.\n\n"
                        + "Would you like us to send a new verification email?")
                .setPositiveButton("Resend Verification Email", (d, w) ->
                        resendVerificationEmail(email, password))
                .setNegativeButton("I'll Check My Email", null)
                .setCancelable(true)
                .show();
    }

    /**
     * Signs in silently (just to get a FirebaseUser object), sends the
     * verification email, then signs out again.
     */
    private void resendVerificationEmail(String email, String password) {
        setLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) { setLoading(false); return; }

                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                mAuth.signOut();
                                setLoading(false);
                                new AlertDialog.Builder(this)
                                        .setTitle("Verification Email Sent")
                                        .setMessage("A new verification link has been sent to:\n\n"
                                                + email
                                                + "\n\nPlease check your inbox and spam folder. "
                                                + "The link expires in 1 hour.")
                                        .setPositiveButton("Got it", null)
                                        .show();
                            })
                            .addOnFailureListener(e -> {
                                mAuth.signOut();
                                setLoading(false);
                                showError("Failed to send email: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Could not resend email: " + e.getMessage());
                });
    }

    // ─── Role verification + navigation ──────────────────────────────────────

    /**
     * Fetches the user's Firestore document to confirm their role matches
     * the portal they are trying to log in through.
     *
     * On success: starts the correct dashboard and clears the back stack.
     * On role mismatch: shows a friendly error and signs the user out.
     */
    private void verifyRoleAndNavigate(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    setLoading(false);

                    if (!doc.exists()) {
                        showError("Account not found. Please sign up first.");
                        mAuth.signOut();
                        return;
                    }

                    String dbRole = doc.getString("role");

                    if (dbRole == null || !dbRole.equals(selectedRole)) {
                        String friendly = dbRole != null
                                ? dbRole.replace("_", " ").toUpperCase()
                                : "UNKNOWN";
                        showError("Wrong portal! Your account is registered as: " + friendly);
                        mAuth.signOut();
                        return;
                    }

                    // ── Start the session clock ───────────────────────────────
                    SessionManager.startSession(this);

                    // ── Navigate to the correct dashboard ─────────────────────
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
                            showError("Unknown role. Please contact the admin.");
                            mAuth.signOut();
                            return;
                    }

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Database error: " + e.getMessage());
                    Log.e(TAG, "Firestore error: " + e.getMessage());
                });
    }

    // ─── Forgot password dialog ───────────────────────────────────────────────

    /**
     * Forgot-password flow.
     * Positive button is null at dialog creation (prevents auto-dismiss).
     * We override the click listener in setOnShowListener so the dialog only
     * closes AFTER Firebase confirms the reset email was actually sent.
     */
    private void showForgotPasswordDialog() {
        final TextInputEditText emailInput = new TextInputEditText(this);
        emailInput.setHint("Enter your @lums.edu.pk email");
        emailInput.setInputType(
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        | android.text.InputType.TYPE_CLASS_TEXT);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        emailInput.setPadding(dp16, dp16, dp16, dp16);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter the email linked to your account "
                        + "and we'll send a reset link.")
                .setView(emailInput)
                .setPositiveButton("Send Reset Link", null) // null prevents auto-dismiss
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button sendBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            sendBtn.setOnClickListener(v -> {
                String email = emailInput.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    emailInput.setError("Please enter your email address");
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInput.setError("Enter a valid email address");
                    return;
                }

                sendBtn.setEnabled(false);
                sendBtn.setText("Sending…");

                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(unused -> {
                            dialog.dismiss();
                            new AlertDialog.Builder(this)
                                    .setTitle("Reset Link Sent!")
                                    .setMessage("A password reset link has been sent to:\n\n"
                                            + email
                                            + "\n\nCheck your inbox (and spam folder). "
                                            + "The link expires in 1 hour.")
                                    .setPositiveButton("Got it", null)
                                    .show();
                        })
                        .addOnFailureListener(e -> {
                            sendBtn.setEnabled(true);
                            sendBtn.setText("Send Reset Link");
                            String errMsg = (e instanceof FirebaseAuthInvalidUserException)
                                    ? "No account found with this email address."
                                    : "Failed to send: " + e.getMessage();
                            emailInput.setError(errMsg);
                            Log.e(TAG, "Password reset failed: " + e.getMessage());
                        });
            });
        });

        dialog.show();
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    /**
     * Toggles the loading state:
     *   true  → disable button, show spinner
     *   false → re-enable button, hide spinner
     */
    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : "LOGIN");
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}