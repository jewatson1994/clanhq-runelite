package com.clanhq.verifier.service;

import com.clanhq.verifier.model.CollectionLogEvidence;
import com.clanhq.verifier.model.TempleCollectionLogResult;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TempleCollectionLogServiceTest
{
    private static final Instant NOW = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    public void parsesFreshRequiredCategoriesAndGlobalSlotCount()
    {
        TempleCollectionLogResult result =
            TempleCollectionLogService.parseResponse(response(
                NOW.minusSeconds(3600).getEpochSecond()), NOW);

        CollectionLogEvidence evidence = result.getEvidence();
        assertTrue(result.isFresh());
        assertTrue(result.getMessage().contains("synced 1 hour ago"));
        assertEquals(Integer.valueOf(800), evidence.getObtainedSlotCount());
        assertTrue(evidence.hasAcquiredItem("chambers of xeric",
            "metamorphic dust"));
        assertTrue(evidence.hasAcquiredItem("yama",
            "rite of vile transference"));
        assertTrue(evidence.hasDoomUniques());
        assertEquals(1, evidence.getPage("chambers of xeric")
            .getTotalSlotCount());
        assertEquals(1, evidence.getPage("theatre of blood")
            .getTotalSlotCount());
        assertEquals(1, evidence.getPage("tombs of amascut")
            .getTotalSlotCount());
    }

    @Test
    public void acceptsOldCollectionLogDataWithoutAnAgeLimit()
    {
        TempleCollectionLogResult result =
            TempleCollectionLogService.parseResponse(response(
                NOW.minusSeconds(365L * 24 * 3600).getEpochSecond()), NOW);

        assertTrue(result.isFresh());
        assertTrue(result.getEvidence().isCaptured());
    }

    @Test
    public void reportsPlayersWithoutTempleCollectionLogData()
    {
        TempleCollectionLogResult result =
            TempleCollectionLogService.parseResponse(
                "{\"error\":\"Player not found\"}", NOW);

        assertEquals(TempleCollectionLogResult.Status.NOT_SYNCED,
            result.getStatus());
    }

    private static String response(long lastChecked)
    {
        return "{\"data\":{"
            + "\"last_checked\":" + lastChecked + ","
            + "\"total_collections_finished\":800,"
            + "\"items\":{"
            + "\"chambers_of_xeric\":["
            + item("Metamorphic dust", 1) + ","
            + item("Xeric's champion", 0) + "],"
            + "\"theatre_of_blood\":["
            + item("Sanguine dust", 1) + ","
            + item("Sinhaza shroud tier 5", 0) + "],"
            + "\"tombs_of_amascut\":["
            + item("Masori crafting kit", 1) + ","
            + item("Icthlarin's shroud (tier 5)", 0) + "],"
            + "\"yama\":[" + item("Rite of vile transference", 1)
            + "],"
            + "\"doom_of_mokhaiotl\":["
            + item("Mokhaiotl cloth", 1) + ","
            + item("Avernic treads", 1) + ","
            + item("Eye of ayak (uncharged)", 1) + "]"
            + "}}}";
    }

    private static String item(String name, int count)
    {
        return "{\"name\":\"" + name + "\",\"count\":" + count
            + "}";
    }
}
