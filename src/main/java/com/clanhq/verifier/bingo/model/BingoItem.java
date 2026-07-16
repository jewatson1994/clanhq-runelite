package com.clanhq.verifier.bingo.model;

import java.util.Objects;

public final class BingoItem
{
    private final int itemId;
    private final String name;
    private final int minimumQuantity;
    private final int points;

    public BingoItem(int itemId, String name, int minimumQuantity, int points)
    {
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.minimumQuantity = minimumQuantity;
        this.points = points;
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
}
