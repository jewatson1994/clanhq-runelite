package com.clanhq.verifier.bingo.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BingoManifestTest
{
    @Test
    public void parsesIdentitySpecificCharacterCheckStatus()
    {
        BingoManifest manifest = BingoManifest.fromJson("{"
            + "\"schema_version\":1,"
            + "\"event_id\":\"BINGO-TEST\","
            + "\"name\":\"Test Bingo\","
            + "\"starts_at\":\"2026-07-19T00:00:00Z\","
            + "\"ends_at\":\"2026-07-20T00:00:00Z\","
            + "\"items\":[{\"item_id\":12934,"
            + "\"name\":\"Zulrah's scales\","
            + "\"minimum_quantity\":100,\"points\":5}],"
            + "\"character_check\":{"
            + "\"status\":\"CHECKPOINT_CAPTURED\","
            + "\"next_phase\":\"CHECKPOINT\","
            + "\"checkpoint_count\":2}}"
        );

        assertEquals("Checkpoint captured (2)",
            manifest.getCharacterCheck().getDisplayStatus());
        assertEquals("Submit Checkpoint",
            manifest.getCharacterCheck().getButtonLabel());
        assertTrue(manifest.getCharacterCheck().canSubmit());
    }
}
