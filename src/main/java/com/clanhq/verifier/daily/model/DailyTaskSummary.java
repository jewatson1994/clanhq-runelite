package com.clanhq.verifier.daily.model;

import com.clanhq.verifier.task.VerificationType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DailyTaskSummary
{
    private final String id;
    private final String category;
    private final String name;
    private final String description;
    private final int target;
    private final int progress;
    private final int reward;
    private final boolean completed;
    private final int awarded;
    private final Integer placement;
    private final VerificationType verificationType;
    private final Integer verificationItemId;
    private final List<Integer> verificationItemIds;
    private final String tier;
    private final String taskType;

    public DailyTaskSummary(String category, String name, String description,
        int target, int progress, int reward, boolean completed, int awarded,
        Integer placement)
    {
        this(null, category, name, description, target, progress, reward,
            completed, awarded, placement, VerificationType.UNKNOWN, null, null, "EASY", "NORMAL");
    }

    public DailyTaskSummary(String category, String name, String description,
        int target, int progress, int reward, boolean completed, int awarded,
        Integer placement, VerificationType verificationType)
    {
        this(null, category, name, description, target, progress, reward,
            completed, awarded, placement, verificationType, null, null, "EASY", "NORMAL");
    }

    public DailyTaskSummary(String id, String category, String name,
        String description,
        int target, int progress, int reward, boolean completed, int awarded,
        Integer placement, VerificationType verificationType,
        Integer verificationItemId)
    {
        this(id, category, name, description, target, progress, reward, completed,
            awarded, placement, verificationType, verificationItemId, null, "EASY", "NORMAL");
    }

    public DailyTaskSummary(String id, String category, String name, String description,
        int target, int progress, int reward, boolean completed, int awarded,
        Integer placement, VerificationType verificationType, Integer verificationItemId,
        List<Integer> verificationItemIds, String tier, String taskType)
    {
        this.id = id;
        this.category = category;
        this.name = name;
        this.description = description;
        this.target = target;
        this.progress = progress;
        this.reward = reward;
        this.completed = completed;
        this.awarded = awarded;
        this.placement = placement;
        this.verificationType = verificationType;
        this.verificationItemId = verificationItemId;
        List<Integer> ids = verificationItemIds == null
            ? new ArrayList<>() : new ArrayList<>(verificationItemIds);
        if (verificationItemId != null && !ids.contains(verificationItemId))
        {
            ids.add(verificationItemId);
        }
        this.verificationItemIds = Collections.unmodifiableList(ids);
        this.tier = tier == null ? "EASY" : tier;
        this.taskType = taskType == null ? "NORMAL" : taskType;
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public int getReward() { return reward; }
    public boolean isCompleted() { return completed; }
    public int getAwarded() { return awarded; }
    public Integer getPlacement() { return placement; }
    public VerificationType getVerificationType() { return verificationType; }
    public Integer getVerificationItemId() { return verificationItemId; }
    public List<Integer> getVerificationItemIds() { return verificationItemIds; }
    public String getTier() { return tier; }
    public String getTaskType() { return taskType; }
    public boolean isDailyDrop() { return "DAILY_DROP".equalsIgnoreCase(taskType); }
}
