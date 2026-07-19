package com.clanhq.verifier;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class PluginReadinessTest
{
    @Test
    public void screenshotsRequireExplicitOptIn()
    {
        ClanHQVerifierConfig config = new ClanHQVerifierConfig() { };
        assertFalse(config.bingoScreenshotsEnabled());
    }
}
