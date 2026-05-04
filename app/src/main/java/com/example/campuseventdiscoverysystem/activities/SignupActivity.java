package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ScrollView;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SignupActivity
 *
 * Handles role-based user registration for:
 * - Student
 * - Event Manager
 * - Admin
 *
 * Features:
 * - Dynamic UI based on selected role
 * - Password strength validation
 * - Firestore user creation
 * - Role-specific fields handling
 * - Email verification flow
 */
public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load signup screen layout
        setContentView(R.layout.activity_signup);

        // Initialize Firebase services
        mAuth        = FirebaseAuth.getInstance();
        db           = FirebaseFirestore.getInstance();

        // Get role passed from previous screen (student/admin/event_manager)
        selectedRole = getIntent().getStringExtra("role");
        if (selectedRole == null) selectedRole = "student";

        // Apply role-based UI styling
        applyRoleTheme();

        // Setup password strength meter listener
        setupPasswordStrengthMeter();

        // Initialize dropdowns (school, batch, society, etc.)
        setupDropdowns();

        // Setup button click handlers
        setupClickListeners();
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    /**
     * Applies different UI themes based on selected user role.
     * Changes colors, visible sections, and card styling.
     */
    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRoleTitle  = findViewById(R.id.tvRoleTitle);
        Button   btnSignUp    = findViewById(R.id.btnSignUp);
        androidx.cardview.widget.CardView signupCard = findViewById(R.id.signupCard);

        ScrollView scrollView = findViewById(R.id.scrollView);

        // Switch UI styling depending on role
        switch (selectedRole) {
            case "admin":
                topBar.setBackgroundColor(getColor(R.color.btn_admin));
                tvRoleTitle.setText("Admin Sign Up");
                btnSignUp.setBackgroundTintList(getColorStateList(R.color.btn_admin));
                signupCard.setCardBackgroundColor(getColor(R.color.card_admin_login));
                if (scrollView != null) scrollView.setBackgroundColor(getColor(R.color.bg_admin_login));
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

    // ─── Dropdowns ────────────────────────────────────────────────────────────

    /**
     * Initializes dropdown menus for:
     * - School selection (students)
     * - Batch year selection (students)
     * - Society selection (event managers from Firestore)
     * - Society position (event managers)
     * - Admin department selection
     */
    private void setupDropdowns() {

        // School picker (student only)
        AutoCompleteTextView spinnerSchool = findViewById(R.id.spinnerSchool);
        if (spinnerSchool != null) {
            List<String> schools = Arrays.asList("SDSB", "SSE", "MGHSS", "SHASOL");
            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, schools);
            spinnerSchool.setAdapter(schoolAdapter);

            // Clear focus after selection (UX improvement)
            spinnerSchool.setOnItemClickListener((parent, view, pos, id) ->
                    spinnerSchool.clearFocus());
        }

        // Batch year dropdown (2020–2030)
        AutoCompleteTextView spinnerBatch = findViewById(R.id.spinnerBatchYear);
        if (spinnerBatch != null) {
            String[] batches = new String[11];
            for (int i = 0; i <= 10; i++) batches[i] = String.valueOf(2020 + i);

            ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, batches);
            spinnerBatch.setAdapter(batchAdapter);

            spinnerBatch.setOnItemClickListener((parent, view, pos, id) ->
                    spinnerBatch.clearFocus());
        }

        // Society dropdown (event manager) — fetched dynamically from Firestore
        AutoCompleteTextView spinnerSociety = findViewById(R.id.etSocietyName);
        if (spinnerSociety != null) {

            spinnerSociety.setInputType(android.text.InputType.TYPE_NULL);
            spinnerSociety.setHint("Select your Society");

            // Load society list from database
            db.collection("societies").orderBy("name").get()
                    .addOnSuccessListener(query -> {

                        List<String> names = new java.util.ArrayList<>();
                        java.util.Map<String, String> nameToId = new java.util.LinkedHashMap<>();

                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                            String n = doc.getString("name");
                            if (n != null) {
                                names.add(n);
                                nameToId.put(n, doc.getId());
                            }
                        }

                        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, names);

                        spinnerSociety.setAdapter(sAdapter);

                        // Store selected societyId in tag for later use
                        spinnerSociety.setOnItemClickListener((parent, view, pos, id) -> {
                            String selected = names.get(pos);
                            spinnerSociety.setTag(nameToId.get(selected));
                            spinnerSociety.clearFocus();
                        });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Could not load societies", Toast.LENGTH_SHORT).show());
        }

        // Society position dropdown (event managers)
        AutoCompleteTextView spinnerPosition = findViewById(R.id.spinnerSocietyPosition);
        if (spinnerPosition != null) {
            List<String> positions = Arrays.asList(
                    "President", "Vice President", "Secretary General",
                    "Event Head", "Marketing Head", "Member");
            ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, positions);
            spinnerPosition.setAdapter(posAdapter);
        }

        // Admin department dropdown
        AutoCompleteTextView spinnerAdminDept = findViewById(R.id.spinnerAdminDepartment);
        if (spinnerAdminDept != null) {
            List<String> depts = Arrays.asList(
                    "Student Affairs", "Academic Affairs", "IT Department",
                    "Finance", "Administration");
            ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, depts);
            spinnerAdminDept.setAdapter(deptAdapter);
        }
    }

    // ─── Password Strength ────────────────────────────────────────────────────

    /**
     * Shows real-time password strength feedback.
     * Uses TextWatcher to evaluate password quality.
     */
    private void setupPasswordStrengthMeter() {

        EditText etPassword = findViewById(R.id.etPassword);
        TextView tvStrength = findViewById(R.id.tvPasswordStrength);

        if (etPassword == null || tvStrength == null) return;

        // Hidden initially until user starts typing
        tvStrength.setVisibility(View.GONE);

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                String pwd = s.toString();

                if (pwd.isEmpty()) {
                    tvStrength.setVisibility(View.GONE);
                    return;
                }

                tvStrength.setVisibility(View.VISIBLE);

                int score = calculatePasswordStrength(pwd);

                if (score <= 25) {
                    tvStrength.setText("Strength: Weak");
                    tvStrength.setTextColor(getColor(android.R.color.holo_red_dark));
                } else if (score <= 50) {
                    tvStrength.setText("Strength: Fair");
                    tvStrength.setTextColor(getColor(android.R.color.holo_orange_dark));
                } else if (score <= 75) {
                    tvStrength.setText("Strength: Good");
                    tvStrength.setTextColor(getColor(android.R.color.holo_blue_dark));
                } else {
                    tvStrength.setText("Strength: Strong ✓");
                    tvStrength.setTextColor(getColor(android.R.color.holo_green_dark));
                }
            }
        });
    }

    /**
     * Simple password scoring system based on:
     * length, uppercase, numbers, and special characters
     */
    private int calculatePasswordStrength(String p) {
        int s = 0;
        if (p.length() >= 6)  s += 25;
        if (p.length() >= 10) s += 15;
        if (p.matches(".*[A-Z].*")) s += 20;
        if (p.matches(".*[0-9].*")) s += 20;
        if (p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*")) s += 20;
        return Math.min(s, 100);
    }

    // ─── Click Handlers ───────────────────────────────────────────────────────

    /**
     * Handles:
     * - Form validation
     * - Role-specific data collection
     * - Firebase Authentication
     * - Firestore user creation
     * - Email verification
     */
    private void setupClickListeners() {

        // UI references
        ImageButton btnBack  = findViewById(R.id.btnBack);
        Button btnSignUp     = findViewById(R.id.btnSignUp);
        TextView tvGoToLogin = findViewById(R.id.tvLogin);
        CheckBox cbTerms     = findViewById(R.id.cbAgreeTerms);

        // Common fields
        EditText etName            = findViewById(R.id.etFullName);
        EditText etEmail           = findViewById(R.id.etEmail);
        EditText etPassword        = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Student fields
        EditText etStudentId            = findViewById(R.id.etStudentId);
        AutoCompleteTextView spinnerSchool = findViewById(R.id.spinnerSchool);
        AutoCompleteTextView spinnerBatch  = findViewById(R.id.spinnerBatchYear);
        EditText etProgram              = findViewById(R.id.etProgram);

        // Event manager fields
        EditText etSocietyName               = findViewById(R.id.etSocietyName);
        AutoCompleteTextView spinnerPosition = findViewById(R.id.spinnerSocietyPosition);
        EditText etPhone                     = findViewById(R.id.etPhone);

        // Admin fields
        EditText etEmployeeId                    = findViewById(R.id.etEmployeeId);
        AutoCompleteTextView spinnerAdminDept    = findViewById(R.id.spinnerAdminDepartment);

        // Navigation actions
        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        // Signup button logic (main flow)
        btnSignUp.setOnClickListener(v -> {

            String name    = etName.getText().toString().trim();
            String email   = etEmail.getText().toString().trim();
            String pwd     = etPassword.getText().toString().trim();
            String confirm = etConfirmPassword.getText().toString().trim();

            // Basic validation checks
            if (TextUtils.isEmpty(name))  { etName.setError("Full name is required"); return; }
            if (TextUtils.isEmpty(email)) { etEmail.setError("Email is required"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email address"); return;
            }

            // Domain restriction (university email only)
            if (!email.endsWith("@lums.edu.pk")) {
                etEmail.setError("Must use a LUMS email (@lums.edu.pk)"); return;
            }

            // Password rules
            if (pwd.length() < 8) {
                etPassword.setError("Password must be at least 8 characters"); return;
            }
            if (calculatePasswordStrength(pwd) <= 25) {
                etPassword.setError("Too weak — add uppercase letters, numbers or symbols"); return;
            }
            if (!pwd.equals(confirm)) {
                etConfirmPassword.setError("Passwords do not match"); return;
            }

            // Terms acceptance check
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please agree to the terms & conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extra role-based data
            Map<String, Object> extraData = new HashMap<>();

            // ─── STUDENT ─────────────────────────────────────────────
            if ("student".equals(selectedRole)) {

                String sid = etStudentId != null ? etStudentId.getText().toString().trim() : "";
                if (TextUtils.isEmpty(sid)) { etStudentId.setError("Student ID required"); return; }

                String school  = spinnerSchool != null ? spinnerSchool.getText().toString().trim() : "";
                String batch   = spinnerBatch  != null ? spinnerBatch.getText().toString().trim()  : "";
                String program = etProgram     != null ? etProgram.getText().toString().trim()     : "";

                extraData.put("studentId", sid);
                extraData.put("school", school);
                extraData.put("batch", batch);
                extraData.put("program", program);

                // ─── EVENT MANAGER ───────────────────────────────────────
            } else if ("event_manager".equals(selectedRole)) {

                String sid = etSocietyName != null && etSocietyName.getTag() != null
                        ? etSocietyName.getTag().toString() : "";

                extraData.put("societyName", etSocietyName.getText().toString().trim());
                extraData.put("societyId", sid);
                extraData.put("position", spinnerPosition.getText().toString());
                extraData.put("phone", etPhone.getText().toString().trim());

                // ─── ADMIN ───────────────────────────────────────────────
            } else if ("admin".equals(selectedRole)) {

                extraData.put("employeeId", etEmployeeId.getText().toString().trim());
                extraData.put("department", spinnerAdminDept.getText().toString());
            }

            // Disable button to prevent duplicate requests
            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating account…");

            // Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener(authResult -> {

                        // Send email verification
                        authResult.getUser().sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {

                                    // User profile data saved in Firestore
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("name", name);
                                    userMap.put("email", email);
                                    userMap.put("role", selectedRole);
                                    userMap.put("createdAt", Timestamp.now());
                                    userMap.putAll(extraData);

                                    db.collection("users")
                                            .document(authResult.getUser().getUid())
                                            .set(userMap)
                                            .addOnSuccessListener(unused -> {

                                                mAuth.signOut();

                                                Toast.makeText(this,
                                                        "Account created! Verify email before login.",
                                                        Toast.LENGTH_LONG).show();

                                                startActivity(new Intent(this, LoginActivity.class));
                                                finish();
                                            });
                                });
                    });
        });
    }
}