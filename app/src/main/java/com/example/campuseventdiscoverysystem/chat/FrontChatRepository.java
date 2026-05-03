package com.example.campuseventdiscoverysystem.chat;

import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * US-28 client for the FronTech chatbot SaaS.
 * One-file REST client supplied by the SaaS owner.
 * AGENT_ID must be filled in with the agent UUID before shipping.
 */
public class FrontChatRepository {

    private static final String BASE_URL = "https://frontech2-production.up.railway.app";
    // TODO(US-28): replace with the agent UUID from the FronTech dashboard.
    private static final String AGENT_ID = "eb221ade-3ffb-4fbd-a28e-150fc5662ff5";
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();
    private final List<JsonObject> history = new ArrayList<>();

    public interface ChatCallback {
        void onReply(String text);
        void onError(String error);
    }

    /** Send a message. `context` is any Map<String,Object> — current screen, data, user info. */
    public void sendMessage(String userMessage,
                            Map<String, Object> context,
                            boolean withVoice,
                            ChatCallback cb) {
        JsonObject body = new JsonObject();
        body.addProperty("agent_id", AGENT_ID);
        body.addProperty("message", userMessage);
        body.addProperty("voice", withVoice);
        if (context != null) body.add("context", gson.toJsonTree(context));

        JsonArray hist = new JsonArray();
        for (JsonObject h : history) hist.add(h);
        body.add("history", hist);

        // Diagnostic: confirm what context the bot is actually receiving.
        // Filter logcat by tag "ChatPayload" to inspect.
        Log.d("ChatPayload", body.toString());

        Request req = new Request.Builder()
                .url(BASE_URL + "/api/chat")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                cb.onError("Network: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response resp) throws IOException {
                String json = resp.body() != null ? resp.body().string() : "";
                if (!resp.isSuccessful()) {
                    cb.onError("Server " + resp.code() + ": " + json);
                    return;
                }
                JsonObject result = gson.fromJson(json, JsonObject.class);
                String reply = result.get("reply").getAsString();

                addTurn("user", userMessage);
                addTurn("assistant", reply);

                cb.onReply(reply);

                if (result.has("audio_base64")) {
                    playAudioBase64(result.get("audio_base64").getAsString());
                }
            }
        });
    }

    private void addTurn(String role, String content) {
        JsonObject turn = new JsonObject();
        turn.addProperty("role", role);
        turn.addProperty("content", content);
        history.add(turn);
        if (history.size() > 20) history.remove(0); // keep last 10 turns
    }

    /** Seed history from persistent storage so the bot has context after app restart. */
    public void seedTurn(String role, String content) {
        addTurn(role, content);
    }

    private void playAudioBase64(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            File tmp = File.createTempFile("front-tts", ".mp3");
            try (FileOutputStream fos = new FileOutputStream(tmp)) { fos.write(bytes); }
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(tmp.getAbsolutePath());
            mp.setOnPreparedListener(MediaPlayer::start);
            mp.setOnCompletionListener(p -> { p.release(); tmp.delete(); });
            mp.prepareAsync();
        } catch (IOException e) {
            // user already saw the text reply
        }
    }

    public void clearHistory() { history.clear(); }
}
