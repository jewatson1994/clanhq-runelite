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

    public List<RequirementResult> getRequirements()
    {
        return requirements;
    }

    public String toChecklistText()
    {
        StringBuilder checklist = new StringBuilder();
        checklist.append(rankName).append(" qualification: ")
            .append(isQualified() ? "QUALIFIED" : "NOT YET VERIFIED")
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
}
