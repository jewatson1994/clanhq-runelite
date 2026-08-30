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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.SkillIconManager;

public final class DailyTasksFeature implements ClanHQFeature
{
    private final DailyTasksApiClient apiClient;
    private final ClanHQVerifierConfig config;
    private final DailyTasksPanel panel;
    private final DailyTasksOverlay overlay;
    private final ScheduledExecutorService executor;
    private final Runnable overviewChanged;
    private volatile ScheduledFuture<?> rotationRefresh;
    private volatile DailyTasksSnapshot snapshot;
    private volatile CompletableFuture<Void> pendingDropObservations =
        CompletableFuture.completedFuture(null);
    private volatile boolean running;

    public DailyTasksFeature(DailyTasksApiClient apiClient,
        ClanHQVerifierConfig config,
        ConfigManager configManager,
        SkillIconManager skillIconManager,
        ScheduledExecutorService executor,
        Runnable overviewChanged)
    {
        this.apiClient = apiClient;
        this.config = config;
        this.executor = executor;
        this.overviewChanged = overviewChanged;
        this.panel = new DailyTasksPanel(
            this::refresh,
            this::claim,
            skillIconManager);
        this.overlay = new DailyTasksOverlay(() -> snapshot, configManager, config);
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
            panel.showUnpaired(
                "Use /plugin pair in Discord, then enter the code in settings.");
            return;
        }
        SwingUtilities.invokeLater(() ->
            panel.setLoading("Loading today's tasks..."));
        apiClient.fetch().thenAccept(result -> SwingUtilities.invokeLater(() ->
        {
            if (!running)
            {
                return;
            }
            result.getSnapshot().ifPresentOrElse(
                snapshot -> {
                    this.snapshot = snapshot;
                    overviewChanged.run();
                    overlay.setSnapshot(snapshot);
                    scheduleRotationRefresh(snapshot);
                    panel.showTasks(snapshot,
                        successMessage == null ? result.getMessage() : successMessage);
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

    public DailyTasksSnapshot getSnapshot()
    {
        return snapshot;
    }

    public void observeDrop(ObservedDrop drop)
    {
        overlay.observeDrop(drop);
        DailyTasksSnapshot current = snapshot;
        if (current == null || drop == null || drop.getItems() == null
            || !isGenericTaskContext(current))
        {
            return;
        }
        List<CompletableFuture<Boolean>> submissions = new ArrayList<>();
        for (DailyTaskSummary task : current.getTasks())
        {
            if (task.getVerificationType() != VerificationType.ITEM_DROP
                || task.getVerificationItemId() == null
                || task.getId() == null || task.getId().trim().isEmpty())
            {
                continue;
            }
            int quantity = 0;
            for (net.runelite.client.game.ItemStack item : drop.getItems())
            {
                if (item.getId() == task.getVerificationItemId())
                {
                    quantity += Math.max(0, item.getQuantity());
                }
            }
            if (quantity > 0)
            {
                submissions.add(apiClient.submitItemDropObservation(task.getId(),
                    drop, task.getVerificationItemId(), quantity));
            }
        }
        if (!submissions.isEmpty())
        {
            pendingDropObservations = CompletableFuture.allOf(
                submissions.toArray(new CompletableFuture<?>[0]));
        }
    }

    private static boolean isGenericTaskContext(DailyTasksSnapshot value)
    {
        if (value.getContext() == null)
        {
            return false;
        }
        String type = value.getContext().getType();
        return type != null && !type.trim().isEmpty()
            && !"daily".equalsIgnoreCase(type)
            && !"daily_tasks".equalsIgnoreCase(type);
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
