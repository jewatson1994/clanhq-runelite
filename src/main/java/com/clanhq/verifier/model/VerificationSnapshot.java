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
    private final boolean rigourActive;
    private final boolean deadeyeActive;
    private final boolean mysticVigourActive;
    private final int herbloreLevel;
    private final DiaryProgress diaryProgress;
    private final RaidKillCounts raidKillCounts;
    private final int collectionLogSlots;

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<ObservedItem> items,
        boolean bankEvidenceCaptured,
        boolean pietyActive)
    {
        this(rsn, totalLevel, combatLevel, items, bankEvidenceCaptured,
            pietyActive, false, false, false, 1,
            new DiaryProgress(0, 0, 12),
            RaidKillCounts.unavailable("Not captured"), 0);
    }

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<ObservedItem> items,
        boolean bankEvidenceCaptured,
        boolean pietyActive,
        boolean rigourActive,
        boolean deadeyeActive,
        boolean mysticVigourActive,
        int herbloreLevel,
        DiaryProgress diaryProgress)
    {
        this(rsn, totalLevel, combatLevel, items, bankEvidenceCaptured,
            pietyActive, rigourActive, deadeyeActive, mysticVigourActive,
            herbloreLevel, diaryProgress,
            RaidKillCounts.unavailable("Not captured"), 0);
    }

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<ObservedItem> items,
        boolean bankEvidenceCaptured,
        boolean pietyActive,
        boolean rigourActive,
        boolean deadeyeActive,
        boolean mysticVigourActive,
        int herbloreLevel,
        DiaryProgress diaryProgress,
        RaidKillCounts raidKillCounts)
    {
        this(rsn, totalLevel, combatLevel, items, bankEvidenceCaptured,
            pietyActive, rigourActive, deadeyeActive, mysticVigourActive,
            herbloreLevel, diaryProgress, raidKillCounts, 0);
    }

    public VerificationSnapshot(
        String rsn,
        int totalLevel,
        int combatLevel,
        List<ObservedItem> items,
        boolean bankEvidenceCaptured,
        boolean pietyActive,
        boolean rigourActive,
        boolean deadeyeActive,
        boolean mysticVigourActive,
        int herbloreLevel,
        DiaryProgress diaryProgress,
        RaidKillCounts raidKillCounts,
        int collectionLogSlots)
    {
        this.rsn = Objects.requireNonNull(rsn);
        this.totalLevel = totalLevel;
        this.combatLevel = combatLevel;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.bankEvidenceCaptured = bankEvidenceCaptured;
        this.pietyActive = pietyActive;
        this.rigourActive = rigourActive;
        this.deadeyeActive = deadeyeActive;
        this.mysticVigourActive = mysticVigourActive;
        this.herbloreLevel = herbloreLevel;
        this.diaryProgress = Objects.requireNonNull(diaryProgress);
        this.raidKillCounts = Objects.requireNonNull(raidKillCounts);
        this.collectionLogSlots = collectionLogSlots;
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

    public boolean isRigourActive() { return rigourActive; }
    public boolean isDeadeyeActive() { return deadeyeActive; }
    public boolean isMysticVigourActive() { return mysticVigourActive; }
    public int getHerbloreLevel() { return herbloreLevel; }
    public DiaryProgress getDiaryProgress() { return diaryProgress; }
    public RaidKillCounts getRaidKillCounts() { return raidKillCounts; }
    public int getCollectionLogSlots() { return collectionLogSlots; }

    public VerificationSnapshot withRaidKillCounts(RaidKillCounts counts)
    {
        return new VerificationSnapshot(rsn, totalLevel, combatLevel, items,
            bankEvidenceCaptured, pietyActive, rigourActive, deadeyeActive,
            mysticVigourActive, herbloreLevel, diaryProgress, counts,
            collectionLogSlots);
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
        preview.append("Rigour unlocked: ")
            .append(rigourActive ? "Yes" : "No").append('\n');
        preview.append("Deadeye unlocked: ")
            .append(deadeyeActive ? "Yes" : "No").append('\n');
        preview.append("Mystic Vigour unlocked: ")
            .append(mysticVigourActive ? "Yes" : "No").append('\n');
        preview.append("Hard diaries: ")
            .append(diaryProgress.getHardCompleted()).append('/')
            .append(diaryProgress.getRegionCount()).append('\n');
        preview.append("Elite diaries: ")
            .append(diaryProgress.getEliteCompleted()).append('/')
            .append(diaryProgress.getRegionCount()).append('\n');
        preview.append("Raid KC: ")
            .append(raidKillCounts.toSummary()).append('\n');
        preview.append("Collection log slots: ")
            .append(collectionLogSlots).append('\n');

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
