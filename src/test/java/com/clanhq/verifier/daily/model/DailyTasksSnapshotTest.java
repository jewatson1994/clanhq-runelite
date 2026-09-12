package com.clanhq.verifier.daily.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        assertNull(snapshot.getTasks().get(0).getTier());
    }

    @Test
    public void parsesFourthDailyDropWithoutGivingNormalTasksATier()
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-07-19T09:00:00Z\",\"tasks\":["
                + "{\"category\":\"SKILLING\",\"name\":\"Mining\",\"target\":1},"
                + "{\"category\":\"PVM\",\"name\":\"Boss\",\"target\":1},"
                + "{\"category\":\"ACTIVITIES\",\"name\":\"Games\",\"target\":1},"
                + "{\"category\":\"DROP\",\"task_type\":\"DAILY_DROP\","
                + "\"tier\":\"HARD\",\"name\":\"Dragon bones\",\"target\":10,"
                + "\"verification\":{\"type\":\"ITEM_DROP\",\"item_id\":1003}}]}"
        );

        assertEquals(4, snapshot.getTasks().size());
        assertNull(snapshot.getTasks().get(0).getTier());
        assertEquals("HARD", snapshot.getTasks().get(3).getTier());
        assertTrue(snapshot.getTasks().get(3).isDailyDrop());
    }
}
