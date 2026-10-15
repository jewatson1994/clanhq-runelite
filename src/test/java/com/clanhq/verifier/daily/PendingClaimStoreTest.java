package com.clanhq.verifier.daily;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class PendingClaimStoreTest
{
    @Test
    public void clickIsPersistedBeforeDeliveryAndRetryKeepsClaimId()
    {
        AtomicReference<String> disk = new AtomicReference<>();
        PendingClaimStore first = new PendingClaimStore(disk::get, disk::set);
        JsonArray progress = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("progress", 10);
        item.addProperty("category", "SKILLING");
        item.addProperty("rsn", "low level");
        progress.add(item);
        JsonObject queued = first.enqueue("installation-a", "2026-09-29",
            "2026-09-30T00:00:00Z", "2026-09-29T23:59:59Z", "rotation-1", "SKILLING", "task-1",
            progress);
        assertNotNull(disk.get());
        progress.get(0).getAsJsonObject().addProperty("progress", 0);

        PendingClaimStore restarted = new PendingClaimStore(disk::get, disk::set);
        JsonObject retry = restarted.enqueue("installation-a", "2026-09-29",
            "2026-09-30T00:00:00Z", "2026-09-29T23:59:59Z", "rotation-1", "SKILLING", "task-1",
            progress);
        assertEquals(PendingClaimStore.string(queued, "claim_id"),
            PendingClaimStore.string(retry, "claim_id"));
        assertEquals("2026-09-29T23:59:59Z",
            PendingClaimStore.string(retry, "clicked_at"));
        assertEquals(10, retry.getAsJsonArray("client_progress")
            .get(0).getAsJsonObject().get("progress").getAsInt());
        assertEquals("low level", retry.getAsJsonArray("client_progress")
            .get(0).getAsJsonObject().get("rsn").getAsString());
        assertEquals(0, restarted.forScope("installation-b").size());
        assertEquals(1, restarted.forScope("installation-a").size());

        restarted.acknowledge(PendingClaimStore.string(retry, "claim_id"));
        assertEquals(0, new PendingClaimStore(disk::get, disk::set)
            .forScope("installation-a").size());
    }

    @Test
    public void upgradeDoesNotReplayUnattributedXpClaims()
    {
        AtomicReference<String> disk = new AtomicReference<>();
        PendingClaimStore oldClient = new PendingClaimStore(disk::get, disk::set);
        JsonArray progress = new JsonArray();
        JsonObject item = new JsonObject();
        item.addProperty("category", "SKILLING");
        item.addProperty("progress", 1000);
        progress.add(item);
        oldClient.enqueue("installation", "2026-10-14", "2026-10-15T00:00:00Z",
            "2026-10-14T23:59:59Z", "context", "SKILLING", "mining", progress);
        oldClient.enqueue("installation", "2026-10-14", "2026-10-15T00:00:00Z",
            "2026-10-14T23:59:59Z", "context", "PVM", "boss", new JsonArray());
        PendingClaimStore upgraded = new PendingClaimStore(disk::get, disk::set);
        assertEquals(1, upgraded.forScope("installation").size());
        assertEquals("PVM", PendingClaimStore.string(
            upgraded.forScope("installation").get(0), "category"));
    }
}
