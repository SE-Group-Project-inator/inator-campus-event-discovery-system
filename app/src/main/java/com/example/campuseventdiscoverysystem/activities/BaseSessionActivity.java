package com.example.campuseventdiscoverysystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * =============================================================================
 * BaseSessionActivity
 * =============================================================================
 *
 * This is the BASE CLASS for ALL protected screens in the system:
 * (Student, Admin, Event Manager dashboards and all child activities)
 *
 * It centralizes session management so NO activity repeats session logic.
 *
 * FEATURES:
 * ---------------------------------------------------------------------------
 * 1. Auto session tracking (idle detection)
 * 2. 15-minute warning dialog with live countdown
 * 3. 20-minute auto logout (hard session expiry)
 * 4. Global logout handling (Firebase + navigation reset)
 * 5. User interaction reset (tap/scroll/key resets timer)
 * 6. Reusable logout dialog for any screen
 *
 * ARCHITECTURE BENEFIT:
 * ---------------------------------------------------------------------------
 * Instead of duplicating session code in every Activity,
 * all screens inherit this class → consistent security layer.
 */
public abstract class BaseSessionActivity extends AppCompatActivity
        implements SessionManager.SessionCallback {

    // ========================= DIALOG + TIMER =========================

    /** Warning dialog shown at 15-minute inactivity mark */
    private AlertDialog warningDialog;

    /** Countdown timer used to update warning dialog in real time */
    private CountDownTimer countDownTimer;

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE MANAGEMENT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when activity becomes visible.
     *
     * Responsibilities:
     * - Registers session callback
     * - Checks if session already expired
     * - Dismisses old warning dialogs
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Attach this activity as session callback listener
        SessionManager.getInstance(this).setCallback(this);

        // If session already expired → force logout
        if (!SessionManager.checkSession(this)) {
            finish();
            return;
        }

        // Clean up any leftover warning dialogs
        dismissWarningDialog();
    }

    /**
     * Called when activity goes to background.
     *
     * Responsibilities:
     * - Removes callback reference (prevents memory leaks)
     * - Cancels countdown timer
     */
    @Override
    protected void onPause() {
        super.onPause();

        SessionManager.getInstance(this).clearCallback();
        cancelCountDown();
    }

    /**
     * Final cleanup when activity is destroyed
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissWarningDialog();
        cancelCountDown();
    }

    /**
     * Called on ANY user interaction (tap, scroll, key press).
     *
     * This resets idle timer so session remains active.
     */
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();

        // Reset inactivity timer
        SessionManager.getInstance(this).resetIdleTimer();

        // If warning is visible, dismiss it immediately
        dismissWarningDialog();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SESSION CALLBACKS (FROM SessionManager)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Triggered when user reaches WARNING threshold (15 min idle)
     *
     * Shows dialog with live countdown until logout.
     */
    @Override
    public void onSessionWarning(long remainingMs) {
        if (isFinishing() || isDestroyed()) return;
        showWarningDialog(remainingMs);
    }

    /**
     * Triggered when session fully expires (20 min idle)
     *
     * Forces logout regardless of UI state.
     */
    @Override
    public void onSessionExpired() {
        if (isFinishing() || isDestroyed()) return;

        dismissWarningDialog();
        performLogout(true); // session expired = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WARNING DIALOG HANDLING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows inactivity warning dialog with countdown
     */
    private void showWarningDialog(long remainingMs) {

        // Prevent duplicate dialogs
        if (warningDialog != null && warningDialog.isShowing()) return;

        warningDialog = new AlertDialog.Builder(this)
                .setTitle("⚠️ Session Expiring Soon")
                .setMessage(formatRemaining(remainingMs))
                .setCancelable(false)

                // User confirms activity → reset session
                .setPositiveButton("I'm Still Here", (d, w) -> {
                    SessionManager.getInstance(this).resetIdleTimer();
                    dismissWarningDialog();
                })

                // Manual logout option
                .setNegativeButton("Log Out Now", (d, w) -> performLogout(false))
                .create();

        warningDialog.show();
        startCountDown(warningDialog, remainingMs);
    }

    /**
     * Starts live countdown inside warning dialog
     */
    private void startCountDown(AlertDialog dialog, long remainingMs) {

        cancelCountDown();

        countDownTimer = new CountDownTimer(remainingMs, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                if (dialog.isShowing()) {
                    TextView tv = dialog.findViewById(android.R.id.message);
                    if (tv != null) {
                        tv.setText(formatRemaining(millisUntilFinished));
                    }
                }
            }

            @Override
            public void onFinish() {
                // Auto logout when countdown ends
                dismissWarningDialog();
                performLogout(true);
            }
        }.start();
    }

    /**
     * Formats remaining time into readable message
     */
    private String formatRemaining(long ms) {

        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;

        return String.format(Locale.getDefault(),
                "You've been inactive and your session will expire in %d:%02d.\n\n" +
                        "Tap \"I'm Still Here\" to continue, or you'll be logged out automatically.",
                minutes, seconds);
    }

    /**
     * Dismisses warning dialog safely
     */
    private void dismissWarningDialog() {
        cancelCountDown();

        if (warningDialog != null && warningDialog.isShowing()) {
            warningDialog.dismiss();
        }
        warningDialog = null;
    }

    /**
     * Cancels countdown timer safely
     */
    private void cancelCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGOUT SYSTEM
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Shows confirmation dialog before logging out manually
     */
    protected void showLogoutDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (d, w) -> performLogout(false))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs complete logout:
     * - Clears session
     * - Signs out Firebase user
     * - Clears activity stack
     * - Redirects to RoleSelectActivity
     */
    protected void performLogout(boolean sessionExpired) {

        SessionManager.clearSession(this);
        FirebaseAuth.getInstance().signOut();

        Intent i = new Intent(this, RoleSelectActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (sessionExpired) {
            i.putExtra("session_expired", true);
        }

        startActivity(i);
        finish();
    }

    /** Convenience method for manual logout */
    protected void performLogout() {
        performLogout(false);
    }
}