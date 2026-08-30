package com.clanhq.verifier.event.model;

import java.time.LocalDate;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClanEventSummaryTest
{
    @Test
    public void parsesClanEventApiResponse()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"schema_version\":1,"
            + "\"event_id\":7,"
            + "\"event_type\":\"SKILL_OF_THE_WEEK\","
            + "\"name\":\"Skill of the Week\","
            + "\"target\":\"Sailing\","
            + "\"start_date\":\"2026-07-17\","
            + "\"end_date\":\"2026-07-24\","
            + "\"start_at\":\"2026-07-17T00:00:00Z\","
            + "\"end_at\":\"2026-07-25T00:00:00Z\","
            + "\"status\":\"SCHEDULED\","
            + "\"event_code\":\"SOTW-ABC123\"}"
        );

        assertEquals(7, event.getEventId());
        assertEquals("SKILL_OF_THE_WEEK", event.getEventType());
        assertEquals("Skill of the Week", event.getName());
        assertEquals("Sailing", event.getTarget());
        assertEquals(LocalDate.of(2026, 7, 17), event.getStartDate());
        assertEquals(LocalDate.of(2026, 7, 24), event.getEndDate());
        assertEquals(Instant.parse("2026-07-17T00:00:00Z"), event.getStartAt());
        assertEquals(Instant.parse("2026-07-25T00:00:00Z"), event.getEndAt());
        assertEquals("SCHEDULED", event.getStatus());
        assertEquals("SOTW-ABC123", event.getEventCode());
        assertTrue(event.isSkillEvent());
        assertFalse(event.isBossEvent());
        assertFalse(event.isActive());
    }

    @Test
    public void parsesLegacyEventWithoutRepeatedSchemaVersion()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":8,"
            + "\"event_type\":\"BOSS_OF_THE_WEEK\","
            + "\"name\":\"Boss of the Week\","
            + "\"target\":\"Vorkath\","
            + "\"start_date\":\"2026-08-21\","
            + "\"end_date\":\"2026-08-27\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"BOTW-ABC123\"}"
        );

        assertEquals("BOSS_OF_THE_WEEK", event.getEventType());
        assertEquals("Vorkath", event.getTarget());
        assertTrue(event.isBossEvent());
        assertTrue(event.isActive());
    }
}
