package com.clanhq.verifier.model;

public enum EvidenceStage
{
    ACCOUNT("Account"),
    GEAR("Gear"),
    RAID_KC("Raid KC"),
    COLLECTION_LOG("Collection Log"),
    POH("POH");

    private final String displayName;

    EvidenceStage(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
