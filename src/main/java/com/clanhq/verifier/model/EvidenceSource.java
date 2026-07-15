package com.clanhq.verifier.model;

public enum EvidenceSource
{
    EQUIPMENT("Equipped items"),
    INVENTORY("Inventory items"),
    BANK("Bank items");

    private final String displayName;

    EvidenceSource(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
