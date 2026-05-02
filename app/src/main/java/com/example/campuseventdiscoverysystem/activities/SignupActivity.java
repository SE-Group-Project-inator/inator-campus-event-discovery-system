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

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth        = FirebaseAuth.getInstance();
        db           = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");
        if (selectedRole == null) selectedRole = "student";

        applyRoleTheme();
        setupPasswordStrengthMeter();
        setupDropdowns();
        setupClickListeners();
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRoleTitle  = findViewById(R.id.tvRoleTitle);
        Button   btnSignUp    = findViewById(R.id.btnSignUp);
        androidx.cardview.widget.CardView signupCard = findViewById(R.id.signupCard);

        ScrollView scrollView = findViewById(R.id.scrollView);

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

    private void setupDropdowns() {
        // School picker (students only)
        AutoCompleteTextView spinnerSchool = findViewById(R.id.spinnerSchool);
        if (spinnerSchool != null) {
            List<String> schools = Arrays.asList("SDSB", "SSE", "MGHSS", "SHASOL");
            ArrayAdapter<String> schoolAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, schools);
            spinnerSchool.setAdapter(schoolAdapter);
            spinnerSchool.setOnItemClickListener((parent, view, pos, id) ->
                    spinnerSchool.clearFocus());
        }

        // Batch year 2020-2030
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

        // Society position (event managers only)
        AutoCompleteTextView spinnerPosition = findViewById(R.id.spinnerSocietyPosition);
        if (spinnerPosition != null) {
            List<String> positions = Arrays.asList(
                    "President", "Vice President", "Secretary General",
                    "Event Head", "Marketing Head", "Member");
            ArrayAdapter<String> posAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, positions);
            spinnerPosition.setAdapter(posAdapter);
        }

        // Admin department
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

    // ─── Password Strength Meter ──────────────────────────────────────────────

    private void setupPasswordStrengthMeter() {
        EditText etPassword = findViewById(R.id.etPassword);
        TextView tvStrength = findViewById(R.id.tvPasswordStrength);
        if (etPassword == null || tvStrength == null) return;

        // Start hidden — only shown after user types
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

    private int calculatePasswordStrength(String p) {
        int s = 0;
        if (p.length() >= 6)  s += 25;
        if (p.length() >= 10) s += 15;
        if (p.matches(".*[A-Z].*")) s += 20;
        if (p.matches(".*[0-9].*")) s += 20;
        if (p.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\\\"\\\\|,.<>/?].*")) s += 20;
        return Math.min(s, 100);
    }

    // ─── Click Listeners ─────────────────────────────────────────────────────

    private void setupClickListeners() {
        ImageButton btnBack  = findViewById(R.id.btnBack);
        Button btnSignUp     = findViewById(R.id.btnSignUp);
        TextView tvGoToLogin = findViewById(R.id.tvLogin);
        CheckBox cbTerms     = findViewById(R.id.cbAgreeTerms);

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
                Toast.makeText(this, "Please agree to the terms & conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> extraData = new HashMap<>();

            if ("student".equals(selectedRole)) {
                String sid = etStudentId != null ? etStudentId.getText().toString().trim() : "";
                if (TextUtils.isEmpty(sid)) { etStudentId.setError("Student ID required"); return; }

                String school  = spinnerSchool != null ? spinnerSchool.getText().toString().trim() : "";
                String batch   = spinnerBatch  != null ? spinnerBatch.getText().toString().trim()  : "";
                String program = etProgram     != null ? etProgram.getText().toString().trim()     : "";

                if (TextUtils.isEmpty(school)) {
                    Toast.makeText(this, "Please select your school", Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(batch)) {
                    Toast.makeText(this, "Please select your batch year", Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(program)) {
                    etProgram.setError("Program is required"); return;
                }

                extraData.put("studentId", sid);
                extraData.put("school",    school);
                extraData.put("batch",     batch);
                extraData.put("program",   program);
                extraData.put("department", school + " — " + program); // backward compat

            } else if ("event_manager".equals(selectedRole)) {
                String sname = etSocietyName != null ? etSocietyName.getText().toString().trim() : "";
                if (TextUtils.isEmpty(sname)) { etSocietyName.setError("Society name required"); return; }
                extraData.put("societyName", sname);
                extraData.put("position",    spinnerPosition != null ? spinnerPosition.getText().toString() : "");
                extraData.put("phone",       etPhone != null ? etPhone.getText().toString().trim() : "");

            } else if ("admin".equals(selectedRole)) {
                String eid = etEmployeeId != null ? etEmployeeId.getText().toString().trim() : "";
                if (TextUtils.isEmpty(eid)) { etEmployeeId.setError("Employee ID required"); return; }
                extraData.put("employeeId",  eid);
                extraData.put("department",  spinnerAdminDept != null ? spinnerAdminDept.getText().toString() : "");
            }

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating account…");

            mAuth.createUserWithEmailAndPassword(email, pwd)
                    .addOnSuccessListener(authResult -> {
                        authResult.getUser().sendEmailVerification()
                                .addOnCompleteListener(emailTask -> {
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
