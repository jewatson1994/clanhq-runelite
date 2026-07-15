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
                new EquipmentItem(22322, "Avernic defender", 1)));

        String preview = snapshot.toPreviewText();

        assertTrue(preview.contains("RSN: Mr Dimples"));
        assertTrue(preview.contains("Total level: 2325"));
        assertTrue(preview.contains("Combat level: 126"));
        assertTrue(preview.contains("Avernic defender (ID 22322)"));
        assertEquals(1, snapshot.getEquipment().size());
    }
}
