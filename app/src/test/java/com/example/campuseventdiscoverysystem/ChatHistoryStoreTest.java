package com.example.campuseventdiscoverysystem;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * US-28 — ChatHistoryStore round-trip and trimming.
 *
 * Robolectric provides a real SharedPreferences implementation backed by an
 * in-memory map, so persistence is exercised end-to-end without a device.
 */
@RunWith(RobolectricTestRunner.class)
public class ChatHistoryStoreTest {

    private Context ctx;

    @Before
    public void setUp() {
        ctx = ApplicationProvider.getApplicationContext();
        // Make sure each test starts clean
        ctx.getSharedPreferences("campus_assistant_chat", Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    @Test
    public void load_returnsEmpty_whenNothingSaved() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        assertTrue(store.load().isEmpty());
    }

    @Test
    public void saveThenLoad_roundTripsMessages() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        store.save(Arrays.asList(
                new ChatMessage(ChatMessage.TYPE_USER, "hi"),
                new ChatMessage(ChatMessage.TYPE_BOT, "hello!")
        ));

        List<ChatMessage> loaded = store.load();
        assertEquals(2, loaded.size());
        assertEquals(ChatMessage.TYPE_USER, loaded.get(0).type);
        assertEquals("hi", loaded.get(0).text);
        assertEquals(ChatMessage.TYPE_BOT, loaded.get(1).type);
        assertEquals("hello!", loaded.get(1).text);
    }

    @Test
    public void perUserIsolation_differentUsersDontShareHistory() {
        ChatHistoryStore a = new ChatHistoryStore(ctx, "userA");
        ChatHistoryStore b = new ChatHistoryStore(ctx, "userB");

        a.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "from A")));
        assertTrue(b.load().isEmpty());
        assertEquals(1, a.load().size());
    }

    @Test
    public void clear_removesHistory() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "x")));
        store.clear();
        assertTrue(store.load().isEmpty());
    }

    @Test
    public void save_trimsToMaxFifty_keepingNewest() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        List<ChatMessage> sixty = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            sixty.add(new ChatMessage(ChatMessage.TYPE_USER, "msg" + i));
        }
        store.save(sixty);

        List<ChatMessage> loaded = store.load();
        assertEquals(50, loaded.size());
        // Oldest 10 dropped — first surviving message is "msg10", last is "msg59"
        assertEquals("msg10", loaded.get(0).text);
        assertEquals("msg59", loaded.get(49).text);
    }

    @Test
    public void nullUserId_treatedAsAnon() {
        ChatHistoryStore store = new ChatHistoryStore(ctx, null);
        store.save(Arrays.asList(new ChatMessage(ChatMessage.TYPE_USER, "anon msg")));
        assertEquals(1, store.load().size());
    }

    @Test
    public void corruptJson_returnsEmptyInsteadOfCrashing() {
        // Manually plant a bad JSON blob under the same key the store uses.
        ctx.getSharedPreferences("campus_assistant_chat", Context.MODE_PRIVATE)
                .edit().putString("history:user1", "{not valid json").commit();

        ChatHistoryStore store = new ChatHistoryStore(ctx, "user1");
        assertTrue(store.load().isEmpty());
    }
}
