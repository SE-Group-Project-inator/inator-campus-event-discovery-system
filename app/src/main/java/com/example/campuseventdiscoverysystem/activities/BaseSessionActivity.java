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
 * BaseSessionActivity
 *
 * Every protected screen (Student, EventManager, Admin dashboards and all
 * their child screens) extends THIS instead of AppCompatActivity directly.
 *
 * What you get for free:
 *   ✅ onResume()          — session validity check on every screen visit
 *   ✅ onUserInteraction() — idle clock resets on every tap/scroll/key press
 *   ✅ 15-min warning      — countdown dialog: "Session expiring in X:XX"
 *   ✅ 20-min hard logout  — auto-redirect to RoleSelectActivity
 *   ✅ showLogoutDialog()  — confirmation dialog, call from any logout button
 *   ✅ performLogout()     — hard logout: clear session + Firebase sign-out + navigate
 *
 * You NEVER write session code in individual activities.
 * To change timeout values, edit the constants in SessionManager.
 */
public abstract class BaseSessionActivity extends AppCompatActivity
        implements SessionManager.SessionCallback {

    // Warning dialog reference — kept so we can dismiss it if the user taps "Stay"
    private AlertDialog warningDialog;
    private CountDownTimer countDownTimer;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();

        // Register this activity as the callback receiver
        SessionManager.getInstance(this).setCallback(this);

        // Hard check: if > 20 min idle, sign out immediately
        if (!SessionManager.checkSession(this)) {
            finish();
            return;
        }

        // Dismiss any stale warning dialog from a previous screen visit
        dismissWarningDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister callback so background timer doesn't hold an Activity reference
        SessionManager.getInstance(this).clearCallback();

        // Always cancel the countdown when we leave the screen
        cancelCountDown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissWarningDialog();
        cancelCountDown();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        // Every tap/scroll/key press resets the idle clock
        SessionManager.getInstance(this).resetIdleTimer();
        // If the warning dialog is showing, dismiss it (user is active)
        dismissWarningDialog();
    }

    // ─── SessionCallback implementation ──────────────────────────────────────

    /**
     * Called by SessionManager when the user has been idle for 15 minutes.
     * Shows a live countdown dialog. If they ignore it, onSessionExpired fires.
     */
    @Override
    public void onSessionWarning(long remainingMs) {
        if (isFinishing() || isDestroyed()) return;
        showWarningDialog(remainingMs);
    }

    /**
     * Called by SessionManager when 20 minutes of inactivity is confirmed.
     * Performs a hard logout regardless of what dialog is showing.
     */
    @Override
    public void onSessionExpired() {
        if (isFinishing() || isDestroyed()) return;
        dismissWarningDialog();
        performLogout(true /* session_expired = true */);
    }

    // ─── Warning Dialog ───────────────────────────────────────────────────────

    private void showWarningDialog(long remainingMs) {
        if (warningDialog != null && warningDialog.isShowing()) return;

        // Build dialog — no auto-dismiss; countdown drives the text
        warningDialog = new AlertDialog.Builder(this)
                .setTitle("⚠️ Session Expiring Soon")
                .setMessage(formatRemaining(remainingMs))
                .setCancelable(false)
                .setPositiveButton("I'm Still Here", (d, w) -> {
                    // User confirmed activity — reset the idle clock
                    SessionManager.getInstance(this).resetIdleTimer();
                    dismissWarningDialog();
                })
                .setNegativeButton("Log Out Now", (d, w) -> performLogout(false))
                .create();

        warningDialog.show();
        startCountDown(warningDialog, remainingMs);
    }

    private void startCountDown(AlertDialog dialog, long remainingMs) {
        cancelCountDown();

        countDownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (dialog.isShowing()) {
                    // Update the dialog message live
                    TextView tv = dialog.findViewById(android.R.id.message);
                    if (tv != null) {
                        tv.setText(formatRemaining(millisUntilFinished));
                    }
                }
            }

            @Override
            public void onFinish() {
                // Timer hit zero — auto-logout even if dialog is still showing
                dismissWarningDialog();
                performLogout(true);
            }
        }.start();
    }

    private String formatRemaining(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format(Locale.getDefault(),
                "You've been inactive and your session will expire in %d:%02d.\n\n" +
                        "Tap \"I'm Still Here\" to continue, or you'll be logged out automatically.",
                minutes, seconds);
    }

    private void dismissWarningDialog() {
        cancelCountDown();
        if (warningDialog != null && warningDialog.isShowing()) {
            warningDialog.dismiss();
        }
        warningDialog = null;
    }

    private void cancelCountDown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    // ─── Logout helpers ───────────────────────────────────────────────────────

    /**
     * Call from any logout button in any activity.
     * Shows a confirmation dialog, then signs out and clears the back stack.
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
     * Hard logout — clears session, signs out of Firebase,
     * navigates to RoleSelectActivity with the back stack cleared.
     *
     * @param sessionExpired pass true if the logout was caused by a timeout
     *                       (RoleSelectActivity will show a toast/banner)
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

    /** Convenience overload — manual logout (not expired). */
    protected void performLogout() {
        performLogout(false);
    }
}