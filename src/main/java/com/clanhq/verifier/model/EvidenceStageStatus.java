package com.clanhq.verifier.model;

public enum EvidenceStageStatus
{
    NOT_CAPTURED("○ Not captured"),
    CAPTURING("… Capturing"),
    CAPTURED("✓ Captured"),
    MANUAL_REVIEW("! Manual review"),
    FAILED("✕ Capture failed");

    private final String displayText;

    EvidenceStageStatus(String displayText)
    {
        this.displayText = displayText;
    }

    public String getDisplayText()
    {
        return displayText;
    }

    public boolean isSubmissionReady()
    {
        return this == CAPTURED || this == MANUAL_REVIEW;
    }
}
