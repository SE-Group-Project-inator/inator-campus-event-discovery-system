package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;

public class RoleSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        Button btnStudent      = findViewById(R.id.btnStudent);
        Button btnEventManager = findViewById(R.id.btnEventManager);
        Button btnAdmin        = findViewById(R.id.btnAdmin);

        // Routes to LoginActivity — LoginActivity already reads the "role" extra
        btnStudent.setOnClickListener(v      -> navigateTo(LoginActivity.class, "student"));
        btnEventManager.setOnClickListener(v -> navigateTo(LoginActivity.class, "event_manager"));
        btnAdmin.setOnClickListener(v        -> navigateTo(LoginActivity.class, "admin"));
    }

    /**
     * Call this from anywhere you want to send the user to SignUpActivity
     * with the correct role already set — e.g. from LoginActivity's "Sign Up" link.
     * Usage: RoleSelectActivity.goToSignUp(this, "student");
     */
    public static void goToSignUp(android.content.Context context, String role) {
        Intent intent = new Intent(context, SignupActivity.class);
        intent.putExtra("role", role);
        context.startActivity(intent);
    }

    private void navigateTo(Class<?> destination, String role) {
        Intent intent = new Intent(this, destination);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}