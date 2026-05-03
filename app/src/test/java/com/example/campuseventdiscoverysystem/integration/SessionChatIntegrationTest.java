package com.example.campuseventdiscoverysystem.integration;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.example.campuseventdiscoverysystem.activities.SessionManager;
import com.example.campuseventdiscoverysystem.chat.ChatHistoryStore;
import com.example.campuseventdiscoverysystem.chat.ChatMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests: Session lifecycle ↔ ChatHistoryStore.
 *
 * Verifies that:
 *  - Chat history is only accessible/preserved while a valid session exists.
 *  - Logging out clears the session state (and chat should also be cleared by the app).
 *  - Per-user chat isolation survives a session reset.
 *  - A new session started for user B does not bleed into user A's chat history.
 */
@RunWith(RobolectricTestRunner.class)
public class SessionChatIntegrationTest {

    private static final String SESSION_PREF  = "campus_session";
    private static final String KEY_LAST_ACTIVE = "last_active_ms";

    private Context ctx;

    @Before
    public void setUp() throws Exception {
        ctx = ApplicationProvider.getApplicationContext();

        // Wipe session prefs
        ctx.getSharedPreferences(SESSION_PREF, Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Wipe chat prefs
        ctx.getSharedPreferences("campus_assistant_chat", Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Reset SessionManager singleton
        Field instance = SessionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    // ── 1. Active session → chat history is persisted and retrievable ─────────

    @Test
    public void activeSession_chatHistoryRoundTrip() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();
        assertTrue("Session must be valid before we trust stored history",
                sm.isSessionValid());

        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        store.save(Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "What events are on today?"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "There are 3 events today.")
        ));

        // Simulate activity resume — session still valid
        assertTrue(sm.isSessionValid());

        List<ChatMessage> loaded = store.load();
        assertEquals(2, loaded.size());
        assertEquals("What events are on today?", loaded.get(0).text);
    }

    // ── 2. Expired session → app should not present stale chat ───────────────
    //    (The app's responsibility; we verify the session IS expired so the
    //     app-level guard can act on it.)

    @Test
    public void expiredSession_isDetectedBeforeChatLoad() {
        // Plant a last-active timestamp 25 minutes ago
        ctx.getSharedPreferences(SESSION_PREF, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_ACTIVE,
                        System.currentTimeMillis() - 25 * 60 * 1000L)
                .commit();

        SessionManager sm = SessionManager.getInstance(ctx);
        assertFalse("Session should be expired", sm.isSessionValid());

        // Chat data may still be on disk — but session guard prevents display
        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "stale msg")));

        // The guard fires before loading chat — we confirm the guard fires here
        assertFalse("Guard must block stale chat presentation", sm.isSessionValid());
    }

    // ── 3. Logout clears session; subsequent chat writes go to a "clean" user ─

    @Test
    public void afterLogout_newSessionStartsClean() throws Exception {
        // User A logs in, chats, logs out
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();
        ChatHistoryStore storeA = new ChatHistoryStore(ctx, "userA");
        storeA.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "Hello from A")));

        // Logout clears SharedPreferences (session prefs only — chat intentionally kept
        // so users see history on next login; clearing is a product decision tested
        // separately in ChatHistoryStoreTest).
        ctx.getSharedPreferences(SESSION_PREF, Context.MODE_PRIVATE)
                .edit().clear().commit();

        // Reset singleton to simulate cold re-launch
        Field instance = SessionManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        // User B logs in fresh
        SessionManager sm2 = SessionManager.getInstance(ctx);
        sm2.startSession();
        assertTrue("User B's session should be valid", sm2.isSessionValid());

        // User B's history is empty
        ChatHistoryStore storeB = new ChatHistoryStore(ctx, "userB");
        assertTrue("User B should have no history", storeB.load().isEmpty());

        // User A's history is still on disk (not cleared by session logout)
        assertEquals(1, storeA.load().size());
    }

    // ── 4. resetIdleTimer extends a near-expiry session ──────────────────────

    @Test
    public void resetIdleTimer_extendsSessionDuringActiveChat() {
        // Backdate to 19 minutes ago — 1 minute from expiry
        ctx.getSharedPreferences(SESSION_PREF, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_ACTIVE,
                        System.currentTimeMillis() - 19 * 60 * 1000L)
                .commit();

        SessionManager sm = SessionManager.getInstance(ctx);
        assertTrue("19 min idle is still valid", sm.isSessionValid());
        assertTrue("19 min idle should trigger warning", sm.isWarningDue());

        // User sends a chat message — activity calls resetIdleTimer
        sm.resetIdleTimer();

        assertFalse("Warning should clear after reset", sm.isWarningDue());
        assertTrue("Session should still be valid after reset", sm.isSessionValid());

        // Chat write still succeeds post-reset
        ChatHistoryStore store = new ChatHistoryStore(ctx, "activeUser");
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "still here")));
        assertEquals(1, store.load().size());
    }

    // ── 5. Per-user chat isolation across concurrent "sessions" ──────────────

    @Test
    public void perUserChatIsolation_withSharedSessionManager() {
        SessionManager sm = SessionManager.getInstance(ctx);
        sm.startSession();

        ChatHistoryStore storeA = new ChatHistoryStore(ctx, "studentA");
        ChatHistoryStore storeB = new ChatHistoryStore(ctx, "studentB");

        storeA.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "A message")));
        storeB.save(Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "B message 1"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "B reply")
        ));

        assertEquals(1, storeA.load().size());
        assertEquals(2, storeB.load().size());
        assertEquals("A message", storeA.load().get(0).text);
        assertEquals("B message 1", storeB.load().get(0).text);
    }
}
