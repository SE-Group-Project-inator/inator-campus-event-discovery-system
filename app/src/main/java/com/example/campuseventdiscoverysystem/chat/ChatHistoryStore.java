package com.example.campuseventdiscoverysystem.chat;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Per-user local persistence of the assistant chat transcript.
 * Backed by SharedPreferences (small, simple, no Firestore quota).
 * Last MAX_PERSISTED messages kept; older ones are pruned on save.
 */
public class ChatHistoryStore {

    private static final String PREF_NAME = "campus_assistant_chat";
    private static final int MAX_PERSISTED = 50;

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final String key;

    public ChatHistoryStore(Context ctx, String userId) {
        this.prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.key = "history:" + (userId != null ? userId : "anon");
    }

    public List<ChatMessage> load() {
        String json = prefs.getString(key, null);
        if (json == null) return new ArrayList<>();
        Type t = new TypeToken<ArrayList<ChatMessage>>() {}.getType();
        try {
            ArrayList<ChatMessage> out = gson.fromJson(json, t);
            return out != null ? out : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void save(List<ChatMessage> messages) {
        List<ChatMessage> trimmed = messages;
        if (messages.size() > MAX_PERSISTED) {
            trimmed = messages.subList(messages.size() - MAX_PERSISTED, messages.size());
        }
        prefs.edit().putString(key, gson.toJson(trimmed)).apply();
    }

    public void clear() {
        prefs.edit().remove(key).apply();
    }
}
