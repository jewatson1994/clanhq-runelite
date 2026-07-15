package com.clanhq.verifier;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public final class ClanHQVerifierPluginTest
{
    private ClanHQVerifierPluginTest()
    {
    }

    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(ClanHQVerifierPlugin.class);
        RuneLite.main(args);
    }
}
