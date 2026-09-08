package com.clanhq.verifier.task;

public enum DropTier
{
    EASY, MEDIUM, HARD;

    public static DropTier from(String value)
    {
        if (value == null)
        {
            return EASY;
        }
        try
        {
            return valueOf(value.trim().toUpperCase());
        }
        catch (IllegalArgumentException exception)
        {
            return EASY;
        }
    }
}
