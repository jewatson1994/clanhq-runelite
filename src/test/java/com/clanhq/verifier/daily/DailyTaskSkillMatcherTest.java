package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import net.runelite.api.Skill;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DailyTaskSkillMatcherTest
{
    @Test
    public void runecraftingDoesNotMatchCrafting()
    {
        DailyTaskSummary task = task("Runecrafting Training",
            "Gain 25,000 Runecrafting experience.");

        assertEquals(Skill.RUNECRAFT,
            DailyTaskSkillMatcher.findTaskSkill(task));
        assertTrue(DailyTaskSkillMatcher.matches(task, "Runecraft"));
        assertFalse(DailyTaskSkillMatcher.matches(task, "Crafting"));
    }

    @Test
    public void craftingStillMatchesCraftingOnly()
    {
        DailyTaskSummary task = task("Crafting Training",
            "Gain 50,000 Crafting experience.");

        assertEquals(Skill.CRAFTING,
            DailyTaskSkillMatcher.findTaskSkill(task));
        assertTrue(DailyTaskSkillMatcher.matches(task, "Crafting"));
        assertFalse(DailyTaskSkillMatcher.matches(task, "Runecraft"));
    }

    private static DailyTaskSummary task(String name, String description)
    {
        return new DailyTaskSummary("SKILLING", name, description,
            25_000, 0, 50, false, 0, null);
    }
}
