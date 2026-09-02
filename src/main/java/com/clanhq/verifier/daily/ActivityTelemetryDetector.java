package com.clanhq.verifier.daily;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;

/** Converts gameplay signals into generic telemetry, without task logic. */
public final class ActivityTelemetryDetector
{
    private static final Map<String, Pattern> CHAT_COMPLETIONS;
    static
    {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("hunter_rumour", Pattern.compile(
            "^you have completed [\\d,]+ rumours? for the hunter guild\\.$",
            Pattern.CASE_INSENSITIVE));
        patterns.put("pest_control_game", Pattern.compile(
            "^(?:you have won! you have earned [\\d,]+ void knight "
                + "commendation points?.*|we(?:'|’)?ve awarded you with "
                + "(?:(?:some|[\\d,]+) )?void knight commendation points? "
                + "and some coins to show our appreciation\\.|"
                + "congratulations! you managed to destroy all the portals! "
                + "we(?:'|’)?ve awarded you [\\d,]+ void knight "
                + "commendation points?\\. please also accept these coins "
                + "as a reward\\.)$",
            Pattern.CASE_INSENSITIVE));
        patterns.put("giants_foundry_commission", Pattern.compile(
            "^total swords made in the giants(?:'|’) foundry:\\s*[\\d,]+$",
            Pattern.CASE_INSENSITIVE));
        patterns.put("barbarian_assault_wave", Pattern.compile(
            "^wave\\s+\\d+\\s+duration\\s*:",
            Pattern.CASE_INSENSITIVE));
        patterns.put("mahogany_homes_contract", Pattern.compile(
            "^you have completed [\\d,]+ contracts with a total of [\\d,]+ points?\\.$",
            Pattern.CASE_INSENSITIVE));
        CHAT_COMPLETIONS = Collections.unmodifiableMap(patterns);
    }

    @FunctionalInterface
    interface ActivitySink
    {
        void observe(String rsn, String activity, int quantity,
            Map<String, String> metadata);
    }

    @FunctionalInterface
    interface ActivityObserver
    {
        void observed(String activity, int quantity,
            Map<String, String> metadata);
    }

    private final ActivitySink sink;
    private final Supplier<String> rsnSupplier;
    private volatile ActivityObserver testObserver;
    private Integer agilityExperience;
    private Integer titheScore;
    private Integer trawlerGamesCompleted;
    private Integer wyrmBasicProgress;
    private Integer wyrmAdvancedProgress;

    public ActivityTelemetryDetector(DailyTasksFeature feature,
        Supplier<String> rsnSupplier)
    {
        this(feature::observeActivity, rsnSupplier);
    }

    ActivityTelemetryDetector(ActivitySink sink, Supplier<String> rsnSupplier)
    {
        this.sink = sink;
        this.rsnSupplier = rsnSupplier;
    }

    void setTestObserver(ActivityObserver observer)
    {
        testObserver = observer;
    }

    public void initializeGameState(int currentAgilityExperience,
        int currentTitheScore, int currentFoundryReputation,
        int currentWyrmBasicProgress, int currentWyrmAdvancedProgress)
    {
        agilityExperience = currentAgilityExperience;
        titheScore = currentTitheScore;
        wyrmBasicProgress = currentWyrmBasicProgress;
        wyrmAdvancedProgress = currentWyrmAdvancedProgress;
    }

    public void initializeTrawlerCompletionCounter(int currentTrawlerGames)
    {
        trawlerGamesCompleted = currentTrawlerGames;
    }

    public void onChatMessage(String message)
    {
        String activity = activityForChatMessage(message);
        if (activity != null)
        {
            emit(activity, 1,
                Collections.singletonMap("signal", cleanChatMessage(message)));
        }
    }

    public void onPestControlDialogue(String message)
    {
        if ("pest_control_game".equals(activityForChatMessage(message)))
        {
            emit("pest_control_game", 1,
                Collections.singletonMap("signal", cleanChatMessage(message)));
        }
    }

    static String activityForChatMessage(String message)
    {
        String cleanMessage = cleanChatMessage(message);
        if (cleanMessage == null) return null;
        for (Map.Entry<String, Pattern> entry : CHAT_COMPLETIONS.entrySet())
        {
            if (entry.getValue().matcher(cleanMessage).find())
            {
                return entry.getKey();
            }
        }
        return null;
    }

    private static String cleanChatMessage(String message)
    {
        if (message == null)
        {
            return null;
        }
        return Text.removeTags(message.replaceAll("(?i)<br\\s*/?>", " "))
            .replace('\u00a0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    public void onAgilityExperience(int experience, WorldPoint location)
    {
        Integer previous = agilityExperience;
        agilityExperience = experience;
        if (previous == null || experience <= previous || location == null)
        {
            return;
        }
        String course = AgilityCourseCompletion.courseAt(location);
        if (course != null)
        {
            onAgilityLap(course);
        }
    }

    public void onWyrmBasicProgress(int progress)
    {
        Integer previous = wyrmBasicProgress;
        wyrmBasicProgress = progress;
        if (previous != null && previous != 6 && progress == 6)
        {
            onAgilityLap("Colossal Wyrm Basic");
        }
    }

    public void onWyrmAdvancedProgress(int progress)
    {
        Integer previous = wyrmAdvancedProgress;
        wyrmAdvancedProgress = progress;
        if (previous != null && previous != 6 && progress == 6)
        {
            onAgilityLap("Colossal Wyrm Advanced");
        }
    }

    public void onAgilityLap(String course)
    {
        Map<String, String> metadata = course == null || course.trim().isEmpty()
            ? Collections.emptyMap()
            : Collections.singletonMap("course", course.trim());
        emit("agility_lap", 1, metadata);
    }

    public void onTitheScoreChanged(int score)
    {
        Integer previous = titheScore;
        titheScore = score;
        if (previous != null && score > previous)
        {
            onTitheFruitDeposited(score - previous);
        }
    }

    public void onTitheFruitDeposited(int quantity)
    {
        if (quantity > 0)
        {
            emit("tithe_farm_fruit_deposited", quantity,
                Collections.emptyMap());
        }
    }

    public void onTrawlerGamesCompletedChanged(int completed)
    {
        trawlerGamesCompleted = emitCounterIncrease(
            trawlerGamesCompleted, completed, "fishing_trawler_game");
    }

    private Integer emitCounterIncrease(Integer previous, int completed,
        String activity)
    {
        if (previous != null && completed > previous)
        {
            emit(activity, completed - previous, Collections.emptyMap());
        }
        return completed;
    }

    private synchronized void emit(String activity, int quantity,
        Map<String, String> metadata)
    {
        ActivityObserver observer = testObserver;
        if (observer != null)
        {
            observer.observed(activity, quantity, metadata);
            return;
        }
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty()) return;
        sink.observe(rsn, activity, quantity, metadata);
    }
}
