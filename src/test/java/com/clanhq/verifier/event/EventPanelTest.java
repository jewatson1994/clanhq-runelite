package com.clanhq.verifier.event;

import com.clanhq.verifier.event.model.ClanEventsSnapshot;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EventPanelTest
{
    @Test
    public void rendersTbdAndDatedEventsTogether() throws Exception
    {
        ClanEventsSnapshot snapshot = ClanEventsSnapshot.fromJson(
            "{\"events\":[{\"event_id\":1,\"event_type\":\"BINGO\","
                + "\"name\":\"Postponed Bingo\",\"start_date\":null,"
                + "\"end_date\":null,\"status\":\"SCHEDULED\"},"
                + "{\"event_id\":2,\"event_type\":\"SKILL_OF_THE_WEEK\","
                + "\"name\":\"Skill of the Week\",\"start_date\":\"2026-10-09\","
                + "\"end_date\":\"2026-10-16\",\"status\":\"ACTIVE\"}]}");
        SwingUtilities.invokeAndWait(() ->
        {
            EventPanel panel = new EventPanel(() -> {}, () -> false, () -> false);
            panel.showEvents("Test Clan", snapshot.getEvents());
            List<String> labels = new ArrayList<>();
            collectLabels(panel, labels);
            assertTrue(labels.stream().anyMatch(text -> text.contains("Postponed Bingo")));
            assertTrue(labels.contains("Scheduled"));
            assertTrue(labels.stream().anyMatch(text -> text.contains("<b>Start</b><br>TBD")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("<b>End</b><br>TBD")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("Oct 9, 2026")));
            assertTrue(labels.stream().anyMatch(text -> text.contains("2 current events.")));
            assertEquals(2, snapshot.getEvents().size());
        });
    }

    private static void collectLabels(Container parent, List<String> labels)
    {
        for (Component child : parent.getComponents())
        {
            if (child instanceof JLabel)
            {
                labels.add(((JLabel) child).getText());
            }
            if (child instanceof Container)
            {
                collectLabels((Container) child, labels);
            }
        }
    }
}
