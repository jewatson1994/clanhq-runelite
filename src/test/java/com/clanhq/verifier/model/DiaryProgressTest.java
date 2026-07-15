package com.clanhq.verifier.model;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiaryProgressTest
{
    @Test
    public void requiresEveryRegionForTierCompletion()
    {
        DiaryProgress incomplete = new DiaryProgress(11, 11, 12);
        DiaryProgress complete = new DiaryProgress(12, 12, 12);

        assertFalse(incomplete.areAllHardComplete());
        assertFalse(incomplete.areAllEliteComplete());
        assertTrue(complete.areAllHardComplete());
        assertTrue(complete.areAllEliteComplete());
    }
}
