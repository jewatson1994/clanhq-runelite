package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.bingo.service.BingoScreenshotService;
import com.clanhq.verifier.feature.ClanHQFeature;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemStack;

public final class BingoFeature implements ClanHQFeature
{
    private final BingoApiClient apiClient;
    private final BingoPanel panel;
    private final BingoScreenshotService screenshotService;
    private final BooleanSupplier screenshotsEnabled;
    private volatile BingoManifest manifest;
    private volatile boolean running;

    public BingoFeature(
        BingoApiClient apiClient,
        BingoScreenshotService screenshotService,
        BooleanSupplier screenshotsEnabled)
    {
        this.apiClient = apiClient;
        this.screenshotService = screenshotService;
        this.screenshotsEnabled = screenshotsEnabled;
        this.panel = new BingoPanel(this::refreshManifest);
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
                    manifest = value;
                    panel.showManifest(value);
                }, () ->
                {
                    manifest = null;
                    panel.showManifestError(result.getMessage());
                });
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
