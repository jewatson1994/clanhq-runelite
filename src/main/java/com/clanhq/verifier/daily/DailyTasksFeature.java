package com.clanhq.verifier.daily;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.daily.model.DailyTaskSummary;
import com.clanhq.verifier.daily.model.DailyTasksSnapshot;
import com.clanhq.verifier.daily.transport.DailyTasksApiClient;
import com.clanhq.verifier.daily.transport.DailyTasksResult;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.loot.ObservedDrop;
import com.clanhq.verifier.task.VerificationType;
import com.google.gson.JsonObject;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

public final class DailyTasksFeature implements ClanHQFeature
{
    private static final Logger log = LoggerFactory.getLogger(DailyTasksFeature.class);
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
    private final ReconciliationLoop<DailyTasksResult> reconciliation;
    private volatile String refreshMessage;
    private final ConfigManager configManager;
    private final PendingClaimStore pendingClaimStore;
    private volatile boolean claimInFlight;
    private volatile Instant nextClaimAttempt = Instant.EPOCH;
    private volatile long deliveryGeneration;
    private volatile ScheduledFuture<?> deliveryTimeout;
    private volatile String installationScope;
    private static final String INSTALLATION_SCOPE_KEY = "dailyTasksProgressInstallation";

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
        this.configManager = configManager;
        this.pendingClaimStore = new PendingClaimStore(configManager);
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
        ensureInstallationScope();
        this.reconciliation = new ReconciliationLoop<>(executor,
            SwingUtilities::invokeLater, apiClient::fetch, this::receiveRefresh);
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
        reconciliation.start();
    }

    @Override
    public void shutDown()
    {
        running = false;
        invalidateDelivery();
        reconciliation.stop();
        refreshMessage = null;
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
        if (successMessage != null) { refreshMessage = successMessage; }
        reconciliation.refresh();
    }

    private void receiveRefresh(DailyTasksResult result, Throwable error)
    {
        ensureInstallationScope();
        if (normalized(config.installationToken()).isEmpty())
        {
            snapshot = null;
            overlay.clearPersistedState();
            clearInfoBoxes();
            panel.showUnpaired(
                "Use /plugin pair in Discord, then enter the code in settings.");
            return;
        }
        if (error != null)
        {
            log.warn("sync client=runelite phase=reconcile status=failed reason={}",
                error.getClass().getSimpleName());
            panel.showError("Refresh failed; retrying automatically shortly.", true);
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
                        refreshMessage == null ? result.getMessage() : refreshMessage);
                    refreshMessage = null;
                    overlay.publishLiveProgress();
                    reconcilePendingClaims(snapshot);
                    log.debug("sync client=runelite phase=reconcile status=applied period={}",
                        snapshot.getPeriodDate());
                },
                () -> panel.showError(result.getMessage(), true));
    }

    public void claim(String category)
    {
        DailyTasksSnapshot current = snapshot;
        if (current == null || current.getPeriodDate().isEmpty())
        {
            panel.showError("Refresh today's tasks before claiming.", true);
            return;
        }
        Instant clickedAt = Instant.now();
        if (!clickedAt.isBefore(current.getResetAt()))
        {
            panel.showError("This task period ended; refresh for the next one.", true);
            return;
        }
        DailyTaskSummary chosen = null;
        for (DailyTaskSummary task : current.getTasks())
        {
            if (category.equals(task.getCategory()) && !task.isCompleted()
                && overlay.progressFor(task) >= task.getTarget())
            {
                chosen = task;
                break;
            }
        }
        if (chosen == null)
        {
            panel.showError("This task is not claimable yet.", true);
            return;
        }
        String contextId = current.getContext() == null
            ? "" : current.getContext().getId();
        pendingClaimStore.enqueue(installationScope, current.getPeriodDate(),
            current.getResetAt().toString(), clickedAt.toString(), contextId, category,
            chosen.getId() == null ? "" : chosen.getId(),
            overlay.buildClientProgress(null));
        panel.setPending(category);
        flushPendingClaim();
    }

    private void reconcilePendingClaims(DailyTasksSnapshot current)
    {
        String contextId = current.getContext() == null
            ? "" : current.getContext().getId();
        boolean olderPending = false;
        boolean currentPending = false;
        for (JsonObject pending : pendingClaimStore.forScope(installationScope))
        {
            if (!current.getPeriodDate().equals(PendingClaimStore.string(pending,
                    "period_date"))
                || !current.getResetAt().toString().equals(
                    PendingClaimStore.string(pending, "reset_at"))
                || !contextId.equals(PendingClaimStore.string(pending, "context_id")))
            {
                olderPending = true;
                continue;
            }
            String taskId = PendingClaimStore.string(pending, "task_id");
            String category = PendingClaimStore.string(pending, "category");
            for (DailyTaskSummary task : current.getTasks())
            {
                if (task.isCompleted() && (taskId.isEmpty()
                    ? category.equals(task.getCategory())
                    : taskId.equals(task.getId())))
                {
                    pendingClaimStore.acknowledge(
                        PendingClaimStore.string(pending, "claim_id"));
                    pending = null;
                    break;
                }
            }
            if (pending != null)
            {
                currentPending = true;
                panel.setPending(category);
            }
        }
        if (olderPending && !currentPending)
        {
            panel.showClaimSyncStatus("An earlier claim is saved; syncing during its grace period if eligible.");
        }
        flushPendingClaim();
    }

    private void flushPendingClaim()
    {
        if (!running || claimInFlight || Instant.now().isBefore(nextClaimAttempt))
        {
            return;
        }
        for (JsonObject pending : pendingClaimStore.forScope(installationScope))
        {
            Instant deadline;
            try
            {
                deadline = Instant.parse(PendingClaimStore.string(pending, "reset_at"))
                    .plus(Duration.ofHours(2));
            }
            catch (RuntimeException error)
            {
                continue;
            }
            if (Instant.now().isAfter(deadline))
            {
                continue;
            }
            claimInFlight = true;
            nextClaimAttempt = Instant.now().plusSeconds(30);
            long generation = ++deliveryGeneration;
            if (executor != null)
            {
                deliveryTimeout = executor.schedule(() -> SwingUtilities.invokeLater(() ->
                {
                    if (generation != deliveryGeneration || !running) { return; }
                    claimInFlight = false;
                    deliveryGeneration++;
                    panel.showClaimSyncStatus(
                        "Claim saved; delivery timed out. Retrying automatically.");
                }), 20, TimeUnit.SECONDS);
            }
            String category = PendingClaimStore.string(pending, "category");
            pendingDropObservations.handle((ignored, error) -> null)
                .thenCompose(ignored -> apiClient.claim(category,
                    PendingClaimStore.string(pending, "period_date"),
                    pending.getAsJsonArray("client_progress"),
                    PendingClaimStore.string(pending, "claim_id"),
                    PendingClaimStore.string(pending, "clicked_at"),
                    PendingClaimStore.string(pending, "reset_at"),
                    PendingClaimStore.string(pending, "context_id")))
                .whenComplete((result, error) -> SwingUtilities.invokeLater(() ->
                {
                    if (generation != deliveryGeneration || !running) { return; }
                    if (deliveryTimeout != null) { deliveryTimeout.cancel(false); }
                    claimInFlight = false;
                    deliveryGeneration++;
                    if (error == null && result != null && result.isSuccessful())
                    {
                        pendingClaimStore.acknowledge(
                            PendingClaimStore.string(pending, "claim_id"));
                        refresh("Claim sent; confirming payout with ClanHQ.");
                    }
                    else
                    {
                        panel.showClaimSyncStatus(
                            "Claim saved; syncing with ClanHQ automatically.");
                        log.warn("sync client=runelite phase=claim_delivery claim_id={} status=pending",
                            PendingClaimStore.string(pending, "claim_id"));
                    }
                }));
            return;
        }
    }

    private void invalidateDelivery()
    {
        deliveryGeneration++;
        claimInFlight = false;
        if (deliveryTimeout != null) { deliveryTimeout.cancel(false); }
        deliveryTimeout = null;
    }

    public void observeSkillExperience(String skillName, int experience)
    {
        overlay.observeSkillExperience(skillName, experience);
    }

    private void ensureInstallationScope()
    {
        String token = normalized(config.installationToken());
        String scope = token.isEmpty() ? "" : tokenFingerprint(token);
        if (scope.equals(installationScope)) { return; }
        String stored = configManager.getConfiguration(
            ClanHQVerifierConfig.GROUP, INSTALLATION_SCOPE_KEY);
        if (!scope.equals(stored))
        {
            invalidateDelivery();
            snapshot = null;
            overlay.clearPersistedState();
            configManager.setConfiguration(
                ClanHQVerifierConfig.GROUP, INSTALLATION_SCOPE_KEY, scope);
        }
        installationScope = scope;
    }

    private static String tokenFingerprint(String token)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte part : digest)
            {
                value.append(String.format("%02x", part & 0xff));
            }
            return value.toString();
        }
        catch (NoSuchAlgorithmException error)
        {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
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
        apiClient.submitActivity(rsn, activity, quantity, metadata)
            .whenComplete((ignored, error) -> refresh());
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
            pendingDropObservations.whenComplete((ignored, error) -> refresh());
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
        long delay = Duration.between(Instant.now(),
            value.getContext().getRotationEndsAt()).toMillis();
        // An expired server snapshot must not create a millisecond retry loop.
        if (delay <= 0) { return; }
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
