package com.clanhq.verifier.model;

import java.util.Objects;

public final class ObservedItem
{
    private final int itemId;
    private final String name;
    private final int quantity;
    private final EvidenceSource source;

    public ObservedItem(
        int itemId,
        String name,
        int quantity,
        EvidenceSource source)
    {
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.quantity = quantity;
        this.source = Objects.requireNonNull(source);
    }

    public int getItemId()
    {
        return itemId;
    }

    public String getName()
    {
        return name;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public EvidenceSource getSource()
    {
        return source;
    }
}
