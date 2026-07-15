package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CollectionLogEvidence
{
    private final Map<String, Map<String, Integer>> pages;

    public CollectionLogEvidence(Map<String, Map<String, Integer>> pages)
    {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        pages.forEach((page, items) -> copy.put(page,
            Collections.unmodifiableMap(new LinkedHashMap<>(items))));
        this.pages = Collections.unmodifiableMap(copy);
    }

    public static CollectionLogEvidence empty()
    {
        return new CollectionLogEvidence(Collections.emptyMap());
    }

    public CollectionLogEvidence merge(CollectionLogEvidence other)
    {
        Map<String, Map<String, Integer>> merged = new LinkedHashMap<>(pages);
        merged.putAll(other.pages);
        return new CollectionLogEvidence(merged);
    }

    public Map<String, Map<String, Integer>> getPages() { return pages; }
    public boolean isCaptured() { return !pages.isEmpty(); }

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
        return pages.isEmpty() ? "No pages captured"
            : String.join(", ", pages.keySet());
    }

    private static boolean hasName(Map<String, Integer> items, String fragment)
    {
        return items.entrySet().stream().anyMatch(entry -> entry.getValue() > 0
            && normalize(entry.getKey()).contains(fragment));
    }

    private static String normalize(String value)
    {
        return value.toLowerCase(Locale.ENGLISH);
    }
}
