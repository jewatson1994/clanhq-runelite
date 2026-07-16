package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementResult;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.UUID;

public final class VerificationPayloadFactory
{
    private VerificationPayloadFactory()
    {
    }

    public static JsonObject create(VerificationSnapshot snapshot,
        ProgressionEvaluation progression)
    {
        JsonObject payload = new JsonObject();
        payload.addProperty("schema_version", 1);
        payload.addProperty("submission_id", UUID.randomUUID().toString());
        payload.addProperty("rsn", snapshot.getRsn());
        payload.addProperty("captured_at", Instant.now().toString());
        payload.addProperty("highest_verified_rank",
            progression.getHighestVerifiedRankName());

        JsonArray ranks = new JsonArray();
        for (RankQualificationResult rank : progression.getRanks())
        {
            JsonObject rankJson = new JsonObject();
            rankJson.addProperty("rank", rank.getRankName());
            JsonArray requirements = new JsonArray();
            for (RequirementResult requirement : rank.getRequirements())
            {
                JsonObject requirementJson = new JsonObject();
                requirementJson.addProperty("name", requirement.getName());
                requirementJson.addProperty("status",
                    requirement.getStatus().name());
                requirementJson.addProperty("detail", requirement.getDetail());
                requirements.add(requirementJson);
            }
            rankJson.add("requirements", requirements);
            ranks.add(rankJson);
        }
        payload.add("ranks", ranks);

        JsonObject evidence = new JsonObject();
        evidence.addProperty("total_level", snapshot.getTotalLevel());
        evidence.addProperty("combat_level", snapshot.getCombatLevel());
        payload.add("evidence", evidence);
        return payload;
    }
}
