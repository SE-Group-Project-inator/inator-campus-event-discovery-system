package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;

public class RoleSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        Button btnStudent = findViewById(R.id.btnStudent);
        Button btnEventManager = findViewById(R.id.btnEventManager);
        Button btnAdmin = findViewById(R.id.btnAdmin);

        btnStudent.setOnClickListener(v -> navigateToLogin("student"));
        btnEventManager.setOnClickListener(v -> navigateToLogin("event_manager"));
        btnAdmin.setOnClickListener(v -> navigateToLogin("admin"));
    }

    private void navigateToLogin(String role) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}