package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.bingo.model.BingoCharacterSubmission;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.bingo.service.BingoScreenshotService;
import com.clanhq.verifier.service.LocalPlayerSnapshotService;
import com.clanhq.verifier.service.SubmissionConsentService;
import com.clanhq.verifier.feature.ClanHQFeature;
import com.clanhq.verifier.event.transport.EventApiClient;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
    private final EventApiClient eventApiClient;
    private final Supplier<String> rsnSupplier;
    private volatile BingoManifest manifest;
    private volatile boolean running;
    private volatile BingoCharacterSubmission pendingCharacterSubmission;

    public BingoFeature(
        BingoApiClient apiClient,
        BingoScreenshotService screenshotService,
        BooleanSupplier screenshotsEnabled,
        LocalPlayerSnapshotService snapshotService,
        ClientThread clientThread,
        SubmissionConsentService consentService,
        EventApiClient eventApiClient,
        Supplier<String> rsnSupplier)
    {
        this.apiClient = apiClient;
        this.screenshotService = screenshotService;
        this.screenshotsEnabled = screenshotsEnabled;
        this.snapshotService = snapshotService;
        this.clientThread = clientThread;
        this.consentService = consentService;
        this.eventApiClient = eventApiClient;
        this.rsnSupplier = rsnSupplier;
        this.panel = new BingoPanel(
            this::refreshManifest,
            this::submitCharacter);
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
        refreshManifest();
    }

    @Override
    public void shutDown()
    {
        running = false;
        manifest = null;
        pendingCharacterSubmission = null;
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
                    panel.showManifest(value);
                    if (value.getCharacterCheck().canSubmit()
                        && !"FINAL".equals(
                            value.getCharacterCheck().getNextPhase()))
                    {
                        joinBingo(value);
                    }
                }, () ->
                {
                    manifest = null;
                    panel.showManifestError(result.getMessage());
                });
            }));
    }

    private void joinBingo(BingoManifest active)
    {
        String rsn = rsnSupplier.get();
        if (rsn == null || rsn.trim().isEmpty())
        {
            panel.showParticipation(false, null,
                "Log in to join this Bingo event.");
            return;
        }
        eventApiClient.joinEventCode(active.getEventId(), rsn)
            .thenAccept(result -> SwingUtilities.invokeLater(() ->
            {
                if (running)
                {
                    panel.showParticipation(
                        result.isJoined(),
                        result.getTeamName(),
                        result.getMessage());
                }
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
        BingoManifest active = manifest;
        if (!running || active == null || rsn == null || rsn.trim().isEmpty())
        {
            return;
        }
        for (ItemStack observed : items)
        {
            BingoItem item = active.findItem(observed.getId()).orElse(null);
            if (item == null || observed.getQuantity() < item.getMinimumQuantity())
            {
                continue;
            }
            BingoDrop drop = new BingoDrop(
                active.getEventId(),
                rsn,
                item,
                observed.getQuantity(),
                sourceType,
                sourceName == null ? "Unknown loot source" : sourceName,
                Instant.now());
            SwingUtilities.invokeLater(() -> panel.showDetected(
                item.getName(), drop.getQuantity(), drop.getSourceName()));
            CompletableFuture<byte[]> screenshot =
                screenshotsEnabled.getAsBoolean()
                    ? screenshotService.capture(drop, active.getName())
                        .handle((bytes, error) -> error == null ? bytes : null)
                    : CompletableFuture.completedFuture(null);
            screenshot.thenCompose(bytes -> apiClient.submit(drop, bytes))
                .thenAccept(result ->
                SwingUtilities.invokeLater(() ->
                {
                    if (running)
                    {
                        panel.showDelivery(item.getName(),
                            result.isSuccessful(), result.getMessage());
                    }
                }));
        }
    }
}
