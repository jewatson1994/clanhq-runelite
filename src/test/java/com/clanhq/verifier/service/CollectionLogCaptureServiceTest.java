package com.clanhq.verifier.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionLogCaptureServiceTest
{
    @Test
    public void parsesObtainedSlotsFromOverviewProgress()
    {
        assertEquals(Integer.valueOf(754),
            CollectionLogCaptureService.parseObtainedSlots(
                "Collection Log Dragon 754 / 1,600"));
        assertEquals(Integer.valueOf(1_002),
            CollectionLogCaptureService.parseObtainedSlots(
                "Progress 1,002 of 1,600"));
    }

    @Test
    public void returnsNullWhenOverviewHasNoSlotProgress()
    {
        assertNull(CollectionLogCaptureService.parseObtainedSlots(
            "Collection Log Dragon"));
    }

    @Test
    public void excludesRaidKcShroudsFromGreenLogCounts()
    {
        assertTrue(CollectionLogCaptureService.isExcludedGreenLogSlot(
            "Icthlarin's shroud (tier 5)"));
        assertTrue(CollectionLogCaptureService.isExcludedGreenLogSlot(
            "Sinhaza shroud tier 5"));
        assertTrue(CollectionLogCaptureService.isExcludedGreenLogSlot(
            "Xeric's champion"));
        assertFalse(CollectionLogCaptureService.isExcludedGreenLogSlot(
            "Masori crafting kit"));
    }

    @Test
    public void recognizesTheYamaCollectionLogPage()
    {
        assertEquals("Yama", CollectionLogCaptureService.knownPageTitle(
            "Collection Log - Yama 3/5"));
    }
}
