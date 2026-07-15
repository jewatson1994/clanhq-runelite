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

    @Test
    public void updatesPrayerEvidenceWithoutDiscardingOtherEvidence()
    {
        VerificationSnapshot original = new VerificationSnapshot(
            "Mr Dimples", 2325, 126, Collections.emptyList(), true,
            false, false, false, false, 99, new DiaryProgress(12, 12, 12));
        VerificationSnapshot prayerEvidence = new VerificationSnapshot(
            "Mr Dimples", 2325, 126, Collections.emptyList(), false,
            true, true, true, true, 99, new DiaryProgress(12, 12, 12));

        VerificationSnapshot updated = original.withPrayerEvidenceFrom(prayerEvidence);

        assertTrue(updated.isPietyActive());
        assertTrue(updated.isRigourActive());
        assertTrue(updated.isDeadeyeActive());
        assertTrue(updated.isMysticVigourActive());
        assertTrue(updated.isBankEvidenceCaptured());
    }

    @Test
    public void updatesItemEvidenceWithoutDiscardingOtherStages()
    {
        VerificationSnapshot original = new VerificationSnapshot(
            "Mr Dimples", 2325, 126, Collections.emptyList(), false,
            true, true, true, true, 99, new DiaryProgress(12, 12, 12))
            .withCollectionLogEvidence(CollectionLogEvidence.slotCount(1200))
            .withPohEvidence(new PohEvidence(true,
                new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    PohEvidence.SPIRITUAL_FAIRY, PohEvidence.JEWELLERY_BOX,
                    PohEvidence.OCCULT_ALTAR, PohEvidence.PORTAL_NEXUS,
                    PohEvidence.REJUVENATION_POOL))))
            .withBoatEvidence(new BoatEvidence(
                new java.util.LinkedHashSet<>(java.util.Arrays.asList(
                    "Skiff", "Sloop")),
                Collections.singletonList("Captured")));
        VerificationSnapshot itemEvidence = new VerificationSnapshot(
            "Mr Dimples", 2325, 126,
            Collections.singletonList(new ObservedItem(
                22322, "Avernic defender", 1, EvidenceSource.BANK)),
            true, false);

        VerificationSnapshot updated = original.withItemEvidenceFrom(itemEvidence);

        assertTrue(updated.isBankEvidenceCaptured());
        assertEquals(1, updated.getItems().size());
        assertTrue(updated.isPietyActive());
        assertTrue(updated.isRigourActive());
        assertEquals(Integer.valueOf(1200),
            updated.getCollectionLogEvidence().getObtainedSlotCount());
        assertTrue(updated.getPohEvidence().isMaxed());
        assertTrue(updated.getBoatEvidence().isCaptured());
    }
}
