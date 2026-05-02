package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.campuseventdiscoverysystem.R;

/**
 * RoleSelectActivity
 *
 * First screen of the app where the user selects their role:
 * Student, Event Manager, or Admin.
 *
 * Based on selection, the user is redirected to LoginActivity
 * with the selected role passed as an Intent extra.
 *
 * Also provides a helper method to directly navigate to SignUpActivity
 * with a predefined role.
 */
public class RoleSelectActivity extends AppCompatActivity {

    /**
     * Called when the activity is created.
     * Initializes UI buttons and sets up role selection navigation.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);

        Button btnStudent      = findViewById(R.id.btnStudent);
        Button btnEventManager = findViewById(R.id.btnEventManager);
        Button btnAdmin        = findViewById(R.id.btnAdmin);

        // Routes to LoginActivity — LoginActivity reads the "role" extra
        btnStudent.setOnClickListener(v      -> navigateTo(LoginActivity.class, "student"));
        btnEventManager.setOnClickListener(v -> navigateTo(LoginActivity.class, "event_manager"));
        btnAdmin.setOnClickListener(v        -> navigateTo(LoginActivity.class, "admin"));
    }

    /**
     * Static helper method to navigate directly to SignUpActivity
     * with a predefined user role.
     *
     * Example usage:
     * RoleSelectActivity.goToSignUp(context, "student");
     *
     * @param context The current context from which navigation is triggered
     * @param role The role to pass to SignupActivity
     */
    public static void goToSignUp(android.content.Context context, String role) {
        Intent intent = new Intent(context, SignupActivity.class);
        intent.putExtra("role", role);
        context.startActivity(intent);
    }

    /**
     * Helper method to navigate to a destination activity
     * while passing the selected role as an Intent extra.
     *
     * @param destination The target Activity class
     * @param role The selected user role
     */
    private void navigateTo(Class<?> destination, String role) {
        Intent intent = new Intent(this, destination);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}