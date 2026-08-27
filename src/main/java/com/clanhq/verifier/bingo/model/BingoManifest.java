package com.clanhq.verifier.bingo.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BingoManifest
{
    private final String eventId;
    private final String name;
    private final Instant startsAt;
    private final Instant endsAt;
    private final List<BingoItem> items;
    private final Map<Integer, BingoItem> itemsById;
    private final BingoCharacterCheckStatus characterCheck;
    private final String serverName;

    public BingoManifest(String eventId, String name, Instant startsAt,
        Instant endsAt, List<BingoItem> items)
    {
        this(eventId, name, startsAt, endsAt, items,
            BingoCharacterCheckStatus.empty(), "ClanHQ");
    }

    public BingoManifest(String eventId, String name, Instant startsAt,
        Instant endsAt, List<BingoItem> items,
        BingoCharacterCheckStatus characterCheck)
    {
        this(eventId, name, startsAt, endsAt, items, characterCheck, "ClanHQ");
    }

    public BingoManifest(String eventId, String name, Instant startsAt,
        Instant endsAt, List<BingoItem> items,
        BingoCharacterCheckStatus characterCheck, String serverName)
    {
        this.eventId = eventId;
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.characterCheck = characterCheck;
        this.serverName = serverName == null || serverName.trim().isEmpty()
            ? "ClanHQ" : serverName.trim();
        Map<Integer, BingoItem> indexed = new LinkedHashMap<>();
        for (BingoItem item : items)
        {
            indexed.put(item.getItemId(), item);
        }
        this.itemsById = Collections.unmodifiableMap(indexed);
    }

    public static BingoManifest fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        if (integer(root, "schema_version") != 1)
        {
            throw new IllegalArgumentException("Unsupported Bingo manifest");
        }
        JsonArray rawItems = root.getAsJsonArray("items");
        if (rawItems == null || rawItems.size() == 0)
        {
            throw new IllegalArgumentException("Bingo board has no items");
        }
        List<BingoItem> items = new ArrayList<>();
        for (JsonElement element : rawItems)
        {
            JsonObject item = element.getAsJsonObject();
            items.add(new BingoItem(
                integer(item, "item_id"),
                text(item, "name"),
                integer(item, "minimum_quantity"),
                integer(item, "points")));
        }
        BingoCharacterCheckStatus characterCheck =
            BingoCharacterCheckStatus.empty();
        if (root.has("character_check")
            && root.get("character_check").isJsonObject())
        {
            JsonObject check = root.getAsJsonObject("character_check");
            characterCheck = new BingoCharacterCheckStatus(
                optionalText(check, "status", "NOT_SUBMITTED"),
                optionalText(check, "next_phase", "BASELINE"),
                optionalInteger(check, "checkpoint_count", 0),
                optionalNullableText(check, "baseline_captured_at"),
                optionalNullableText(check, "final_captured_at"));
        }
        return new BingoManifest(
            text(root, "event_id"),
            text(root, "name"),
            Instant.parse(text(root, "starts_at")),
            Instant.parse(text(root, "ends_at")),
            items,
            characterCheck,
            root.has("server_name") ? root.get("server_name").getAsString()
                : "ClanHQ");
    }

    private static String text(JsonObject value, String key)
    {
        if (!value.has(key) || !value.get(key).isJsonPrimitive())
        {
            throw new IllegalArgumentException("Missing Bingo " + key);
        }
        String result = value.get(key).getAsString().trim();
        if (result.isEmpty())
        {
            throw new IllegalArgumentException("Missing Bingo " + key);
        }
        return result;
    }

    private static int integer(JsonObject value, String key)
    {
        if (!value.has(key) || !value.get(key).isJsonPrimitive())
        {
            throw new IllegalArgumentException("Missing Bingo " + key);
        }
        return value.get(key).getAsInt();
    }

    private static String optionalText(JsonObject value, String key,
        String fallback)
    {
        String result = optionalNullableText(value, key);
        return result == null ? fallback : result;
    }

    private static String optionalNullableText(JsonObject value, String key)
    {
        if (!value.has(key) || value.get(key).isJsonNull()
            || !value.get(key).isJsonPrimitive())
        {
            return null;
        }
        String result = value.get(key).getAsString().trim();
        return result.isEmpty() ? null : result;
    }

    private static int optionalInteger(JsonObject value, String key,
        int fallback)
    {
        return value.has(key) && value.get(key).isJsonPrimitive()
            ? value.get(key).getAsInt() : fallback;
    }

    public String getEventId()
    {
        return eventId;
    }

    public String getName()
    {
        return name;
    }

    public Instant getStartsAt()
    {
        return startsAt;
    }

    public Instant getEndsAt()
    {
        return endsAt;
    }

    public List<BingoItem> getItems()
    {
        return items;
    }

    public BingoCharacterCheckStatus getCharacterCheck()
    {
        return characterCheck;
    }

    public String getServerName()
    {
        return serverName;
    }

    public Optional<BingoItem> findItem(int itemId)
    {
        return Optional.ofNullable(itemsById.get(itemId));
    }
}
