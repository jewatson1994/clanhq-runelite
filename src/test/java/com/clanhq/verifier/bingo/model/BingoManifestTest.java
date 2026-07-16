package com.clanhq.verifier.bingo.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BingoManifestTest
{
    @Test
    public void parsesAndIndexesServerManifest()
    {
        BingoManifest manifest = BingoManifest.fromJson("{"
            + "\"schema_version\":1,"
            + "\"event_id\":\"summer-2026\","
            + "\"name\":\"Summer Bingo\","
            + "\"starts_at\":\"2026-08-01T00:00:00Z\","
            + "\"ends_at\":\"2026-08-08T00:00:00Z\","
            + "\"items\":[{"
            + "\"item_id\":4151,"
            + "\"name\":\"Abyssal whip\","
            + "\"minimum_quantity\":1,"
            + "\"points\":5}]}"
        );

        assertEquals("summer-2026", manifest.getEventId());
        assertEquals("Summer Bingo", manifest.getName());
        assertEquals(1, manifest.getItems().size());
        assertTrue(manifest.findItem(4151).isPresent());
        assertEquals(5, manifest.findItem(4151).get().getPoints());
        assertFalse(manifest.findItem(11840).isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsManifestWithoutItems()
    {
        BingoManifest.fromJson("{"
            + "\"schema_version\":1,"
            + "\"event_id\":\"empty\","
            + "\"name\":\"Empty\","
            + "\"starts_at\":\"2026-08-01T00:00:00Z\","
            + "\"ends_at\":\"2026-08-08T00:00:00Z\","
            + "\"items\":[]}"
        );
    }
}
