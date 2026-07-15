package com.clanhq.verifier.model;

import java.util.Objects;

public final class EquipmentItem
{
    private final int itemId;
    private final String name;
    private final int quantity;

    public EquipmentItem(int itemId, String name, int quantity)
    {
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.quantity = quantity;
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
}
