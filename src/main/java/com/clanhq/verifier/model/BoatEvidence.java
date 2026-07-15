package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BoatEvidence
{
    private final Set<String> boatTypes;
    private final List<BoatConfiguration> configurations;
    private final List<String> visibleDetails;

    public BoatEvidence(Set<String> boatTypes, List<String> visibleDetails)
    {
        this(boatTypes, Collections.emptyList(), visibleDetails);
    }

    public BoatEvidence(Set<String> boatTypes,
        List<BoatConfiguration> configurations, List<String> visibleDetails)
    {
        Set<String> types = new LinkedHashSet<>(
            Objects.requireNonNull(boatTypes));
        Objects.requireNonNull(configurations).stream()
            .map(BoatConfiguration::getType).forEach(types::add);
        this.boatTypes = Collections.unmodifiableSet(types);
        this.configurations = Collections.unmodifiableList(
            new ArrayList<>(configurations));
        this.visibleDetails = Collections.unmodifiableList(
            new ArrayList<>(Objects.requireNonNull(visibleDetails)));
    }

    public static BoatEvidence notCaptured()
    {
        return new BoatEvidence(Collections.emptySet(), Collections.emptyList());
    }

    public Set<String> getBoatTypes()
    {
        return boatTypes;
    }

    public List<BoatConfiguration> getConfigurations()
    {
        return configurations;
    }

    public List<String> getVisibleDetails()
    {
        return visibleDetails;
    }

    public boolean isCaptured()
    {
        return !configurations.isEmpty() || !visibleDetails.isEmpty();
    }

    public boolean hasSkiffAndSloop()
    {
        return boatTypes.contains("Skiff") && boatTypes.contains("Sloop");
    }

    public boolean hasStructuredConfigurations()
    {
        return !configurations.isEmpty();
    }

    public boolean hasMaxedBoat(String type)
    {
        return configurations.stream()
            .anyMatch(boat -> boat.isType(type) && boat.isMaxedCore());
    }

    public BoatEvidence merge(BoatEvidence other)
    {
        Set<String> types = new LinkedHashSet<>(boatTypes);
        types.addAll(other.boatTypes);
        Map<Integer, BoatConfiguration> boats = new LinkedHashMap<>();
        configurations.forEach(boat -> boats.put(boat.getSlot(), boat));
        other.configurations.forEach(boat -> boats.put(boat.getSlot(), boat));
        Set<String> details = new LinkedHashSet<>(visibleDetails);
        details.addAll(other.visibleDetails);
        return new BoatEvidence(types, new ArrayList<>(boats.values()),
            new ArrayList<>(details));
    }

    public String toSummary()
    {
        if (!isCaptured())
        {
            return "Not captured";
        }
        if (!configurations.isEmpty())
        {
            return configurations.stream().map(BoatConfiguration::toSummary)
                .collect(java.util.stream.Collectors.joining("; "));
        }
        String boats = boatTypes.isEmpty() ? "boat type not identified"
            : String.join(", ", boatTypes);
        return boats + "; " + visibleDetails.size() + " visible details";
    }
}
