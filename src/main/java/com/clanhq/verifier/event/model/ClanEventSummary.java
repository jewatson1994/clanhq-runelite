package com.clanhq.verifier.event.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.LocalDate;

public final class ClanEventSummary
{
    private final long eventId;
    private final String eventType;
    private final String name;
    private final String target;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;
    private final String eventCode;

    public ClanEventSummary(long eventId, String eventType, String name,
        String target, LocalDate startDate, LocalDate endDate, String status,
        String eventCode)
    {
        this.eventId = eventId;
        this.eventType = eventType;
        this.name = name;
        this.target = target;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.eventCode = eventCode;
    }

    public static ClanEventSummary fromJson(String json)
    {
        JsonObject value = new JsonParser().parse(json).getAsJsonObject();
        if (value.get("schema_version").getAsInt() != 1)
        {
            throw new IllegalArgumentException("Unsupported event response");
        }
        return new ClanEventSummary(
            value.get("event_id").getAsLong(),
            value.get("event_type").getAsString(),
            value.get("name").getAsString(),
            value.has("target") && !value.get("target").isJsonNull()
                ? value.get("target").getAsString() : null,
            LocalDate.parse(value.get("start_date").getAsString()),
            LocalDate.parse(value.get("end_date").getAsString()),
            value.get("status").getAsString(),
            value.get("event_code").getAsString());
    }

    public long getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getName() { return name; }
    public String getTarget() { return target; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
    public String getEventCode() { return eventCode; }

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
