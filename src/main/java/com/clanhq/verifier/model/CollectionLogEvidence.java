package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CollectionLogEvidence
{
    private final Map<String, CollectionLogPageEvidence> pages;
    private final String collectionRank;
    private final Integer obtainedSlotCount;

    public CollectionLogEvidence(Map<String, Map<String, Integer>> legacyPages)
    {
        this(legacyPages, null);
    }

    public CollectionLogEvidence(Map<String, Map<String, Integer>> legacyPages,
        String collectionRank)
    {
        Map<String, CollectionLogPageEvidence> converted = new LinkedHashMap<>();
        legacyPages.forEach((title, items) -> converted.put(title,
            new CollectionLogPageEvidence(title, items, items.size(), items.size())));
        this.pages = Collections.unmodifiableMap(converted);
        this.collectionRank = collectionRank;
        this.obtainedSlotCount = null;
    }

    private CollectionLogEvidence(Map<String, CollectionLogPageEvidence> pages,
        String collectionRank, Integer obtainedSlotCount)
    {
        this.pages = Collections.unmodifiableMap(new LinkedHashMap<>(pages));
        this.collectionRank = collectionRank;
        this.obtainedSlotCount = obtainedSlotCount;
    }

    public static CollectionLogEvidence empty()
    {
        return new CollectionLogEvidence(Collections.emptyMap(), null, null);
    }

    public static CollectionLogEvidence page(String title,
        Map<String, Integer> acquiredItems, int acquiredSlots, int totalSlots)
    {
        CollectionLogPageEvidence page = new CollectionLogPageEvidence(
            title, acquiredItems, acquiredSlots, totalSlots);
        return new CollectionLogEvidence(Collections.singletonMap(title, page),
            null, null);
    }

    public static CollectionLogEvidence overview(String rank,
        Integer obtainedSlots)
    {
        return new CollectionLogEvidence(Collections.emptyMap(), rank,
            obtainedSlots);
    }

    public CollectionLogEvidence merge(CollectionLogEvidence other)
    {
        Map<String, CollectionLogPageEvidence> merged = new LinkedHashMap<>(pages);
        merged.putAll(other.pages);
        return new CollectionLogEvidence(merged,
            other.collectionRank == null ? collectionRank : other.collectionRank,
            other.obtainedSlotCount == null ? obtainedSlotCount
                : other.obtainedSlotCount);
    }

    public String getCollectionRank() { return collectionRank; }
    public Integer getObtainedSlotCount() { return obtainedSlotCount; }
    public boolean isCaptured()
    {
        return !pages.isEmpty() || collectionRank != null
            || obtainedSlotCount != null;
    }

    public boolean hasCollectionRank(String expectedRank)
    {
        return collectionRank != null
            && normalize(collectionRank).equals(normalize(expectedRank));
    }

    public boolean hasPage(String pageFragment)
    {
        return findPage(pageFragment) != null;
    }

    public boolean isPageGreenLogged(String pageFragment)
    {
        CollectionLogPageEvidence page = findPage(pageFragment);
        return page != null && page.isGreenLogged();
    }

    public CollectionLogPageEvidence getPage(String pageFragment)
    {
        return findPage(pageFragment);
    }

    public boolean hasAcquiredItem(String pageFragment, String itemFragment)
    {
        CollectionLogPageEvidence page = findPage(pageFragment);
        return page != null && hasName(page.getAcquiredItems(), itemFragment);
    }

    public boolean hasDoomUniques()
    {
        CollectionLogPageEvidence page = findPage("doom");
        return page != null
            && hasName(page.getAcquiredItems(), "cloth")
            && hasName(page.getAcquiredItems(), "boots")
            && hasName(page.getAcquiredItems(), "eye");
    }

    public String toSummary()
    {
        StringBuilder summary = new StringBuilder();
        if (collectionRank != null)
        {
            summary.append("Rank ").append(collectionRank);
        }
        if (obtainedSlotCount != null)
        {
            appendSeparator(summary);
            summary.append(obtainedSlotCount).append(" slots");
        }
        for (CollectionLogPageEvidence page : pages.values())
        {
            appendSeparator(summary);
            summary.append(page.toSummary());
        }
        return summary.length() == 0 ? "No Collection Log evidence" : summary.toString();
    }

    private CollectionLogPageEvidence findPage(String fragment)
    {
        String expected = normalize(fragment);
        return pages.entrySet().stream()
            .filter(entry -> normalize(entry.getKey()).contains(expected))
            .map(Map.Entry::getValue)
            .findFirst().orElse(null);
    }

    private static boolean hasName(Map<String, Integer> items, String fragment)
    {
        String expected = normalize(fragment);
        return items.entrySet().stream().anyMatch(entry -> entry.getValue() > 0
            && normalize(entry.getKey()).contains(expected));
    }

    private static void appendSeparator(StringBuilder summary)
    {
        if (summary.length() > 0)
        {
            summary.append("; ");
        }
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ENGLISH);
    }
}
