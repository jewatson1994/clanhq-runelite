package com.clanhq.verifier.bingo;

import com.clanhq.verifier.bingo.model.BingoDrop;
import com.clanhq.verifier.bingo.model.BingoItem;
import com.clanhq.verifier.bingo.model.BingoManifest;
import com.clanhq.verifier.bingo.transport.BingoApiClient;
import com.clanhq.verifier.feature.ClanHQFeature;
import java.time.Instant;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import net.runelite.client.game.ItemStack;

public final class BingoFeature implements ClanHQFeature
{
    private final BingoApiClient apiClient;
    private final BingoPanel panel;
    private volatile BingoManifest manifest;
    private volatile boolean running;

    public BingoFeature(BingoApiClient apiClient)
    {
        this.apiClient = apiClient;
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
        return "Automatically submit eligible Bingo NPC drops to ClanHQ.";
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

    public void onNpcLoot(String rsn, String sourceName,
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
                "NPC_LOOT",
                sourceName == null ? "Unknown NPC" : sourceName,
                Instant.now());
            SwingUtilities.invokeLater(() -> panel.showDetected(
                item.getName(), drop.getQuantity(), drop.getSourceName()));
            apiClient.submit(drop).thenAccept(result ->
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
