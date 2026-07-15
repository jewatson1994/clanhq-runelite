package com.clanhq.verifier.service;

import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

/** Evaluates Iron Drop's cumulative progression ranks from locally observed evidence. */
public final class IronDropQualificationService
{
    private static final List<String> RANK_NAMES = Collections.unmodifiableList(Arrays.asList(
        "Opal", "Jade", "Topaz", "Sapphire", "Emerald", "Ruby", "Diamond",
        "Dragon", "Dragonstone", "TzKal", "Onyx", "Kitten", "Maxed",
        "Completionism", "Zenyte"));
    private final OpalQualificationService opalService;

    @Inject
    public IronDropQualificationService(OpalQualificationService opalService)
    {
        this.opalService = opalService;
    }

    public List<RankQualificationResult> evaluate(VerificationSnapshot snapshot)
    {
        List<RankQualificationResult> ranks = new ArrayList<>();
        ranks.add(opalService.evaluate(snapshot));
        ranks.add(rank("Jade", ranks, jade(snapshot)));
        ranks.add(rank("Topaz", ranks, topaz(snapshot)));
        ranks.add(rank("Sapphire", ranks, sapphire(snapshot)));
        ranks.add(rank("Emerald", ranks, emerald(snapshot)));
        ranks.add(rank("Ruby", ranks, ruby(snapshot)));
        ranks.add(rank("Diamond", ranks, diamond(snapshot)));
        ranks.add(rank("Dragon", ranks, dragon(snapshot)));
        ranks.add(rank("Dragonstone", ranks, dragonstone(snapshot)));
        ranks.add(rank("TzKal", ranks, tzkal(snapshot)));
        ranks.add(rank("Onyx", ranks, onyx(snapshot)));
        ranks.add(rank("Kitten", ranks, kitten(snapshot)));
        ranks.add(rank("Maxed", ranks, maxed(snapshot)));
        ranks.add(rank("Completionism", ranks, completionism()));
        ranks.add(rank("Zenyte", ranks, zenyte()));
        return ranks;
    }

    public List<String> getRankNames()
    {
        return RANK_NAMES;
    }

    public RankQualificationResult evaluateTarget(
        VerificationSnapshot snapshot,
        String rankName)
    {
        if ("Opal".equals(rankName))
        {
            return opalService.evaluate(snapshot);
        }

        List<RequirementResult> requirements = requirementsFor(snapshot, rankName);
        requirements.add(0, manual(
            "Previous rank verified in ClanHQ",
            "ClanHQ will validate this when the review ticket is created"));
        return new RankQualificationResult(rankName, requirements);
    }

    private List<RequirementResult> requirementsFor(
        VerificationSnapshot snapshot,
        String rankName)
    {
        switch (rankName)
        {
            case "Jade": return jade(snapshot);
            case "Topaz": return topaz(snapshot);
            case "Sapphire": return sapphire(snapshot);
            case "Emerald": return emerald(snapshot);
            case "Ruby": return ruby(snapshot);
            case "Diamond": return diamond(snapshot);
            case "Dragon": return dragon(snapshot);
            case "Dragonstone": return dragonstone(snapshot);
            case "TzKal": return tzkal(snapshot);
            case "Onyx": return onyx(snapshot);
            case "Kitten": return kitten(snapshot);
            case "Maxed": return maxed(snapshot);
            case "Completionism": return completionism();
            case "Zenyte": return zenyte();
            default: throw new IllegalArgumentException(
                "Unknown Iron Drop rank: " + rankName);
        }
    }

    private List<RequirementResult> jade(VerificationSnapshot s)
    {
        return list(level(s, 1950), skill("78 Herblore", s.getHerbloreLevel(), 78),
            item(s, "Mage Arena II cape", "imbued god cape", "imbued saradomin cape",
                "imbued zamorak cape", "imbued guthix cape"),
            item(s, "Fire cape", "fire cape", "infernal cape"),
            item(s, "Ava's Assembler", "ava's assembler", "dizana's quiver"),
            item(s, "Fighter's torso or better", "fighter torso", "torva platebody",
                "bandos chestplate", "oathplate chest"),
            distinctItems(s, "5 Moons uniques", 5, "moon helm", "moon chestplate", "moon tassets",
                "blue moon spear", "dual macuahuitl", "eclipse atlatl"),
            count(s, "Elite Void (ranged)", 4, "void ranger helm", "elite void top",
                "elite void robe", "void knight gloves"));
    }

    private List<RequirementResult> topaz(VerificationSnapshot s)
    {
        return list(level(s, 2000), item(s, "Trident of the seas or better", "trident",
                "sanguinesti", "tumeken's shadow"),
            prayer("Mystic Vigour unlocked", s.isMysticVigourActive()),
            prayer("Deadeye unlocked", s.isDeadeyeActive()),
            count(s, "2 Zenyte jewellery", 2, "amulet of torture", "necklace of anguish",
                "tormented bracelet", "ring of suffering"),
            state("All hard achievement diaries", s.getDiaryProgress().areAllHardComplete(),
                s.getDiaryProgress().getHardCompleted() + "/12 regions complete"));
    }

    private List<RequirementResult> sapphire(VerificationSnapshot s)
    {
        return list(level(s, 2050), item(s, "Blood moon tassets or better", "blood moon tassets",
                "bandos tassets", "torva platelegs", "oathplate legs"),
            item(s, "Ghommal's hilt 3+", "ghommal's hilt 3", "ghommal's hilt 4",
                "ghommal's hilt 5", "ghommal's hilt 6"),
            count(s, "2 of 3 Zulrah uniques", 2, "serpentine helm", "magic fang", "tanzanite fang"),
            item(s, "Bow of faerdhinen", "bow of faerdhinen"),
            count(s, "Crystal armour set", 3, "crystal helm", "crystal body", "crystal legs"));
    }

    private List<RequirementResult> emerald(VerificationSnapshot s)
    {
        return list(level(s, 2100), item(s, "Occult necklace", "occult necklace"),
            item(s, "Primordial boots or Avernic treads", "primordial boots", "avernic treads"),
            count(s, "All Zenyte jewellery", 4, "amulet of torture", "necklace of anguish",
                "tormented bracelet", "ring of suffering"),
            item(s, "BGS, DWH, or Elder maul", "bandos godsword", "dragon warhammer", "elder maul"),
            count(s, "All Dagannoth rings", 4, "berserker ring", "archers ring", "seers ring", "warrior ring"));
    }

    private List<RequirementResult> ruby(VerificationSnapshot s)
    {
        return list(level(s, 2150), item(s, "Quest cape", "quest point cape"),
            prayer("Dexterous prayer scroll (Rigour unlocked)", s.isRigourActive()),
            item(s, "Osmumten's fang", "osmumten's fang"),
            quantity(s, "2 Tormented synapses", 2, "tormented synapse"),
            item(s, "Burning claws or Dragon claws", "burning claws", "dragon claws"));
    }

    private List<RequirementResult> diamond(VerificationSnapshot s)
    {
        return list(level(s, 2175), manual("4 Chambers of Xeric uniques", "Collection-log evidence required"),
            manual("3 Tombs of Amascut uniques", "Collection-log evidence required"),
            item(s, "Assembler kit", "masori assembler", "max cape"),
            item(s, "Voidwaker", "voidwaker"),
            manual("Rite of Vile Transference", "Prayer unlock detection pending"));
    }

    private List<RequirementResult> dragon(VerificationSnapshot s)
    {
        return list(level(s, 2200), manual("Maxed POH", "House inspection required"),
            item(s, "Ghommal's hilt 4+", "ghommal's hilt 4", "ghommal's hilt 5", "ghommal's hilt 6"),
            item(s, "Dragon hunter lance", "dragon hunter lance"), item(s, "Venator bow", "venator bow"),
            manual("All Cerberus boots or upgraded Avernic treads", "Exact set verification pending"));
    }

    private List<RequirementResult> dragonstone(VerificationSnapshot s)
    {
        return list(level(s, 2250), item(s, "Amulet of rancour", "amulet of rancour"),
            count(s, "2 Ancient rings", 2, "ultor ring", "bellator ring", "magus ring", "venator ring"),
            count(s, "2 of 3 Virtus pieces or better", 2, "virtus mask", "virtus robe top",
                "virtus robe bottom", "ancestral hat", "ancestral robe top", "ancestral robe bottom"));
    }

    private List<RequirementResult> tzkal(VerificationSnapshot s)
    {
        return list(level(s, 2300), item(s, "Infernal cape", "infernal cape"),
            item(s, "Dizana's quiver", "dizana's quiver"),
            state("Achievement Diary Cape", s.getDiaryProgress().areAllEliteComplete(),
                s.getDiaryProgress().getEliteCompleted() + "/12 elite diaries complete"),
            count(s, "All four completed godswords", 4, "armadyl godsword", "bandos godsword",
                "saradomin godsword", "zamorak godsword"));
    }

    private List<RequirementResult> onyx(VerificationSnapshot s)
    {
        return list(level(s, 2325), item(s, "Blessed Dizana's quiver", "blessed dizana's quiver"),
            manual("2 Theatre of Blood uniques (including Avernic)", "Collection-log evidence required"),
            manual("1 Nex unique", "Collection-log evidence required"),
            manual("All Doom uniques", "Collection-log evidence required"));
    }

    private List<RequirementResult> kitten(VerificationSnapshot s)
    {
        return list(level(s, 2350), manual("7 COX / 5 TOA / 3 TOB uniques", "Collection-log evidence required"),
            count(s, "3 Ancient rings", 3, "ultor ring", "bellator ring", "magus ring", "venator ring"),
            manual("750 collection-log slots", "Collection-log evidence required"),
            item(s, "Ghommal's hilt 5+", "ghommal's hilt 5", "ghommal's hilt 6"));
    }

    private List<RequirementResult> maxed(VerificationSnapshot s)
    {
        return list(item(s, "Max cape", "max cape"), raidKillCount(s, 1000),
            count(s, "2 mega weapons", 2, "twisted bow", "scythe of vitur", "tumeken's shadow"),
            item(s, "Soulreaper axe", "soulreaper axe"),
            armourSet(s, "Full Ancestral", "ancestral robes set",
                new String[] {"ancestral hat"},
                new String[] {"ancestral robe top"},
                new String[] {"ancestral robe bottom"}),
            armourSet(s, "Full fortified Masori", "masori armour set (f)",
                new String[] {"masori mask (f)"},
                new String[] {"masori body (f)"},
                new String[] {"masori chaps (f)"}),
            oathplateOrTorva(s),
            item(s, "Maxed Avernic treads", "avernic treads (max)"));
    }

    private List<RequirementResult> completionism()
    {
        return list(manual("Completionism collection log and cosmetics", "Collection-log/cosmetic evidence required"),
            manual("Maxed skiff and sloop", "Sailing vessel evidence required"));
    }

    private List<RequirementResult> zenyte()
    {
        return list(manual("Grandmaster Combat Achievements", "Combat-achievement evidence required"),
            manual("All raids green logged", "Collection-log evidence required"),
            manual("Hill giant club", "Item evidence will be added with collection-log capture"));
    }

    private RankQualificationResult rank(String name, List<RankQualificationResult> prior, List<RequirementResult> own)
    {
        boolean missing = prior.stream().anyMatch(RankQualificationResult::hasMissingEvidence);
        boolean manualReview = prior.stream().anyMatch(RankQualificationResult::requiresManualReview);
        RequirementResult previousRanks;
        if (missing)
        {
            previousRanks = new RequirementResult("Previous-rank evidence",
                RequirementStatus.MISSING, "A preceding rank has missing automated evidence");
        }
        else if (manualReview)
        {
            previousRanks = manual("Previous-rank evidence",
                "A preceding rank includes staff-review requirements");
        }
        else
        {
            previousRanks = state("Previous-rank evidence", true,
                "Automated evidence is complete");
        }
        own.add(0, previousRanks);
        return new RankQualificationResult(name, own);
    }

    private RequirementResult level(VerificationSnapshot s, int required) { return skill(required + " total level", s.getTotalLevel(), required); }
    private RequirementResult skill(String name, int actual, int required) { return state(name, actual >= required, actual + " / " + required); }
    private RequirementResult prayer(String name, boolean unlocked) { return unlocked ? state(name, true, "Unlock detected") : new RequirementResult(name, RequirementStatus.MISSING, "Not unlocked"); }
    private RequirementResult raidKillCount(VerificationSnapshot snapshot, int required)
    {
        if (!snapshot.getRaidKillCounts().isAvailable())
        {
            return manual(required + " combined raids KC",
                snapshot.getRaidKillCounts().getDetail());
        }
        int combined = snapshot.getRaidKillCounts().getCombined();
        return state(required + " combined raids KC", combined >= required,
            snapshot.getRaidKillCounts().toSummary());
    }
    private RequirementResult oathplateOrTorva(VerificationSnapshot snapshot)
    {
        boolean oathplate = hasCompleteSet(snapshot, "oathplate armour set",
            new String[] {"oathplate helm"},
            new String[] {"oathplate chest"},
            new String[] {"oathplate legs"});
        boolean torva = hasCompleteSet(snapshot, "torva armour set",
            new String[] {"torva full helm"},
            new String[] {"torva platebody"},
            new String[] {"torva platelegs"});
        return setEvidenceResult(snapshot, "Full Oathplate or Torva",
            oathplate || torva,
            oathplate ? "Complete Oathplate set found"
                : torva ? "Complete Torva set found" : "No complete set");
    }
    private RequirementResult armourSet(VerificationSnapshot snapshot,
        String name, String boxedSet, String[]... slots)
    {
        return setEvidenceResult(snapshot, name,
            hasCompleteSet(snapshot, boxedSet, slots), "Complete set");
    }
    private RequirementResult setEvidenceResult(VerificationSnapshot snapshot,
        String name, boolean passed, String detail)
    {
        if (passed)
        {
            return state(name, true, detail);
        }
        if (!snapshot.isBankEvidenceCaptured())
        {
            return manual(name, "Open the bank during capture");
        }
        return new RequirementResult(name, RequirementStatus.MISSING,
            detail.equals("Complete set") ? "Complete set not found" : detail);
    }
    private boolean hasCompleteSet(VerificationSnapshot snapshot,
        String boxedSet, String[]... slots)
    {
        if (snapshot.getItems().stream().anyMatch(item ->
            matchesUsable(item, boxedSet)))
        {
            return true;
        }
        return Arrays.stream(slots).allMatch(slot -> snapshot.getItems().stream()
            .anyMatch(item -> matchesUsable(item, slot)));
    }
    private static boolean matchesUsable(ObservedItem item, String... fragments)
    {
        return !item.getName().toLowerCase(Locale.ENGLISH).contains("damaged")
            && matches(item, fragments);
    }
    private RequirementResult item(VerificationSnapshot s, String name, String... fragments) { return matched(s, name, 1, false, fragments); }
    private RequirementResult count(VerificationSnapshot s, String name, int required, String... fragments) { return matched(s, name, required, true, fragments); }
    private RequirementResult quantity(VerificationSnapshot s, String name, int required, String... fragments)
    {
        int found = s.getItems().stream().filter(i -> matches(i, fragments))
            .mapToInt(ObservedItem::getQuantity).max().orElse(0);
        return evidenceResult(s, name, found >= required, found + " / " + required);
    }
    private RequirementResult distinctItems(VerificationSnapshot s, String name, int required, String... fragments)
    {
        long found = s.getItems().stream().filter(i -> matches(i, fragments))
            .map(ObservedItem::getItemId).distinct().count();
        return evidenceResult(s, name, found >= required, found + " / " + required);
    }
    private RequirementResult matched(VerificationSnapshot s, String name, int required, boolean distinct, String... fragments)
    {
        long found = distinct ? Arrays.stream(fragments).filter(f -> s.getItems().stream().anyMatch(i -> matches(i, f))).count()
            : s.getItems().stream().filter(i -> matches(i, fragments)).limit(1).count();
        return evidenceResult(s, name, found >= required, found + " / " + required);
    }
    private RequirementResult evidenceResult(VerificationSnapshot s, String name, boolean passed, String detail)
    {
        if (passed) return state(name, true, detail);
        return s.isBankEvidenceCaptured() ? new RequirementResult(name, RequirementStatus.MISSING, detail + " found")
            : manual(name, "Open the bank during capture (" + detail + " found so far)");
    }
    private static boolean matches(ObservedItem item, String... fragments)
    {
        String name = item.getName().toLowerCase(Locale.ENGLISH);
        return Arrays.stream(fragments).anyMatch(f -> name.contains(f.toLowerCase(Locale.ENGLISH)));
    }
    private RequirementResult state(String name, boolean passed, String detail) { return new RequirementResult(name, passed ? RequirementStatus.PASSED : RequirementStatus.MISSING, detail); }
    private RequirementResult manual(String name, String detail) { return new RequirementResult(name, RequirementStatus.UNVERIFIED, detail); }
    private List<RequirementResult> list(RequirementResult... results) { return new ArrayList<>(Arrays.asList(results)); }
}
