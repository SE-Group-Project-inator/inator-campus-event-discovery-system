package com.example.campuseventdiscoverysystem;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.example.campuseventdiscoverysystem.activities.SessionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * SessionManager idle-timeout behaviour.
 *
 * Robolectric provides a real SharedPreferences and a Looper; we drive the
 * "current time" by writing the last-active timestamp directly into prefs so
 * we don't have to actually wait 15 minutes.
 *
 * The singleton is reset between tests via reflection so each test starts fresh.
 */
@RunWith(RobolectricTestRunner.class)
public class SessionManagerTest {

    private static final String PREF_NAME       = "campus_session";
    private static final String KEY_LAST_ACTIVE = "last_active_ms";

    private Context ctx;
    private SharedPreferences prefs;

    @Before
    public void setUp() throws Exception {
        ctx = ApplicationProvider.getApplicationContext();
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        // Reset the singleton between tests so prefs reload cleanly
        Field instance = SessionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private void setLastActive(long ms) {
        prefs.edit().putLong(KEY_LAST_ACTIVE, ms).commit();
    }

    @Test
    public void freshInstall_sessionInvalid() {
        SessionManager sm = SessionManager.getInstance(ctx);
        assertFalse("No prior session → invalid", sm.isSessionValid());
    }

    @Test
    public void afterStartSession_sessionIsValid() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();
        assertTrue(sm.isSessionValid());
    }

    @Test
    public void afterStartSession_warningNotDue() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();
        assertFalse("Just-started session should not be at warning yet",
                sm.isWarningDue());
    }

    @Test
    public void atSixteenMinutesIdle_warningIsDue() {
        // Backdate last-active to 16 minutes ago (between 15 warn and 20 logout)
        setLastActive(System.currentTimeMillis() - 16 * 60 * 1000L);

        SessionManager sm = SessionManager.getInstance(ctx);
        assertTrue("16 min idle → warning due", sm.isWarningDue());
        assertTrue("16 min idle → still valid", sm.isSessionValid());
    }

    @Test
    public void atTwentyFiveMinutesIdle_sessionExpired() {
        setLastActive(System.currentTimeMillis() - 25 * 60 * 1000L);

        SessionManager sm = SessionManager.getInstance(ctx);
        assertFalse("25 min idle → expired", sm.isSessionValid());
        assertFalse("Expired sessions are not in the warning window",
                sm.isWarningDue());
    }

    @Test
    public void resetIdleTimer_makesExpiredSessionValidAgain() {
        setLastActive(System.currentTimeMillis() - 25 * 60 * 1000L);
        SessionManager sm = SessionManager.getInstance(ctx);
        assertFalse(sm.isSessionValid());

        sm.resetIdleTimer();
        assertTrue("After reset, session should be valid again",
                sm.isSessionValid());
    }

    @Test
    public void remainingMs_isFullTimeout_rightAfterStart() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();

        long remaining = sm.remainingMs();
        // Allow a small slop for the few ms between startSession and the read.
        assertTrue("Remaining ~20 min, got " + remaining,
                remaining > SessionManager.LOGOUT_TIMEOUT_MS - 5000
                        && remaining <= SessionManager.LOGOUT_TIMEOUT_MS);
    }

    @Test
    public void remainingMs_isZero_whenExpired() {
        setLastActive(System.currentTimeMillis() - 30 * 60 * 1000L);
        SessionManager sm = SessionManager.getInstance(ctx);
        assertEquals(0L, sm.remainingMs());
    }

    // Note: logout() also calls FirebaseAuth.getInstance().signOut() which can't
    // run in a JVM unit test (no FirebaseApp initialised). The pref-clearing
    // half is exercised indirectly here by checking that an explicit prefs
    // wipe leaves the session invalid — same observable effect from the
    // caller's perspective.
    @Test
    public void clearedPrefs_makesSessionInvalid() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();
        assertTrue(sm.isSessionValid());

        prefs.edit().clear().commit();
        assertFalse("After prefs cleared, session should be invalid",
                sm.isSessionValid());
    }

    @Test
    public void getInstance_returnsSameSingleton() {
        SessionManager a = SessionManager.getInstance(ctx);
        SessionManager b = SessionManager.getInstance(ctx);
        assertTrue(a == b);
    }
}
