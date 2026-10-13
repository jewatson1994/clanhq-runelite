package com.clanhq.verifier.bingo;

import com.clanhq.verifier.ClanHQVerifierConfig;
import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.bingo.model.RaidProgress;
import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.bingo.service.BingoScreenshotService;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.SubmissionConsentService;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.loot.ObservedDrop;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.callback.ClientThread;

public final class BingoFeature implements ClanHQFeature
{
    private final BingoApiClient apiClient;
    private final BingoPanel panel;
    private final BingoScreenshotService screenshotService;
    private final BooleanSupplier screenshotsEnabled;
    private final LocalPlayerSnapshotService snapshotService;
    private final ClientThread clientThread;
    private final SubmissionConsentService consentService;
    private final ItemManager itemManager;
    private final Supplier<ClanHQVerifierConfig.BingoSubmitMode> submitModeSupplier;
    private final Runnable overviewChanged;
    private volatile BingoManifest manifest;
    private volatile RaidProgress raidProgress = RaidProgress.empty();
    private volatile boolean running;
    private volatile BingoCharacterSubmission pendingCharacterSubmission;

    public BingoFeature(
        BingoApiClient apiClient,
        BingoScreenshotService screenshotService,
        BooleanSupplier screenshotsEnabled,
        LocalPlayerSnapshotService snapshotService,
        ClientThread clientThread,
        SubmissionConsentService consentService,
        ItemManager itemManager,
        Supplier<ClanHQVerifierConfig.BingoSubmitMode> submitModeSupplier,
        Runnable overviewChanged)
    {
        this.apiClient = apiClient;
        this.screenshotService = screenshotService;
        this.screenshotsEnabled = screenshotsEnabled;
        this.snapshotService = snapshotService;
        this.clientThread = clientThread;
        this.consentService = consentService;
        this.itemManager = itemManager;
        this.submitModeSupplier = submitModeSupplier;
        this.overviewChanged = overviewChanged;
        this.panel = new BingoPanel(this::refreshAll);
    }

    @Override
    public String getId()
    {
        return "bingo";
    }

    @Override
    public String getDisplayName()
    {
        return "Bingo";
    }

    @Override
    public String getNavigationIconResource()
    {
        return "/com/clanhq/verifier/icons/bingo.png";
    }

    @Override
    public String getDescription()
    {
        return "Automatically submit eligible RuneLite loot to ClanHQ.";
    }

    @Override
    public JComponent getPanel()
    {
        return panel;
    }

    @Override
    public void startUp()
    {
        running = true;
        refreshAll();
    }

    @Override
    public void shutDown()
    {
        running = false;
        manifest = null;
        raidProgress = RaidProgress.empty();
        pendingCharacterSubmission = null;
    }

    /**
     * Refreshes both the Bingo manifest and the raid-unique progress
     * display together - what the panel's "Refresh" button triggers.
     */
    public void refreshAll()
    {
        refreshManifest();
        refreshRaidProgress();
    }

    public void refreshManifest()
    {
        panel.setLoading();
        apiClient.fetchManifest().thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                result.getManifest().ifPresentOrElse(value ->
                {
                    if (manifest == null
                        || !manifest.getEventId().equals(value.getEventId()))
                    {
                        pendingCharacterSubmission = null;
                    }
                    manifest = value;
                    overviewChanged.run();
                    panel.showManifest(value);
                }, () ->
                {
                    manifest = null;
                    panel.showManifestError(result.getMessage(),
                        result.getSiteUrl().orElse(null));
                });
            }));
    }

    /**
     * Refreshes the permanent, clan-wide raid-unique progress display
     * (e.g. "CoX - 3/12"). Independent of the active Bingo manifest/event:
     * it simply comes back empty when Bingo is disabled server-side, same
     * as the rest of this feature.
     */
    public void refreshRaidProgress()
    {
        apiClient.fetchRaidProgress().thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                raidProgress = result.getProgress().orElse(RaidProgress.empty());
                panel.showRaidProgress(raidProgress);
            }));
    }

    public void submitCharacter()
    {
        BingoManifest active = manifest;
        if (!running || active == null)
        {
            panel.showCharacterSubmission(false,
                "Load an active Bingo board first.");
            return;
        }
        if (!active.getCharacterCheck().canSubmit())
        {
            panel.showCharacterSubmission(false,
                "No additional character check is currently required.");
            return;
        }
        if (!consentService.confirm(
            panel, "Bingo " + active.getName()))
        {
            panel.showCharacterSubmissionCancelled();
            return;
        }
        panel.setCharacterSubmitting();
        BingoCharacterSubmission pending = pendingCharacterSubmission;
        if (pending != null)
        {
            deliverCharacter(pending);
            return;
        }
        clientThread.invokeLater(() ->
        {
            try
            {
                BingoCharacterSubmission captured =
                    new BingoCharacterSubmission(
                        active.getEventId(),
                        snapshotService.captureCompleteItemsEvidence());
                pendingCharacterSubmission = captured;
                deliverCharacter(captured);
            }
            catch (RuntimeException exception)
            {
                SwingUtilities.invokeLater(() -> panel.showCharacterSubmission(
                    false, exception.getMessage()));
            }
        });
    }

    private void deliverCharacter(BingoCharacterSubmission submission)
    {
        apiClient.submitCharacter(submission).thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                if (result.isSuccessful())
                {
                    pendingCharacterSubmission = null;
                    panel.showCharacterSubmission(true, result.getMessage());
                    refreshManifest();
                }
                else
                {
                    panel.showCharacterSubmission(false, result.getMessage());
                }
            }));
    }

    public void onLoot(String rsn, String sourceType, String sourceName,
        Collection<ItemStack> items)
    {
        onDrop(new ObservedDrop(rsn, sourceType, sourceName, items,
            Instant.now()));
    }

    public BingoManifest getManifest()
    {
        return manifest;
    }

    public void onDrop(ObservedDrop observedDrop)
    {
        if (!running || observedDrop == null
            || observedDrop.getPlayer() == null
            || observedDrop.getPlayer().trim().isEmpty())
        {
            return;
        }
        // Raid-unique progress is a permanent, clan-wide tracker: it must
        // keep recognizing drops even when there is no active Bingo
        // manifest/event right now (e.g. between events), so it is
        // checked against the independently-fetched raidProgress catalog
        // rather than the (possibly null) active manifest below.
        RaidProgress tracked = raidProgress;
        for (ItemStack observed : observedDrop.getItems())
        {
            if (tracked.isTracked(observed.getId()))
            {
                apiClient.reportRaidDrop(observed.getId())
                    .thenRun(this::refreshRaidProgress);
            }
        }

        BingoManifest active = manifest;
        if (active == null)
        {
            return;
        }
        for (ItemStack observed : observedDrop.getItems())
        {
            BingoItem item = active.findItem(observed.getId()).orElse(null);
            if (item == null || observed.getQuantity() < item.getMinimumQuantity())
            {
                continue;
            }

            BingoDrop bingoDrop = new BingoDrop(
                active.getEventId(),
                observedDrop.getPlayer(),
                item,
                observed.getQuantity(),
                observedDrop.getSourceType(),
                observedDrop.getSourceName() == null ? "Unknown loot source"
                    : observedDrop.getSourceName(),
                observedDrop.getObservedAt());

            if (submitModeSupplier.get()
                == ClanHQVerifierConfig.BingoSubmitMode.MANUAL_CONFIRM)
            {
                BufferedImage icon = itemManager == null
                    ? null : itemManager.getImage(item.getItemId());
                SwingUtilities.invokeLater(() -> panel.addPendingUserDrop(
                    icon, item.getName(), bingoDrop.getQuantity(),
                    bingoDrop.getSourceName(), bingoDrop.getOccurredAt(),
                    handle -> submitDrop(bingoDrop, active, handle)));
            }
            else
            {
                SwingUtilities.invokeLater(() -> panel.showUserActivityDetected(
                    item.getName(), bingoDrop.getQuantity(),
                    bingoDrop.getSourceName()));
                submitDrop(bingoDrop, active, null);
            }
        }
    }

    /**
     * Captures a screenshot (if enabled) and submits the drop to ClanHQ.
     * Shared by both submit modes: Automatic calls this immediately after
     * detection, Manual calls it once the player clicks "Send to ClanHQ"
     * (or "Retry") on the pending card. When {@code handle} is non-null
     * the outcome is reported back onto that card; otherwise it goes to
     * the User Activity log the way automatic submissions always have.
     */
    private void submitDrop(BingoDrop bingoDrop, BingoManifest active,
        BingoPanel.PendingDropHandle handle)
    {
        if (handle != null)
        {
            SwingUtilities.invokeLater(handle::markSending);
        }
        CompletableFuture<byte[]> screenshot =
            screenshotsEnabled.getAsBoolean()
                ? screenshotService.capture(bingoDrop, active.getName())
                    .handle((bytes, error) -> error == null ? bytes : null)
                : CompletableFuture.completedFuture(null);
        screenshot.thenCompose(bytes -> apiClient.submit(bingoDrop, bytes))
            .thenAccept(result ->
            SwingUtilities.invokeLater(() ->
            {
                if (!running)
                {
                    return;
                }
                if (handle != null)
                {
                    handle.markResult(result.isSuccessful(), result.getMessage());
                }
                else
                {
                    panel.showUserActivityDelivery(bingoDrop.getItem().getName(),
                        result.isSuccessful(), result.getMessage());
                }
            }));
    }
}
