package com.clanhq.verifier.service;

import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ItemID;

@SuppressWarnings("deprecation")
public final class OpalQualificationService
{
    private static final int REQUIRED_TOTAL_LEVEL = 1900;
    private static final int REQUIRED_COMBAT_LEVEL = 90;

    private static final Set<Integer> HILT_IDS = ids(
        ItemID.GHOMMALS_HILT_2,
        ItemID.GHOMMALS_HILT_3,
        ItemID.GHOMMALS_HILT_4,
        ItemID.GHOMMALS_HILT_5,
        ItemID.GHOMMALS_HILT_6);

    private static final Set<Integer> BARROWS_GLOVES_IDS = ids(
        ItemID.BARROWS_GLOVES,
        ItemID.BARROWS_GLOVES_WRAPPED);

    private static final Set<Integer> POWERED_STAFF_IDS = ids(
        ItemID.WARPED_SCEPTRE,
        ItemID.WARPED_SCEPTRE_UNCHARGED,
        ItemID.TRIDENT_OF_THE_SEAS,
        ItemID.TRIDENT_OF_THE_SEAS_FULL,
        ItemID.TRIDENT_OF_THE_SEAS_E,
        ItemID.TRIDENT_OF_THE_SWAMP,
        ItemID.TRIDENT_OF_THE_SWAMP_E,
        ItemID.SANGUINESTI_STAFF,
        ItemID.SANGUINESTI_STAFF_UNCHARGED,
        ItemID.HOLY_SANGUINESTI_STAFF,
        ItemID.HOLY_SANGUINESTI_STAFF_UNCHARGED,
        ItemID.TUMEKENS_SHADOW,
        ItemID.TUMEKENS_SHADOW_UNCHARGED);

    private static final Set<Integer> MELEE_WEAPON_IDS = ids(
        ItemID.ZOMBIE_AXE,
        ItemID.ABYSSAL_WHIP,
        ItemID.ABYSSAL_WHIP_OR,
        ItemID.ABYSSAL_TENTACLE,
        ItemID.ABYSSAL_TENTACLE_OR,
        ItemID.OSMUMTENS_FANG,
        ItemID.OSMUMTENS_FANG_OR,
        ItemID.GHRAZI_RAPIER,
        ItemID.HOLY_GHRAZI_RAPIER,
        ItemID.BLADE_OF_SAELDOR,
        ItemID.BLADE_OF_SAELDOR_INACTIVE,
        ItemID.BLADE_OF_SAELDOR_C,
        ItemID.SOULREAPER_AXE,
        ItemID.SOULREAPER_AXE_O,
        ItemID.SCYTHE_OF_VITUR,
        ItemID.SCYTHE_OF_VITUR_UNCHARGED,
        ItemID.HOLY_SCYTHE_OF_VITUR,
        ItemID.HOLY_SCYTHE_OF_VITUR_UNCHARGED,
        ItemID.SANGUINE_SCYTHE_OF_VITUR,
        ItemID.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED);

    private static final Set<Integer> DEFENDER_IDS = ids(
        ItemID.DRAGON_DEFENDER,
        ItemID.DRAGON_DEFENDER_T,
        ItemID.DRAGON_DEFENDER_L,
        ItemID.DRAGON_DEFENDER_LT,
        ItemID.AVERNIC_DEFENDER,
        ItemID.AVERNIC_DEFENDER_L,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_5,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_5_L,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_6,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_6_L);

    @Inject
    public OpalQualificationService()
    {
    }

    public RankQualificationResult evaluate(VerificationSnapshot snapshot)
    {
        List<RequirementResult> results = new ArrayList<>();
        results.add(levelRequirement(
            "1900 total level",
            snapshot.getTotalLevel(),
            REQUIRED_TOTAL_LEVEL));
        results.add(levelRequirement(
            "90+ combat",
            snapshot.getCombatLevel(),
            REQUIRED_COMBAT_LEVEL));
        results.add(itemRequirement(
            snapshot,
            "Ghommal's hilt 2+",
            HILT_IDS));
        results.add(itemRequirement(
            snapshot,
            "Barrows gloves",
            BARROWS_GLOVES_IDS));
        results.add(pietyRequirement(snapshot));
        results.add(itemRequirement(
            snapshot,
            "Warped sceptre or better",
            POWERED_STAFF_IDS));
        results.add(itemRequirement(
            snapshot,
            "Zombie axe or better",
            MELEE_WEAPON_IDS));
        results.add(itemRequirement(
            snapshot,
            "Dragon defender or better",
            DEFENDER_IDS));

        return new RankQualificationResult("Opal", results);
    }

    private static RequirementResult levelRequirement(
        String name,
        int actual,
        int required)
    {
        return new RequirementResult(
            name,
            actual >= required
                ? RequirementStatus.PASSED
                : RequirementStatus.MISSING,
            actual + " / " + required);
    }

    private static RequirementResult itemRequirement(
        VerificationSnapshot snapshot,
        String name,
        Set<Integer> acceptedIds)
    {
        Optional<ObservedItem> match = snapshot.findItem(acceptedIds);
        if (match.isPresent())
        {
            ObservedItem item = match.get();
            return new RequirementResult(
                name,
                RequirementStatus.PASSED,
                item.getName() + " in "
                    + item.getSource().getDisplayName().toLowerCase());
        }

        if (!snapshot.isBankEvidenceCaptured())
        {
            return new RequirementResult(
                name,
                RequirementStatus.UNVERIFIED,
                "Open the bank once, then capture again");
        }

        return new RequirementResult(
            name,
            RequirementStatus.MISSING,
            "Not found in equipment, inventory, or captured bank");
    }

    private static RequirementResult pietyRequirement(
        VerificationSnapshot snapshot)
    {
        if (snapshot.isPietyActive())
        {
            return new RequirementResult(
                "Piety unlocked",
                RequirementStatus.PASSED,
                "Piety is active");
        }

        return new RequirementResult(
            "Piety unlocked",
            RequirementStatus.UNVERIFIED,
            "Activate Piety, then capture again");
    }

    private static Set<Integer> ids(Integer... itemIds)
    {
        return new HashSet<>(Arrays.asList(itemIds));
    }
}
