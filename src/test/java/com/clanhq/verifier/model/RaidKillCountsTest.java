package com.clanhq.verifier.model;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RaidKillCountsTest
{
    @Test
    public void combinesAllSixAgreedRaidCategories()
    {
        RaidKillCounts counts = RaidKillCounts.available(
            420, 35, 180, 20, 310, 95);

        assertEquals(1060, counts.getCombined());
        assertEquals("COX 420, CM 35, TOB 180, HMT 20, TOA 310, "
            + "Expert 95 (combined 1060)", counts.toSummary());
    }

    @Test
    public void unavailableLookupDoesNotPretendToBeZeroKc()
    {
        RaidKillCounts counts = RaidKillCounts.unavailable("Timed out");

        assertFalse(counts.isAvailable());
        assertEquals("Timed out", counts.toSummary());
    }
}
