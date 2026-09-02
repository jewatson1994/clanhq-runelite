package com.clanhq.verifier.daily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ActivityTelemetryDetectorTest
{
    private final List<Observation> observations = new ArrayList<>();
    private ActivityTelemetryDetector detector;

    @Before
    public void setUp()
    {
        detector = new ActivityTelemetryDetector(
            (rsn, activity, quantity, metadata) -> observations.add(
                new Observation(rsn, activity, quantity, metadata)),
            () -> "Mr Dimples");
    }

    @Test
    public void exposesEveryRuneLiteBackedActivity()
    {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(
            "pest_control_game", "hunter_rumour", "fishing_trawler_game",
            "barbarian_assault_wave", "giants_foundry_commission",
            "mahogany_homes_contract", "agility_lap",
            "tithe_farm_fruit_deposited"));
        assertEquals(expected, ActivityTelemetryDetector.supportedActivities());
    }

    @Test
    public void recognizesRuneLiteBarbarianAssaultDurationMessage()
    {
        detector.onChatMessage("Wave 10 duration: 4:29");

        assertEquals(Arrays.asList("barbarian_assault_wave"), activities());
    }

    @Test
    public void recognizesExactPestControlRewardDialogueOncePerCallback()
    {
        detector.onPestControlDialogue(
            "Congratulations! You managed to destroy all the portals!<br>"
                + "We've awarded you <col=0000ff>8 Void Knight Commendation points</col>. "
                + "Please also accept these coins as a reward.");

        assertEquals(Arrays.asList("pest_control_game"), activities());
        assertEquals(1, observations.get(0).quantity);
    }

    @Test
    public void countsAllJagexBackedCompletionCounterDeltas()
    {
        List<String> counterActivities = Arrays.asList(
            "pest_control_game", "hunter_rumour", "fishing_trawler_game",
            "giants_foundry_commission", "mahogany_homes_contract");
        for (String activity : counterActivities)
        {
            detector.resetCompletionCounter(activity, 10);
            detector.onCompletionCounter(activity, 12);
        }

        assertEquals(counterActivities, activities());
        assertTrue(observations.stream().allMatch(value -> value.quantity == 2));
    }

    @Test
    public void ignoresCounterInitializationAndReset()
    {
        detector.onCompletionCounter("hunter_rumour", 20);
        detector.onCompletionCounter("hunter_rumour", 0);
        assertTrue(observations.isEmpty());
    }

    @Test
    public void doesNotDiscardIdenticalConsecutiveCompletions()
    {
        detector.onChatMessage("Wave 9 duration: 2:05");
        detector.onChatMessage("Wave 9 duration: 2:05");
        assertEquals(2, observations.size());
    }

    @Test
    public void countsStandardAgilityLapOnlyAtCourseEndpoint()
    {
        detector.resetSession(1_000, 0, 0, 0);
        detector.onAgilityExperience(1_010, 12853, 3235, 3417, 0);
        detector.onAgilityExperience(1_020, 12853, 3236, 3417, 0);

        assertEquals(1, observations.size());
        assertEquals("agility_lap", observations.get(0).activity);
        assertEquals("Varrock", observations.get(0).metadata.get("course"));
    }

    @Test
    public void countsEverySupportedStandardCourseEndpoint()
    {
        int[][] endpoints = {
            {9781, 2484, 3437, 0}, {6200, 1554, 3640, 0},
            {12338, 3103, 3261, 0}, {13105, 3299, 3194, 0},
            {13356, 3364, 2830, 0}, {12853, 3236, 3417, 0},
            {10559, 2652, 4039, 1}, {10039, 2543, 3553, 0},
            {13878, 3510, 3485, 0}, {11050, 2770, 2747, 0},
            {5944, 1522, 3625, 0}, {12084, 3029, 3332, 0},
            {11837, 2993, 3933, 0}, {14234, 3528, 9873, 0},
            {10806, 2704, 3464, 0}, {13358, 3363, 2998, 0},
            {10553, 2653, 3676, 0}, {12895, 3240, 6109, 0},
            {10547, 2668, 3297, 0}
        };
        detector.resetSession(1_000, 0, 0, 0);
        int experience = 1_000;
        for (int[] endpoint : endpoints)
        {
            experience += 10;
            detector.onAgilityExperience(experience, endpoint[0],
                endpoint[1], endpoint[2], endpoint[3]);
        }

        assertEquals(endpoints.length, observations.size());
        assertTrue(activities().stream().allMatch("agility_lap"::equals));
    }

    @Test
    public void countsWyrmLapOnlyWhenProgressEntersCompletionState()
    {
        detector.resetSession(1_000, 0, 5, 5);
        detector.onWyrmAgilityProgress(false, 6);
        detector.onWyrmAgilityProgress(false, 6);
        detector.onWyrmAgilityProgress(true, 6);

        assertEquals(2, observations.size());
        assertEquals("Colossal Wyrm Basic",
            observations.get(0).metadata.get("course"));
        assertEquals("Colossal Wyrm Advanced",
            observations.get(1).metadata.get("course"));
    }

    @Test
    public void countsPositiveTitheDepositsAndIgnoresSackReset()
    {
        detector.resetSession(1_000, 12, 0, 0);
        detector.onTitheSackAmount(17);
        detector.onTitheSackAmount(0);
        detector.onTitheSackAmount(3);

        assertEquals(2, observations.size());
        assertEquals(5, observations.get(0).quantity);
        assertEquals(3, observations.get(1).quantity);
        assertTrue(activities().stream().allMatch(
            "tithe_farm_fruit_deposited"::equals));
    }

    @Test
    public void ignoresSignalsWithoutLoggedInCharacter()
    {
        detector = new ActivityTelemetryDetector(
            (rsn, activity, quantity, metadata) -> observations.add(
                new Observation(rsn, activity, quantity, metadata)),
            () -> null);
        detector.onChatMessage("Wave 9 duration: 2:05");
        assertTrue(observations.isEmpty());
    }

    private List<String> activities()
    {
        List<String> result = new ArrayList<>();
        for (Observation observation : observations)
        {
            result.add(observation.activity);
        }
        return result;
    }

    private static final class Observation
    {
        private final String rsn;
        private final String activity;
        private final int quantity;
        private final Map<String, String> metadata;

        private Observation(String rsn, String activity, int quantity,
            Map<String, String> metadata)
        {
            this.rsn = rsn;
            this.activity = activity;
            this.quantity = quantity;
            this.metadata = metadata;
        }
    }
}
