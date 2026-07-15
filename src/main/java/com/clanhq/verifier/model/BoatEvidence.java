package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BoatEvidence
{
    private final Set<String> boatTypes;
    private final List<String> visibleDetails;

    public BoatEvidence(Set<String> boatTypes, List<String> visibleDetails)
    {
        this.boatTypes = Collections.unmodifiableSet(
            new LinkedHashSet<>(Objects.requireNonNull(boatTypes)));
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

    public List<String> getVisibleDetails()
    {
        return visibleDetails;
    }

    public boolean isCaptured()
    {
        return !visibleDetails.isEmpty();
    }

    public boolean hasSkiffAndSloop()
    {
        return boatTypes.contains("Skiff") && boatTypes.contains("Sloop");
    }

    public BoatEvidence merge(BoatEvidence other)
    {
        Set<String> types = new LinkedHashSet<>(boatTypes);
        types.addAll(other.boatTypes);
        Set<String> details = new LinkedHashSet<>(visibleDetails);
        details.addAll(other.visibleDetails);
        return new BoatEvidence(types, new ArrayList<>(details));
    }

    public String toSummary()
    {
        if (!isCaptured())
        {
            return "Not captured";
        }
        String boats = boatTypes.isEmpty()
            ? "boat type not identified" : String.join(", ", boatTypes);
        return boats + "; " + visibleDetails.size() + " visible details";
    }
}
