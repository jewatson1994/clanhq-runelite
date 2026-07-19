package com.clanhq.verifier.daily.transport;

public final class DailyActionResult
{
    private final boolean successful;
    private final String message;
    private final int rewardAmount;
    private final String currencyName;
    private final String currencySymbol;

    public DailyActionResult(boolean successful, String message,
        int rewardAmount, String currencyName, String currencySymbol)
    {
        this.successful = successful;
        this.message = message;
        this.rewardAmount = rewardAmount;
        this.currencyName = currencyName;
        this.currencySymbol = currencySymbol;
    }

    public boolean isSuccessful() { return successful; }
    public String getMessage() { return message; }
    public int getRewardAmount() { return rewardAmount; }
    public String getCurrencyName() { return currencyName; }
    public String getCurrencySymbol() { return currencySymbol; }
}
