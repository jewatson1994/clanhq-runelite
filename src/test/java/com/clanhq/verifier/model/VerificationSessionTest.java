package com.clanhq.verifier.model;

import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VerificationSessionTest
{
    @Test
    public void becomesReadyWhenEveryRequiredSourceIsCapturedOrReviewable()
    {
        VerificationSession session = new VerificationSession(
            EnumSet.of(EvidenceStage.CHARACTER, EvidenceStage.GEAR,
                EvidenceStage.POH));
        session.bindRsn("Mr Dimples");
        session.setStatus(EvidenceStage.CHARACTER, EvidenceStageStatus.CAPTURED);
        session.setStatus(EvidenceStage.GEAR, EvidenceStageStatus.CAPTURED);

        assertFalse(session.isReadyForSubmission());

        session.setStatus(EvidenceStage.POH, EvidenceStageStatus.MANUAL_REVIEW);
        assertTrue(session.isReadyForSubmission());
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsEvidenceFromAnotherCharacter()
    {
        VerificationSession session = new VerificationSession(
            EnumSet.of(EvidenceStage.CHARACTER));
        session.bindRsn("Mr Dimples");
        session.bindRsn("Not Mr Dimples");
    }
}
