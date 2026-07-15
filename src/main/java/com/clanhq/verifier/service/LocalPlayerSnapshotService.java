package com.clanhq.verifier.service;

import com.clanhq.verifier.model.EquipmentItem;
import com.clanhq.verifier.model.VerificationSnapshot;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.gameval.InventoryID;

public final class LocalPlayerSnapshotService
{
    private final Client client;

    @Inject
    public LocalPlayerSnapshotService(Client client)
    {
        this.client = client;
    }

    public VerificationSnapshot capture()
    {
        Player player = client.getLocalPlayer();

        if (client.getGameState() != GameState.LOGGED_IN || player == null)
        {
            throw new IllegalStateException(
                "Log into Old School RuneScape before capturing evidence.");
        }

        return new VerificationSnapshot(
            player.getName(),
            client.getTotalLevel(),
            player.getCombatLevel(),
            captureEquipment());
    }

    private List<EquipmentItem> captureEquipment()
    {
        List<EquipmentItem> equipment = new ArrayList<>();
        ItemContainer container = client.getItemContainer(InventoryID.WORN);

        if (container == null)
        {
            return equipment;
        }

        for (Item item : container.getItems())
        {
            if (item.getId() <= 0)
            {
                continue;
            }

            equipment.add(new EquipmentItem(
                item.getId(),
                client.getItemDefinition(item.getId()).getName(),
                item.getQuantity()));
        }

        return equipment;
    }
}
