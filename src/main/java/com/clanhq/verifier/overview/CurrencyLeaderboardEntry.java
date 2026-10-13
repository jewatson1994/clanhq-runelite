package com.clanhq.verifier.overview;

import com.google.gson.JsonObject;

/**
 * One row of the clan's all-time DripDrops leaderboard, as embedded in
 * the {@code GET /api/v1/identity} response. Server-resolved to a
 * canonical RSN already - see
 * {@code PluginInstallationService._all_time_leaderboard_payload} - so
 * the client has nothing left to do but display it.
 */
public final class CurrencyLeaderboardEntry
{
    private final String rsn;
    private final long score;

    public CurrencyLeaderboardEntry(String rsn, long score)
    {
        this.rsn = rsn == null ? "" : rsn.trim();
        this.score = score;
    }

    public static CurrencyLeaderboardEntry fromJson(JsonObject value)
    {
        return new CurrencyLeaderboardEntry(
            value.has("rsn") && !value.get("rsn").isJsonNull()
                ? value.get("rsn").getAsString() : "",
            value.has("score") && !value.get("score").isJsonNull()
                ? value.get("score").getAsLong() : 0L);
    }

    public String getRsn()
    {
        return rsn;
    }

    public long getScore()
    {
        return score;
    }
}
