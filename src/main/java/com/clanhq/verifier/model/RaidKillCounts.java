package com.clanhq.verifier.model;

public final class RaidKillCounts
{
    private final boolean available;
    private final String detail;
    private final int cox;
    private final int coxChallengeMode;
    private final int tob;
    private final int tobHardMode;
    private final int toa;
    private final int toaExpert;

    private RaidKillCounts(boolean available, String detail, int cox,
        int coxChallengeMode, int tob, int tobHardMode, int toa, int toaExpert)
    {
        this.available = available;
        this.detail = detail;
        this.cox = cox;
        this.coxChallengeMode = coxChallengeMode;
        this.tob = tob;
        this.tobHardMode = tobHardMode;
        this.toa = toa;
        this.toaExpert = toaExpert;
    }

    public static RaidKillCounts available(int cox, int coxChallengeMode,
        int tob, int tobHardMode, int toa, int toaExpert)
    {
        return new RaidKillCounts(true, "Hiscores loaded", cox,
            coxChallengeMode, tob, tobHardMode, toa, toaExpert);
    }

    public static RaidKillCounts unavailable(String detail)
    {
        return new RaidKillCounts(false, detail, 0, 0, 0, 0, 0, 0);
    }

    public boolean isAvailable() { return available; }
    public String getDetail() { return detail; }
    public int getCox() { return cox; }
    public int getCoxChallengeMode() { return coxChallengeMode; }
    public int getTob() { return tob; }
    public int getTobHardMode() { return tobHardMode; }
    public int getToa() { return toa; }
    public int getToaExpert() { return toaExpert; }

    public int getCombined()
    {
        return cox + coxChallengeMode + tob + tobHardMode + toa + toaExpert;
    }

    public String toSummary()
    {
        if (!available)
        {
            return detail;
        }
        return "COX " + cox + ", CM " + coxChallengeMode
            + ", TOB " + tob + ", HMT " + tobHardMode
            + ", TOA " + toa + ", Expert " + toaExpert
            + " (combined " + getCombined() + ")";
    }
}
