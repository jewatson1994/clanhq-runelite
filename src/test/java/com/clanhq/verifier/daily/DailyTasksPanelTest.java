package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DailyTasksPanelTest
{
    private static String formatted(String value)
    {
        return DateTimeFormatter.ofPattern("MMM d, h:mm a")
            .withZone(ZoneId.systemDefault()).format(Instant.parse(value));
    }

    @Test
    public void showsHourlyResetSeparatelyFromRushEnd()
    {
        for (String type : new String[]{"DROP_RUSH", "DRIP_RUSH"})
        {
            DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
                "{\"reset_at\":\"2026-09-08T09:00:00Z\",\"tasks\":[],\"context\":{"
                + "\"type\":\"" + type + "\",\"ends_at\":\"2026-09-08T09:00:00+00:00\","
                + "\"rotation_ends_at\":\"2026-09-07T15:00:00+00:00\"}}");
            assertEquals(Instant.parse("2026-09-07T15:00:00Z"),
                snapshot.getContext().getRotationEndsAt());
            assertEquals("<html>Tasks reset: " + formatted("2026-09-07T15:00:00Z")
                + "<br>Rush ends: " + formatted("2026-09-08T09:00:00Z") + "</html>",
                DailyTasksPanel.resetText(snapshot));
        }
    }

    @Test
    public void regularDailiesKeepDailyResetLabel()
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-09-08T09:00:00Z\",\"tasks\":[]}");
        assertEquals("Resets at: " + formatted("2026-09-08T09:00:00Z"),
            DailyTasksPanel.resetText(snapshot));
    }
}
