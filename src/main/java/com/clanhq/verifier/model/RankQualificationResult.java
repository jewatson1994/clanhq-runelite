package com.clanhq.verifier.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RankQualificationResult
{
    private final String rankName;
    private final List<RequirementResult> requirements;

    public RankQualificationResult(
        String rankName,
        List<RequirementResult> requirements)
    {
        this.rankName = Objects.requireNonNull(rankName);
        this.requirements = Collections.unmodifiableList(
            new ArrayList<>(requirements));
    }

    public boolean isQualified()
    {
        return requirements.stream()
            .allMatch(result -> result.getStatus() == RequirementStatus.PASSED);
    }

    public boolean hasMissingEvidence()
    {
        return requirements.stream()
            .anyMatch(result -> result.getStatus() == RequirementStatus.MISSING);
    }

    public boolean requiresManualReview()
    {
        return requirements.stream()
            .anyMatch(result -> result.getStatus() == RequirementStatus.UNVERIFIED);
    }

    public List<RequirementResult> getRequirements()
    {
        return requirements;
    }

    public String getRankName()
    {
        return rankName;
    }

    public String toChecklistText()
    {
        StringBuilder checklist = new StringBuilder();
        checklist.append(rankName).append(" evidence: ")
            .append(evidenceSummary())
            .append('\n');

        for (RequirementResult requirement : requirements)
        {
            checklist.append(requirement.getStatus().getSymbol())
                .append(' ')
                .append(requirement.getName())
                .append(" — ")
                .append(requirement.getDetail())
                .append('\n');
        }

        return checklist.toString().trim();
    }

    private String evidenceSummary()
    {
        if (hasMissingEvidence())
        {
            return "MISSING AUTOMATED EVIDENCE";
        }
        if (requiresManualReview())
        {
            return "MANUAL REVIEW REQUIRED";
        }
        return "AUTOMATED CHECKS PASSED";
    }
}
