package com.clanhq.verifier.service;

import java.util.Arrays;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoatCaptureServiceTest
{
    @Test
    public void identifiesBoatTypesFromSelectionPanelSizes()
    {
        Set<String> skiff = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Boat Customisation", "Boat Size: Small (2x5)"));
        Set<String> sloop = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Boat Customisation", "Boat Size: Medium (3x10)"));

        assertTrue(skiff.contains("Skiff"));
        assertFalse(skiff.contains("Sloop"));
        assertTrue(sloop.contains("Sloop"));
        assertFalse(sloop.contains("Skiff"));
    }

    @Test
    public void stillIdentifiesExplicitBoatNames()
    {
        Set<String> types = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Skiff", "Sloop"));

        assertTrue(types.contains("Skiff"));
        assertTrue(types.contains("Sloop"));
    }
}
