package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class VerificationSnapshot
{
    private final String rsn;
    private final int totalLevel;
    private final int combatLevel;
    private final List<ObservedItem> items;
    private final boolean bankEvidenceCaptured;
    private final boolean pietyActive;

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<ObservedItem> items,
        boolean bankEvidenceCaptured,
        boolean pietyActive)
    {
        this.rsn = Objects.requireNonNull(rsn);
        this.totalLevel = totalLevel;
        this.combatLevel = combatLevel;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.bankEvidenceCaptured = bankEvidenceCaptured;
        this.pietyActive = pietyActive;
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

    public List<ObservedItem> getItems()
    {
        return items;
    }

    public boolean isBankEvidenceCaptured()
    {
        return bankEvidenceCaptured;
    }

    public boolean isPietyActive()
    {
        return pietyActive;
    }

    public Optional<ObservedItem> findItem(Set<Integer> acceptedItemIds)
    {
        return items.stream()
            .filter(item -> acceptedItemIds.contains(item.getItemId()))
            .findFirst();
    }

    public String toPreviewText()
    {
        StringBuilder preview = new StringBuilder();
        preview.append("RSN: ").append(rsn).append('\n');
        preview.append("Total level: ").append(totalLevel).append('\n');
        preview.append("Combat level: ").append(combatLevel).append('\n');
        preview.append("Bank evidence: ")
            .append(bankEvidenceCaptured ? "Captured" : "Not captured")
            .append('\n');
        preview.append("Piety active: ")
            .append(pietyActive ? "Yes" : "No")
            .append('\n');

        for (EvidenceSource source : EvidenceSource.values())
        {
            if (source == EvidenceSource.BANK && !bankEvidenceCaptured)
            {
                continue;
            }

            preview.append(source.getDisplayName()).append(':').append('\n');

            boolean found = false;
            for (ObservedItem item : items)
            {
                if (item.getSource() != source)
                {
                    continue;
                }

                found = true;
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

            if (!found)
            {
                preview.append("- None detected").append('\n');
            }
        }

        return preview.toString().trim();
    }
}
