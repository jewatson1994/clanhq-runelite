package com.clanhq.verifier.daily;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ActivityTelemetryDetectorTest
{
    @Test
    public void recognizesEveryChatDrivenCompletionMessage()
    {
        assertEquals("hunter_rumour",
            ActivityTelemetryDetector.activityForChatMessage(
                "You have completed <col=ef1020>18</col> rumours for the Hunter Guild."));
        assertEquals("pest_control_game",
            ActivityTelemetryDetector.activityForChatMessage(
                "You have won! You have earned 5 Void Knight commendation points."));
        assertEquals("pest_control_game",
            ActivityTelemetryDetector.activityForChatMessage(
                "We've awarded you with Void Knight Commendation points and some coins to show our appreciation."));
        assertEquals("pest_control_game",
            ActivityTelemetryDetector.activityForChatMessage(
                "Congratulations! You managed to destroy all the portals!<br>"
                    + "We've awarded you <col=0000ff>8 Void Knight Commendation points</col>. "
                    + "Please also accept these coins as a reward."));
        assertEquals("giants_foundry_commission",
            ActivityTelemetryDetector.activityForChatMessage(
                "Total swords made in the Giants' Foundry: 93"));
        assertEquals("barbarian_assault_wave",
            ActivityTelemetryDetector.activityForChatMessage(
                "<col=ef1020>Wave 9 duration: <col=ffffff>2:05"));
        assertEquals("mahogany_homes_contract",
            ActivityTelemetryDetector.activityForChatMessage(
                "You have completed 27 contracts with a total of 108 points."));
    }

    @Test
    public void rejectsOldGuessedMessagesAndNonCompletionMessages()
    {
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "You have completed the game"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "You completed your Hunter rumour"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "You have successfully defended the island!"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "Fishing Trawler game ended"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "You completed a Giants' Foundry commission"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "You completed your Mahogany Homes contract"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "All of the Penance Fighters have been killed!"));
        assertNull(ActivityTelemetryDetector.activityForChatMessage(
            "---- Wave: 2 ----"));
    }

    @Test
    public void pestControlDialogueOnlyEmitsPestControl()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);

        detector.onPestControlDialogue(
            "Congratulations! You managed to destroy all the portals!<br>"
                + "We've awarded you 8 Void Knight Commendation points. "
                + "Please also accept these coins as a reward.");
        detector.onPestControlDialogue(
            "You have completed 18 rumours for the Hunter Guild.");

        assertEquals(1, events.size());
        assertEvent(events.get(0), "pest_control_game", 1);
    }

    @Test
    public void standardAgilityLapUsesXpGainAtCourseEnd()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeGameState(1_000, 0, 0, 0, 0);

        detector.onAgilityExperience(1_010,
            new WorldPoint(3103, 3261, 0));
        detector.onAgilityExperience(1_010,
            new WorldPoint(3103, 3261, 0));
        detector.onAgilityExperience(1_020,
            new WorldPoint(3103, 3261, 0));

        assertEquals(2, events.size());
        assertEvent(events.get(0), "agility_lap", 1);
        assertEquals("Draynor Village",
            events.get(0).metadata.get("course"));
    }

    @Test
    public void agilityXpAwayFromCourseEndDoesNotCountLap()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeGameState(1_000, 0, 0, 0, 0);

        detector.onAgilityExperience(1_010,
            new WorldPoint(3200, 3200, 0));

        assertEquals(0, events.size());
    }

    @Test
    public void colossalWyrmProgressCountsEachCompletedLapOnce()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeGameState(0, 0, 0, 0, 0);

        detector.onWyrmBasicProgress(6);
        detector.onWyrmBasicProgress(6);
        detector.onWyrmBasicProgress(0);
        detector.onWyrmBasicProgress(6);
        detector.onWyrmAdvancedProgress(6);

        assertEquals(3, events.size());
        assertEquals("Colossal Wyrm Basic",
            events.get(0).metadata.get("course"));
        assertEquals("Colossal Wyrm Advanced",
            events.get(2).metadata.get("course"));
    }

    @Test
    public void titheScoreIncreaseCountsDepositedFruitQuantity()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeGameState(0, 10, 0, 0, 0);

        detector.onTitheScoreChanged(15);
        detector.onTitheScoreChanged(0);
        detector.onTitheScoreChanged(3);

        assertEquals(2, events.size());
        assertEvent(events.get(0), "tithe_farm_fruit_deposited", 5);
        assertEvent(events.get(1), "tithe_farm_fruit_deposited", 3);
    }

    @Test
    public void trawlerCompletionCounterEmitsCorrectActivity()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeTrawlerCompletionCounter(30);

        assertEquals(0, events.size());

        detector.onTrawlerGamesCompletedChanged(31);

        assertEquals(1, events.size());
        assertEvent(events.get(0), "fishing_trawler_game", 1);
    }

    @Test
    public void completionCountersIgnoreUnchangedAndDecreasedValues()
    {
        List<ActivityEvent> events = new ArrayList<>();
        ActivityTelemetryDetector detector = detector(events);
        detector.initializeTrawlerCompletionCounter(31);

        detector.onTrawlerGamesCompletedChanged(31);

        assertEquals(0, events.size());
    }

    private static ActivityTelemetryDetector detector(
        List<ActivityEvent> events)
    {
        return new ActivityTelemetryDetector(
            (rsn, activity, quantity, metadata) -> events.add(
                new ActivityEvent(rsn, activity, quantity, metadata)),
            () -> "Mr Dimples");
    }

    private static void assertEvent(ActivityEvent event, String activity,
        int quantity)
    {
        assertEquals("Mr Dimples", event.rsn);
        assertEquals(activity, event.activity);
        assertEquals(quantity, event.quantity);
    }

    private static final class ActivityEvent
    {
        private final String rsn;
        private final String activity;
        private final int quantity;
        private final Map<String, String> metadata;

        private ActivityEvent(String rsn, String activity, int quantity,
            Map<String, String> metadata)
        {
            this.rsn = rsn;
            this.activity = activity;
            this.quantity = quantity;
            this.metadata = metadata;
        }
    }
}
