package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CollectionLogEvidence
{
    private final Map<String, Map<String, Integer>> pages;
    private final String collectionRank;

    public CollectionLogEvidence(Map<String, Map<String, Integer>> pages)
    {
        this(pages, null);
    }

    public CollectionLogEvidence(Map<String, Map<String, Integer>> pages,
        String collectionRank)
    {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        pages.forEach((page, items) -> copy.put(page,
            Collections.unmodifiableMap(new LinkedHashMap<>(items))));
        this.pages = Collections.unmodifiableMap(copy);
        this.collectionRank = collectionRank;
    }

    public static CollectionLogEvidence empty()
    {
        return new CollectionLogEvidence(Collections.emptyMap());
    }

    public CollectionLogEvidence merge(CollectionLogEvidence other)
    {
        Map<String, Map<String, Integer>> merged = new LinkedHashMap<>(pages);
        merged.putAll(other.pages);
        return new CollectionLogEvidence(merged,
            other.collectionRank == null ? collectionRank : other.collectionRank);
    }

    public Map<String, Map<String, Integer>> getPages() { return pages; }
    public String getCollectionRank() { return collectionRank; }
    public boolean isCaptured() { return !pages.isEmpty() || collectionRank != null; }

    public boolean hasCollectionRank(String expectedRank)
    {
        return collectionRank != null
            && normalize(collectionRank).equals(normalize(expectedRank));
    }

    public boolean hasPage(String pageFragment)
    {
        String expected = normalize(pageFragment);
        return pages.keySet().stream()
            .anyMatch(page -> normalize(page).contains(expected));
    }

    public boolean hasAcquiredItem(String pageFragment, String itemFragment)
    {
        String expectedPage = normalize(pageFragment);
        return pages.entrySet().stream()
            .filter(entry -> normalize(entry.getKey()).contains(expectedPage))
            .anyMatch(entry -> hasName(entry.getValue(), itemFragment));
    }

    public boolean hasDoomUniques()
    {
        return pages.entrySet().stream()
            .filter(entry -> normalize(entry.getKey()).contains("doom"))
            .anyMatch(entry -> hasName(entry.getValue(), "cloth")
                && hasName(entry.getValue(), "boots")
                && hasName(entry.getValue(), "eye"));
    }

    public String toSummary()
    {
        String pageSummary = pages.isEmpty() ? "No pages captured"
            : String.join(", ", pages.keySet());
        return collectionRank == null ? pageSummary
            : "Rank " + collectionRank + "; " + pageSummary;
    }

    private static boolean hasName(Map<String, Integer> items, String fragment)
    {
        String expected = normalize(fragment);
        return items.entrySet().stream().anyMatch(entry -> entry.getValue() > 0
            && normalize(entry.getKey()).contains(expected));
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ENGLISH);
    }
}
