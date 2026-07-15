package com.clanhq.verifier.service;

import net.runelite.api.QuestState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LocalPlayerSnapshotServiceTest
{
    @Test
    public void verifiesPietyFromKingsRansomAndKnightWavesCompletion()
    {
        assertTrue(LocalPlayerSnapshotService.hasPietyUnlock(
            QuestState.FINISHED, 8));
        assertFalse(LocalPlayerSnapshotService.hasPietyUnlock(
            QuestState.FINISHED, 7));
        assertFalse(LocalPlayerSnapshotService.hasPietyUnlock(
            QuestState.IN_PROGRESS, 8));
    }
}
