package com.clanhq.verifier.model;

import java.util.Objects;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ObservedItem
{
    private final int itemId;
    private final String name;
    private final int quantity;
    private final EvidenceSource source;
    private final Map<String, Object> equipment;

    public ObservedItem(
        int itemId,
        String name,
        int quantity,
        EvidenceSource source)
    {
        this(itemId, name, quantity, source, Collections.emptyMap());
    }

    public ObservedItem(
        int itemId,
        String name,
        int quantity,
        EvidenceSource source,
        Map<String, Object> equipment)
    {
        this.itemId = itemId;
        this.name = Objects.requireNonNull(name);
        this.quantity = quantity;
        this.source = Objects.requireNonNull(source);
        this.equipment = Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(equipment)));
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

    public Map<String, Object> getEquipment()
    {
        return equipment;
    }
}
