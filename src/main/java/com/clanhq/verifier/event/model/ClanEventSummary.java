package com.clanhq.verifier.event.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Instant;
import java.time.LocalDate;

public final class ClanEventSummary
{
    private final long eventId;
    private final String eventType;
    private final String name;
    private final String target;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Instant startAt;
    private final Instant endAt;
    private final String status;
    private final String eventCode;
    private final String serverName;

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode, String serverName)
    {
        this(eventId, eventType, name, target, startDate, endDate, status,
            eventCode, null, null, serverName);
    }

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode, Instant startAt, Instant endAt, String serverName)
    {
        this.eventId = eventId;
        this.eventType = eventType;
        this.name = name;
        this.target = target;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.eventCode = eventCode;
        this.serverName = serverName;
    }

    public static ClanEventSummary fromJson(String json)
    {
        JsonObject value = new JsonParser().parse(json).getAsJsonObject();
        if (value.get("schema_version").getAsInt() != 1)
        {
            throw new IllegalArgumentException("Unsupported event response");
        }
        Instant startAt = value.has("start_at")
            && !value.get("start_at").isJsonNull()
            ? Instant.parse(value.get("start_at").getAsString()) : null;
        Instant endAt = value.has("end_at")
            && !value.get("end_at").isJsonNull()
            ? Instant.parse(value.get("end_at").getAsString()) : null;
        return new ClanEventSummary(
            value.get("event_id").getAsLong(),
            value.get("event_type").getAsString(),
            value.get("name").getAsString(),
            value.has("target") && !value.get("target").isJsonNull()
                ? value.get("target").getAsString() : null,
            LocalDate.parse(value.get("start_date").getAsString()),
            LocalDate.parse(value.get("end_date").getAsString()),
            value.get("status").getAsString(),
            value.get("event_code").getAsString(),
            startAt,
            endAt,
            value.has("server_name") ? value.get("server_name").getAsString()
                : "ClanHQ");
    }

    public long getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Instant getStartAt() { return startAt; }
    public Instant getEndAt() { return endAt; }
    public String getStatus() { return status; }
    public String getEventCode() { return eventCode; }
    public String getServerName() { return serverName; }

    public boolean isActive()
    {
        return "ACTIVE".equals(status);
    }

    public boolean isSkillEvent()
    {
        return eventType.startsWith("SKILL_");
    }

    public boolean isBossEvent()
    {
        return eventType.startsWith("BOSS_");
    }
}
