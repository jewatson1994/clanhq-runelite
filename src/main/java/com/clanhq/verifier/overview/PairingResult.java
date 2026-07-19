package com.clanhq.verifier.overview;

public final class PairingResult
{
    private final boolean successful;
    private final String message;
    private final int rewardAmount;

    PairingResult(boolean successful, String message, int rewardAmount)
    {
        this.successful = successful;
        this.message = message;
        this.rewardAmount = rewardAmount;
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public int getRewardAmount() { return rewardAmount; }
}
