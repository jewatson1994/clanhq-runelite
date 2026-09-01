package com.clanhq.verifier.daily;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts gameplay signals into generic telemetry, without task logic. */
public final class ActivityTelemetryDetector
{
    @FunctionalInterface
    public interface ActivityObserver
    {
        void observe(String rsn, String activity, int quantity,
            Map<String, String> metadata);
    }

    private static final Map<String, Pattern> CHAT_COMPLETIONS;
    private static final Set<String> SUPPORTED_ACTIVITIES;
    private static final Pattern BARBARIAN_ASSAULT_WAVE_START = Pattern.compile(
        "^----\\s*Wave:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARBARIAN_ASSAULT_WAVE_DURATION = Pattern.compile(
        "^Wave\\s+(\\d+)\\s+duration\\s*:", Pattern.CASE_INSENSITIVE);

    static
    {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("barbarian_assault_wave", Pattern.compile(
            "wave (?:complete|completed)", Pattern.CASE_INSENSITIVE));
        CHAT_COMPLETIONS = Collections.unmodifiableMap(patterns);

        Set<String> supported = new LinkedHashSet<>();
        Collections.addAll(supported,
            "pest_control_game", "hunter_rumour", "fishing_trawler_game",
            "barbarian_assault_wave", "giants_foundry_commission",
            "mahogany_homes_contract", "agility_lap",
            "tithe_farm_fruit_deposited");
        SUPPORTED_ACTIVITIES = Collections.unmodifiableSet(supported);
    }

    private final ActivityObserver observer;
    private final Supplier<String> rsnSupplier;
    private int lastAgilityExperience = -1;
    private int lastTitheSackAmount = -1;
    private int lastBasicWyrmProgress = -1;
    private int lastAdvancedWyrmProgress = -1;
    private String barbarianAssaultWave;
    private boolean barbarianAssaultWaveCompleted;
    private final Map<String, Integer> completionCounters = new HashMap<>();

    public ActivityTelemetryDetector(DailyTasksFeature feature,
        Supplier<String> rsnSupplier)
    {
        this(feature::observeActivity, rsnSupplier);
    }

    public ActivityTelemetryDetector(ActivityObserver observer,
        Supplier<String> rsnSupplier)
    {
        this.observer = observer;
        this.rsnSupplier = rsnSupplier;
    }

    public static Set<String> supportedActivities()
    {
        return SUPPORTED_ACTIVITIES;
    }

    public void resetSession(int agilityExperience, int titheSackAmount,
        int basicWyrmProgress, int advancedWyrmProgress)
    {
        lastAgilityExperience = agilityExperience;
        lastTitheSackAmount = titheSackAmount;
        lastBasicWyrmProgress = basicWyrmProgress;
        lastAdvancedWyrmProgress = advancedWyrmProgress;
        barbarianAssaultWave = null;
        barbarianAssaultWaveCompleted = false;
    }

    public void onChatMessage(String message)
    {
        if (message == null)
        {
            return;
        }
        Matcher waveStart = BARBARIAN_ASSAULT_WAVE_START.matcher(message);
        if (waveStart.find())
        {
            barbarianAssaultWave = waveStart.group(1);
            barbarianAssaultWaveCompleted = false;
            return;
        }
        Matcher waveDuration = BARBARIAN_ASSAULT_WAVE_DURATION.matcher(message);
        if (waveDuration.find())
        {
            onBarbarianAssaultWaveCompleted(
                waveDuration.group(1), message);
            return;
        }
        for (Map.Entry<String, Pattern> entry : CHAT_COMPLETIONS.entrySet())
        {
            if (entry.getValue().matcher(message).find())
            {
                emit(entry.getKey(), 1,
                    Collections.singletonMap("signal", message));
                return;
            }
        }
    }

    /** Record the wave-complete interface independently of chat settings. */
    public void onBarbarianAssaultWaveCompleted()
    {
        onBarbarianAssaultWaveCompleted(null, "wave_complete_interface");
    }

    private void onBarbarianAssaultWaveCompleted(String wave, String signal)
    {
        if (barbarianAssaultWaveCompleted
            && (wave == null || barbarianAssaultWave == null
                || wave.equals(barbarianAssaultWave)))
        {
            return;
        }
        if (wave != null)
        {
            barbarianAssaultWave = wave;
        }
        barbarianAssaultWaveCompleted = true;
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("signal", signal);
        if (barbarianAssaultWave != null)
        {
            metadata.put("wave", barbarianAssaultWave);
        }
        emit("barbarian_assault_wave", 1, metadata);
    }

    /** Seed a monotonic Jagex activity counter without recording progress. */
    public void resetCompletionCounter(String activity, int value)
    {
        if (SUPPORTED_ACTIVITIES.contains(activity))
        {
            completionCounters.put(activity, value);
        }
    }

    /** Record the positive delta from a Jagex-maintained completion counter. */
    public void onCompletionCounter(String activity, int value)
    {
        if (!SUPPORTED_ACTIVITIES.contains(activity))
        {
            return;
        }
        Integer previous = completionCounters.put(activity, value);
        if (previous == null)
        {
            return;
        }
        int completed = value - previous;
        if (completed > 0)
        {
            emit(activity, completed, Collections.emptyMap());
        }
    }

    /** Detect a standard course lap from an Agility XP event at its endpoint. */
    public void onAgilityExperience(int experience, int regionId,
        int x, int y, int plane)
    {
        if (lastAgilityExperience < 0)
        {
            lastAgilityExperience = experience;
            return;
        }
        if (experience <= lastAgilityExperience)
        {
            lastAgilityExperience = experience;
            return;
        }
        lastAgilityExperience = experience;
        String course = completedCourse(regionId, x, y, plane);
        if (course != null)
        {
            onAgilityLap(course);
        }
    }

    /** Detect the two Colossal Wyrm courses from their completion progress. */
    public void onWyrmAgilityProgress(boolean advanced, int progress)
    {
        int previous = advanced
            ? lastAdvancedWyrmProgress : lastBasicWyrmProgress;
        if (advanced)
        {
            lastAdvancedWyrmProgress = progress;
        }
        else
        {
            lastBasicWyrmProgress = progress;
        }
        if (progress == 6 && previous != 6)
        {
            onAgilityLap(advanced
                ? "Colossal Wyrm Advanced" : "Colossal Wyrm Basic");
        }
    }

    /** Count only positive sack changes; reward/reset decreases are ignored. */
    public void onTitheSackAmount(int amount)
    {
        if (lastTitheSackAmount < 0)
        {
            lastTitheSackAmount = amount;
            return;
        }
        int deposited = amount - lastTitheSackAmount;
        lastTitheSackAmount = amount;
        if (deposited > 0)
        {
            onTitheFruitDeposited(deposited);
        }
    }

    public void onAgilityLap(String course)
    {
        Map<String, String> metadata = course == null || course.trim().isEmpty()
            ? Collections.emptyMap()
            : Collections.singletonMap("course", course.trim());
        emit("agility_lap", 1, metadata);
    }

    public void onTitheFruitDeposited(int quantity)
    {
        if (quantity > 0)
        {
            emit("tithe_farm_fruit_deposited", quantity,
                Collections.emptyMap());
        }
    }

    private void emit(String activity, int quantity,
        Map<String, String> metadata)
    {
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty())
        {
            return;
        }
        // A callback is one game event. Do not discard identical consecutive
        // waves/contracts; the API assigns every submission an idempotency ID.
        observer.observe(rsn, activity, quantity, metadata);
    }

    private static String completedCourse(int regionId, int x, int y,
        int plane)
    {
        switch (regionId)
        {
            case 9781: return at(x, y, plane, 2484, 3437, 0,
                2487, 3437, 0) ? "Gnome Stronghold" : null;
            case 6200: return at(x, y, plane, 1554, 3640, 0)
                ? "Shayzien Basic" : null;
            case 12338: return at(x, y, plane, 3103, 3261, 0)
                ? "Draynor" : null;
            case 13105: return at(x, y, plane, 3299, 3194, 0)
                ? "Al Kharid" : null;
            case 13356: return at(x, y, plane, 3364, 2830, 0)
                ? "Agility Pyramid" : null;
            case 12853: return at(x, y, plane, 3236, 3417, 0)
                ? "Varrock" : null;
            case 10559: return at(x, y, plane, 2652, 4039, 1)
                ? "Penguin" : null;
            case 10039: return at(x, y, plane, 2543, 3553, 0)
                ? "Barbarian" : null;
            case 13878: return at(x, y, plane, 3510, 3485, 0)
                ? "Canifis" : null;
            case 11050: return at(x, y, plane, 2770, 2747, 0)
                ? "Ape Atoll" : null;
            case 5944: return at(x, y, plane, 1522, 3625, 0)
                ? "Shayzien Advanced" : null;
            case 12084: return at(x, y, plane, 3029, 3332, 0,
                3029, 3333, 0, 3029, 3334, 0, 3029, 3335, 0)
                ? "Falador" : null;
            case 11837: return at(x, y, plane, 2993, 3933, 0,
                2994, 3933, 0, 2995, 3933, 0) ? "Wilderness" : null;
            case 14234: return at(x, y, plane, 3528, 9873, 0)
                ? "Werewolf" : null;
            case 10806: return at(x, y, plane, 2704, 3464, 0)
                ? "Seers' Village" : null;
            case 13358: return at(x, y, plane, 3363, 2998, 0)
                ? "Pollnivneach" : null;
            case 10553: return at(x, y, plane, 2653, 3676, 0)
                ? "Rellekka" : null;
            case 12895: return at(x, y, plane, 3240, 6109, 0)
                ? "Prifddinas" : null;
            case 10547: return at(x, y, plane, 2668, 3297, 0)
                ? "Ardougne" : null;
            default: return null;
        }
    }

    private static boolean at(int x, int y, int plane, int... points)
    {
        for (int index = 0; index < points.length; index += 3)
        {
            if (x == points[index] && y == points[index + 1]
                && plane == points[index + 2])
            {
                return true;
            }
        }
        return false;
    }
}
