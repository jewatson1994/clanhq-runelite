package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.loot.ObservedDrop;
import com.clanhq.verifier.task.VerificationType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import java.util.function.IntUnaryOperator;

/** The single ITEM_DROP matching implementation shared by panel and overlay. */
final class ItemDropMatcher
{
    static final class Match
    {
        private final int canonicalItemId;
        private final int quantity;

        Match(int canonicalItemId, int quantity)
        {
            this.canonicalItemId = canonicalItemId;
            this.quantity = quantity;
        }

        int getCanonicalItemId() { return canonicalItemId; }
        int getQuantity() { return quantity; }
    }

    private ItemDropMatcher() { }

    static Map<String, Match> findMatches(DailyTasksSnapshot snapshot,
        ObservedDrop drop, ItemManager itemManager)
    {
        return findMatches(snapshot, drop, itemManager == null
            ? itemId -> itemId : itemManager::canonicalize);
    }

    static Map<String, Match> findMatches(DailyTasksSnapshot snapshot,
        ObservedDrop drop, IntUnaryOperator canonicalizer)
    {
        if (snapshot == null || drop == null || drop.getItems() == null)
        {
            return Collections.emptyMap();
        }
        Map<String, Match> matches = new HashMap<>();
        for (ItemStack item : drop.getItems())
        {
            int canonicalItemId = canonicalizer.applyAsInt(item.getId());
            int quantity = Math.max(0, item.getQuantity());
            if (quantity <= 0)
            {
                continue;
            }
            for (DailyTaskSummary task : snapshot.getTasks())
            {
                if (task.getId() == null || task.getId().trim().isEmpty()
                    || task.getVerificationType() != VerificationType.ITEM_DROP
                    || !task.getVerificationItemIds().contains(canonicalItemId))
                {
                    continue;
                }
                Match previous = matches.get(task.getId());
                matches.put(task.getId(), new Match(canonicalItemId,
                    (previous == null ? 0 : previous.getQuantity()) + quantity));
            }
        }
        return matches;
    }
}
