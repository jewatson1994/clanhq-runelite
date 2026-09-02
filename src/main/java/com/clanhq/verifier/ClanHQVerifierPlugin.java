package com.clanhq.verifier;

import com.clanhq.verifier.bingo.BingoFeature;
import com.clanhq.verifier.bingo.service.BingoScreenshotService;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.character.CharacterSyncApiClient;
import com.clanhq.verifier.character.CharacterSyncFeature;
import com.clanhq.verifier.daily.DailyTasksFeature;
import com.clanhq.verifier.daily.ActivityTelemetryDetector;
import com.clanhq.verifier.daily.ActivityTelemetryTestFeature;
import com.clanhq.verifier.daily.transport.DailyTasksApiClient;
import com.clanhq.verifier.event.EventFeature;
import com.clanhq.verifier.event.transport.EventApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.overview.IdentityApiClient;
import com.clanhq.verifier.overview.OverviewFeature;
import com.clanhq.verifier.service.ApiDestinationService;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.SubmissionConsentService;
import com.clanhq.verifier.loot.ObservedDrop;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.plugins.loottracker.LootTrackerPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;

@PluginDescriptor(
    name = "ClanHQ",
    description = "Clan tools for character sync, events, Bingo, and daily tasks",
    tags = {"clan", "events", "bingo", "daily", "verification"})
@PluginDependency(LootTrackerPlugin.class)
public final class ClanHQVerifierPlugin extends Plugin
{
    private static final int TRAWLER_BASELINE_DELAY_TICKS = 2;

    @Inject private ClientThread clientThread;
    @Inject @Named("developerMode") private boolean developerMode;
    @Inject private Client client;
    @Inject private OkHttpClient httpClient;
    @Inject private DrawManager drawManager;
    @Inject private ScheduledExecutorService executor;
    @Inject private ClientToolbar clientToolbar;
    @Inject private OverlayManager overlayManager;
    @Inject private LocalPlayerSnapshotService snapshotService;
    @Inject private ApiDestinationService apiDestinationService;
    @Inject private SkillIconManager skillIconManager;
    @Inject private ClanHQVerifierConfig config;
    @Inject private ConfigManager configManager;

    private ClanHQPanel shellPanel;
    private BingoFeature bingoFeature;
    private EventFeature eventFeature;
    private DailyTasksFeature dailyTasksFeature;
    private ActivityTelemetryDetector activityTelemetryDetector;
    private OverviewFeature overviewFeature;
    private volatile String loggedInRsn;
    private String lastActivityDialogueText;
    private boolean trawlerCompletionCounterReady;
    private int trawlerBaselineTicksRemaining;
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
            || "dailyTasksEnabled".equals(event.getKey())
            || "dailyTasksOverlay".equals(event.getKey()))
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
            configManager,
            () -> dailyTasksFeature == null ? null : dailyTasksFeature.getSnapshot(),
            () -> bingoFeature == null ? null : bingoFeature.getManifest(),
            () -> loggedInRsn);
        SubmissionConsentService submissionConsent =
            new SubmissionConsentService(config, apiDestinationService);
        enabled.add(overviewFeature);
        CharacterSyncFeature characterSyncFeature = new CharacterSyncFeature(
            new CharacterSyncApiClient(
                httpClient, config, apiDestinationService),
            snapshotService,
            clientThread,
            submissionConsent);
        enabled.add(characterSyncFeature);
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
                submissionConsent,
                new EventApiClient(
                    httpClient, config, apiDestinationService),
                this::currentRsn,
                () -> { if (overviewFeature != null) overviewFeature.refreshSummary(); });
            enabled.add(bingoFeature);
        }
        if (config.dailyTasksEnabled())
        {
            dailyTasksFeature = new DailyTasksFeature(
                new DailyTasksApiClient(
                    httpClient, config, apiDestinationService),
                config,
                configManager,
                skillIconManager,
                executor,
                () -> { if (overviewFeature != null) overviewFeature.refreshSummary(); });
            activityTelemetryDetector = new ActivityTelemetryDetector(
                dailyTasksFeature, this::currentRsn);
            resetTrawlerCompletionBaseline();
            clientThread.invoke(this::initializeActivityTelemetry);
            enabled.add(dailyTasksFeature);
            if (developerMode)
            {
                enabled.add(new ActivityTelemetryTestFeature(
                    activityTelemetryDetector));
            }
            if (config.dailyTasksOverlay())
            {
                overlayManager.add(dailyTasksFeature.getOverlay());
            }
        }
        features = enabled;
        shellPanel = new ClanHQPanel(enabled);
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
        if (dailyTasksFeature != null)
        {
            overlayManager.remove(dailyTasksFeature.getOverlay());
        }
        features.forEach(ClanHQFeature::shutDown);
        features = Collections.emptyList();
        overviewFeature = null;
        bingoFeature = null;
        eventFeature = null;
        dailyTasksFeature = null;
        activityTelemetryDetector = null;
        resetTrawlerCompletionBaseline();
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
        ObservedDrop observedDrop = new ObservedDrop(
            client.getLocalPlayer().getName(),
            event.getType().name(),
            event.getName(),
            event.getItems(),
            java.time.Instant.now());
        if (bingoFeature != null)
        {
            bingoFeature.onDrop(observedDrop);
        }
        if (eventFeature != null) { eventFeature.onLoot(event.getName()); }
        if (dailyTasksFeature != null)
        {
            dailyTasksFeature.observeDrop(observedDrop);
            dailyTasksFeature.observeLoot(event.getName());
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        if (eventFeature != null)
        {
            eventFeature.onSkillExperience(
                event.getSkill().getName(), event.getXp());
        }
        if (dailyTasksFeature != null)
        {
            dailyTasksFeature.observeSkillExperience(
                event.getSkill().getName(), event.getXp());
        }
        if (activityTelemetryDetector != null
            && event.getSkill() == Skill.AGILITY
            && client.getLocalPlayer() != null)
        {
            activityTelemetryDetector.onAgilityExperience(
                event.getXp(), client.getLocalPlayer().getWorldLocation());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            loggedInRsn = null;
            resetTrawlerCompletionBaseline();
            return;
        }
        loggedInRsn = currentRsn();
        resetTrawlerCompletionBaseline();
        initializeActivityTelemetry();
        if (overviewFeature != null) { overviewFeature.refresh(); }
        if (eventFeature != null) { eventFeature.refresh(); }
        if (bingoFeature != null) { bingoFeature.refreshManifest(); }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        if (activityTelemetryDetector != null
            && (event.getType() == ChatMessageType.GAMEMESSAGE
                || event.getType() == ChatMessageType.SPAM))
        {
            activityTelemetryDetector.onChatMessage(event.getMessage());
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (activityTelemetryDetector == null)
        {
            return;
        }
        if (event.getVarbitId() == VarbitID.HOSIDIUS_TITHE_SCORE)
        {
            activityTelemetryDetector.onTitheScoreChanged(event.getValue());
        }
        else if (event.getVarbitId()
            == VarbitID.VARLAMORE_WYRM_AGILITY_BASIC_PROGRESS)
        {
            activityTelemetryDetector.onWyrmBasicProgress(event.getValue());
        }
        else if (event.getVarbitId()
            == VarbitID.VARLAMORE_WYRM_AGILITY_ADVANCED_PROGRESS)
        {
            activityTelemetryDetector.onWyrmAdvancedProgress(event.getValue());
        }
        else if (event.getVarbitId()
            == VarbitID.COLLECTION_MINIGAMES_TRAWLER_COMPLETED)
        {
            if (trawlerCompletionCounterReady)
            {
                activityTelemetryDetector.onTrawlerGamesCompletedChanged(
                    event.getValue());
            }
        }
    }

    /**
     * LOGGED_IN can arrive before RuneLite has populated the local player.
     * Recheck on ticks and refresh Overview only when the character changes.
     */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        String current = currentRsn();
        if (!Objects.equals(current, loggedInRsn))
        {
            loggedInRsn = current;
            if (overviewFeature != null)
            {
                overviewFeature.refresh();
            }
        }
        initializeTrawlerCompletionBaseline();
        detectActivityDialogue();
    }

    private void resetTrawlerCompletionBaseline()
    {
        trawlerCompletionCounterReady = false;
        trawlerBaselineTicksRemaining = TRAWLER_BASELINE_DELAY_TICKS;
    }

    private void initializeTrawlerCompletionBaseline()
    {
        // Account varbits briefly read as zero while login data is loading.
        // Wait two ticks so the lifetime total becomes a baseline, not progress.
        if (activityTelemetryDetector == null
            || trawlerCompletionCounterReady
            || --trawlerBaselineTicksRemaining > 0)
        {
            return;
        }
        activityTelemetryDetector.initializeTrawlerCompletionCounter(
            client.getVarbitValue(
                VarbitID.COLLECTION_MINIGAMES_TRAWLER_COMPLETED));
        trawlerCompletionCounterReady = true;
    }

    private void detectActivityDialogue()
    {
        if (activityTelemetryDetector == null)
        {
            lastActivityDialogueText = null;
            return;
        }
        Widget dialogueText = client.getWidget(WidgetInfo.DIALOG_NPC_TEXT);
        if (dialogueText == null || dialogueText.isHidden()
            || dialogueText.getText() == null
            || dialogueText.getText().trim().isEmpty())
        {
            lastActivityDialogueText = null;
            return;
        }
        String currentText = dialogueText.getText();
        if (!currentText.equals(lastActivityDialogueText))
        {
            lastActivityDialogueText = currentText;
            activityTelemetryDetector.onPestControlDialogue(currentText);
        }
    }

    private String currentRsn()
    {
        return client.getLocalPlayer() == null
            ? null : client.getLocalPlayer().getName();
    }

    private void initializeActivityTelemetry()
    {
        if (activityTelemetryDetector == null
            || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        activityTelemetryDetector.initializeGameState(
            client.getSkillExperience(Skill.AGILITY),
            client.getVarbitValue(VarbitID.HOSIDIUS_TITHE_SCORE),
            client.getVarpValue(
                VarPlayerID.GIANTS_FOUNDRY_REWARD_SHOP_POINTS),
            client.getVarbitValue(
                VarbitID.VARLAMORE_WYRM_AGILITY_BASIC_PROGRESS),
            client.getVarbitValue(
                VarbitID.VARLAMORE_WYRM_AGILITY_ADVANCED_PROGRESS));
    }

    private static BufferedImage createIcon()
    {
        return ImageUtil.loadImageResource(
            ClanHQVerifierPlugin.class,
            "/com/clanhq/verifier/icons/overview.png");
    }
}
