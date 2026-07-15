package com.clanhq.verifier.service;

import com.clanhq.verifier.model.RaidKillCounts;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;

public final class RaidKillCountService
{
    private final HiscoreClient hiscoreClient;
    private CompletableFuture<HiscoreResult> lookup;

    @Inject
    public RaidKillCountService(HiscoreClient hiscoreClient)
    {
        this.hiscoreClient = hiscoreClient;
    }

    public void startLookup(String rsn)
    {
        lookup = hiscoreClient.lookupAsync(rsn, HiscoreEndpoint.NORMAL);
    }

    public CompletableFuture<RaidKillCounts> lookupAsync(String rsn)
    {
        return hiscoreClient.lookupAsync(rsn, HiscoreEndpoint.NORMAL)
            .handle((result, error) ->
            {
                if (error != null || result == null)
                {
                    return RaidKillCounts.unavailable(
                        "RuneScape hiscores were unavailable");
                }
                return fromResult(result);
            });
    }

    public RaidKillCounts finishLookup()
    {
        if (lookup == null)
        {
            return RaidKillCounts.unavailable("Hiscore lookup was not started");
        }
        if (!lookup.isDone())
        {
            return RaidKillCounts.unavailable("Hiscore lookup did not finish during capture");
        }
        if (lookup.isCompletedExceptionally())
        {
            return RaidKillCounts.unavailable("RuneScape hiscores were unavailable");
        }

        HiscoreResult result = lookup.getNow(null);
        if (result == null)
        {
            return RaidKillCounts.unavailable("No hiscore result was returned");
        }
        return fromResult(result);
    }

    private static RaidKillCounts fromResult(HiscoreResult result)
    {
        return RaidKillCounts.available(
            count(result, HiscoreSkill.CHAMBERS_OF_XERIC),
            count(result, HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE),
            count(result, HiscoreSkill.THEATRE_OF_BLOOD),
            count(result, HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE),
            count(result, HiscoreSkill.TOMBS_OF_AMASCUT),
            count(result, HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT));
    }

    public void cancelLookup()
    {
        if (lookup != null)
        {
            lookup.cancel(true);
            lookup = null;
        }
    }

    private static int count(HiscoreResult result, HiscoreSkill activity)
    {
        Skill value = result.getSkill(activity);
        return value == null ? 0 : Math.max(value.getLevel(), 0);
    }
}
