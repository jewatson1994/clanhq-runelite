package com.clanhq.verifier.model;

public final class DiaryProgress
{
    private final int hardCompleted;
    private final int eliteCompleted;
    private final int regionCount;

    public DiaryProgress(int hardCompleted, int eliteCompleted, int regionCount)
    {
        this.hardCompleted = hardCompleted;
        this.eliteCompleted = eliteCompleted;
        this.regionCount = regionCount;
    }

    public int getHardCompleted() { return hardCompleted; }
    public int getEliteCompleted() { return eliteCompleted; }
    public int getRegionCount() { return regionCount; }
    public boolean areAllHardComplete() { return hardCompleted == regionCount; }
    public boolean areAllEliteComplete() { return eliteCompleted == regionCount; }
}
