package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class VerificationSnapshot
{
    private final String rsn;
    private final int totalLevel;
    private final int combatLevel;
    private final List<EquipmentItem> equipment;

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<EquipmentItem> equipment)
    {
        this.rsn = Objects.requireNonNull(rsn);
        this.totalLevel = totalLevel;
        this.combatLevel = combatLevel;
        this.equipment = Collections.unmodifiableList(
            new ArrayList<>(equipment));
    }

    public String getRsn()
    {
        return rsn;
    }

    public int getTotalLevel()
    {
        return totalLevel;
    }

    public int getCombatLevel()
    {
        return combatLevel;
    }

    public List<EquipmentItem> getEquipment()
    {
        return equipment;
    }

    public String toPreviewText()
    {
        StringBuilder preview = new StringBuilder();
        preview.append("RSN: ").append(rsn).append('\n');
        preview.append("Total level: ").append(totalLevel).append('\n');
        preview.append("Combat level: ").append(combatLevel).append('\n');
        preview.append("Equipped items:").append('\n');

        if (equipment.isEmpty())
        {
            preview.append("- None detected");
            return preview.toString();
        }

        for (EquipmentItem item : equipment)
        {
            preview.append("- ")
                .append(item.getName())
                .append(" (ID ")
                .append(item.getItemId())
                .append(")");

            if (item.getQuantity() > 1)
            {
                preview.append(" x").append(item.getQuantity());
            }

            preview.append('\n');
        }

        return preview.toString().trim();
    }
}
