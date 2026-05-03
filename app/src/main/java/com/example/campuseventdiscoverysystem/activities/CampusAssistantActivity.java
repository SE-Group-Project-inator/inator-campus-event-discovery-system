package com.example.campuseventdiscoverysystem.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.adapters.ChatMessageAdapter;
import com.example.campuseventdiscoverysystem.chat.ChatHistoryStore;
import com.example.campuseventdiscoverysystem.chat.ChatMessage;
import com.example.campuseventdiscoverysystem.chat.FrontChatRepository;
import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.recommendations.RecommendationEngine;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * US-28 Campus AI Assistant.
 *
 * Chat screen backed by the FronTech SaaS. The bot receives a context payload
 * with the signed-in student's identity + onboarding interests so it can
 * personalise answers about events and recommendations.
 *
 * Voice replies are opt-in via the header toggle; the SaaS returns audio_base64
 * which FrontChatRepository plays via MediaPlayer.
 */
public class CampusAssistantActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private TextInputEditText etMessage;
    private ImageButton btnSend, btnVoiceInput;
    private SwitchCompat switchVoice;
    private ChatHistoryStore historyStore;
    private ActivityResultLauncher<Intent> voiceLauncher;

    private static final int MAX_UPCOMING_IN_CONTEXT = 15;
    private static final int MAX_RECS_IN_CONTEXT = 5;
    private static final SimpleDateFormat ISO =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    private final FrontChatRepository chat = new FrontChatRepository();
    private final Map<String, Object> userContext = new HashMap<>();
    /** 3 async loaders (events, RSVPs, recs) must finish before the first send. */
    private final java.util.concurrent.atomic.AtomicInteger contextLoadersPending =
            new java.util.concurrent.atomic.AtomicInteger(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_assistant);

        rv = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        switchVoice = findViewById(R.id.switchVoice);
        ImageButton btnBack = findViewById(R.id.btnBack);

        adapter = new ChatMessageAdapter(messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        historyStore = new ChatHistoryStore(this,
                current != null ? current.getUid() : null);
        registerVoiceLauncher();

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendCurrentMessage());
        btnVoiceInput.setOnClickListener(v -> launchVoiceInput());

        // Hold sends until the bot has events/RSVPs/recs to look at.
        btnSend.setEnabled(false);
        etMessage.setHint("Loading your campus context…");

        loadStudentContext();
        loadEventsContext();
        loadUserRsvpsContext();
        loadRecommendationsContext();

        rehydrateHistoryOrGreet();
    }

    private void rehydrateHistoryOrGreet() {
        List<ChatMessage> saved = historyStore.load();
        if (saved.isEmpty()) {
            appendBot("Hey! I'm your campus assistant. Ask me about upcoming events, "
                    + "what to RSVP to, or your recommendations.");
            return;
        }
        // Re-display past transcript and seed the SaaS history so the bot has memory.
        for (ChatMessage m : saved) {
            messages.add(m);
            chat.seedTurn(m.type == ChatMessage.TYPE_USER ? "user" : "assistant", m.text);
        }
        adapter.notifyDataSetChanged();
        rv.scrollToPosition(messages.size() - 1);
    }

    private void registerVoiceLauncher() {
        voiceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK
                            || result.getData() == null) return;
                    List<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        etMessage.setText(matches.get(0));
                        etMessage.setSelection(etMessage.length());
                    }
                });
    }

    private void launchVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask the assistant…");
        try {
            voiceLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Speech recognition isn't available on this device.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadStudentContext() {
        userContext.put("screen", "CampusAssistantActivity");
        userContext.put("app", "Campus-Inator");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        userContext.put("userId", user.getUid());
        if (user.getEmail() != null) userContext.put("userEmail", user.getEmail());

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener(this::mergeUserDoc);
    }

    private void mergeUserDoc(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return;

        copyIfPresent(doc, "name", "userName");
        copyIfPresent(doc, "school", "school");
        copyIfPresent(doc, "program", "program");
        copyIfPresent(doc, "batch", "batch");
        copyIfPresent(doc, "department", "department");
        copyIfPresent(doc, "studentId", "studentId");
        copyIfPresent(doc, "role", "role");

        Object prefs = doc.get("preferences");
        if (prefs instanceof Map) {
            Object cats = ((Map<?, ?>) prefs).get("categories");
            if (cats instanceof List) {
                userContext.put("interestCategories", cats);
            }
        }
    }

    private void copyIfPresent(DocumentSnapshot doc, String docKey, String ctxKey) {
        Object v = doc.get(docKey);
        if (v != null) userContext.put(ctxKey, v);
    }

    /** Pulls upcoming active events into the chat context so the bot can answer
     *  "what's coming up", "any tech events this week", etc. */
    private void loadEventsContext() {
        FirebaseFirestore.getInstance()
                .collection("events")
                .whereEqualTo("status", "active")
                .whereGreaterThanOrEqualTo("date", new Timestamp(new Date()))
                .orderBy("date")
                .limit(MAX_UPCOMING_IN_CONTEXT)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> events = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        events.add(eventToContextMap(doc));
                    }
                    userContext.put("upcomingEvents", events);
                    userContext.put("currentDateTime", ISO.format(new Date()));
                    userContext.put("currentTimeZone",
                            java.util.TimeZone.getDefault().getID());
                    onContextLoaderDone();
                })
                .addOnFailureListener(e -> onContextLoaderDone());
    }

    /** Adds the current student's existing RSVPs so the bot can answer
     *  "what am I going to" / "is event X on my list". */
    private void loadUserRsvpsContext() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance()
                .collection("rsvps")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> mine = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("eventId", doc.getString("eventId"));
                        entry.put("eventName", doc.getString("eventName"));
                        mine.add(entry);
                    }
                    userContext.put("myRsvps", mine);
                    onContextLoaderDone();
                })
                .addOnFailureListener(e -> onContextLoaderDone());
    }

    /** Personalised recs from US-22 — same algorithm the home screen uses. */
    private void loadRecommendationsContext() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        RecommendationEngine engine = new RecommendationEngine(
                FirebaseFirestore.getInstance(), MAX_RECS_IN_CONTEXT);
        engine.getRecommendations(user.getUid(), new RecommendationEngine.Callback() {
            @Override public void onRecommendations(List<Event> recs, String reason) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Event e : recs) out.add(eventModelToMap(e));
                userContext.put("recommendations", out);
                userContext.put("recommendationReason", reason);
                onContextLoaderDone();
            }
            @Override public void onError(Exception e) { onContextLoaderDone(); }
        });
    }

    private void onContextLoaderDone() {
        if (contextLoadersPending.decrementAndGet() == 0) {
            runOnUiThread(() -> {
                btnSend.setEnabled(true);
                etMessage.setHint("Ask about events, recommendations…");
            });
        }
    }

    private Map<String, Object> eventToContextMap(QueryDocumentSnapshot doc) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", doc.getId());
        m.put("title", doc.getString("title"));
        m.put("description", doc.getString("description"));
        m.put("venue", doc.getString("venue"));
        m.put("society", doc.getString("society"));
        m.put("category", doc.getString("category"));
        m.put("startTime", doc.getString("startTime"));
        m.put("endTime", doc.getString("endTime"));
        Timestamp ts = doc.getTimestamp("date");
        if (ts != null) m.put("date", ISO.format(ts.toDate()));
        Long cap = doc.getLong("capacity");
        Long reg = doc.getLong("registeredCount");
        if (cap != null) m.put("capacity", cap);
        if (reg != null) m.put("registered", reg);
        Double price = doc.getDouble("price");
        m.put("price", price != null ? price : 0.0);
        return m;
    }

    private Map<String, Object> eventModelToMap(Event e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("title", e.getTitle());
        m.put("venue", e.getVenue());
        m.put("society", e.getSociety());
        m.put("category", e.getCategory());
        m.put("startTime", e.getStartTime());
        Timestamp ts = e.getDate();
        if (ts != null) m.put("date", ISO.format(ts.toDate()));
        m.put("price", e.getPrice());
        return m;
    }

    private void sendCurrentMessage() {
        String msg = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) return;

        appendUser(msg);
        etMessage.setText("");
        btnSend.setEnabled(false);

        boolean voice = switchVoice != null && switchVoice.isChecked();

        chat.sendMessage(msg, userContext, voice, new FrontChatRepository.ChatCallback() {
            @Override public void onReply(String text) {
                runOnUiThread(() -> {
                    appendBot(text);
                    btnSend.setEnabled(true);
                });
            }
            @Override public void onError(String err) {
                runOnUiThread(() -> {
                    btnSend.setEnabled(true);
                    Toast.makeText(CampusAssistantActivity.this, err,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void appendUser(String text) { append(new ChatMessage(ChatMessage.TYPE_USER, text)); }
    private void appendBot(String text)  { append(new ChatMessage(ChatMessage.TYPE_BOT,  text)); }

    private void append(ChatMessage m) {
        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);
        if (historyStore != null) historyStore.save(messages);
    }
}
