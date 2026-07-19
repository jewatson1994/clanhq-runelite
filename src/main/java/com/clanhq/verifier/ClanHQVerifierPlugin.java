package com.clanhq.verifier;

import com.clanhq.verifier.bingo.BingoFeature;
import com.clanhq.verifier.bingo.service.BingoScreenshotService;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.character.CharacterSyncApiClient;
import com.clanhq.verifier.character.CharacterSyncFeature;
import com.clanhq.verifier.daily.DailyTasksFeature;
import com.clanhq.verifier.daily.transport.DailyTasksApiClient;
import com.clanhq.verifier.event.EventFeature;
import com.clanhq.verifier.event.transport.EventApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.overview.IdentityApiClient;
import com.clanhq.verifier.overview.OverviewFeature;
import com.clanhq.verifier.service.ApiDestinationService;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;

@PluginDescriptor(
    name = "ClanHQ",
    description = "Clan tools for character sync, events, Bingo, and daily tasks",
    tags = {"clan", "events", "bingo", "daily", "verification"})
@PluginDependency(LootTrackerPlugin.class)
public final class ClanHQVerifierPlugin extends Plugin
{
    @Inject private ClientThread clientThread;
    @Inject private Client client;
    @Inject private OkHttpClient httpClient;
    @Inject private DrawManager drawManager;
    @Inject private ScheduledExecutorService executor;
    @Inject private ClientToolbar clientToolbar;
    @Inject private LocalPlayerSnapshotService snapshotService;
    @Inject private ApiDestinationService apiDestinationService;
    @Inject private ClanHQVerifierConfig config;
    @Inject private ConfigManager configManager;

    private ClanHQPanel shellPanel;
    private BingoFeature bingoFeature;
    private EventFeature eventFeature;
    private DailyTasksFeature dailyTasksFeature;
    private OverviewFeature overviewFeature;
    private List<ClanHQFeature> features = Collections.emptyList();
    private NavigationButton navigationButton;

    @Provides
    ClanHQVerifierConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(ClanHQVerifierConfig.class);
    }

    @Override
    protected void startUp()
    {
        rebuildFeatures();
    }

    @Override
    protected void shutDown()
    {
        disposeFeatures();
        shellPanel = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!ClanHQVerifierConfig.GROUP.equals(event.getGroup()))
        {
            return;
        }
        if ("bingoEnabled".equals(event.getKey())
            || "eventsEnabled".equals(event.getKey())
            || "dailyTasksEnabled".equals(event.getKey()))
        {
            SwingUtilities.invokeLater(this::rebuildFeatures);
            return;
        }
        SwingUtilities.invokeLater(() ->
        {
            if (overviewFeature != null) { overviewFeature.refresh(); }
            if (bingoFeature != null) { bingoFeature.refreshManifest(); }
            if (eventFeature != null) { eventFeature.refresh(); }
            if (dailyTasksFeature != null) { dailyTasksFeature.refresh(); }
        });
    }

    private void rebuildFeatures()
    {
        disposeFeatures();
        List<ClanHQFeature> enabled = new ArrayList<>();
        overviewFeature = new OverviewFeature(
            new IdentityApiClient(httpClient, config, apiDestinationService),
            config,
            configManager);
        enabled.add(overviewFeature);
        enabled.add(new CharacterSyncFeature(
            new CharacterSyncApiClient(
                httpClient, config, apiDestinationService),
            snapshotService,
            clientThread));
        if (config.eventsEnabled())
        {
            eventFeature = new EventFeature(new EventApiClient(
                httpClient, config, apiDestinationService), this::currentRsn);
            enabled.add(eventFeature);
        }
        if (config.bingoEnabled())
        {
            bingoFeature = new BingoFeature(
                new BingoApiClient(httpClient, config, apiDestinationService),
                new BingoScreenshotService(drawManager, executor),
                config::bingoScreenshotsEnabled,
                snapshotService,
                clientThread,
                new EventApiClient(
                    httpClient, config, apiDestinationService),
                this::currentRsn);
            enabled.add(bingoFeature);
        }
        if (config.dailyTasksEnabled())
        {
            dailyTasksFeature = new DailyTasksFeature(
                new DailyTasksApiClient(
                    httpClient, config, apiDestinationService),
                config);
            enabled.add(dailyTasksFeature);
        }
        features = enabled;
        shellPanel = new ClanHQPanel(features);
        features.forEach(ClanHQFeature::startUp);
        navigationButton = NavigationButton.builder()
            .tooltip("ClanHQ")
            .icon(createIcon())
            .panel(shellPanel)
            .build();
        clientToolbar.addNavigation(navigationButton);
    }

    private void disposeFeatures()
    {
        features.forEach(ClanHQFeature::shutDown);
        features = Collections.emptyList();
        overviewFeature = null;
        bingoFeature = null;
        eventFeature = null;
        dailyTasksFeature = null;
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
            navigationButton = null;
        }
    }

    @Subscribe
    public void onLootReceived(LootReceived event)
    {
        if (client.getLocalPlayer() == null) { return; }
        if (bingoFeature != null)
        {
            bingoFeature.onLoot(
                client.getLocalPlayer().getName(),
                event.getType().name(),
                event.getName(),
                event.getItems());
        }
        if (eventFeature != null) { eventFeature.onLoot(event.getName()); }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (eventFeature != null)
        {
            eventFeature.onSkillExperience(
                event.getSkill().getName(), event.getXp());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN) { return; }
        if (overviewFeature != null) { overviewFeature.refresh(); }
        if (eventFeature != null) { eventFeature.refresh(); }
        if (bingoFeature != null) { bingoFeature.refreshManifest(); }
    }

    private String currentRsn()
    {
        return client.getLocalPlayer() == null
            ? null : client.getLocalPlayer().getName();
    }

    private static BufferedImage createIcon()
    {
        BufferedImage icon = new BufferedImage(
            16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = icon.createGraphics();
        graphics.setColor(new Color(212, 175, 55));
        graphics.fillOval(1, 1, 14, 14);
        graphics.setColor(new Color(30, 33, 36));
        graphics.fillOval(4, 4, 8, 8);
        graphics.dispose();
        return icon;
    }
}
