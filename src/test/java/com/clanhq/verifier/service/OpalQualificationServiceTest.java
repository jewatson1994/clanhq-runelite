package com.clanhq.verifier.service;

import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.RankQualificationResult;
import com.clanhq.verifier.model.RequirementStatus;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.Arrays;
import java.util.Collections;
import net.runelite.api.ItemID;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class OpalQualificationServiceTest
{
    private final OpalQualificationService service =
        new OpalQualificationService();

    @Test
    public void qualifiesWhenEveryRequirementIsObserved()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples",
            2094,
            122,
            Arrays.asList(
                item(ItemID.GHOMMALS_HILT_3),
                item(ItemID.BARROWS_GLOVES),
                item(ItemID.TRIDENT_OF_THE_SEAS),
                item(ItemID.ABYSSAL_WHIP),
                item(ItemID.AVERNIC_DEFENDER)),
            true,
            true);

        assertTrue(service.evaluate(snapshot).isQualified());
    }

    @Test
    public void marksUnseenItemsNotCapturedUntilBankIsCaptured()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples",
            2094,
            122,
            Collections.emptyList(),
            false,
            false);

        RankQualificationResult result = service.evaluate(snapshot);

        assertFalse(result.isQualified());
        assertTrue(result.getRequirements().stream().anyMatch(requirement ->
            requirement.getStatus() == RequirementStatus.NOT_CAPTURED));
    }

    @Test
    public void marksUnseenItemsMissingAfterBankIsCaptured()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples",
            2094,
            122,
            Collections.emptyList(),
            true,
            true);

        RankQualificationResult result = service.evaluate(snapshot);

        assertFalse(result.isQualified());
        assertTrue(result.getRequirements().stream().anyMatch(requirement ->
            requirement.getStatus() == RequirementStatus.MISSING));
    }

    private static ObservedItem item(int itemId)
    {
        return new ObservedItem(
            itemId,
            "Test item " + itemId,
            1,
            EvidenceSource.BANK);
    }
}
