package com.clanhq.verifier.model;

import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VerificationSnapshotTest
{
    @Test
    public void formatsTheExactCapturedEvidence()
    {
        VerificationSnapshot snapshot = new VerificationSnapshot(
            "Mr Dimples",
            2325,
            126,
            Collections.singletonList(
                new ObservedItem(
                    22322,
                    "Avernic defender",
                    1,
                    EvidenceSource.EQUIPMENT)),
            true,
            true);

        String preview = snapshot.toPreviewText();

        assertTrue(preview.contains("RSN: Mr Dimples"));
        assertTrue(preview.contains("Total level: 2325"));
        assertTrue(preview.contains("Combat level: 126"));
        assertTrue(preview.contains("Avernic defender (ID 22322)"));
        assertTrue(preview.contains("Bank evidence: Captured"));
        assertEquals(1, snapshot.getItems().size());
    }

    @Test
    public void addsRaidEvidenceWithoutChangingCharacterEvidence()
    {
        VerificationSnapshot original = new VerificationSnapshot(
            "Mr Dimples", 2325, 126, Collections.emptyList(), false, false);

        VerificationSnapshot updated = original.withRaidKillCounts(
            RaidKillCounts.available(100, 10, 20, 5, 30, 15));

        assertEquals("Mr Dimples", updated.getRsn());
        assertEquals(180, updated.getRaidKillCounts().getCombined());
        assertEquals(2325, updated.getTotalLevel());
    }
}
