package com.clanhq.verifier;

import net.runelite.client.plugins.PluginDependency;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClanHQPluginMetadataTest
{
    /**
     * Previously asserted a {@code @PluginDependency(LootTrackerPlugin.class)}
     * on the plugin. RuneLite 1.13 rejects a {@code @PluginDependency} on a
     * plugin that exposes no injectable services - LootTrackerPlugin
     * doesn't, and ClanHQVerifierPlugin never injected it, only subscribed
     * to its LootReceived event (onLootReceived), which needs no formal
     * dependency declaration. Other plugin-hub authors hit the identical
     * "Plugin dependency LootTrackerPlugin does not expose any services"
     * load failure and the fix was dropping the annotation, so this test
     * now guards the opposite invariant: don't reintroduce it.
     */
    @Test
    public void doesNotDeclareAnyPluginDependency()
    {
        PluginDependency[] dependencies = ClanHQVerifierPlugin.class
            .getAnnotationsByType(PluginDependency.class);

        assertEquals(0, dependencies.length);
    }
}
