package com.clanhq.verifier.model;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressionEvaluationTest
{
    @Test
    public void stopsAtTheFirstRankThatIsNotFullyVerified()
    {
        ProgressionEvaluation evaluation = new ProgressionEvaluation(
            Arrays.asList(
                rank("Opal", RequirementStatus.PASSED),
                rank("Jade", RequirementStatus.PASSED),
                rank("Topaz", RequirementStatus.MISSING),
                rank("Sapphire", RequirementStatus.PASSED)));

        assertEquals("Jade", evaluation.getHighestVerifiedRankName());
        assertEquals("Topaz", evaluation.getNextRank().get().getRankName());
        assertEquals(1, evaluation.getOutstandingRequirements().size());
    }

    @Test
    public void separatesMissingEvidenceFromStaffReview()
    {
        RankQualificationResult opal = new RankQualificationResult("Opal",
            Arrays.asList(
                requirement("Bank item", RequirementStatus.MISSING),
                requirement("Bank capture", RequirementStatus.NOT_CAPTURED),
                requirement("Collection Log", RequirementStatus.UNVERIFIED),
                requirement("Total level", RequirementStatus.PASSED)));

        ProgressionEvaluation evaluation = new ProgressionEvaluation(
            Collections.singletonList(opal));
        String text = evaluation.toProgressText();

        assertTrue(text.contains("Next Rank requires:\n1. Bank item"));
        assertTrue(text.contains("2. Bank capture"));
        assertTrue(text.contains("Staff Review:\n1. Collection Log"));
        assertEquals(1, evaluation.count(RequirementStatus.PASSED));
    }

    @Test
    public void reportsCompletionWhenEveryRankPasses()
    {
        ProgressionEvaluation evaluation = new ProgressionEvaluation(
            Arrays.asList(rank("Opal", RequirementStatus.PASSED),
                rank("Jade", RequirementStatus.PASSED)));

        assertEquals("Jade", evaluation.getHighestVerifiedRankName());
        assertFalse(evaluation.getNextRank().isPresent());
        assertTrue(evaluation.toProgressText()
            .contains("All configured ranks verified"));
    }

    private static RankQualificationResult rank(String name,
        RequirementStatus status)
    {
        return new RankQualificationResult(name,
            Collections.singletonList(requirement("Requirement", status)));
    }

    private static RequirementResult requirement(String name,
        RequirementStatus status)
    {
        return new RequirementResult(name, status, "Detail");
    }
}
