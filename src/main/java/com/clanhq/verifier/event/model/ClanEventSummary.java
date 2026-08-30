package com.clanhq.verifier.event.model;

import com.clanhq.verifier.task.VerificationType;
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
    private final VerificationType verificationType;

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode, String serverName)
    {
        this(eventId, eventType, name, target, startDate, endDate, status,
            eventCode, null, null, serverName, null);
    }

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode, Instant startAt, Instant endAt, String serverName)
    {
        this(eventId, eventType, name, target, startDate, endDate, status,
            eventCode, startAt, endAt, serverName, null);
    }

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode, Instant startAt, Instant endAt, String serverName,
        VerificationType verificationType)
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
        this.verificationType = verificationType == null
            ? VerificationType.UNKNOWN : verificationType;
    }

    public static ClanEventSummary fromJson(String json)
    {
        JsonObject value = new JsonParser().parse(json).getAsJsonObject();
        // The event-list envelope carries the API schema version. Older
        // servers did not repeat that field on every event, so accept both
        // shapes while still rejecting an explicitly unsupported version.
        if (value.has("schema_version")
            && !value.get("schema_version").isJsonNull()
            && value.get("schema_version").getAsInt() != 1)
        {
            throw new IllegalArgumentException("Unsupported event response");
        }
        Instant startAt = value.has("start_at")
            && !value.get("start_at").isJsonNull()
            ? Instant.parse(value.get("start_at").getAsString()) : null;
        Instant endAt = value.has("end_at")
            && !value.get("end_at").isJsonNull()
            ? Instant.parse(value.get("end_at").getAsString()) : null;
        String eventType = value.get("event_type").getAsString();
        VerificationType verificationType = value.has("verification")
            && value.get("verification").isJsonObject()
                ? VerificationType.from(value.getAsJsonObject("verification")
                    .has("type")
                        ? value.getAsJsonObject("verification").get("type")
                            .getAsString() : null)
                : VerificationType.from(eventType.startsWith("SKILL_")
                    ? "SKILL_XP" : eventType.startsWith("BOSS_")
                        ? "NPC_KILL" : null);
        return new ClanEventSummary(
            value.get("event_id").getAsLong(),
            eventType,
            value.get("name").getAsString(),
            value.has("target") && !value.get("target").isJsonNull()
                ? value.get("target").getAsString() : null,
            LocalDate.parse(value.get("start_date").getAsString()),
            LocalDate.parse(value.get("end_date").getAsString()),
            value.get("status").getAsString(),
            value.has("event_code") && !value.get("event_code").isJsonNull()
                ? value.get("event_code").getAsString() : "",
            startAt,
            endAt,
            value.has("server_name") && !value.get("server_name").isJsonNull()
                ? value.get("server_name").getAsString() : "ClanHQ",
            verificationType);
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
    public VerificationType getVerificationType() { return verificationType; }

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
