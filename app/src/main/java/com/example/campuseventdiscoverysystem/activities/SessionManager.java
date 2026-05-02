package com.example.campuseventdiscoverysystem.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;

/**
 * SessionManager — Singleton
 *
 * Production-level inactivity session timeout:
 *   • Warn user at 15 minutes of inactivity (via BaseSessionActivity dialog)
 *   • Hard auto-logout at 20 minutes of inactivity
 *   • Timer resets on every user interaction (tap, scroll, key press)
 *   • Persistent across app restarts via SharedPreferences
 *
 * Architecture:
 *   SessionManager stores the last-active timestamp in SharedPreferences.
 *   BaseSessionActivity checks it in onResume() and onUserInteraction().
 *   A background Handler fires the warning/logout callbacks.
 *
 * Usage (all automatic via BaseSessionActivity — no manual calls needed):
 *   SessionManager.getInstance(context).startSession()      → call on login success
 *   SessionManager.getInstance(context).resetIdleTimer()    → call on user interaction
 *   SessionManager.getInstance(context).isSessionValid()    → true if < 20 min idle
 *   SessionManager.getInstance(context).isWarningDue()      → true if >= 15 min idle
 *   SessionManager.getInstance(context).logout()            → clear + sign out
 */
public class SessionManager {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final String PREF_NAME       = "campus_session";
    private static final String KEY_LAST_ACTIVE = "last_active_ms";

    /** Show "Session expiring soon" warning after this much idle time. */
    public static final long WARNING_TIMEOUT_MS = 15 * 60 * 1000L; // 15 minutes

    /** Hard logout after this much idle time (5 min after warning). */
    public static final long LOGOUT_TIMEOUT_MS  = 20 * 60 * 1000L; // 20 minutes

    // ─── Singleton ────────────────────────────────────────────────────────────

    private static volatile SessionManager instance;

    private final SharedPreferences prefs;
    private final Context appContext;

    /**
     * Background handler that fires the warning + logout Runnables.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Registered callback — set by BaseSessionActivity so it can show the dialog.
     */
    private SessionCallback callback;

    private SessionManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Thread-safe singleton accessor. */
    public static SessionManager getInstance(Context context) {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager(context);
                }
            }
        }
        return instance;
    }

    // ─── Callback interface ───────────────────────────────────────────────────

    /**
     * Implement this in BaseSessionActivity to receive warning / logout signals
     * from the background timer without coupling SessionManager to any Activity class.
     */
    public interface SessionCallback {
        /** Called when 15 minutes of inactivity is reached — show warning dialog. */
        void onSessionWarning(long remainingMs);

        /** Called when 20 minutes of inactivity is reached — perform logout. */
        void onSessionExpired();
    }

    /** BaseSessionActivity registers itself here in onResume(). */
    public void setCallback(SessionCallback cb) {
        this.callback = cb;
    }

    /** BaseSessionActivity unregisters in onPause() to avoid leaks. */
    public void clearCallback() {
        this.callback = null;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Call immediately after a successful login.
     * Records the current timestamp and arms the idle timers.
     */
    public void startSession() {
        updateLastActive();
        armIdleTimers();
    }

    /**
     * Call on every user interaction (handled automatically by BaseSessionActivity).
     * Resets the idle clock and re-arms the timers.
     */
    public void resetIdleTimer() {
        updateLastActive();
        armIdleTimers();
    }

    /**
     * Returns true if the user has been active within the last 20 minutes.
     * Returns false if session was never started or has expired.
     */
    public boolean isSessionValid() {
        long lastActive = prefs.getLong(KEY_LAST_ACTIVE, 0L);
        if (lastActive == 0L) return false;
        return (System.currentTimeMillis() - lastActive) < LOGOUT_TIMEOUT_MS;
    }

    /**
     * Returns true if the idle time has crossed the 15-minute warning threshold
     * but has not yet reached the 20-minute hard-logout threshold.
     */
    public boolean isWarningDue() {
        long idle = idleTimeMs();
        return idle >= WARNING_TIMEOUT_MS && idle < LOGOUT_TIMEOUT_MS;
    }

    /**
     * How many milliseconds remain before hard-logout.
     * Returns 0 if already expired.
     */
    public long remainingMs() {
        long remaining = LOGOUT_TIMEOUT_MS - idleTimeMs();
        return Math.max(0, remaining);
    }

    /**
     * Clear all session data and sign out of Firebase.
     * Does NOT navigate — the caller is responsible for redirecting.
     */
    public void logout() {
        cancelIdleTimers();
        prefs.edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
    }

    // ─── Package-private helpers (used by BaseSessionActivity) ────────────────

    /**
     * Checks whether the session is still valid.
     * If expired → calls logout() + redirects to RoleSelectActivity → returns false.
     * If still valid → resets the idle timer → returns true.
     */
    static boolean checkSession(Context context) {
        SessionManager sm = getInstance(context);
        if (!sm.isSessionValid()) {
            sm.logout();
            Intent i = new Intent(context, RoleSelectActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.putExtra("session_expired", true);
            context.startActivity(i);
            return false;
        }
        // Still valid — reset the idle clock
        sm.resetIdleTimer();
        return true;
    }

    static void startSession(Context context) {
        getInstance(context).startSession();
    }

    static void refreshSession(Context context) {
        getInstance(context).resetIdleTimer();
    }

    static void clearSession(Context context) {
        getInstance(context).logout();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void updateLastActive() {
        prefs.edit().putLong(KEY_LAST_ACTIVE, System.currentTimeMillis()).apply();
    }

    private long idleTimeMs() {
        long lastActive = prefs.getLong(KEY_LAST_ACTIVE, 0L);
        if (lastActive == 0L) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastActive;
    }

    /** Arms two delayed Runnables: one for the 15-min warning, one for 20-min logout. */
    private void armIdleTimers() {
        cancelIdleTimers();

        long idle     = idleTimeMs();
        long toWarn   = WARNING_TIMEOUT_MS - idle;
        long toLogout = LOGOUT_TIMEOUT_MS  - idle;

        if (toWarn > 0) {
            handler.postDelayed(warningRunnable, toWarn);
        }
        if (toLogout > 0) {
            handler.postDelayed(logoutRunnable, toLogout);
        }
    }

    private void cancelIdleTimers() {
        handler.removeCallbacks(warningRunnable);
        handler.removeCallbacks(logoutRunnable);
    }

    private final Runnable warningRunnable = new Runnable() {
        @Override
        public void run() {
            if (callback != null && isWarningDue()) {
                callback.onSessionWarning(remainingMs());
            }
        }
    };

    private final Runnable logoutRunnable = new Runnable() {
        @Override
        public void run() {
            Context ctx = appContext;

            if (!isSessionValid()) {
                if (callback != null) {
                    callback.onSessionExpired();
                } else {
                    logout();
                    Intent i = new Intent(ctx, RoleSelectActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    i.putExtra("session_expired", true);
                    ctx.startActivity(i);
                }
            }
        }
    };
}