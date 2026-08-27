package com.clanhq.verifier.event.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClanEventsSnapshot
{
    private final List<ClanEventSummary> events;
    private final String serverName;

    private ClanEventsSnapshot(List<ClanEventSummary> events, String serverName)
    {
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.serverName = serverName;
    }

    public static ClanEventsSnapshot fromJson(String json)
    {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        if (root.get("schema_version").getAsInt() != 1)
        {
            throw new IllegalArgumentException("Unsupported events response");
        }
        JsonArray values = root.getAsJsonArray("events");
        List<ClanEventSummary> events = new ArrayList<>();
        for (JsonElement element : values)
        {
            events.add(ClanEventSummary.fromJson(element.toString()));
        }
        return new ClanEventsSnapshot(
            events,
            root.has("server_name")
                ? root.get("server_name").getAsString() : "ClanHQ");
    }

    public List<ClanEventSummary> getEvents() { return events; }
    public String getServerName() { return serverName; }
}
