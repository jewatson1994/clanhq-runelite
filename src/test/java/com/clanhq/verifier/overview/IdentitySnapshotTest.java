package com.clanhq.verifier.overview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IdentitySnapshotTest
{
    @Test
    public void preservesExplicitZeroEarnedTotal()
    {
        IdentitySnapshot snapshot = IdentitySnapshot.fromJson(
            "{\"device_name\":\"RuneLite\",\"rsns\":[],"
                + "\"currency_balance\":293,\"all_time_earned\":0,"
                + "\"all_time_rank\":null}");

        assertEquals(0, snapshot.getAllTimeEarned());
        assertEquals(0, snapshot.getAllTimeRank());
    }

    @Test
    public void usesBalanceOnlyWhenOlderApiOmitsEarnedTotal()
    {
        IdentitySnapshot snapshot = IdentitySnapshot.fromJson(
            "{\"device_name\":\"RuneLite\",\"rsns\":[],"
                + "\"currency_balance\":293}");

        assertEquals(293, snapshot.getAllTimeEarned());
    }
}
