package com.clanhq.verifier.service;

import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;

public final class LocalPlayerSnapshotService
{
    private final Client client;
    private final Map<String, ObservedItem> observedItems =
        new LinkedHashMap<>();
    private boolean captureActive;
    private boolean bankEvidenceCaptured;
    private boolean pietyObserved;
    private String sessionRsn;

    @Inject
    public LocalPlayerSnapshotService(Client client)
    {
        this.client = client;
    }

    public void startCaptureSession()
    {
        Player player = requireLoggedInPlayer();

        observedItems.clear();
        captureActive = true;
        bankEvidenceCaptured = false;
        pietyObserved = false;
        sessionRsn = player.getName();

        observeCurrentState();

        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank != null)
        {
            addItems(bank, EvidenceSource.BANK);
            bankEvidenceCaptured = true;
        }
    }

    public void observeCurrentState()
    {
        if (!captureActive)
        {
            return;
        }

        Player player = requireLoggedInPlayer();
        ensureSamePlayer(player);

        addItems(
            client.getItemContainer(InventoryID.WORN),
            EvidenceSource.EQUIPMENT);
        addItems(
            client.getItemContainer(InventoryID.INV),
            EvidenceSource.INVENTORY);

        pietyObserved = pietyObserved
            || client.getVarbitValue(VarbitID.PRAYER_PIETY) == 1;
    }

    public VerificationSnapshot finishCaptureSession()
    {
        if (!captureActive)
        {
            throw new IllegalStateException("No capture session is active.");
        }

        observeCurrentState();
        Player player = requireLoggedInPlayer();
        captureActive = false;

        return new VerificationSnapshot(
            sessionRsn,
            client.getTotalLevel(),
            player.getCombatLevel(),
            new ArrayList<>(observedItems.values()),
            bankEvidenceCaptured,
            pietyObserved);
    }

    public boolean isCaptureActive()
    {
        return captureActive;
    }

    public void cancelCaptureSession()
    {
        observedItems.clear();
        captureActive = false;
        bankEvidenceCaptured = false;
        pietyObserved = false;
        sessionRsn = null;
    }

    private Player requireLoggedInPlayer()
    {
        Player player = client.getLocalPlayer();

        if (client.getGameState() != GameState.LOGGED_IN || player == null)
        {
            throw new IllegalStateException(
                "Log into Old School RuneScape before capturing evidence.");
        }

        return player;
    }

    public void observeItemContainer(ItemContainerChanged event)
    {
        if (!captureActive || event.getContainerId() != InventoryID.BANK)
        {
            return;
        }

        Player player = requireLoggedInPlayer();
        ensureSamePlayer(player);

        addItems(
            event.getItemContainer(),
            EvidenceSource.BANK);
        bankEvidenceCaptured = true;
    }

    private void addItems(
        ItemContainer container,
        EvidenceSource source)
    {
        if (container == null)
        {
            return;
        }

        for (Item item : container.getItems())
        {
            if (item.getId() <= 0)
            {
                continue;
            }

            ObservedItem observedItem = new ObservedItem(
                item.getId(),
                client.getItemDefinition(item.getId()).getName(),
                item.getQuantity(),
                source);

            observedItems.put(
                source.name() + ':' + item.getId(),
                observedItem);
        }
    }

    private void ensureSamePlayer(Player player)
    {
        if (!player.getName().equals(sessionRsn))
        {
            cancelCaptureSession();
            throw new IllegalStateException(
                "The logged-in character changed during capture.");
        }
    }
}
