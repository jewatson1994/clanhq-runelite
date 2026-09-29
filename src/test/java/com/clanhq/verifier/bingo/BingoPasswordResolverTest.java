package com.clanhq.verifier.bingo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BingoPasswordResolverTest
{
    @Test
    public void manifestValueOverridesLocalValue()
    {
        assertEquals("clan-wide",
            BingoPasswordResolver.resolve("mine", "clan-wide"));
    }

    @Test
    public void blankManifestValueFallsBackToLocal()
    {
        assertEquals("mine", BingoPasswordResolver.resolve("mine", "   "));
    }

    @Test
    public void nullManifestValueFallsBackToLocal()
    {
        assertEquals("mine", BingoPasswordResolver.resolve("mine", null));
    }

    @Test
    public void bothMissingResolvesToEmptyString()
    {
        assertEquals("", BingoPasswordResolver.resolve(null, null));
        assertEquals("", BingoPasswordResolver.resolve("", "  "));
    }

    @Test
    public void manifestValueIsTrimmed()
    {
        assertEquals("clan-wide",
            BingoPasswordResolver.resolve("mine", "  clan-wide  "));
    }

    @Test
    public void localValueIsTrimmedWhenUsed()
    {
        assertEquals("mine", BingoPasswordResolver.resolve("  mine  ", null));
    }

    @Test
    public void nullLocalValueWithNoManifestResolvesToEmptyString()
    {
        assertEquals("", BingoPasswordResolver.resolve(null, ""));
    }
}
