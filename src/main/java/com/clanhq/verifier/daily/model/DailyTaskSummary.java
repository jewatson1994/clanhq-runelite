package com.clanhq.verifier.daily.model;

public final class DailyTaskSummary
{
    private final String category;
    private final String name;
    private final String description;
    private final int target;
    private final int progress;
    private final int reward;
    private final boolean completed;
    private final int awarded;
    private final Integer placement;

    public DailyTaskSummary(String category, String name, String description,
        int target, int progress, int reward, boolean completed, int awarded,
        Integer placement)
    {
        this.category = category;
        this.name = name;
        this.description = description;
        this.target = target;
        this.progress = progress;
        this.reward = reward;
        this.completed = completed;
        this.awarded = awarded;
        this.placement = placement;
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public int getReward() { return reward; }
    public boolean isCompleted() { return completed; }
    public int getAwarded() { return awarded; }
    public Integer getPlacement() { return placement; }
}
