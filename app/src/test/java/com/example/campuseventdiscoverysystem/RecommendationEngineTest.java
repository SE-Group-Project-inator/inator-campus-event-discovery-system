package com.example.campuseventdiscoverysystem;

import com.example.campuseventdiscoverysystem.recommendations.RecommendationEngine;
import com.example.campuseventdiscoverysystem.recommendations.RsvpAttendance;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * US-22 — pure ranking logic tests for the most-common-society pick
 * with most-recent-attendance tie-break.
 */
public class RecommendationEngineTest {

    @Test
    public void emptyHistory_returnsNull() {
        assertNull(RecommendationEngine.pickTopSociety(Collections.emptyList()));
    }

    @Test
    public void singleAttendance_returnsThatSociety() {
        List<RsvpAttendance> hist = Collections.singletonList(
                new RsvpAttendance("e1", "Math Society", 100L));
        assertEquals("Math Society", RecommendationEngine.pickTopSociety(hist));
    }

    @Test
    public void mostFrequentSocietyWins() {
        List<RsvpAttendance> hist = Arrays.asList(
                new RsvpAttendance("e1", "Tech",  100L),
                new RsvpAttendance("e2", "Tech",  200L),
                new RsvpAttendance("e3", "Music", 300L)
        );
        assertEquals("Tech", RecommendationEngine.pickTopSociety(hist));
    }

    @Test
    public void tieBrokenByMostRecentAttendance() {
        List<RsvpAttendance> hist = Arrays.asList(
                new RsvpAttendance("e1", "Tech",  100L),
                new RsvpAttendance("e2", "Tech",  200L),
                new RsvpAttendance("e3", "Music", 300L),
                new RsvpAttendance("e4", "Music", 500L)
        );
        // Both "Tech" and "Music" appear twice. Music's latest (500) > Tech's latest (200).
        assertEquals("Music", RecommendationEngine.pickTopSociety(hist));
    }

    @Test
    public void nullAndEmptySocietiesAreIgnored() {
        List<RsvpAttendance> hist = Arrays.asList(
                new RsvpAttendance("e1", null,    100L),
                new RsvpAttendance("e2", "",      200L),
                new RsvpAttendance("e3", "Drama", 300L)
        );
        assertEquals("Drama", RecommendationEngine.pickTopSociety(hist));
    }

    @Test
    public void allSocietiesNullOrEmpty_returnsNull() {
        List<RsvpAttendance> hist = Arrays.asList(
                new RsvpAttendance("e1", null, 100L),
                new RsvpAttendance("e2", "",   200L)
        );
        assertNull(RecommendationEngine.pickTopSociety(hist));
    }
}
