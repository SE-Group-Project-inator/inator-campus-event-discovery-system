package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    // Created programmatically — not in XML (fixes "cannot find symbol passwordStrengthBar")
    private ProgressBar strengthBar;
    private TextView tvStrengthLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");
        if (selectedRole == null) selectedRole = "student";

        applyRoleTheme();
        injectStrengthMeterBelowPassword();
        setupPasswordStrengthMeter();
        setupClickListeners();
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRoleTitle  = findViewById(R.id.tvRoleTitle);
        Button btnSignUp      = findViewById(R.id.btnSignUp);
        androidx.cardview.widget.CardView signupCard = findViewById(R.id.signupCard);

        switch (selectedRole) {
            case "admin":
                topBar.setBackgroundColor(getColor(R.color.btn_admin));
                tvRoleTitle.setText("Admin Sign Up");
                btnSignUp.setBackgroundTintList(getColorStateList(R.color.btn_admin));
                signupCard.setCardBackgroundColor(getColor(R.color.card_tan));
                findViewById(R.id.sectionAdmin).setVisibility(View.VISIBLE);
                break;
            case "event_manager":
                topBar.setBackgroundColor(getColor(R.color.btn_eventmgr));
                tvRoleTitle.setText("Event Manager Sign Up");
                btnSignUp.setBackgroundTintList(getColorStateList(R.color.btn_eventmgr));
                signupCard.setCardBackgroundColor(getColor(R.color.card_event));
                findViewById(R.id.sectionEventManager).setVisibility(View.VISIBLE);
                break;
            case "student":
            default:
                topBar.setBackgroundColor(getColor(R.color.btn_student));
                tvRoleTitle.setText("Student Sign Up");
                btnSignUp.setBackgroundTintList(getColorStateList(R.color.btn_student));
                signupCard.setCardBackgroundColor(getColor(R.color.card_student));
                findViewById(R.id.sectionStudent).setVisibility(View.VISIBLE);
                break;
        }
    }

    // ─── Strength meter (injected at runtime) ─────────────────────────────────

    /**
     * ROOT CAUSE FIX:
     * The XML layout (activity_signup.xml) has R.id.tvPasswordStrength but NO
     * ProgressBar with id "passwordStrengthBar". Calling findViewById() on a
     * missing ID returns null — but the old code stored it and called methods on
     * it, causing a NullPointerException at runtime (and the compile error you saw
     * was because the ID was never declared in R.java).
     *
     * Solution: create the ProgressBar entirely in Java and insert it into the
     * layout right after the password TextInputLayout — no XML change needed.
     */
    private void injectStrengthMeterBelowPassword() {

        EditText etPassword = findViewById(R.id.etPassword);
        if (etPassword == null) return;

        View passwordLayout = (View) etPassword.getParent();
        if (!(passwordLayout.getParent() instanceof LinearLayout)) return;

        LinearLayout parent = (LinearLayout) passwordLayout.getParent();

        // 🔥 FIX 1: prevent duplicate injection
        if (strengthBar != null && strengthBar.getParent() != null) {
            ((ViewGroup) strengthBar.getParent()).removeView(strengthBar);
        }

        if (tvStrengthLabel != null && tvStrengthLabel.getParent() != null) {
            ((ViewGroup) tvStrengthLabel.getParent()).removeView(tvStrengthLabel);
        }

        // Build ProgressBar
        strengthBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);

        strengthBar.setMax(100);
        strengthBar.setProgress(0);
        strengthBar.setVisibility(View.GONE);

        int dp8 = (int) (8 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 20);
        lp.setMargins(0, dp8, 0, 0);

        strengthBar.setLayoutParams(lp);

        int idx = parent.indexOfChild(passwordLayout);

        // 🔥 FIX 2: safe insert
        parent.addView(strengthBar, idx + 1);

        if (tvStrengthLabel != null) {
            parent.addView(tvStrengthLabel, idx + 2);
            tvStrengthLabel.setVisibility(View.GONE);
        }
    }

    private void setupPasswordStrengthMeter() {
        EditText etPassword = findViewById(R.id.etPassword);
        if (etPassword == null || strengthBar == null) return;

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                String pwd = s.toString();
                if (pwd.isEmpty()) {
                    strengthBar.setVisibility(View.GONE);
                    if (tvStrengthLabel != null) tvStrengthLabel.setVisibility(View.GONE);
                    return;
                }
                strengthBar.setVisibility(View.VISIBLE);
                if (tvStrengthLabel != null) tvStrengthLabel.setVisibility(View.VISIBLE);

                int score = calculatePasswordStrength(pwd);
                strengthBar.setProgress(score);

                if (score <= 25)      applyStrengthUI("Weak",     android.R.color.holo_red_dark);
                else if (score <= 50) applyStrengthUI("Fair",     android.R.color.holo_orange_dark);
                else if (score <= 75) applyStrengthUI("Good",     android.R.color.holo_blue_dark);
                else                  applyStrengthUI("Strong ✓", android.R.color.holo_green_dark);
            }
        });
    }

    private void applyStrengthUI(String label, int colorRes) {
        strengthBar.setProgressTintList(getColorStateList(colorRes));
        if (tvStrengthLabel != null) {
            tvStrengthLabel.setText(label);
            tvStrengthLabel.setTextColor(getColor(colorRes));
        }
    }

    private int calculatePasswordStrength(String p) {
        int s = 0;
        if (p.length() >= 6)  s += 25;
        if (p.length() >= 10) s += 15;
        if (p.matches(".*[A-Z].*")) s += 20;
        if (p.matches(".*[0-9].*")) s += 20;
        if (p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) s += 20;
        return Math.min(s, 100);
    }

    // ─── Click Listeners ─────────────────────────────────────────────────────

    private void setupClickListeners() {
        ImageButton btnBack   = findViewById(R.id.btnBack);
        Button btnSignUp      = findViewById(R.id.btnSignUp);
        TextView tvGoToLogin  = findViewById(R.id.tvLogin);
        CheckBox cbTerms      = findViewById(R.id.cbAgreeTerms);

        EditText etName            = findViewById(R.id.etFullName);
        EditText etEmail           = findViewById(R.id.etEmail);
        EditText etPassword        = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        EditText etStudentId                  = findViewById(R.id.etStudentId);
        AutoCompleteTextView spinnerDept      = findViewById(R.id.spinnerDepartment);
        AutoCompleteTextView spinnerBatch     = findViewById(R.id.spinnerBatchYear);
        EditText etSocietyName                = findViewById(R.id.etSocietyName);
        AutoCompleteTextView spinnerPosition  = findViewById(R.id.spinnerSocietyPosition);
        EditText etPhone                      = findViewById(R.id.etPhone);
        EditText etEmployeeId                 = findViewById(R.id.etEmployeeId);
        AutoCompleteTextView spinnerAdminDept = findViewById(R.id.spinnerAdminDepartment);

        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            String name    = etName.getText().toString().trim();
            String email   = etEmail.getText().toString().trim();
            String pwd     = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name))  { etName.setError("Full name is required"); return; }
            if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address"); return;
            }
            if (!email.endsWith("@lums.edu.pk")) {
                etEmail.setError("Must use a LUMS email (@lums.edu.pk)"); return;
            }
            if (pwd.length() < 8) {
                etPassword.setError("Password must be at least 8 characters"); return;
            }
            if (calculatePasswordStrength(pwd) <= 25) {
                etPassword.setError("Too weak — add uppercase letters, numbers or symbols"); return;
            }
            if (!pwd.equals(confirm)) {
                etConfirmPassword.setError("Passwords do not match"); return;
            }
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please agree to the terms & conditions",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> extraData = new HashMap<>();
            if ("student".equals(selectedRole)) {
                String sid = etStudentId.getText().toString().trim();
                if (TextUtils.isEmpty(sid)) { etStudentId.setError("Student ID required"); return; }
                extraData.put("studentId",  sid);
                extraData.put("department", spinnerDept  != null ? spinnerDept.getText().toString()  : "");
                extraData.put("batch",      spinnerBatch != null ? spinnerBatch.getText().toString() : "");
                // Drives the post-login routing into StudentOnboardingActivity.
                extraData.put("onboardingComplete", false);
            } else if ("event_manager".equals(selectedRole)) {
                String sname = etSocietyName.getText().toString().trim();
                if (TextUtils.isEmpty(sname)) { etSocietyName.setError("Society name required"); return; }
                extraData.put("societyName", sname);
                extraData.put("position",    spinnerPosition != null ? spinnerPosition.getText().toString() : "");
                extraData.put("phone",       etPhone != null ? etPhone.getText().toString().trim() : "");
            } else if ("admin".equals(selectedRole)) {
                String eid = etEmployeeId.getText().toString().trim();
                if (TextUtils.isEmpty(eid)) { etEmployeeId.setError("Employee ID required"); return; }
                extraData.put("employeeId",  eid);
                extraData.put("department",  spinnerAdminDept != null ? spinnerAdminDept.getText().toString() : "");
            }

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating account…");

            mAuth.createUserWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener(authResult -> {
                        // ── STEP 1: Send verification email ──────────────────────────────
                        authResult.getUser().sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {
                                    // ── STEP 2: Save Firestore profile ───────────────────
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("name",           name);
                                    userMap.put("email",          email);
                                    userMap.put("role",           selectedRole);
                                    userMap.put("createdAt",      Timestamp.now());
                                    userMap.put("eventsAttended", 0);
                                    userMap.put("following",      0);
                                    userMap.putAll(extraData);

                                    db.collection("users")
                                            .document(authResult.getUser().getUid())
                                            .set(userMap)
                                            .addOnSuccessListener(unused -> {
                                                // Sign out immediately — force them to verify first
                                                mAuth.signOut();

                                                Toast.makeText(this,
                                                        "Account created! 📧 A verification link has been sent to "
                                                                + email + ". Please verify before logging in.",
                                                        Toast.LENGTH_LONG).show();

                                                Intent i = new Intent(this, LoginActivity.class);
                                                i.putExtra("role", selectedRole);
                                                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                startActivity(i);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                authResult.getUser().delete();
                                                btnSignUp.setEnabled(true);
                                                btnSignUp.setText("CREATE ACCOUNT");
                                                Toast.makeText(this,
                                                        "Error saving profile: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                            });
                                });
                    })
                    .addOnFailureListener(e -> {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("CREATE ACCOUNT");
                        String msg = e.getMessage();
                        if (msg != null && msg.contains("email address is already in use"))
                            msg = "An account with this email already exists.";
                        Toast.makeText(this, "Sign-up failed: " + msg, Toast.LENGTH_LONG).show();
                    });
        });
    }
}