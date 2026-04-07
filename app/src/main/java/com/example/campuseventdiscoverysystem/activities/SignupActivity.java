package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuseventdiscoverysystem.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        selectedRole = getIntent().getStringExtra("role");

        // If no role passed, default to student
        if (selectedRole == null) selectedRole = "student";

        applyRoleTheme();
        setupClickListeners();
    }

    private void applyRoleTheme() {
        RelativeLayout topBar = findViewById(R.id.topBar);
        TextView tvRoleTitle = findViewById(R.id.tvRoleTitle);
        Button btnSignUp = findViewById(R.id.btnSignUp);
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

    private void setupClickListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnSignUp = findViewById(R.id.btnSignUp);
        TextView tvGoToLogin = findViewById(R.id.tvLogin);
        CheckBox cbTerms = findViewById(R.id.cbAgreeTerms);

        EditText etName = findViewById(R.id.etFullName);
        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Role specific fields
        EditText etStudentId = findViewById(R.id.etStudentId);
        AutoCompleteTextView spinnerDept = findViewById(R.id.spinnerDepartment);
        AutoCompleteTextView spinnerBatch = findViewById(R.id.spinnerBatchYear);

        EditText etSocietyName = findViewById(R.id.etSocietyName);
        AutoCompleteTextView spinnerPosition = findViewById(R.id.spinnerSocietyPosition);
        EditText etPhone = findViewById(R.id.etPhone);

        EditText etEmployeeId = findViewById(R.id.etEmployeeId);
        AutoCompleteTextView spinnerAdminDept = findViewById(R.id.spinnerAdminDepartment);

        btnBack.setOnClickListener(v -> finish());
        tvGoToLogin.setOnClickListener(v -> finish());

        btnSignUp.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Full name is required");
                return;
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Email is required");
                return;
            }
            if (!email.endsWith("@lums.edu.pk")) {
                etEmail.setError("Use LUMS email");
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password too short");
                return;
            }
            if (!password.equals(confirmPassword)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show();
                return;
            }

            // Role specific validation
            Map<String, Object> extraData = new HashMap<>();
            if (selectedRole.equals("student")) {
                String sid = etStudentId.getText().toString().trim();
                if (TextUtils.isEmpty(sid)) {
                    etStudentId.setError("Student ID required");
                    return;
                }
                extraData.put("studentId", sid);
                extraData.put("department", spinnerDept.getText().toString());
                extraData.put("batch", spinnerBatch.getText().toString());
            } else if (selectedRole.equals("event_manager")) {
                String sname = etSocietyName.getText().toString().trim();
                if (TextUtils.isEmpty(sname)) {
                    etSocietyName.setError("Society Name required");
                    return;
                }
                extraData.put("societyName", sname);
                extraData.put("position", spinnerPosition.getText().toString());
                extraData.put("phone", etPhone.getText().toString().trim());
            } else if (selectedRole.equals("admin")) {
                String eid = etEmployeeId.getText().toString().trim();
                if (TextUtils.isEmpty(eid)) {
                    etEmployeeId.setError("Employee ID required");
                    return;
                }
                extraData.put("employeeId", eid);
                extraData.put("department", spinnerAdminDept.getText().toString());
            }

            btnSignUp.setEnabled(false);
            btnSignUp.setText("Creating...");

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("name", name);
                        userMap.put("email", email);
                        userMap.put("role", selectedRole);
                        userMap.put("createdAt", com.google.firebase.Timestamp.now());
                        userMap.putAll(extraData);

                        db.collection("users").document(uid).set(userMap)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, LoginActivity.class);
                                    intent.putExtra("role", selectedRole);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    authResult.getUser().delete();
                                    btnSignUp.setEnabled(true);
                                    btnSignUp.setText("CREATE ACCOUNT");
                                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText("CREATE ACCOUNT");
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}