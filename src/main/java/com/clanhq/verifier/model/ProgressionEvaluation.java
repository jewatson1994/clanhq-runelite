package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Summarizes the highest contiguous rank proven by captured evidence. */
public final class ProgressionEvaluation
{
    private final List<RankQualificationResult> ranks;
    private final int nextRankIndex;

    public ProgressionEvaluation(List<RankQualificationResult> ranks)
    {
        this.ranks = Collections.unmodifiableList(new ArrayList<>(
            Objects.requireNonNull(ranks)));
        int index = 0;
        while (index < this.ranks.size()
            && this.ranks.get(index).isQualified())
        {
            index++;
        }
        nextRankIndex = index;
    }

    public String getHighestVerifiedRankName()
    {
        return nextRankIndex == 0
            ? "None" : ranks.get(nextRankIndex - 1).getRankName();
    }

    public Optional<RankQualificationResult> getNextRank()
    {
        return nextRankIndex >= ranks.size()
            ? Optional.empty() : Optional.of(ranks.get(nextRankIndex));
    }

    public List<RequirementResult> getNextRankRequirements()
    {
        return getNextRank().map(RankQualificationResult::getRequirements)
            .orElse(Collections.emptyList());
    }

    public List<RequirementResult> getOutstandingRequirements()
    {
        return getNextRankRequirements().stream()
            .filter(requirement -> requirement.getStatus()
                == RequirementStatus.MISSING
                || requirement.getStatus()
                == RequirementStatus.NOT_CAPTURED)
            .collect(Collectors.toList());
    }

    public List<RequirementResult> getStaffReviewRequirements()
    {
        return requirementsWithStatus(RequirementStatus.UNVERIFIED);
    }

    public long count(RequirementStatus status)
    {
        return getNextRankRequirements().stream()
            .filter(requirement -> requirement.getStatus() == status)
            .count();
    }

    public String toProgressText()
    {
        StringBuilder text = new StringBuilder();
        text.append("Highest verified rank: ")
            .append(getHighestVerifiedRankName()).append('\n');
        text.append("Next rank: ")
            .append(getNextRank().map(RankQualificationResult::getRankName)
                .orElse("All configured ranks verified"))
            .append("\n\nNext Rank requires:\n");
        appendNumbered(text, getOutstandingRequirements());
        text.append("\nStaff Review:\n");
        appendNumbered(text, getStaffReviewRequirements());
        return text.toString().trim();
    }

    private List<RequirementResult> requirementsWithStatus(
        RequirementStatus status)
    {
        return getNextRankRequirements().stream()
            .filter(requirement -> requirement.getStatus() == status)
            .collect(Collectors.toList());
    }

    private static void appendNumbered(StringBuilder text,
        List<RequirementResult> requirements)
    {
        if (requirements.isEmpty())
        {
            text.append("None\n");
            return;
        }
        for (int index = 0; index < requirements.size(); index++)
        {
            RequirementResult requirement = requirements.get(index);
            text.append(index + 1).append(". ")
                .append(requirement.getName()).append(" - ")
                .append(requirement.getDetail()).append('\n');
        }
    }
}
