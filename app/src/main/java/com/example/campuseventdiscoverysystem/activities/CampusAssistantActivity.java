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
 * =============================================================================
 * CampusAssistantActivity
 * =============================================================================
 *
 * AI-powered campus chatbot screen that provides:
 *
 * FEATURES:
 * ---------------------------------------------------------------------------
 * - Chat with AI assistant about campus events
 * - Personalized recommendations (based on user profile)
 * - Voice input (speech-to-text)
 * - Optional voice response (via SaaS audio_base64)
 * - Conversation history persistence
 * - Real-time Firestore context injection:
 *      • Upcoming events
 *      • User RSVPs
 *      • Personalized recommendations
 *      • User profile data
 *
 * BACKEND:
 * ---------------------------------------------------------------------------
 * Uses FrontChatRepository (SaaS API wrapper) which:
 * - Sends chat messages
 * - Maintains conversation memory
 * - Supports voice response playback
 */
public class CampusAssistantActivity extends AppCompatActivity {

    // ========================= UI =========================
    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messages = new ArrayList<>();

    private TextInputEditText etMessage;
    private ImageButton btnSend, btnVoiceInput;
    private SwitchCompat switchVoice;

    private ChatHistoryStore historyStore;
    private ActivityResultLauncher<Intent> voiceLauncher;

    // ========================= CONTEXT CONFIG =========================
    private static final int MAX_UPCOMING_IN_CONTEXT = 15;
    private static final int MAX_RECS_IN_CONTEXT = 5;

    /** Date formatter used for event timestamps */
    private static final SimpleDateFormat ISO =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    // ========================= AI BACKEND =========================
    private final FrontChatRepository chat = new FrontChatRepository();

    /** Context injected into AI (user profile + events + RSVPs + recs) */
    private final Map<String, Object> userContext = new HashMap<>();

    /**
     * Tracks async loading of:
     * 1. Events
     * 2. RSVPs
     * 3. Recommendations
     *
     * Chat is disabled until all are loaded.
     */
    private final java.util.concurrent.atomic.AtomicInteger contextLoadersPending =
            new java.util.concurrent.atomic.AtomicInteger(3);

    /**
     * Activity lifecycle start.
     * Initializes UI, chat system, voice input, and context loaders.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_assistant);

        // Bind UI
        rv = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        switchVoice = findViewById(R.id.switchVoice);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerView
        adapter = new ChatMessageAdapter(messages);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Initialize history storage
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        historyStore = new ChatHistoryStore(this,
                current != null ? current.getUid() : null);

        // Voice input setup
        registerVoiceLauncher();

        // Navigation
        btnBack.setOnClickListener(v -> finish());

        // Chat actions
        btnSend.setOnClickListener(v -> sendCurrentMessage());
        btnVoiceInput.setOnClickListener(v -> launchVoiceInput());

        // Disable chat until context loads
        btnSend.setEnabled(false);
        etMessage.setHint("Loading your campus context…");

        // Load AI context layers
        loadStudentContext();
        loadEventsContext();
        loadUserRsvpsContext();
        loadRecommendationsContext();

        // Restore previous chat or greet user
        rehydrateHistoryOrGreet();
    }

    /**
     * Restores previous chat history or shows greeting message.
     */
    private void rehydrateHistoryOrGreet() {

        List<ChatMessage> saved = historyStore.load();

        if (saved.isEmpty()) {
            appendBot("Hey! I'm your campus assistant. Ask me about upcoming events, "
                    + "what to RSVP to, or your recommendations.");
            return;
        }

        // Restore chat history into UI + AI memory
        for (ChatMessage m : saved) {
            messages.add(m);
            chat.seedTurn(m.type == ChatMessage.TYPE_USER ? "user" : "assistant", m.text);
        }

        adapter.notifyDataSetChanged();
        rv.scrollToPosition(messages.size() - 1);
    }

    /**
     * Registers speech-to-text launcher
     */
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

    /**
     * Launches voice input intent
     */
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

    /**
     * Loads basic student profile into AI context
     */
    private void loadStudentContext() {

        userContext.put("screen", "CampusAssistantActivity");
        userContext.put("app", "Campus-Inator");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        userContext.put("userId", user.getUid());
        if (user.getEmail() != null)
            userContext.put("userEmail", user.getEmail());

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).get()
                .addOnSuccessListener(this::mergeUserDoc);
    }

    /**
     * Merges Firestore user document into AI context
     */
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

    /**
     * Copies Firestore field into context map if exists
     */
    private void copyIfPresent(DocumentSnapshot doc, String docKey, String ctxKey) {
        Object v = doc.get(docKey);
        if (v != null) userContext.put(ctxKey, v);
    }

    /**
     * Loads upcoming events into AI context
     */
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

    /**
     * Loads user RSVPs into context
     */
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

    /**
     * Loads personalized recommendations into context
     */
    private void loadRecommendationsContext() {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        RecommendationEngine engine = new RecommendationEngine(
                FirebaseFirestore.getInstance(), MAX_RECS_IN_CONTEXT);

        engine.getRecommendations(user.getUid(), new RecommendationEngine.Callback() {

            @Override
            public void onRecommendations(List<Event> recs, String reason) {

                List<Map<String, Object>> out = new ArrayList<>();

                for (Event e : recs) out.add(eventModelToMap(e));

                userContext.put("recommendations", out);
                userContext.put("recommendationReason", reason);

                onContextLoaderDone();
            }

            @Override
            public void onError(Exception e) {
                onContextLoaderDone();
            }
        });
    }

    /**
     * Marks one context loader as completed
     */
    private void onContextLoaderDone() {

        if (contextLoadersPending.decrementAndGet() == 0) {
            runOnUiThread(() -> {
                btnSend.setEnabled(true);
                etMessage.setHint("Ask about events, recommendations…");
            });
        }
    }

    /**
     * Converts Firestore event into AI context format
     */
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

    /**
     * Converts Event model into AI context format
     */
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

    /**
     * Sends user message to AI backend
     */
    private void sendCurrentMessage() {

        String msg = etMessage.getText() == null
                ? "" : etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(msg)) return;

        appendUser(msg);
        etMessage.setText("");
        btnSend.setEnabled(false);

        boolean voice = switchVoice != null && switchVoice.isChecked();

        chat.sendMessage(msg, userContext, voice,
                new FrontChatRepository.ChatCallback() {

                    @Override
                    public void onReply(String text) {
                        runOnUiThread(() -> {
                            appendBot(text);
                            btnSend.setEnabled(true);
                        });
                    }

                    @Override
                    public void onError(String err) {
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(CampusAssistantActivity.this,
                                    err, Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    /**
     * Adds user message to chat UI
     */
    private void appendUser(String text) {
        append(new ChatMessage(ChatMessage.TYPE_USER, text));
    }

    /**
     * Adds bot message to chat UI
     */
    private void appendBot(String text) {
        append(new ChatMessage(ChatMessage.TYPE_BOT, text));
    }

    /**
     * Inserts message into list + updates UI + saves history
     */
    private void append(ChatMessage m) {

        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        rv.scrollToPosition(messages.size() - 1);

        if (historyStore != null)
            historyStore.save(messages);
    }
}