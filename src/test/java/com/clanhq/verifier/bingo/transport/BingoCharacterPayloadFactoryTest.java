package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BingoCharacterPayloadFactoryTest
{
    @Test
    public void serializesEveryCapturedItemBySource()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Drop Player", 2200, 126, Arrays.asList(
                new ObservedItem(1, "Unconfigured bank item", 400,
                    EvidenceSource.BANK),
                new ObservedItem(2, "Inventory item", 2,
                    EvidenceSource.INVENTORY),
                new ObservedItem(3, "Equipped item", 1,
                    EvidenceSource.EQUIPMENT)), true, true)
            .withAccountMetrics(Collections.singletonMap(
                "tempoross_reward_permits", 12));
        BingoCharacterSubmission submission =
            new BingoCharacterSubmission("BINGO-TEST01", snapshot);

        JsonObject payload = BingoCharacterPayloadFactory.create(submission);

        assertEquals("Drop Player", payload.get("rsn").getAsString());
        assertEquals("BINGO-TEST01", payload.get("event_id").getAsString());
        assertFalse(payload.get("submission_id").getAsString().isEmpty());
        assertFalse(payload.get("captured_at").getAsString().isEmpty());
        assertEquals(1, payload.getAsJsonArray("bank").size());
        assertEquals(400, payload.getAsJsonArray("bank").get(0)
            .getAsJsonObject().get("quantity").getAsInt());
        assertEquals(1, payload.getAsJsonArray("inventory").size());
        assertEquals(1, payload.getAsJsonArray("equipment").size());
        assertEquals(12, payload.getAsJsonObject("evidence")
            .getAsJsonObject("metrics")
            .get("tempoross_reward_permits").getAsInt());
    }
}
