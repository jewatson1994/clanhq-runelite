package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import java.util.Locale;
import net.runelite.api.Skill;

/** Resolves a daily skilling task to one RuneLite skill without substring collisions. */
final class DailyTaskSkillMatcher
{
    private DailyTaskSkillMatcher()
    {
    }

    static boolean matches(DailyTaskSummary task, String observedSkillName)
    {
        Skill expected = findTaskSkill(task);
        Skill observed = findBestSkill(observedSkillName);
        return expected != null && expected == observed;
    }

    static Skill findTaskSkill(DailyTaskSummary task)
    {
        if (task == null)
        {
            return null;
        }
        return findBestSkill(task.getName() + " " + task.getDescription());
    }

    private static Skill findBestSkill(String value)
    {
        String normalized = normalize(value);
        Skill best = null;
        int bestLength = -1;
        for (Skill skill : Skill.values())
        {
            String candidate = normalize(skill.getName());
            if (!candidate.isEmpty() && normalized.contains(candidate)
                && candidate.length() > bestLength)
            {
                best = skill;
                bestLength = candidate.length();
            }
        }
        return best;
    }

    private static String normalize(String value)
    {
        return value == null ? ""
            : value.replace('_', ' ').trim().toLowerCase(Locale.ENGLISH);
    }
}
