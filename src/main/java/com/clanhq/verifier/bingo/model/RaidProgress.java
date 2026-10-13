package com.clanhq.verifier.bingo.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A member's permanent, clan-wide progress toward each raid's "uniques"
 * (e.g. "COX" -&gt; 3/12), as reported by {@code GET /api/v1/raid-progress}.
 * Independent of any single Bingo event - see
 * {@code services.raid_unique_progress_service.RaidUniqueProgressService}
 * on the server for the authoritative definition.
 *
 * {@link #isTracked(int)} is what lets {@link
 * com.clanhq.verifier.bingo.BingoFeature#onDrop} recognize a raid-unique
 * drop and auto-report it (see {@link
 * com.clanhq.verifier.bingo.transport.BingoApiClient#reportRaidDrop})
 * without needing an active Bingo manifest/event at all - this catalog
 * comes from the same server-side source (the "raid" tag on items in
 * config/bingo.json) but, unlike the manifest, is not gated on any
 * particular event being open.
 */
public final class RaidProgress
{
    private final Map<String, Raid> raids;
    private final Set<Integer> trackedItemIds;

    public RaidProgress(Map<String, Raid> raids, Set<Integer> trackedItemIds)
    {
        this.raids = Collections.unmodifiableMap(new LinkedHashMap<>(raids));
        this.trackedItemIds = Collections.unmodifiableSet(
            new HashSet<>(trackedItemIds));
    }

    public static RaidProgress empty()
    {
        return new RaidProgress(Collections.emptyMap(), Collections.emptySet());
    }

    public static RaidProgress fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        Map<String, Raid> raids = new LinkedHashMap<>();
        Set<Integer> trackedItemIds = new HashSet<>();
        if (root.has("raids") && root.get("raids").isJsonObject())
        {
            JsonObject rawRaids = root.getAsJsonObject("raids");
            for (Entry<String, com.google.gson.JsonElement> entry
                : rawRaids.entrySet())
            {
                if (!entry.getValue().isJsonObject())
                {
                    continue;
                }
                JsonObject raid = entry.getValue().getAsJsonObject();
                int obtained = raid.has("obtained")
                    ? raid.get("obtained").getAsInt() : 0;
                int total = raid.has("total") ? raid.get("total").getAsInt() : 0;
                raids.put(entry.getKey(), new Raid(obtained, total));
                if (raid.has("items") && raid.get("items").isJsonArray())
                {
                    JsonArray items = raid.getAsJsonArray("items");
                    for (com.google.gson.JsonElement element : items)
                    {
                        if (!element.isJsonObject())
                        {
                            continue;
                        }
                        JsonObject itemObject = element.getAsJsonObject();
                        if (itemObject.has("item_id"))
                        {
                            trackedItemIds.add(
                                itemObject.get("item_id").getAsInt());
                        }
                    }
                }
            }
        }
        return new RaidProgress(raids, trackedItemIds);
    }

    public Set<String> getRaidKeys()
    {
        return raids.keySet();
    }

    public Raid getRaid(String key)
    {
        return raids.get(key);
    }

    public boolean isEmpty()
    {
        return raids.isEmpty();
    }

    /**
     * Whether {@code itemId} is currently one of the raid uniques being
     * tracked (Bingo enabled, and the item carries a "raid" tag in
     * config/bingo.json) - independent of any active Bingo event.
     */
    public boolean isTracked(int itemId)
    {
        return trackedItemIds.contains(itemId);
    }

    public static final class Raid
    {
        private final int obtained;
        private final int total;

        public Raid(int obtained, int total)
        {
            this.obtained = obtained;
            this.total = total;
        }

        public int getObtained()
        {
            return obtained;
        }

        public int getTotal()
        {
            return total;
        }
    }
}
