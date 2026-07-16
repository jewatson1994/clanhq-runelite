package com.clanhq.verifier;

import java.util.Arrays;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClanHQPluginMetadataTest
{
    @Test
    public void requiresRuneLiteLootTracker()
    {
        PluginDependency[] dependencies = ClanHQVerifierPlugin.class
            .getAnnotationsByType(PluginDependency.class);

        assertTrue(Arrays.stream(dependencies)
            .anyMatch(dependency ->
                dependency.value().equals(LootTrackerPlugin.class)));
    }
}
