package com.clanhq.verifier.bingo.model;

import java.util.Objects;

public final class BingoItem
{
    private final int itemId;
    private final String name;
    private final int minimumQuantity;
    private final int points;
    private final String raid;

    public BingoItem(int itemId, String name, int minimumQuantity, int points)
    {
        this(itemId, name, minimumQuantity, points, null);
    }

    public BingoItem(int itemId, String name, int minimumQuantity, int points,
        String raid)
    {
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.minimumQuantity = minimumQuantity;
        this.points = points;
        this.raid = raid;
    }

    public int getItemId()
    {
        return itemId;
    }

    public String getName()
    {
        return name;
    }

    public int getMinimumQuantity()
    {
        return minimumQuantity;
    }

    public int getPoints()
    {
        return points;
    }

    /**
     * Tags this item as one of a raid's "uniques" (e.g. "COX", "TOB",
     * "TOA"), or {@code null} when it isn't tracked by the permanent,
     * clan-wide raid completion tracker. See
     * {@link com.clanhq.verifier.bingo.RaidProgressFeature}.
     */
    public String getRaid()
    {
        return raid;
    }
}
