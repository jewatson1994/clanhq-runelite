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
    public void recognizesDoomUniquesAcrossCapturedPageEvidence()
    {
        Map<String, Integer> items = new LinkedHashMap<>();
        items.put("Doom cloth", 1);
        items.put("Doom boots", 1);
        items.put("Eye of Doom", 1);
        CollectionLogEvidence evidence = new CollectionLogEvidence(
            Collections.singletonMap("Doom of Mokhaiotl", items));

        assertTrue(evidence.hasDoomUniques());
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
