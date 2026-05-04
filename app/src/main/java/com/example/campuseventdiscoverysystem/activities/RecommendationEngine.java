package com.example.campuseventdiscoverysystem.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campuseventdiscoverysystem.models.Event;
import com.example.campuseventdiscoverysystem.recommendations.RsvpAttendance;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ============================================================
 * RecommendationEngine
 * ============================================================
 *
 * PURPOSE:
 * Intelligent recommendation system that suggests campus events
 * based on user behavior, preferences, and popularity trends.
 *
 * RECOMMENDATION STRATEGY (Priority Order):
 *
 * 1. FOLLOWED SOCIETIES (Primary Signal)
 *    → Suggest events from societies the user follows.
 *
 * 2. USER PREFERENCES (Cold Start Handling)
 *    → Uses onboarding-selected categories if no follow data exists.
 *
 * 3. TRENDING EVENTS (Fallback)
 *    → Most popular upcoming events sorted by registrations.
 *
 * FILTER RULES:
 * - Only ACTIVE events are considered
 * - Past events are automatically excluded
 * - Already RSVP'd events are excluded
 *
 * OUTPUT:
 * Returns a ranked list of Event objects with reasoning label.
 */
public class RecommendationEngine {

    /**
     * Callback interface for async recommendation results.
     */
    public interface Callback {

        /**
         * Called when recommendations are successfully generated.
         *
         * @param events Recommended event list
         * @param reason Explanation of recommendation source
         */
        void onRecommendations(@NonNull List<Event> events, @NonNull String reason);

        /**
         * Called when recommendation generation fails.
         */
        void onError(@NonNull Exception e);
    }

    // Firestore database reference
    private final FirebaseFirestore db;

    // Maximum number of recommendations returned
    private final int limit;

    public RecommendationEngine(FirebaseFirestore db, int limit) {
        this.db = db;
        this.limit = limit;
    }

    /**
     * MAIN ENTRY POINT
     *
     * Builds personalized recommendations for a user by:
     * - Fetching followed societies
     * - Fetching RSVP history (to avoid duplicates)
     * - Falling back to preferences or trending events
     */
    public void getRecommendations(@NonNull String uid, @NonNull Callback cb) {

        // Step 1: Get societies followed by user
        db.collection("societyFollows")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(followSnap -> {

                    List<String> followedSocietyIds = new ArrayList<>();

                    for (DocumentSnapshot doc : followSnap.getDocuments()) {
                        String sid = doc.getString("societyId");
                        if (sid != null) followedSocietyIds.add(sid);
                    }

                    // Step 2: Get RSVP history to exclude already attended events
                    loadRsvpdIds(uid, rsvpdEventIds -> {

                        // If user follows societies → prioritize them
                        if (!followedSocietyIds.isEmpty()) {

                            fetchUpcomingByFollowedSocieties(
                                    followedSocietyIds,
                                    rsvpdEventIds,
                                    events -> {

                                        if (!events.isEmpty()) {
                                            cb.onRecommendations(events,
                                                    "From societies you follow");
                                        } else {
                                            loadPreferencesAndRecommend(uid, rsvpdEventIds, cb);
                                        }
                                    },
                                    cb::onError
                            );

                        } else {
                            loadPreferencesAndRecommend(uid, rsvpdEventIds, cb);
                        }

                    }, cb::onError);

                })

                // Fallback if society follow query fails
                .addOnFailureListener(e ->
                        loadRsvpdIds(uid,
                                rsvpdEventIds ->
                                        loadPreferencesAndRecommend(uid, rsvpdEventIds, cb),
                                cb::onError));
    }

    /**
     * Loads all RSVP'd event IDs for a user.
     * Used to avoid recommending already attended events.
     */
    private void loadRsvpdIds(String uid,
                              Consumer<Set<String>> ok,
                              Consumer<Exception> err) {

        db.collection("rsvps")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(snap -> {

                    Set<String> ids = new HashSet<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String eid = d.getString("eventId");
                        if (eid != null) ids.add(eid);
                    }

                    ok.accept(ids);
                })
                .addOnFailureListener(err::accept);
    }

    /**
     * Fetch events from societies the user follows.
     */
    private void fetchUpcomingByFollowedSocieties(
            List<String> societyIds,
            Set<String> exclude,
            Consumer<List<Event>> ok,
            Consumer<Exception> err) {

        // Firestore whereIn limit safety (max 10)
        List<String> ids = societyIds.size() > 10
                ? societyIds.subList(0, 10)
                : societyIds;

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereIn("societyId", ids)
                .get()
                .addOnSuccessListener(snap ->
                        ok.accept(mapFilterSort(snap, exclude, null)))
                .addOnFailureListener(err::accept);
    }

    /**
     * Loads user preferences and uses them for recommendation fallback.
     */
    private void loadPreferencesAndRecommend(
            String uid,
            Set<String> exclude,
            Callback cb) {

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {

                    List<String> cats = readPreferenceCategories(doc);

                    if (cats != null && !cats.isEmpty()) {

                        fetchUpcomingByCategories(cats, exclude, events -> {

                            if (!events.isEmpty()) {
                                cb.onRecommendations(events,
                                        "Picked from your interests");
                            } else {
                                fallbackTrending(exclude, cb);
                            }

                        }, cb::onError);

                    } else {
                        fallbackTrending(exclude, cb);
                    }

                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Extracts category preferences from Firestore user document.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static List<String> readPreferenceCategories(DocumentSnapshot doc) {

        if (doc == null || !doc.exists()) return null;

        Object prefs = doc.get("preferences");
        if (!(prefs instanceof Map)) return null;

        Object cats = ((Map<String, Object>) prefs).get("categories");
        if (!(cats instanceof List)) return null;

        List<String> out = new ArrayList<>();
        for (Object o : (List<Object>) cats) {
            if (o instanceof String) out.add((String) o);
        }

        return out;
    }

    /**
     * Fallback recommendation:
     * Trending events sorted by popularity (registeredCount).
     */
    private void fallbackTrending(Set<String> exclude, Callback cb) {

        db.collection("events")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snap -> {

                    long nowMs = System.currentTimeMillis();
                    List<Event> out = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {

                        if (exclude.contains(doc.getId())) continue;

                        Event e = doc.toObject(Event.class);
                        if (e == null) continue;

                        e.setId(doc.getId());

                        Timestamp ts = e.getDate();

                        // Only future events
                        if (ts == null || ts.toDate().getTime() < nowMs) continue;

                        out.add(e);
                    }

                    // Sort by popularity
                    Collections.sort(out, (a, b) ->
                            Integer.compare(
                                    b.getRegisteredCount(),
                                    a.getRegisteredCount()
                            ));

                    // Limit results
                    if (out.size() > limit)
                        out = new ArrayList<>(out.subList(0, limit));

                    cb.onRecommendations(out, "Trending on campus");
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Fetch events by category preference.
     */
    private void fetchUpcomingByCategories(
            List<String> categories,
            Set<String> exclude,
            Consumer<List<Event>> ok,
            Consumer<Exception> err) {

        db.collection("events")
                .whereEqualTo("status", "active")
                .whereIn("category", categories)
                .get()
                .addOnSuccessListener(snap ->
                        ok.accept(mapFilterSort(snap, exclude, null)))
                .addOnFailureListener(err::accept);
    }

    /**
     * Shared filtering + sorting logic:
     * - removes past events
     * - removes excluded events
     * - limits results
     */
    private List<Event> mapFilterSort(
            QuerySnapshot snap,
            Set<String> exclude,
            java.util.Comparator<Event> secondary) {

        long nowMs = System.currentTimeMillis();
        List<Event> out = new ArrayList<>();

        for (DocumentSnapshot doc : snap.getDocuments()) {

            if (exclude.contains(doc.getId())) continue;

            Event e = doc.toObject(Event.class);
            if (e == null) continue;

            e.setId(doc.getId());

            Timestamp ts = e.getDate();

            if (ts == null || ts.toDate().getTime() < nowMs) continue;

            out.add(e);
        }

        Collections.sort(out, (a, b) -> {

            int byDate = Long.compare(
                    a.getDate().toDate().getTime(),
                    b.getDate().toDate().getTime()
            );

            if (byDate != 0 || secondary == null) return byDate;

            return secondary.compare(a, b);
        });

        if (out.size() > limit)
            out = new ArrayList<>(out.subList(0, limit));

        return out;
    }

    /**
     * ============================================================
     * PURE RANKING FUNCTION (TESTABLE LOGIC)
     * ============================================================
     *
     * Determines most relevant society based on:
     * - Attendance frequency
     * - Recency of attendance
     */
    @Nullable
    public static String pickTopSociety(@NonNull List<RsvpAttendance> history) {

        if (history.isEmpty()) return null;

        Map<String, Integer> count = new HashMap<>();
        Map<String, Long> latest = new HashMap<>();

        for (RsvpAttendance a : history) {

            if (a.society == null || a.society.isEmpty()) continue;

            count.merge(a.society, 1, Integer::sum);

            Long prev = latest.get(a.society);

            if (prev == null || a.dateMillis > prev)
                latest.put(a.society, a.dateMillis);
        }

        if (count.isEmpty()) return null;

        String best = null;
        int bestCount = -1;
        long bestRecency = Long.MIN_VALUE;

        for (Map.Entry<String, Integer> e : count.entrySet()) {

            int c = e.getValue();
            long r = latest.getOrDefault(e.getKey(), Long.MIN_VALUE);

            if (c > bestCount || (c == bestCount && r > bestRecency)) {
                best = e.getKey();
                bestCount = c;
                bestRecency = r;
            }
        }

        return best;
    }

    /**
     * Functional interface for internal async handling.
     */
    private interface Consumer<T> {
        void accept(T t);
    }
}