package com.clanhq.verifier.service;

import com.clanhq.verifier.model.DiaryProgress;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RaidKillCounts;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IronDropQualificationServiceTest
{
    private final IronDropQualificationService service =
        new IronDropQualificationService(new OpalQualificationService());

    @Test
    public void evaluatesEveryProgressionRankAndBlocksCumulativeSkipping()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12));

        List<RankQualificationResult> results = service.evaluate(snapshot);

        assertEquals(15, results.size());
        assertEquals("Opal", results.get(0).getRankName());
        assertEquals("Zenyte", results.get(14).getRankName());
        assertFalse(results.get(1).isQualified());
        assertEquals(RequirementStatus.MISSING,
            results.get(1).getRequirements().get(0).getStatus());
    }

    @Test
    public void evaluatesOnlyTheSelectedTargetAndDefersPriorRankToClanHQ()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2000, 126, Collections.emptyList(), false,
            false, false, false, false, 99, new DiaryProgress(0, 0, 12));

        RankQualificationResult result = service.evaluateTarget(snapshot, "Jade");

        assertEquals("Jade", result.getRankName());
        assertEquals("Previous rank verified in ClanHQ",
            result.getRequirements().get(0).getName());
        assertEquals(RequirementStatus.UNVERIFIED,
            result.getRequirements().get(0).getStatus());
        assertTrue(service.getRankNames().contains("Zenyte"));
    }

    @Test
    public void passesCombinedRaidKcFromAllHiscoreCategories()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12),
            RaidKillCounts.available(420, 35, 180, 20, 310, 95));

        RankQualificationResult result = service.evaluateTarget(snapshot, "Maxed");

        assertEquals(RequirementStatus.PASSED,
            result.getRequirements().stream()
                .filter(item -> item.getName().equals("1000 combined raids KC"))
                .findFirst().orElseThrow(AssertionError::new).getStatus());
    }
}
