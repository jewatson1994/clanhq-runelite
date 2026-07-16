package com.clanhq.verifier.event.model;

import java.time.LocalDate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
            + "\"status\":\"SCHEDULED\","
            + "\"event_code\":\"SOTW-ABC123\"}"
        );

        assertEquals(7, event.getEventId());
        assertEquals("SKILL_OF_THE_WEEK", event.getEventType());
        assertEquals("Skill of the Week", event.getName());
        assertEquals("Sailing", event.getTarget());
        assertEquals(LocalDate.of(2026, 7, 17), event.getStartDate());
        assertEquals(LocalDate.of(2026, 7, 24), event.getEndDate());
        assertEquals("SCHEDULED", event.getStatus());
        assertEquals("SOTW-ABC123", event.getEventCode());
    }
}
