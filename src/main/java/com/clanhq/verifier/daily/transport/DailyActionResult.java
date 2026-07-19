package com.clanhq.verifier.daily.transport;

public final class DailyActionResult
{
    private final boolean successful;
    private final String message;
    private final int rewardAmount;

    public DailyActionResult(boolean successful, String message,
        int rewardAmount)
    {
        this.successful = successful;
        this.message = message;
        this.rewardAmount = rewardAmount;
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public int getRewardAmount() { return rewardAmount; }
}
