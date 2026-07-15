package com.clanhq.verifier;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(ClanHQVerifierConfig.GROUP)
public interface ClanHQVerifierConfig extends Config
{
    String GROUP = "clanhqVerifier";

    @ConfigSection(
        name = "ClanHQ connection",
        description = "Where review evidence will be submitted",
        position = 0)
    String CONNECTION_SECTION = "connection";

    @ConfigItem(
        keyName = "apiBaseUrl",
        name = "ClanHQ API URL",
        description = "HTTPS destination supplied by your clan; localhost HTTP is allowed for development",
        section = CONNECTION_SECTION,
        position = 0)
    default String apiBaseUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "clanCode",
        name = "Clan code",
        description = "Optional community code supplied by your clan",
        section = CONNECTION_SECTION,
        position = 1)
    default String clanCode()
    {
        return "";
    }
}
