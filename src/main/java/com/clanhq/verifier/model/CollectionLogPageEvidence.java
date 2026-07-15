package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CollectionLogPageEvidence
{
    private final String pageTitle;
    private final Map<String, Integer> acquiredItems;
    private final int acquiredSlotCount;
    private final int totalSlotCount;

    public CollectionLogPageEvidence(String pageTitle,
        Map<String, Integer> acquiredItems, int acquiredSlotCount,
        int totalSlotCount)
    {
        this.pageTitle = Objects.requireNonNull(pageTitle);
        this.acquiredItems = Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(acquiredItems)));
        if (acquiredSlotCount < 0 || totalSlotCount < acquiredSlotCount)
        {
            throw new IllegalArgumentException("Invalid Collection Log slot counts.");
        }
        this.acquiredSlotCount = acquiredSlotCount;
        this.totalSlotCount = totalSlotCount;
    }

    public String getPageTitle() { return pageTitle; }
    public Map<String, Integer> getAcquiredItems() { return acquiredItems; }
    public int getAcquiredSlotCount() { return acquiredSlotCount; }
    public int getTotalSlotCount() { return totalSlotCount; }

    public boolean isGreenLogged()
    {
        return totalSlotCount > 0 && acquiredSlotCount == totalSlotCount;
    }

    public String toSummary()
    {
        return pageTitle + " " + acquiredSlotCount + "/" + totalSlotCount;
    }
}
