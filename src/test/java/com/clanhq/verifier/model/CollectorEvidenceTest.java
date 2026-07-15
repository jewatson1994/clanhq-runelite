package com.clanhq.verifier.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectorEvidenceTest
{
    @Test
    public void mergesBoatPanelsWithoutClaimingUpgradeVerification()
    {
        BoatEvidence skiff = new BoatEvidence(
            Collections.singleton("Skiff"), Arrays.asList("Skiff", "Hull level 3"));
        BoatEvidence sloop = new BoatEvidence(
            Collections.singleton("Sloop"), Arrays.asList("Sloop", "Hull level 3"));

        BoatEvidence combined = skiff.merge(sloop);

        assertTrue(combined.hasSkiffAndSloop());
        assertEquals(3, combined.getVisibleDetails().size());
        assertTrue(combined.toSummary().contains("Skiff, Sloop"));
    }

    @Test
    public void recognizesDoomUniquesAcrossCapturedPageEvidence()
    {
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("Doom cloth", 1);
        items.put("Doom boots", 1);
        items.put("Eye of Doom", 1);
        CollectionLogEvidence evidence = new CollectionLogEvidence(
            Collections.singletonMap("Doom of Mokhaiotl", items));

        assertTrue(evidence.hasDoomUniques());
        assertTrue(evidence.hasPage("doom"));
        assertTrue(evidence.hasAcquiredItem("Doom", "cloth"));
    }

    @Test
    public void mergesHeaderSlotCountWithCapturedPages()
    {
        CollectionLogEvidence count = CollectionLogEvidence.slotCount(1200);
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("Twisted bow", 1);
        CollectionLogEvidence page = new CollectionLogEvidence(
            Collections.singletonMap("Chambers of Xeric", items));

        CollectionLogEvidence combined = count.merge(page);

        assertEquals(Integer.valueOf(1200), combined.getObtainedSlotCount());
        assertTrue(combined.hasPage("Chambers"));
        assertTrue(combined.toSummary().contains("1200 slots"));
    }

    @Test
    public void retainsHeaderSlotsAndPageCompletion()
    {
        CollectionLogEvidence count = CollectionLogEvidence.slotCount(812);
        Map<String, Integer> acquired = new LinkedHashMap<>();
        acquired.put("Twisted bow", 1);
        CollectionLogEvidence greenPage = CollectionLogEvidence.page(
            "Chambers of Xeric", acquired, 12, 12);

        CollectionLogEvidence combined = count.merge(greenPage);

        assertEquals(Integer.valueOf(812), combined.getObtainedSlotCount());
        assertTrue(combined.isPageGreenLogged("chambers"));
        assertTrue(combined.toSummary().contains("12/12"));
    }

    @Test
    public void requiresAllFiveFacilitiesAndOwnerBuildMode()
    {
        PohEvidence evidence = new PohEvidence(true, new LinkedHashSet<>(Arrays.asList(
            PohEvidence.SPIRITUAL_FAIRY,
            PohEvidence.JEWELLERY_BOX,
            PohEvidence.OCCULT_ALTAR,
            PohEvidence.PORTAL_NEXUS,
            PohEvidence.REJUVENATION_POOL)));

        assertTrue(evidence.isMaxed());
        assertEquals("5/5 required facilities found in owner build mode",
            evidence.toSummary());
    }
}
