package com.clanhq.verifier.transport;

import com.clanhq.verifier.model.ProgressionEvaluation;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VerificationPayloadFactoryTest
{
    @Test
    public void serializesRankEvidenceForServerValidation()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2094, 122, Collections.emptyList(), true, true);
        RankQualificationResult opal = new RankQualificationResult("Opal",
            Collections.singletonList(new RequirementResult("Total level",
                RequirementStatus.PASSED, "2094 / 1900")));
        RankQualificationResult jade = new RankQualificationResult("Jade",
            Collections.singletonList(new RequirementResult("Herblore",
                RequirementStatus.UNVERIFIED, "Staff review")));
        ProgressionEvaluation progression = new ProgressionEvaluation(
            Arrays.asList(opal, jade));

        JsonObject payload = VerificationPayloadFactory.create(snapshot,
            progression);

        assertEquals(1, payload.get("schema_version").getAsInt());
        assertEquals("Mr Dimples", payload.get("rsn").getAsString());
        assertEquals("Opal",
            payload.get("highest_verified_rank").getAsString());
        assertEquals("UNVERIFIED", payload.getAsJsonArray("ranks").get(1)
            .getAsJsonObject().getAsJsonArray("requirements").get(0)
            .getAsJsonObject().get("status").getAsString());
        assertEquals(2094, payload.getAsJsonObject("evidence")
            .get("total_level").getAsInt());
        assertTrue(!payload.getAsJsonObject("evidence").has("summary"));
    }
}
