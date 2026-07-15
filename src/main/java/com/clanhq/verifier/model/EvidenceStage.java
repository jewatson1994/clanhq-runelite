package com.clanhq.verifier.model;

public enum EvidenceStage
{
    CHARACTER("Character"),
    PRAYERS("Prayers"),
    GEAR("Gear"),
    RAID_KC("Raid KC"),
    COLLECTION_LOG("Collection Log"),
    BOAT("Boat"),
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
