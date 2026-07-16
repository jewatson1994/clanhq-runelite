package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import com.google.gson.JsonObject;
import java.time.Instant;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BingoApiClientTest
{
    @Test
    public void createsVersionedDropPayload()
    {
        BingoDrop drop = new BingoDrop(
            "summer-2026",
            "Mr Dimples",
            new BingoItem(4151, "Abyssal whip", 1, 5),
            2,
            "NPC_LOOT",
            "Abyssal demon",
            Instant.parse("2026-08-02T03:04:05Z"));

        JsonObject payload = BingoApiClient.payload(drop);

        assertEquals(1, payload.get("schema_version").getAsInt());
        assertEquals("summer-2026", payload.get("event_id").getAsString());
        assertEquals("Mr Dimples", payload.get("rsn").getAsString());
        assertEquals(4151, payload.get("item_id").getAsInt());
        assertEquals(2, payload.get("quantity").getAsInt());
        assertEquals("NPC_LOOT", payload.get("source_type").getAsString());
        assertEquals("Abyssal demon", payload.get("source_name").getAsString());
        assertEquals("2026-08-02T03:04:05Z",
            payload.get("occurred_at").getAsString());
    }
}
