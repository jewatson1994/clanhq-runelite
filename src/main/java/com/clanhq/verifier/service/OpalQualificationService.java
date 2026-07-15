package com.clanhq.verifier.service;

import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.rules.OpalItemRequirements;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

public final class OpalQualificationService
{
    private static final int REQUIRED_TOTAL_LEVEL = 1900;
    private static final int REQUIRED_COMBAT_LEVEL = 90;

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
            OpalItemRequirements.hiltIds()));
        results.add(itemRequirement(
            snapshot,
            "Barrows gloves",
            OpalItemRequirements.barrowsGlovesIds()));
        results.add(pietyRequirement(snapshot));
        results.add(itemRequirement(
            snapshot,
            "Warped sceptre or better",
            OpalItemRequirements.poweredStaffIds()));
        results.add(itemRequirement(
            snapshot,
            "Zombie axe or better",
            OpalItemRequirements.meleeWeaponIds()));
        results.add(itemRequirement(
            snapshot,
            "Dragon defender or better",
            OpalItemRequirements.defenderIds()));

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
}
