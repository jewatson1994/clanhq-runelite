package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Field;
import javax.swing.JLabel;
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

    @Test
    public void rendersAllFourDailyCardsInTheSummary()
        throws Exception
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-09-08T09:00:00Z\",\"tasks\":["
                + task("1", "Mining", "SKILLING") + ","
                + task("2", "Boss", "PVM") + ","
                + task("3", "Games", "ACTIVITIES") + ","
                + "{\"id\":\"4\",\"category\":\"DROP\","
                + "\"task_type\":\"DAILY_DROP\",\"tier\":\"MEDIUM\","
                + "\"name\":\"Medium item\",\"description\":\"Collect 1\","
                + "\"target\":1,\"progress\":0,\"reward\":50,"
                + "\"verification\":{\"type\":\"ITEM_DROP\",\"item_id\":1001}}]}"
        );
        DailyTasksPanel panel = new DailyTasksPanel(() -> { }, ignored -> { }, null);
        panel.showTasks(snapshot, "");

        Field field = DailyTasksPanel.class.getDeclaredField("summaryLabel");
        field.setAccessible(true);
        assertEquals("<html><body style='width: 180px'>0 / 4 Complete</body></html>",
            ((JLabel) field.get(panel)).getText());
    }

    private static String task(String id, String name, String category)
    {
        return "{\"id\":\"" + id + "\",\"category\":\"" + category + "\","
            + "\"name\":\"" + name + "\",\"description\":\"Do it\","
            + "\"target\":1,\"progress\":0,\"reward\":50}";
    }
}
