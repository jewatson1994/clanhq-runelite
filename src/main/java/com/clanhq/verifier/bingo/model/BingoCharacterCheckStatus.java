package com.clanhq.verifier.bingo.model;

public final class BingoCharacterCheckStatus
{
    private final String status;
    private final String nextPhase;
    private final int checkpointCount;
    private final String baselineCapturedAt;
    private final String finalCapturedAt;

    public BingoCharacterCheckStatus(String status, String nextPhase,
        int checkpointCount, String baselineCapturedAt, String finalCapturedAt)
    {
        this.status = status;
        this.nextPhase = nextPhase;
        this.checkpointCount = checkpointCount;
        this.baselineCapturedAt = baselineCapturedAt;
        this.finalCapturedAt = finalCapturedAt;
    }

    public static BingoCharacterCheckStatus empty()
    {
        return new BingoCharacterCheckStatus(
            "NOT_SUBMITTED", "BASELINE", 0, null, null);
    }

    public String getStatus() { return status; }
    public String getNextPhase() { return nextPhase; }
    public int getCheckpointCount() { return checkpointCount; }
    public String getBaselineCapturedAt() { return baselineCapturedAt; }
    public String getFinalCapturedAt() { return finalCapturedAt; }

    public String getDisplayStatus()
    {
        switch (status)
        {
            case "BASELINE_CAPTURED": return "Baseline captured";
            case "CHECKPOINT_CAPTURED":
                return "Checkpoint captured (" + checkpointCount + ")";
            case "FINALIZED": return "Finalized";
            default: return "Not submitted";
        }
    }

    public String getButtonLabel()
    {
        switch (nextPhase)
        {
            case "BASELINE": return "Submit Pre-Bingo Check";
            case "CHECKPOINT": return "Submit Checkpoint";
            case "FINAL": return "Submit Final Check";
            default: return "Character Submit";
        }
    }

    public boolean canSubmit()
    {
        return !"COMPLETE".equals(nextPhase) && !"CLOSED".equals(nextPhase);
    }
}
