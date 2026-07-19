package com.clanhq.verifier.daily.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DailyTasksSnapshotTest
{
    @Test
    public void parsesServerOwnedDailyTaskDefinitions()
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-07-19T09:00:00Z\",\"balance\":125,\"tasks\":[{"
                + "\"category\":\"PVM\",\"name\":\"Zulrah Hunt\","
                + "\"description\":\"Defeat Zulrah 10 times.\","
                + "\"target\":10,\"progress\":4,\"reward\":50,"
                + "\"completed\":false,"
                + "\"awarded\":0,\"placement\":null}]}"
        );

        assertEquals(1, snapshot.getTasks().size());
        assertEquals("Zulrah Hunt", snapshot.getTasks().get(0).getName());
        assertEquals(50, snapshot.getTasks().get(0).getReward());
        assertEquals(4, snapshot.getTasks().get(0).getProgress());
        assertEquals(125, snapshot.getBalance());
        assertFalse(snapshot.getTasks().get(0).isCompleted());
    }
}
