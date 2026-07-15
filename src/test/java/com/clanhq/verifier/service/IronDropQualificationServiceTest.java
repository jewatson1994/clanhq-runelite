package com.clanhq.verifier.service;

import com.clanhq.verifier.model.DiaryProgress;
import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RaidKillCounts;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import net.runelite.api.ItemID;
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

    @Test
    public void verifiesCompleteMaxedArmourSetsWithoutMixingSetFamilies()
    {
        List<ObservedItem> items = Arrays.asList(
            bankItem(1, "Ancestral hat"),
            bankItem(2, "Ancestral robe top"),
            bankItem(3, "Ancestral robe bottom"),
            bankItem(4, "Masori mask (f)"),
            bankItem(5, "Masori body (f)"),
            bankItem(6, "Masori chaps (f)"),
            bankItem(7, "Radiant oathplate helm"),
            bankItem(8, "Radiant oathplate chest"),
            bankItem(9, "Radiant oathplate legs"),
            bankItem(10, "Avernic treads (max)"));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, items, true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12));

        RankQualificationResult result = service.evaluateTarget(snapshot, "Maxed");

        assertPassed(result, "Full Ancestral");
        assertPassed(result, "Full fortified Masori");
        assertPassed(result, "Full Oathplate or Torva");
        assertPassed(result, "Maxed Avernic treads");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void verifiesCollectionSlotsUpgradedTreadsAndHillGiantClub()
    {
        List<ObservedItem> items = Arrays.asList(
            bankItem(ItemID.AVERNIC_TREADS_PRPE, "Avernic treads (pr, pe)"),
            bankItem(ItemID.HILL_GIANT_CLUB, "Hill giant club"));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, items, true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12),
            RaidKillCounts.unavailable("Not requested"), 800);

        assertPassed(service.evaluateTarget(snapshot, "Dragon"),
            "All Cerberus boots or 2-upgrade Avernic treads");
        assertPassed(service.evaluateTarget(snapshot, "Kitten"),
            "750 collection-log slots");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "Hill giant club");
    }

    private static ObservedItem bankItem(int id, String name)
    {
        return new ObservedItem(id, name, 1, EvidenceSource.BANK);
    }

    private static void assertPassed(RankQualificationResult result, String name)
    {
        assertEquals(RequirementStatus.PASSED,
            result.getRequirements().stream()
                .filter(item -> item.getName().equals(name))
                .findFirst().orElseThrow(AssertionError::new).getStatus());
    }
}
