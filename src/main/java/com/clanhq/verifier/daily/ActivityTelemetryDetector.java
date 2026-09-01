package com.clanhq.verifier.daily;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Converts gameplay signals into generic telemetry, without task logic. */
public final class ActivityTelemetryDetector
{
    private static final Map<String, Pattern> CHAT_COMPLETIONS;
    static
    {
        Map<String, Pattern> patterns = new HashMap<>();
        patterns.put("pest_control_game", Pattern.compile("you have completed the game", Pattern.CASE_INSENSITIVE));
        patterns.put("hunter_rumour", Pattern.compile("completed (?:your )?hunter rumour", Pattern.CASE_INSENSITIVE));
        patterns.put("fishing_trawler_game", Pattern.compile("fishing trawler.*(?:game|trip).*(?:complete|ended)", Pattern.CASE_INSENSITIVE));
        patterns.put("barbarian_assault_wave", Pattern.compile("wave complete", Pattern.CASE_INSENSITIVE));
        patterns.put("giants_foundry_commission", Pattern.compile("completed (?:a )?(?:giants' foundry )?(?:commission|sword)", Pattern.CASE_INSENSITIVE));
        patterns.put("mahogany_homes_contract", Pattern.compile("completed (?:your )?mahogany homes contract", Pattern.CASE_INSENSITIVE));
        CHAT_COMPLETIONS = Collections.unmodifiableMap(patterns);
    }

    private final DailyTasksFeature feature;
    private final Supplier<String> rsnSupplier;
    private String lastSignalKey;

    public ActivityTelemetryDetector(DailyTasksFeature feature, Supplier<String> rsnSupplier)
    {
        this.feature = feature;
        this.rsnSupplier = rsnSupplier;
    }

    public void onChatMessage(String message)
    {
        if (message == null) return;
        for (Map.Entry<String, Pattern> entry : CHAT_COMPLETIONS.entrySet())
        {
            if (entry.getValue().matcher(message).find())
            {
                emit(entry.getKey(), 1, Collections.singletonMap("signal", message));
                return;
            }
        }
    }

    public void onAgilityLap(String course)
    {
        Map<String, String> metadata = course == null || course.trim().isEmpty()
            ? Collections.emptyMap() : Collections.singletonMap("course", course.trim());
        emit("agility_lap", 1, metadata);
    }

    public void onTitheFruitDeposited(int quantity)
    {
        if (quantity > 0) emit("tithe_farm_fruit_deposited", quantity, Collections.emptyMap());
    }

    private synchronized void emit(String activity, int quantity, Map<String, String> metadata)
    {
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty()) return;
        // Deduplicate only an identical signal delivered back-to-back.  There
        // is no time-window suppression, so legitimate rapid completions are
        // never discarded merely because they occurred close together.
        String signalKey = activity + "|" + quantity + "|" + metadata.toString();
        if (signalKey.equals(lastSignalKey)) return;
        lastSignalKey = signalKey;
        feature.observeActivity(rsn, activity, quantity, metadata);
    }
}
