package com.clanhq.verifier.rules;

import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class RankItemCatalogTest
{
    private final RankItemCatalog catalog = new RankItemCatalog();

    @Test
    public void includesAcceptedRankItems()
    {
        assertTrue(catalog.isRelevant(ItemID.GHOMMALS_HILT_2));
        assertTrue(catalog.isRelevant(ItemID.TUMEKENS_SHADOW));
        assertTrue(catalog.isRelevant(ItemID.AVERNIC_DEFENDER));
    }

    @Test
    public void excludesUnrelatedBankItems()
    {
        assertFalse(catalog.isRelevant(ItemID.LOBSTER));
        assertFalse(catalog.isRelevant(ItemID.COINS_995));
    }
}
