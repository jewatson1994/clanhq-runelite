package com.clanhq.verifier.service;

import com.clanhq.verifier.model.DiaryProgress;
import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.EvidenceStage;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RaidKillCounts;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.model.PohEvidence;
import com.clanhq.verifier.model.CollectionLogEvidence;
import com.clanhq.verifier.model.BoatEvidence;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
        assertTrue(service.getRequiredStages("Dragon").contains(EvidenceStage.POH));
        assertTrue(service.getRequiredStages("Maxed").contains(EvidenceStage.RAID_KC));
        assertTrue(service.getRequiredStages("Topaz").contains(EvidenceStage.PRAYERS));
        assertTrue(service.getRequiredStages("Completionism").contains(EvidenceStage.BOAT));
        assertTrue(service.getRequiredStages("Kitten")
            .contains(EvidenceStage.COLLECTION_OVERVIEW));
        assertTrue(service.getRequiredStages("Zenyte")
            .containsAll(Arrays.asList(EvidenceStage.COX_LOG,
                EvidenceStage.TOB_LOG, EvidenceStage.TOA_LOG)));
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
            RaidKillCounts.unavailable("Not requested"), 0)
            .withCollectionLogEvidence(CollectionLogEvidence.overview(
                "Dragon", 800));

        assertPassed(service.evaluateTarget(snapshot, "Dragon"),
            "All Cerberus boots or 2-upgrade Avernic treads");
        assertPassed(service.evaluateTarget(snapshot, "Kitten"),
            "750 collection-log slots");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "Hill giant club");
    }

    @Test
    public void verifiesOwnedRaidUniquesAndGrandmasterReward()
    {
        List<ObservedItem> items = Arrays.asList(
            bankItem(101, "Dexterous prayer scroll"),
            bankItem(102, "Dragon hunter crossbow"),
            bankItem(103, "Twisted buckler"),
            bankItem(104, "Twisted bow"),
            bankItem(105, "Osmumten's fang"),
            bankItem(106, "Lightbearer"),
            bankItem(107, "Masori body (f)"),
            bankItem(108, "Avernic defender"),
            bankItem(109, "Scythe of vitur"),
            bankItem(110, "Ghommal's hilt 6"));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, items, true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12));

        RankQualificationResult diamond = service.evaluateTarget(snapshot, "Diamond");
        assertPassed(diamond, "4 Chambers of Xeric uniques");
        assertPassed(diamond, "3 Tombs of Amascut uniques");
        assertPassed(service.evaluateTarget(snapshot, "Onyx"),
            "2 Theatre of Blood uniques (including Avernic)");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "Grandmaster Combat Achievements");
    }

    @Test
    public void combinesOwnedAndCollectionLogRaidUniques()
    {
        Map<String, Integer> logged = new LinkedHashMap<>();
        logged.put("Dexterous prayer scroll", 1);
        logged.put("Arcane prayer scroll", 1);
        logged.put("Twisted buckler", 1);
        CollectionLogEvidence collectionLog = new CollectionLogEvidence(
            Collections.singletonMap("Chambers of Xeric", logged));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126,
            Collections.singletonList(bankItem(200, "Twisted bow")), true,
            false).withCollectionLogEvidence(collectionLog);

        assertPassed(service.evaluateTarget(snapshot, "Diamond"),
            "4 Chambers of Xeric uniques");
    }

    @Test
    public void verifiesCapturedMaxedPohAndDoomCollectionLog()
    {
        VerificationSnapshot base = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false);
        PohEvidence poh = new PohEvidence(true, new LinkedHashSet<>(Arrays.asList(
            PohEvidence.SPIRITUAL_FAIRY, PohEvidence.JEWELLERY_BOX,
            PohEvidence.OCCULT_ALTAR, PohEvidence.PORTAL_NEXUS,
            PohEvidence.REJUVENATION_POOL)));
        Map<String, Integer> doomItems = new LinkedHashMap<>();
        doomItems.put("Doom cloth", 1);
        doomItems.put("Doom boots", 1);
        doomItems.put("Eye of Doom", 1);
        CollectionLogEvidence doom = new CollectionLogEvidence(
            Collections.singletonMap("Doom of Mokhaiotl", doomItems));
        VerificationSnapshot snapshot = base.withPohEvidence(poh)
            .withCollectionLogEvidence(doom);

        assertPassed(service.evaluateTarget(snapshot, "Dragon"), "Maxed POH");
        assertPassed(service.evaluateTarget(snapshot, "Onyx"), "All Doom uniques");
    }

    @Test
    public void includesCapturedBoatPanelsForStaffReview()
    {
        VerificationSnapshot base = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false);
        VerificationSnapshot snapshot = base.withBoatEvidence(new BoatEvidence(
            new LinkedHashSet<>(Arrays.asList("Skiff", "Sloop")),
            Arrays.asList("Skiff", "Sloop", "Hull level 3")));

        RankQualificationResult result = service.evaluateTarget(
            snapshot, "Completionism");

        assertEquals(RequirementStatus.UNVERIFIED,
            result.getRequirements().stream()
                .filter(item -> item.getName().equals("Maxed skiff and sloop"))
                .findFirst().orElseThrow(AssertionError::new).getStatus());
        assertTrue(result.getRequirements().stream()
            .filter(item -> item.getName().equals("Maxed skiff and sloop"))
            .findFirst().orElseThrow(AssertionError::new).getDetail()
            .contains("Skiff, Sloop"));
    }

    @Test
    public void verifiesAutomatableCompletionismItems()
    {
        List<ObservedItem> items = Arrays.asList(
            bankItem(301, "Metamorphic dust"),
            bankItem(302, "Sanguine dust"),
            bankItem(303, "Sanguine ornament kit"),
            bankItem(304, "Holy ornament kit"),
            bankItem(305, "Saturated heart"),
            bankItem(306, "Amulet of rancour"),
            new ObservedItem(307, "Twisted ancestral colour kit", 3,
                EvidenceSource.BANK),
            bankItem(308, "Champion's cape"),
            bankItem(309, "Expert dragon archer hat"));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, items, true, false);

        RankQualificationResult result = service.evaluateTarget(
            snapshot, "Completionism");

        assertPassed(result, "Metamorphic dust");
        assertPassed(result, "Sanguine dust");
        assertPassed(result, "3 Twisted ancestral colour kits");
        assertPassed(result, "Expert dragon archer hat");
    }

    @Test
    public void verifiesDragonCollectionLogRankFromOverview()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(new CollectionLogEvidence(
                Collections.emptyMap(), "Dragon"));

        assertPassed(service.evaluateTarget(snapshot, "Completionism"),
            "Dragon Collection Log rank");
    }

    @Test
    public void verifiesCollectionSlotsAndRaidGreenLogs()
    {
        Map<String, Integer> item = Collections.singletonMap("Unique", 1);
        CollectionLogEvidence evidence = CollectionLogEvidence.overview(
            "Dragon", 800)
            .merge(CollectionLogEvidence.page(
                "Chambers of Xeric", item, 12, 12))
            .merge(CollectionLogEvidence.page(
                "Theatre of Blood", item, 17, 17))
            .merge(CollectionLogEvidence.page(
                "Tombs of Amascut", item, 27, 27));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(evidence);

        assertPassed(service.evaluateTarget(snapshot, "Kitten"),
            "750 collection-log slots");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "All raids green logged");
    }

    @Test
    public void verifiesAllEightToaCosmeticsFromCollectionLog()
    {
        Map<String, Integer> cosmetics = new LinkedHashMap<>();
        cosmetics.put("Remnant of kephri", 1);
        cosmetics.put("Remnant of ba-ba", 1);
        cosmetics.put("Remnant of akkha", 1);
        cosmetics.put("Remnant of zebak", 1);
        cosmetics.put("Menaphite ornament kit", 1);
        cosmetics.put("Cursed phalanx", 1);
        cosmetics.put("Ancient remnant", 1);
        cosmetics.put("Masori crafting kit", 1);
        CollectionLogEvidence evidence = CollectionLogEvidence.page(
            "Tombs of Amascut", cosmetics, 8, 27);
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(evidence);

        assertPassed(service.evaluateTarget(snapshot, "Completionism"),
            "All TOA cosmetics and transmogs");
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
