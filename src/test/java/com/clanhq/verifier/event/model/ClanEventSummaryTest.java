package com.clanhq.verifier.event.model;

import java.time.LocalDate;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
        assertTrue(event.getStandings().isEmpty());
    }

    @Test
    public void parsesStandingsWhenPresent()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":9,"
            + "\"event_type\":\"SKILL_OF_THE_WEEK\","
            + "\"name\":\"Skill of the Week\","
            + "\"target\":\"Defence\","
            + "\"start_date\":\"2026-09-25\","
            + "\"end_date\":\"2026-10-01\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"SOTW-XYZ789\","
            + "\"standings\":["
            + "{\"rsn\":\"IronDillon\",\"team_name\":null,\"score\":1806538},"
            + "{\"rsn\":\"Senpai Devil\",\"team_name\":\"Ares\",\"score\":1498074}"
            + "]}"
        );

        assertEquals(2, event.getStandings().size());
        assertEquals("IronDillon", event.getStandings().get(0).getRsn());
        assertEquals(1806538L, event.getStandings().get(0).getScore());
        assertEquals(null, event.getStandings().get(0).getTeamName());
        assertEquals("Ares", event.getStandings().get(1).getTeamName());
    }

    @Test
    public void standingsAreEmptyWhenAbsentFromJson()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":10,"
            + "\"event_type\":\"BINGO\","
            + "\"name\":\"Winter Bingo\","
            + "\"start_date\":\"2026-01-01\","
            + "\"end_date\":\"2026-01-08\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"BINGO-ABC\"}"
        );

        assertTrue(event.getStandings().isEmpty());
    }

    @Test
    public void parsesRankOnStandingsWhenPresent()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":11,"
            + "\"event_type\":\"SKILL_OF_THE_WEEK\","
            + "\"name\":\"Skill of the Week\","
            + "\"target\":\"Defence\","
            + "\"start_date\":\"2026-09-25\","
            + "\"end_date\":\"2026-10-01\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"SOTW-RANK\","
            + "\"standings\":["
            + "{\"rsn\":\"IronDillon\",\"team_name\":null,\"score\":1806538,\"rank\":1},"
            + "{\"rsn\":\"Senpai Devil\",\"team_name\":\"Ares\",\"score\":1498074,\"rank\":2}"
            + "]}"
        );

        assertEquals(1, event.getStandings().get(0).getRank());
        assertEquals(2, event.getStandings().get(1).getRank());
    }

    @Test
    public void parsesMyStandingWhenPresent()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":12,"
            + "\"event_type\":\"SKILL_OF_THE_WEEK\","
            + "\"name\":\"Skill of the Week\","
            + "\"target\":\"Defence\","
            + "\"start_date\":\"2026-09-25\","
            + "\"end_date\":\"2026-10-01\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"SOTW-MINE\","
            + "\"standings\":["
            + "{\"rsn\":\"IronDillon\",\"team_name\":null,\"score\":1806538,\"rank\":1}"
            + "],"
            + "\"my_standing\":"
            + "{\"rsn\":\"Mr Dimples\",\"team_name\":null,\"score\":42000,\"rank\":15}"
            + "}"
        );

        assertEquals("Mr Dimples", event.getMyStanding().getRsn());
        assertEquals(42000L, event.getMyStanding().getScore());
        assertEquals(15, event.getMyStanding().getRank());
    }

    @Test
    public void myStandingIsNullWhenAbsentFromJson()
    {
        ClanEventSummary event = ClanEventSummary.fromJson("{"
            + "\"event_id\":13,"
            + "\"event_type\":\"BOSS_OF_THE_WEEK\","
            + "\"name\":\"Boss of the Week\","
            + "\"start_date\":\"2026-09-25\","
            + "\"end_date\":\"2026-10-01\","
            + "\"status\":\"ACTIVE\","
            + "\"event_code\":\"BOTW-NOMINE\"}"
        );

        assertNull(event.getMyStanding());
    }
}
