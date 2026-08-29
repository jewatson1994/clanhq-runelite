package com.clanhq.verifier.loot;

import java.time.Instant;
import java.util.Collection;
import net.runelite.client.game.ItemStack;

/** A single RuneLite loot-tracker notification shared by ClanHQ features. */
public final class ObservedDrop
{
    private final String player;
    private final String sourceType;
    private final String sourceName;
    private final Collection<ItemStack> items;
    private final Instant observedAt;

    public ObservedDrop(String player, String sourceType, String sourceName,
        Collection<ItemStack> items, Instant observedAt)
    {
        this.player = player;
        this.sourceType = sourceType;
        this.sourceName = sourceName;
        this.items = items;
        this.observedAt = observedAt;
    }

    public String getPlayer() { return player; }
    public String getSourceType() { return sourceType; }
    public String getSourceName() { return sourceName; }
    public Collection<ItemStack> getItems() { return items; }
    public Instant getObservedAt() { return observedAt; }
}
