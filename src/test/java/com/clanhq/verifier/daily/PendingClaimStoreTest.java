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
        assertEquals(0, restarted.forScope("installation-b").size());
        assertEquals(1, restarted.forScope("installation-a").size());

        restarted.acknowledge(PendingClaimStore.string(retry, "claim_id"));
        assertEquals(0, new PendingClaimStore(disk::get, disk::set)
            .forScope("installation-a").size());
    }
}
