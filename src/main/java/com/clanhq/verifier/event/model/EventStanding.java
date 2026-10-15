package com.clanhq.verifier.event.model;

import com.google.gson.JsonObject;

/**
 * One row of a ClanHQ weekly competition leaderboard (Skill/Boss of the
 * Week today), as embedded in the active event's payload from
 * {@code GET /api/v1/events}. Sourced server-side from Wise Old Man
 * snapshots - the same authoritative data behind the Discord "Weekly
 * Competitions" embed, not the live RuneLite-observation estimate.
 */
public final class EventStanding
{
    private final String rsn;
    private final String teamName;
    private final long score;
    private final int rank;

    public EventStanding(String rsn, String teamName, long score)
    {
        this(rsn, teamName, score, 0);
    }

    public EventStanding(String rsn, String teamName, long score, int rank)
    {
        this.rsn = rsn == null ? "" : rsn.trim();
        this.teamName = teamName == null || teamName.trim().isEmpty()
            ? null : teamName.trim();
        this.score = score;
        this.rank = rank;
    }

    public static EventStanding fromJson(JsonObject value)
    {
        return new EventStanding(
            value.has("rsn") && !value.get("rsn").isJsonNull()
                ? value.get("rsn").getAsString() : "",
            value.has("team_name") && !value.get("team_name").isJsonNull()
                ? value.get("team_name").getAsString() : null,
            value.has("score") && !value.get("score").isJsonNull()
                ? value.get("score").getAsLong() : 0L,
            value.has("rank") && !value.get("rank").isJsonNull()
                ? value.get("rank").getAsInt() : 0);
    }

    public String getRsn()
    {
        return rsn;
    }

    public String getTeamName()
    {
        return teamName;
    }

    public long getScore()
    {
        return score;
    }

    /**
     * 1-based leaderboard position, as computed server-side. 0 when the
     * source JSON didn't include one (not expected for the shapes this
     * class is built from today, but kept degrade-safe for older/odd
     * payloads rather than throwing).
     */
    public int getRank()
    {
        return rank;
    }
}
