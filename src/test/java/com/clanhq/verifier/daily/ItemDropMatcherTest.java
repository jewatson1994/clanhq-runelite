package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.loot.ObservedDrop;
import java.time.Instant;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.client.game.ItemStack;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ItemDropMatcherTest
{
    @Test
    public void onlyMatchingAssignmentIdsReceiveQuantityIncludingNotedItems()
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-09-08T09:00:00Z\",\"tasks\":["
            + task("a", "Ensouled ogre head", 1001, 1) + ","
            + task("b", "Mithril longsword", 1002, 1) + ","
            + task("c", "Dragon bones", 1003, 10) + "]}");

        ObservedDrop oneNotedDragonBone = new ObservedDrop(
            "Tester", "PVM", "Boss", Collections.singletonList(
                new ItemStack(2003, 1)), Instant.parse("2026-09-07T10:00:00Z"));
        Map<String, ItemDropMatcher.Match> first = ItemDropMatcher.findMatches(
            snapshot, oneNotedDragonBone, itemId -> itemId == 2003 ? 1003 : itemId);
        assertFalse(first.containsKey("a"));
        assertFalse(first.containsKey("b"));
        assertEquals(1, first.get("c").getQuantity());

        ObservedDrop threeDragonBones = new ObservedDrop(
            "Tester", "PVM", "Boss", Collections.singletonList(
                new ItemStack(1003, 3)), Instant.parse("2026-09-07T10:01:00Z"));
        Map<String, ItemDropMatcher.Match> second = ItemDropMatcher.findMatches(
            snapshot, threeDragonBones, itemId -> itemId);
        assertFalse(second.containsKey("a"));
        assertFalse(second.containsKey("b"));
        assertEquals(3, second.get("c").getQuantity());

        ObservedDrop ogreHead = new ObservedDrop(
            "Tester", "PVM", "Boss", Collections.singletonList(
                new ItemStack(1001, 1)), Instant.parse("2026-09-07T10:02:00Z"));
        Map<String, ItemDropMatcher.Match> reverse = ItemDropMatcher.findMatches(
            snapshot, ogreHead, itemId -> itemId);
        assertTrue(reverse.containsKey("a"));
        assertEquals(1, reverse.get("a").getQuantity());
        assertFalse(reverse.containsKey("b"));
        assertFalse(reverse.containsKey("c"));
    }

    @Test
    public void sidebarUpdatesTheAssignmentCardInsteadOfTheFirstDropCard() throws Exception
    {
        DailyTasksSnapshot snapshot = DailyTasksSnapshot.fromJson(
            "{\"reset_at\":\"2026-09-08T09:00:00Z\",\"tasks\":["
            + task("a", "Ensouled ogre head", 1001, 1) + ","
            + task("b", "Mithril longsword", 1002, 1) + ","
            + task("c", "Dragon bones", 1003, 10) + "]}");
        DailyTasksPanel panel = new DailyTasksPanel(() -> { }, ignored -> { }, null);
        panel.showTasks(snapshot, "");

        panel.updateLiveProgress("c", 1);
        assertEquals(0, cardProgress(panel, "a"));
        assertEquals(0, cardProgress(panel, "b"));
        assertEquals(1, cardProgress(panel, "c"));

        panel.updateLiveProgress("c", 4);
        panel.updateLiveProgress("a", 1);
        assertEquals(1, cardProgress(panel, "a"));
        assertEquals(0, cardProgress(panel, "b"));
        assertEquals(4, cardProgress(panel, "c"));
    }

    private static int cardProgress(DailyTasksPanel panel, String assignmentId)
        throws Exception
    {
        Field cardsField = DailyTasksPanel.class.getDeclaredField("cards");
        cardsField.setAccessible(true);
        for (Object card : (List<?>) cardsField.get(panel))
        {
            Field id = card.getClass().getDeclaredField("assignmentId");
            id.setAccessible(true);
            if (assignmentId.equals(id.get(card)))
            {
                Field progress = card.getClass().getDeclaredField("currentProgress");
                progress.setAccessible(true);
                return progress.getInt(card);
            }
        }
        throw new AssertionError("Assignment card not found: " + assignmentId);
    }

    private static String task(String id, String name, int itemId, int target)
    {
        return "{\"id\":\"" + id + "\",\"category\":\"DROP\","
            + "\"name\":\"" + name + "\",\"description\":\"Collect\","
            + "\"target\":" + target + ",\"progress\":0,\"reward\":25,"
            + "\"verification\":{\"type\":\"ITEM_DROP\",\"item_id\":" + itemId + "}}"
            ;
    }
}
