package com.clanhq.verifier.bingo.transport;

import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.ObservedItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class BingoCharacterPayloadFactory
{
    private BingoCharacterPayloadFactory()
    {
    }

    public static JsonObject create(BingoCharacterSubmission submission)
    {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema_version", 1);
        payload.addProperty("submission_id", submission.getSubmissionId());
        payload.addProperty("event_id", submission.getEventId());
        payload.addProperty("rsn", submission.getSnapshot().getRsn());
        payload.addProperty("captured_at", submission.getCapturedAt().toString());

        JsonObject evidence = new JsonObject();
        evidence.addProperty("total_level",
            submission.getSnapshot().getTotalLevel());
        evidence.addProperty("combat_level",
            submission.getSnapshot().getCombatLevel());
        JsonObject metrics = new JsonObject();
        submission.getSnapshot().getAccountMetrics().forEach(
            metrics::addProperty);
        evidence.add("metrics", metrics);
        payload.add("evidence", evidence);

        payload.add("bank", items(submission, EvidenceSource.BANK));
        payload.add("inventory", items(submission, EvidenceSource.INVENTORY));
        payload.add("equipment", items(submission, EvidenceSource.EQUIPMENT));
        return payload;
    }

    private static JsonArray items(BingoCharacterSubmission submission,
        EvidenceSource source)
    {
        JsonArray values = new JsonArray();
        for (ObservedItem item : submission.getSnapshot().getItems())
        {
            if (item.getSource() != source)
            {
                continue;
            }
            JsonObject value = new JsonObject();
            value.addProperty("item_id", item.getItemId());
            value.addProperty("name", item.getName());
            value.addProperty("quantity", item.getQuantity());
            values.add(value);
        }
        return values;
    }
}
