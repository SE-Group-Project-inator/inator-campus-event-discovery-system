package com.example.campuseventdiscoverysystem.recommendations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.campuseventdiscoverysystem.models.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * US-22 Personalised Recommendations.
 *
 * Strategy (in order):
 *   1. History-based — find the student's most-attended society.
 *      Tie-break: most-recent attendance wins.
 *      Recommend upcoming events from that society (excluding ones already RSVPd).
 *   2. Cold-start — if the student has no attendance history, use the category
 *      preferences captured during onboarding (users/{uid}.preferences.categories).
 *   3. Trending fallback — if neither signal is available, return the top
 *      upcoming events by registeredCount.
 */
public class RecommendationEngine {

    public interface Callback {
        void onRecommendations(@NonNull List<Event> events, @NonNull String reason);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;
    private final int limit;

    public RecommendationEngine(FirebaseFirestore db, int limit) {
        this.db = db;
        this.limit = limit;
    }

    public void getRecommendations(@NonNull String uid, @NonNull Callback cb) {
        loadAttendanceHistory(uid, history -> {
            Set<String> rsvpdEventIds = new HashSet<>();
            for (RsvpAttendance a : history) rsvpdEventIds.add(a.eventId);

            String topSociety = pickTopSociety(history);
            if (topSociety != null) {
                fetchUpcomingBySociety(topSociety, rsvpdEventIds, events -> {
                    if (!events.isEmpty()) {
                        cb.onRecommendations(events,
                                "Because you've been to " + topSociety + " events");
                    } else {
                        loadPreferencesAndRecommend(uid, rsvpdEventIds, cb);
                    }
                }, cb::onError);
            } else {
                loadPreferencesAndRecommend(uid, rsvpdEventIds, cb);
            }
        }, cb::onError);
    }

    private void loadPreferencesAndRecommend(String uid,
                                             Set<String> exclude,
                                             Callback cb) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    List<String> cats = readPreferenceCategories(doc);
                    if (cats != null && !cats.isEmpty()) {
                        fetchUpcomingByCategories(cats, exclude, events -> {
                            if (!events.isEmpty()) {
                                cb.onRecommendations(events, "Picked from your interests");
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

    private void fallbackTrending(Set<String> exclude, Callback cb) {
        // Single equality filter → no composite index required.
        // Date filter, popularity sort, and limit applied client-side.
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
                        if (ts == null || ts.toDate().getTime() < nowMs) continue;
                        out.add(e);
                    }
                    Collections.sort(out, (a, b) ->
                            Integer.compare(b.getRegisteredCount(), a.getRegisteredCount()));
                    if (out.size() > limit) out = new ArrayList<>(out.subList(0, limit));
                    cb.onRecommendations(out, "Trending on campus");
                })
                .addOnFailureListener(cb::onError);
    }

    private void fetchUpcomingBySociety(String society,
                                        Set<String> exclude,
                                        Consumer<List<Event>> ok,
                                        Consumer<Exception> err) {
        // Two equality filters only → satisfied by Firestore auto-index, no
        // composite-index deploy required. Date filter + ordering done client-side.
        db.collection("events")
                .whereEqualTo("status", "active")
                .whereEqualTo("society", society)
                .get()
                .addOnSuccessListener(snap -> ok.accept(mapFilterSort(snap, exclude, null)))
                .addOnFailureListener(err::accept);
    }

    private void fetchUpcomingByCategories(List<String> categories,
                                           Set<String> exclude,
                                           Consumer<List<Event>> ok,
                                           Consumer<Exception> err) {
        // whereIn + equality only → auto-indexed. Date filter + ordering done client-side.
        db.collection("events")
                .whereEqualTo("status", "active")
                .whereIn("category", categories)
                .get()
                .addOnSuccessListener(snap -> ok.accept(mapFilterSort(snap, exclude, null)))
                .addOnFailureListener(err::accept);
    }

    /**
     * Single-pass mapper: drops events the user already RSVP'd, drops past events,
     * sorts by date asc, applies limit. Optional secondary comparator to break ties
     * (e.g. by registeredCount) when two events share the same date.
     */
    private List<Event> mapFilterSort(QuerySnapshot snap,
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
            int byDate = Long.compare(a.getDate().toDate().getTime(),
                                      b.getDate().toDate().getTime());
            if (byDate != 0 || secondary == null) return byDate;
            return secondary.compare(a, b);
        });
        if (out.size() > limit) out = new ArrayList<>(out.subList(0, limit));
        return out;
    }

    private void loadAttendanceHistory(String uid,
                                       Consumer<List<RsvpAttendance>> ok,
                                       Consumer<Exception> err) {
        db.collection("rsvps")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(rsvpSnap -> {
                    List<DocumentSnapshot> rsvps = rsvpSnap.getDocuments();
                    if (rsvps.isEmpty()) {
                        ok.accept(Collections.emptyList());
                        return;
                    }

                    List<RsvpAttendance> out = new ArrayList<>();
                    AtomicInteger remaining = new AtomicInteger(rsvps.size());

                    for (DocumentSnapshot r : rsvps) {
                        String eventId = r.getString("eventId");
                        if (eventId == null) {
                            if (remaining.decrementAndGet() == 0) ok.accept(out);
                            continue;
                        }
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(eventDoc -> {
                                    String society = eventDoc.getString("society");
                                    Timestamp ts = eventDoc.getTimestamp("date");
                                    long millis = ts != null ? ts.toDate().getTime() : 0L;
                                    if (society != null && !society.isEmpty()) {
                                        synchronized (out) {
                                            out.add(new RsvpAttendance(eventId, society, millis));
                                        }
                                    }
                                    if (remaining.decrementAndGet() == 0) ok.accept(out);
                                })
                                .addOnFailureListener(e -> {
                                    if (remaining.decrementAndGet() == 0) ok.accept(out);
                                });
                    }
                })
                .addOnFailureListener(err::accept);
    }

    /**
     * Pure ranking logic — testable without Firestore.
     * Returns the society with the highest attendance count, tie-broken by
     * most recent attendance. Returns null when input is empty.
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
            if (prev == null || a.dateMillis > prev) {
                latest.put(a.society, a.dateMillis);
            }
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

    // Local Consumer to avoid requiring Java 8 desugar for java.util.function on older minSdk.
    private interface Consumer<T> { void accept(T t); }
}
