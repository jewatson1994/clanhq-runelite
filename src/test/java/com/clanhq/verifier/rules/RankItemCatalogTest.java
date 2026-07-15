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

    @Test
    public void includesLaterRankItemsByNameWithoutRetainingUnrelatedItems()
    {
        assertTrue(catalog.isRelevant(999_001, "Tormented synapse"));
        assertTrue(catalog.isRelevant(999_002, "Blessed Dizana's quiver"));
        assertFalse(catalog.isRelevant(999_003, "Raw lobster"));
    }
}
