package com.clanhq.verifier.daily;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.daily.transport.DailyTasksApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.loot.ObservedDrop;
import com.clanhq.verifier.task.VerificationType;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public final class DailyTasksFeature implements ClanHQFeature
{
    private final DailyTasksApiClient apiClient;
    private final ClanHQVerifierConfig config;
    private final DailyTasksPanel panel;
    private final DailyTasksOverlay overlay;
    private final ScheduledExecutorService executor;
    private final Runnable overviewChanged;
    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;
    private final SpriteManager spriteManager;
    private final Plugin plugin;
    private final InfoBoxManager infoBoxManager;
    private final Map<String, DailyTaskInfoBox> infoBoxes = new HashMap<>();
    private volatile ScheduledFuture<?> rotationRefresh;
    private volatile DailyTasksSnapshot snapshot;
    private volatile CompletableFuture<Void> pendingDropObservations =
        CompletableFuture.completedFuture(null);
    private volatile boolean running;
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();

    public DailyTasksFeature(DailyTasksApiClient apiClient,
        ClanHQVerifierConfig config,
        ConfigManager configManager,
        SkillIconManager skillIconManager,
        ScheduledExecutorService executor,
        Runnable overviewChanged)
    {
        this(apiClient, config, configManager, skillIconManager, executor,
            overviewChanged, null, null, null, null);
    }

    public DailyTasksFeature(DailyTasksApiClient apiClient,
        ClanHQVerifierConfig config,
        ConfigManager configManager,
        SkillIconManager skillIconManager,
        ScheduledExecutorService executor,
        Runnable overviewChanged,
        ItemManager itemManager)
    {
        this(apiClient, config, configManager, skillIconManager, executor,
            overviewChanged, itemManager, null, null, null);
    }

    public DailyTasksFeature(DailyTasksApiClient apiClient,
        ClanHQVerifierConfig config,
        ConfigManager configManager,
        SkillIconManager skillIconManager,
        ScheduledExecutorService executor,
        Runnable overviewChanged,
        ItemManager itemManager,
        Plugin plugin,
        InfoBoxManager infoBoxManager,
        SpriteManager spriteManager)
    {
        this.apiClient = apiClient;
        this.config = config;
        this.executor = executor;
        this.overviewChanged = overviewChanged;
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
        this.spriteManager = spriteManager;
        this.plugin = plugin;
        this.infoBoxManager = infoBoxManager;
        this.panel = new DailyTasksPanel(
            this::refresh,
            this::claim,
            skillIconManager);
        this.overlay = new DailyTasksOverlay(() -> snapshot, configManager,
            (assignmentId, progress) -> SwingUtilities.invokeLater(() ->
            {
                panel.updateLiveProgress(assignmentId, progress);
                updateInfoBoxes(snapshot);
            }), itemManager,
            config::dailyTasksOverlayOpacity, skillIconManager, spriteManager);
    }

    @Override
    public String getId() { return "daily-tasks"; }

    @Override
    public String getDisplayName() { return "Dailies"; }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/dailies.png";
    }

    @Override
    public String getDescription()
    {
        return "View and claim WOM-verified currency daily tasks.";
    }

    @Override
    public JComponent getPanel() { return panel; }

    public DailyTasksOverlay getOverlay() { return overlay; }

    @Override
    public void startUp()
    {
        running = true;
        refresh();
    }

    @Override
    public void shutDown()
    {
        running = false;
        ScheduledFuture<?> scheduled = rotationRefresh;
        if (scheduled != null)
        {
            scheduled.cancel(false);
            rotationRefresh = null;
        }
        snapshot = null;
        overlay.setSnapshot(null);
        clearInfoBoxes();
    }

    public void refresh()
    {
        refresh(null);
    }

    private void refresh(String successMessage)
    {
        if (!running)
        {
            return;
        }
        if (normalized(config.installationToken()).isEmpty())
        {
            snapshot = null;
            overlay.clearPersistedState();
            clearInfoBoxes();
            panel.showUnpaired(
                "Use /plugin pair in Discord, then enter the code in settings.");
            return;
        }
        if (!refreshInFlight.compareAndSet(false, true))
        {
            return;
        }
        SwingUtilities.invokeLater(() ->
            panel.setLoading("Loading today's tasks..."));
        apiClient.fetch().thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            refreshInFlight.set(false);
            if (!running)
            {
                return;
            }
            result.getSnapshot().ifPresentOrElse(
                snapshot -> {
                    this.snapshot = snapshot;
                    overviewChanged.run();
                    overlay.setSnapshot(snapshot);
                    updateInfoBoxes(snapshot);
                    scheduleRotationRefresh(snapshot);
                    panel.showTasks(snapshot,
                        successMessage == null ? result.getMessage() : successMessage);
                    overlay.publishLiveProgress();
                },
                () -> panel.showError(result.getMessage(), true));
        }));
    }

    public void claim(String category)
    {
        DailyTasksSnapshot current = snapshot;
        if (current == null || current.getPeriodDate().isEmpty())
        {
            panel.showError("Refresh today's tasks before claiming.", true);
            return;
        }
        panel.setLoading("Checking saved client progress for the "
            + category.toLowerCase() + " task...");
        panel.setClaiming(category);
        CompletableFuture<Void> observations = pendingDropObservations;
        observations.handle((ignored, error) -> null)
            .thenCompose(ignored -> apiClient.claim(
                category,
                current.getPeriodDate(),
                overlay.buildClientProgress(null)))
            .thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            if (!running)
            {
                return;
            }
            if (!result.isSuccessful())
            {
                panel.restoreClaim(category);
                panel.showError(result.getMessage(), true);
                return;
            }
            String message = result.getMessage();
            if (result.getRewardAmount() > 0)
            {
                message += " Awarded " + result.getRewardAmount() + " "
                    + result.getCurrencyName()
                    + (result.getCurrencySymbol().isEmpty()
                        ? "." : " " + result.getCurrencySymbol() + ".");
            }
            refresh(message);
            }));
    }

    public void observeSkillExperience(String skillName, int experience)
    {
        overlay.observeSkillExperience(skillName, experience);
    }

    public void observeLoot(String sourceName)
    {
        overlay.observeLoot(sourceName);
    }

    private void updateInfoBoxes(DailyTasksSnapshot value)
    {
        if (infoBoxManager == null || plugin == null
            || !config.dailyTasksOverlay()
            || config.dailyTasksDisplayMode()
                != ClanHQVerifierConfig.DailyTasksDisplayMode.INFO_BOXES
            || value == null)
        {
            clearInfoBoxes();
            return;
        }

        Set<String> activeKeys = new HashSet<>();
        for (DailyTaskSummary task : value.getTasks())
        {
            String key = task.getId() == null
                ? task.getCategory() + ":" + task.getName() : task.getId();
            activeKeys.add(key);
            DailyTaskInfoBox infoBox = infoBoxes.get(key);
            if (infoBox == null)
            {
                infoBox = new DailyTaskInfoBox(task, overlay.progressFor(task),
                    plugin, itemManager, skillIconManager, spriteManager,
                    infoBoxManager);
                infoBoxes.put(key, infoBox);
                infoBoxManager.addInfoBox(infoBox);
            }
            else
            {
                infoBox.update(task, overlay.progressFor(task));
            }
        }

        infoBoxes.entrySet().removeIf(entry ->
        {
            if (activeKeys.contains(entry.getKey()))
            {
                return false;
            }
            infoBoxManager.removeInfoBox(entry.getValue());
            return true;
        });
    }

    private void clearInfoBoxes()
    {
        if (infoBoxManager == null)
        {
            return;
        }
        infoBoxes.values().forEach(infoBoxManager::removeInfoBox);
        infoBoxes.clear();
    }

    /** Forward a recognized gameplay event without coupling the plugin to task state. */
    public void observeActivity(String rsn, String activity, int quantity,
        Map<String, String> metadata)
    {
        if (!running || normalized(rsn).isEmpty()
            || normalized(activity).isEmpty() || quantity <= 0)
        {
            return;
        }
        apiClient.submitActivity(rsn, activity, quantity, metadata);
    }

    public DailyTasksSnapshot getSnapshot()
    {
        return snapshot;
    }

    public void observeDrop(ObservedDrop drop)
    {
        DailyTasksSnapshot current = snapshot;
        Map<String, ItemDropMatcher.Match> matches = ItemDropMatcher.findMatches(
            current, drop, itemManager);
        overlay.observeDrop(drop);
        if (matches.isEmpty())
        {
            return;
        }
        List<CompletableFuture<Boolean>> submissions = new ArrayList<>();
        for (DailyTaskSummary task : current.getTasks())
        {
            ItemDropMatcher.Match match = matches.get(task.getId());
            if (match == null)
            {
                continue;
            }
            submissions.add(apiClient.submitItemDropObservation(task.getId(),
                drop, match.getCanonicalItemId(), match.getQuantity()));
        }
        if (!submissions.isEmpty())
        {
            pendingDropObservations = CompletableFuture.allOf(
                submissions.toArray(new CompletableFuture<?>[0]));
        }
    }

    private void scheduleRotationRefresh(DailyTasksSnapshot value)
    {
        if (executor == null || value.getContext() == null
            || value.getContext().getRotationEndsAt() == null)
        {
            return;
        }
        ScheduledFuture<?> previous = rotationRefresh;
        if (previous != null)
        {
            previous.cancel(false);
        }
        long delay = Math.max(1, Duration.between(Instant.now(),
            value.getContext().getRotationEndsAt()).toMillis());
        rotationRefresh = executor.schedule(() ->
        {
            if (running)
            {
                refresh();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private static String normalized(String value)
    {
        return value == null ? "" : value.trim();
    }
}
