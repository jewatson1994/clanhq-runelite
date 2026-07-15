package com.clanhq.verifier.model;

public enum EvidenceStage
{
    CHARACTER("Character"),
    PRAYERS("Prayers"),
    GEAR("Gear"),
    RAID_KC("Raid KC"),
    COX_LOG("COX Collection Log"),
    TOB_LOG("TOB Collection Log"),
    TOA_LOG("TOA Collection Log"),
    DOOM_LOG("Doom Collection Log"),
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
