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

    public BingoManifest(String eventId, String name, Instant startsAt,
        Instant endsAt, List<BingoItem> items)
    {
        this.eventId = eventId;
        this.name = name;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
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
        return new BingoManifest(
            text(root, "event_id"),
            text(root, "name"),
            Instant.parse(text(root, "starts_at")),
            Instant.parse(text(root, "ends_at")),
            items);
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

    public Optional<BingoItem> findItem(int itemId)
    {
        return Optional.ofNullable(itemsById.get(itemId));
    }
}
