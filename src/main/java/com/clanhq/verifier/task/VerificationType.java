package com.clanhq.verifier.task;

/** Safe, fixed observation capabilities understood by the RuneLite client. */
public enum VerificationType
{
    SKILL_XP,
    NPC_KILL,
    ITEM_DROP,
    CLUE_COMPLETE,
    MINIGAME_SCORE,
    UNKNOWN;

    public static VerificationType from(String value)
    {
        if (value == null)
        {
            return UNKNOWN;
        }
        try
        {
            return valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException exception)
        {
            return UNKNOWN;
        }
    }
}
