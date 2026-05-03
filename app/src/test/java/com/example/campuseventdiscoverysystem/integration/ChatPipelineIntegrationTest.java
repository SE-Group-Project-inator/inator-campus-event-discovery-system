package com.example.campuseventdiscoverysystem.integration;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.campuseventdiscoverysystem.chat.ChatHistoryStore;
import com.example.campuseventdiscoverysystem.chat.ChatMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests: Campus AI Assistant chat pipeline.
 *
 * Tests the full local pipeline used by CampusAssistantActivity:
 *   1. User sends a message → added to the in-memory list
 *   2. Bot reply received → added to the in-memory list
 *   3. Conversation is saved to ChatHistoryStore (SharedPreferences)
 *   4. Activity restores history on resume
 *   5. History is seeded into FrontChatRepository for context continuity
 *   6. Conversation is capped at 50 messages (oldest pruned)
 *   7. Clear wipes both in-memory state and persistent store
 *
 * FrontChatRepository's network call is not exercised here (belongs in
 * integration tests that mock OkHttp or hit a test server). We test only
 * the message-building and history-management logic that is pure Java.
 */
@RunWith(RobolectricTestRunner.class)
public class ChatPipelineIntegrationTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        ctx.getSharedPreferences("campus_assistant_chat", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    // ── 1. Full single-turn conversation: user sends, bot replies, saved ──

    @Test
    public void singleTurn_savedAndRestored() {
        List<ChatMessage> messages = new ArrayList<>();

        // User sends
        messages.add(new ChatMessage(ChatMessage.TYPE_USER, "What events are happening today?"));
        // Bot replies
        messages.add(new ChatMessage(ChatMessage.TYPE_BOT, "There are 3 events today: Tech Talk, Art Show, and Sports Day."));

        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_001");
        store.save(messages);

        List<ChatMessage> restored = store.load();
        assertEquals(2, restored.size());
        assertEquals(ChatMessage.TYPE_USER, restored.get(0).type);
        assertEquals("What events are happening today?", restored.get(0).text);
        assertEquals(ChatMessage.TYPE_BOT,  restored.get(1).type);
        assertTrue(restored.get(1).text.contains("3 events"));
    }

    // ── 2. Multi-turn conversation preserved in order ─────────────────────

    @Test
    public void multiTurn_orderPreserved() {
        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "Hi"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "Hello! How can I help?"),
                new ChatMessage(ChatMessage.TYPE_USER, "Show me tech events"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "Here are the tech events: ..."),
                new ChatMessage(ChatMessage.TYPE_USER, "Register me for the first one"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "To register, please visit the event page.")
        );

        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_002");
        store.save(messages);

        List<ChatMessage> restored = store.load();
        assertEquals(6, restored.size());
        // Verify alternating user/bot pattern
        for (int i = 0; i < restored.size(); i++) {
            int expectedType = (i % 2 == 0) ? ChatMessage.TYPE_USER : ChatMessage.TYPE_BOT;
            assertEquals("Message " + i + " should be type " + expectedType,
                    expectedType, restored.get(i).type);
        }
    }

    // ── 3. History cap: 51st message prunes the oldest ────────────────────

    @Test
    public void historyCap_51Messages_oldestPruned() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            messages.add(new ChatMessage(ChatMessage.TYPE_USER, "message_" + i));
        }

        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_003");
        store.save(messages);

        List<ChatMessage> restored = store.load();
        assertEquals("Should be capped at 50", 50, restored.size());
        assertEquals("Oldest message pruned; first is message_1", "message_1", restored.get(0).text);
        assertEquals("Newest message preserved", "message_50", restored.get(49).text);
    }

    // ── 4. History exactly at the cap (50) is not pruned ─────────────────

    @Test
    public void historyCap_exactly50Messages_noneDropped() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            messages.add(new ChatMessage(ChatMessage.TYPE_BOT, "reply_" + i));
        }

        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_004");
        store.save(messages);

        assertEquals(50, store.load().size());
        assertEquals("reply_0",  store.load().get(0).text);
        assertEquals("reply_49", store.load().get(49).text);
    }

    // ── 5. Clear wipes history; subsequent save starts fresh ─────────────

    @Test
    public void clearAndRestartConversation() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_005");
        store.save(Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "old message"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "old reply")
        ));
        assertEquals(2, store.load().size());

        store.clear();
        assertTrue("History should be empty after clear", store.load().isEmpty());

        // New conversation starts fresh
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "new session start")));
        List<ChatMessage> fresh = store.load();
        assertEquals(1, fresh.size());
        assertEquals("new session start", fresh.get(0).text);
    }

    // ── 6. Seeding FrontChatRepository from persisted history ────────────
    //    (We test the seeding logic independently of the HTTP layer)

    @Test
    public void seedingHistory_correctTurnOrderForRepository() {
        // Simulate what CampusAssistantActivity does when it restores history
        // and seeds it into FrontChatRepository.
        List<ChatMessage> persisted = Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "prior user msg"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "prior bot reply")
        );

        // Build the turn list the way the activity does
        List<String[]> turns = new ArrayList<>();
        for (ChatMessage msg : persisted) {
            String role = (msg.type == ChatMessage.TYPE_USER) ? "user" : "assistant";
            turns.add(new String[]{role, msg.text});
        }

        assertEquals(2, turns.size());
        assertEquals("user",      turns.get(0)[0]);
        assertEquals("prior user msg", turns.get(0)[1]);
        assertEquals("assistant", turns.get(1)[0]);
        assertEquals("prior bot reply", turns.get(1)[1]);
    }

    // ── 7. Per-user isolation: two users share the same device ────────────

    @Test
    public void twoUsers_chatHistoryIsolated() {
        ChatHistoryStore storeStudent  = new ChatHistoryStore(ctx, "student_uid");
        ChatHistoryStore storeManager  = new ChatHistoryStore(ctx, "manager_uid");

        storeStudent.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "student query")));
        storeManager.save(Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "manager query"),
                new ChatMessage(ChatMessage.TYPE_BOT,  "manager reply")
        ));

        assertEquals(1, storeStudent.load().size());
        assertEquals("student query", storeStudent.load().get(0).text);

        assertEquals(2, storeManager.load().size());
        assertEquals("manager query", storeManager.load().get(0).text);
    }

    // ── 8. ChatMessage type constants are distinct ────────────────────────

    @Test
    public void chatMessageTypeConstants_areDistinct() {
        assertNotEquals("TYPE_USER and TYPE_BOT must differ",
                ChatMessage.TYPE_USER, ChatMessage.TYPE_BOT);
    }

    // ── 9. Empty text in a message is stored without NPE ─────────────────

    @Test
    public void chatMessage_emptyText_storedSafely() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_006");
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "")));
        List<ChatMessage> loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals("", loaded.get(0).text);
    }

    // ── 10. Incremental save: append-style pattern used by the activity ───

    @Test
    public void incrementalSave_appendPattern() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "uid_007");

        // Turn 1
        List<ChatMessage> turn1 = new ArrayList<>();
        turn1.add(new ChatMessage(ChatMessage.TYPE_USER, "first"));
        turn1.add(new ChatMessage(ChatMessage.TYPE_BOT,  "first reply"));
        store.save(turn1);

        // Activity appends turn 2 to the in-memory list and re-saves
        List<ChatMessage> turn2 = new ArrayList<>(store.load());
        turn2.add(new ChatMessage(ChatMessage.TYPE_USER, "second"));
        turn2.add(new ChatMessage(ChatMessage.TYPE_BOT,  "second reply"));
        store.save(turn2);

        List<ChatMessage> final_ = store.load();
        assertEquals(4, final_.size());
        assertEquals("first",        final_.get(0).text);
        assertEquals("second reply", final_.get(3).text);
    }
}
