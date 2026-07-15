package com.clanhq.verifier.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PohEvidence
{
    public static final String SPIRITUAL_FAIRY = "Spiritual fairy ring";
    public static final String JEWELLERY_BOX = "Ornate jewellery box";
    public static final String OCCULT_ALTAR = "Occult altar";
    public static final String PORTAL_NEXUS = "Crystalline portal nexus";
    public static final String REJUVENATION_POOL = "Ornate rejuvenation pool";

    private final boolean ownerBuildMode;
    private final Set<String> facilities;

    public PohEvidence(boolean ownerBuildMode, Set<String> facilities)
    {
        this.ownerBuildMode = ownerBuildMode;
        this.facilities = Collections.unmodifiableSet(
            new LinkedHashSet<>(facilities));
    }

    public static PohEvidence notCaptured()
    {
        return new PohEvidence(false, Collections.emptySet());
    }

    public boolean isOwnerBuildMode() { return ownerBuildMode; }
    public Set<String> getFacilities() { return facilities; }

    public boolean isMaxed()
    {
        return ownerBuildMode
            && facilities.contains(SPIRITUAL_FAIRY)
            && facilities.contains(JEWELLERY_BOX)
            && facilities.contains(OCCULT_ALTAR)
            && facilities.contains(PORTAL_NEXUS)
            && facilities.contains(REJUVENATION_POOL);
    }

    public String toSummary()
    {
        return ownerBuildMode
            ? facilities.size() + "/5 required facilities found in owner build mode"
            : "Not captured in owner build mode";
    }
}
