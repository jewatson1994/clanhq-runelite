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
import com.clanhq.verifier.model.BoatConfiguration;
import com.clanhq.verifier.model.ProgressionEvaluation;
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
    public void evaluatesEveryProgressionRankFromTheSameSnapshot()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12));

        List<RankQualificationResult> results = service.evaluate(snapshot);

        assertEquals(15, results.size());
        assertEquals("Opal", results.get(0).getRankName());
        assertEquals("Zenyte", results.get(14).getRankName());
        assertFalse(results.get(1).isQualified());
        assertEquals(RequirementStatus.PASSED,
            results.get(1).getRequirements().get(0).getStatus());
    }

    @Test
    public void evaluatesAnIndividualRankWithoutAClanHqPlaceholder()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2000, 126, Collections.emptyList(), false,
            false, false, false, false, 99, new DiaryProgress(0, 0, 12));

        RankQualificationResult result = service.evaluateTarget(snapshot, "Jade");

        assertEquals("Jade", result.getRankName());
        assertEquals("1950 total level",
            result.getRequirements().get(0).getName());
        assertEquals(RequirementStatus.PASSED,
            result.getRequirements().get(0).getStatus());
        assertTrue(service.getRankNames().contains("Zenyte"));
        assertTrue(service.getRequiredStages("Dragon").contains(EvidenceStage.POH));
        assertTrue(service.getRequiredStages("Maxed").contains(EvidenceStage.RAID_KC));
        assertTrue(service.getRequiredStages("Topaz").contains(EvidenceStage.PRAYERS));
        assertTrue(service.getRequiredStages("Diamond").contains(EvidenceStage.YAMA_LOG));
        assertTrue(service.getRequiredStages("Completionism").contains(EvidenceStage.BOAT));
        assertTrue(service.getRequiredStages("Completionism")
            .containsAll(Arrays.asList(EvidenceStage.COX_LOG,
                EvidenceStage.TOB_LOG, EvidenceStage.TOA_LOG)));
        assertTrue(service.getRequiredStages("Zenyte")
            .containsAll(Arrays.asList(EvidenceStage.COX_LOG,
                EvidenceStage.TOB_LOG, EvidenceStage.TOA_LOG)));
        assertEquals(EvidenceStage.values().length,
            service.getAllEvidenceStages().size());
    }

    @Test
    public void calculatesTheHighestContiguousVerifiedRank()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2094, 122, Arrays.asList(
                bankItem(ItemID.GHOMMALS_HILT_3, "Ghommal's hilt 3"),
                bankItem(ItemID.BARROWS_GLOVES, "Barrows gloves"),
                bankItem(ItemID.TRIDENT_OF_THE_SEAS, "Trident of the seas"),
                bankItem(ItemID.ABYSSAL_WHIP, "Abyssal whip"),
                bankItem(ItemID.AVERNIC_DEFENDER, "Avernic defender")),
            true, true);

        ProgressionEvaluation progression =
            service.evaluateProgression(snapshot);

        assertEquals("Opal", progression.getHighestVerifiedRankName());
        assertEquals("Jade", progression.getNextRank().get().getRankName());
    }

    @Test
    public void acceptsMasoriAssemblerForTheJadeAssemblerRequirement()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 1950, 100,
            Collections.singletonList(bankItem(99, "Masori assembler")),
            true, true);

        assertPassed(service.evaluateTarget(snapshot, "Jade"),
            "Ava's Assembler or better");
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
            .withCollectionLogEvidence(CollectionLogEvidence.slotCount(800));

        assertPassed(service.evaluateTarget(snapshot, "Dragon"),
            "All Cerberus boots or 2-upgrade Avernic treads");
        assertPassed(service.evaluateTarget(snapshot, "Kitten"),
            "750 collection-log slots");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "Hill giant club");
    }

    @Test
    public void verifiesOwnedRaidUniquesAndGrandmasterCombatAchievements()
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
            bankItem(109, "Scythe of vitur"));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, items, true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12))
            .withGrandmasterCombatAchievements(true);

        RankQualificationResult diamond = service.evaluateTarget(snapshot, "Diamond");
        assertPassed(diamond, "4 Chambers of Xeric uniques");
        assertPassed(diamond, "3 Tombs of Amascut uniques");
        assertPassed(service.evaluateTarget(snapshot, "Onyx"),
            "2 Theatre of Blood uniques (including Avernic)");
        assertPassed(service.evaluateTarget(snapshot, "Zenyte"),
            "Grandmaster Combat Achievements");
    }

    @Test
    public void verifiesRiteOfVileTransferenceFromTheYamaLog()
    {
        Map<String, Integer> logged = new LinkedHashMap<>();
        logged.put("Rite of Vile Transference", 1);
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2175, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(new CollectionLogEvidence(
                Collections.singletonMap("Yama", logged)));

        assertPassed(service.evaluateTarget(snapshot, "Diamond"),
            "Rite of Vile Transference");
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
    public void verifiesMaxedSkiffAndSloopFromStructuredBoatEvidence()
    {
        VerificationSnapshot base = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false);
        VerificationSnapshot snapshot = base.withBoatEvidence(new BoatEvidence(
            Collections.emptySet(), Arrays.asList(
                new BoatConfiguration(1, "Skiff", "Rosewood hull",
                    "Rosewood mast and cotton sails", "Dragon helm",
                    "Dragon keel"),
                new BoatConfiguration(2, "Sloop", "Rosewood hull",
                    "Rosewood mast and cotton sails", "Dragon helm",
                    "Dragon keel")), Collections.emptyList()));

        RankQualificationResult result = service.evaluateTarget(
            snapshot, "Completionism");

        assertEquals(RequirementStatus.PASSED,
            result.getRequirements().stream()
                .filter(item -> item.getName().equals("Maxed skiff and sloop"))
                .findFirst().orElseThrow(AssertionError::new).getStatus());
        assertTrue(result.getRequirements().stream()
            .filter(item -> item.getName().equals("Maxed skiff and sloop"))
            .findFirst().orElseThrow(AssertionError::new).getDetail()
            .contains("Rosewood hull"));
    }

    @Test
    public void rejectsBoatsWithIncompleteCoreUpgrades()
    {
        VerificationSnapshot base = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false);
        VerificationSnapshot snapshot = base.withBoatEvidence(new BoatEvidence(
            Collections.emptySet(), Arrays.asList(
                new BoatConfiguration(1, "Skiff", "Wooden hull",
                    "Wooden mast and linen sails", "Bronze helm",
                    "Bronze keel"),
                new BoatConfiguration(2, "Sloop", "Rosewood hull",
                    "Rosewood mast and cotton sails", "Dragon helm",
                    "Dragon keel")), Collections.emptyList()));

        RankQualificationResult result = service.evaluateTarget(
            snapshot, "Completionism");

        assertEquals(RequirementStatus.MISSING,
            result.getRequirements().stream()
                .filter(item -> item.getName().equals("Maxed skiff and sloop"))
                .findFirst().orElseThrow(AssertionError::new).getStatus());
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
    public void verifiesDragonCollectionLogRankFromHeaderCount()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(CollectionLogEvidence.slotCount(1200));

        assertPassed(service.evaluateTarget(snapshot, "Completionism"),
            "Dragon Collection Log rank");
    }

    @Test
    public void verifiesCollectionSlotsAndRaidGreenLogs()
    {
        Map<String, Integer> item = Collections.singletonMap("Unique", 1);
        CollectionLogEvidence evidence = CollectionLogEvidence.slotCount(800)
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

    @Test
    public void verifiesCoxAndTobCosmeticsFromCollectionLogs()
    {
        Map<String, Integer> cox = Collections.singletonMap(
            "Metamorphic dust", 1);
        Map<String, Integer> tob = new LinkedHashMap<>();
        tob.put("Sanguine dust", 1);
        tob.put("Sanguine ornament kit", 1);
        tob.put("Holy ornament kit", 1);
        CollectionLogEvidence evidence = CollectionLogEvidence.page(
            "Chambers of Xeric", cox, 1, 12)
            .merge(CollectionLogEvidence.page(
                "Theatre of Blood", tob, 3, 12));
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples", 2350, 126, Collections.emptyList(), false, false)
            .withCollectionLogEvidence(evidence);

        RankQualificationResult result = service.evaluateTarget(
            snapshot, "Completionism");
        assertPassed(result, "Metamorphic dust");
        assertPassed(result, "Sanguine dust");
        assertPassed(result, "Sanguine ornament kit");
        assertPassed(result, "Holy ornament kit");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void fullyCapturedFixtureQualifiesForEveryProgressionRank()
    {
        List<ObservedItem> items = new java.util.ArrayList<>(Arrays.asList(
            bankItem(ItemID.GHOMMALS_HILT_6, "Ghommal's hilt 6"),
            bankItem(ItemID.BARROWS_GLOVES, "Barrows gloves"),
            bankItem(ItemID.TUMEKENS_SHADOW, "Tumeken's shadow"),
            bankItem(ItemID.SOULREAPER_AXE, "Soulreaper axe"),
            bankItem(ItemID.AVERNIC_DEFENDER, "Avernic defender"),
            bankItem(1001, "Imbued god cape"),
            bankItem(1002, "Infernal cape"),
            bankItem(1003, "Blessed Dizana's quiver"),
            bankItem(1004, "Torva full helm"),
            bankItem(1005, "Torva platebody"),
            bankItem(1006, "Torva platelegs"),
            bankItem(1007, "Blood moon helm"),
            bankItem(1008, "Blood moon chestplate"),
            bankItem(1009, "Blood moon tassets"),
            bankItem(1010, "Blue moon spear"),
            bankItem(1011, "Dual macuahuitl"),
            bankItem(1012, "Void ranger helm"),
            bankItem(1013, "Elite void top"),
            bankItem(1014, "Elite void robe"),
            bankItem(1015, "Void knight gloves"),
            bankItem(1016, "Amulet of torture"),
            bankItem(1017, "Necklace of anguish"),
            bankItem(1018, "Tormented bracelet"),
            bankItem(1019, "Ring of suffering"),
            bankItem(1020, "Serpentine helm"),
            bankItem(1021, "Magic fang"),
            bankItem(1022, "Bow of faerdhinen (c)"),
            bankItem(1023, "Crystal helm"),
            bankItem(1024, "Crystal body"),
            bankItem(1025, "Crystal legs"),
            bankItem(1026, "Occult necklace"),
            bankItem(1027, "Primordial boots"),
            bankItem(1028, "Bandos godsword"),
            bankItem(1029, "Berserker ring"),
            bankItem(1030, "Archers ring"),
            bankItem(1031, "Seers ring"),
            bankItem(1032, "Warrior ring"),
            bankItem(1033, "Quest point cape"),
            bankItem(1034, "Osmumten's fang"),
            new ObservedItem(1035, "Tormented synapse", 2,
                EvidenceSource.BANK),
            bankItem(1036, "Dragon claws"),
            bankItem(1037, "Max cape"),
            bankItem(1038, "Voidwaker"),
            bankItem(1039, "Dragon hunter lance"),
            bankItem(1040, "Venator bow"),
            bankItem(ItemID.AVERNIC_TREADS_MAX, "Avernic treads (max)"),
            bankItem(1041, "Amulet of rancour"),
            bankItem(1042, "Ultor ring"),
            bankItem(1043, "Bellator ring"),
            bankItem(1044, "Magus ring"),
            bankItem(1045, "Ancestral hat"),
            bankItem(1046, "Ancestral robe top"),
            bankItem(1047, "Ancestral robe bottom"),
            bankItem(1048, "Armadyl godsword"),
            bankItem(1049, "Saradomin godsword"),
            bankItem(1050, "Zamorak godsword"),
            bankItem(1051, "Twisted bow"),
            bankItem(1052, "Scythe of vitur"),
            bankItem(1053, "Masori mask (f)"),
            bankItem(1054, "Masori body (f)"),
            bankItem(1055, "Masori chaps (f)"),
            bankItem(1056, "Saturated heart"),
            new ObservedItem(1057, "Twisted ancestral colour kit", 3,
                EvidenceSource.BANK),
            bankItem(1058, "Champion's cape"),
            bankItem(1059, "Expert dragon archer hat"),
            bankItem(ItemID.HILL_GIANT_CLUB, "Hill giant club")));

        CollectionLogEvidence collectionLog = CollectionLogEvidence
            .slotCount(1300)
            .merge(greenLog("Chambers of Xeric",
                "Dexterous prayer scroll", "Arcane prayer scroll",
                "Dragon hunter crossbow", "Twisted buckler",
                "Dinh's bulwark", "Ancestral hat",
                "Ancestral robe top", "Metamorphic dust"))
            .merge(greenLog("Theatre of Blood", "Avernic defender",
                "Ghrazi rapier", "Sanguinesti staff", "Scythe of vitur",
                "Sanguine dust", "Sanguine ornament kit",
                "Holy ornament kit"))
            .merge(greenLog("Tombs of Amascut", "Osmumten's fang",
                "Lightbearer", "Elidinis' ward", "Masori mask",
                "Masori body", "Remnant of kephri", "Remnant of ba-ba",
                "Remnant of akkha", "Remnant of zebak",
                "Menaphite ornament kit", "Cursed phalanx",
                "Ancient remnant", "Masori crafting kit"))
            .merge(greenLog("Yama", "Rite of Vile Transference"))
            .merge(greenLog("Doom of Mokhaiotl", "Doom cloth",
                "Doom boots", "Eye of Doom"));

        PohEvidence poh = new PohEvidence(true,
            new LinkedHashSet<>(Arrays.asList(PohEvidence.SPIRITUAL_FAIRY,
                PohEvidence.JEWELLERY_BOX, PohEvidence.OCCULT_ALTAR,
                PohEvidence.PORTAL_NEXUS,
                PohEvidence.REJUVENATION_POOL)));
        BoatEvidence boats = new BoatEvidence(Collections.emptySet(),
            Arrays.asList(
                new BoatConfiguration(1, "Skiff", "Rosewood base",
                    "Rosewood mast and sails", "Dragon helm",
                    "Dragon keel"),
                new BoatConfiguration(2, "Sloop", "Rosewood base",
                    "Rosewood mast and sails", "Dragon helm",
                    "Dragon keel")),
            Collections.emptyList());
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Fixture", 2376, 126, items, true, true, true, true, true,
            99, new DiaryProgress(12, 12, 12),
            RaidKillCounts.available(300, 200, 300, 100, 300, 100))
            .withCollectionLogEvidence(collectionLog)
            .withPohEvidence(poh)
            .withBoatEvidence(boats)
            .withGrandmasterCombatAchievements(true);

        List<RankQualificationResult> results = service.evaluate(snapshot);

        assertEquals(service.getRankNames().size(), results.size());
        for (RankQualificationResult rank : results)
        {
            assertTrue(rank.getRankName() + " should qualify",
                rank.isQualified());
        }
        ProgressionEvaluation progression =
            service.evaluateProgression(snapshot);
        assertEquals("Zenyte", progression.getHighestVerifiedRankName());
        assertFalse(progression.getNextRank().isPresent());
    }

    private static CollectionLogEvidence greenLog(String title,
        String... itemNames)
    {
        Map<String, Integer> items = new LinkedHashMap<>();
        Arrays.stream(itemNames).forEach(name -> items.put(name, 1));
        return CollectionLogEvidence.page(title, items, items.size(),
            items.size());
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
