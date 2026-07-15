package com.clanhq.verifier.service;

import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.DiaryProgress;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.VerificationSnapshot;
import com.clanhq.verifier.rules.RankItemCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

public final class LocalPlayerSnapshotService
{
    private final Client client;
    private final RankItemCatalog rankItemCatalog;

    @Inject
    public LocalPlayerSnapshotService(
        Client client,
        RankItemCatalog rankItemCatalog)
    {
        this.client = client;
        this.rankItemCatalog = rankItemCatalog;
    }

    public VerificationSnapshot captureItemsEvidence()
    {
        Player player = requireLoggedInPlayer();
        net.runelite.api.widgets.Widget bankFrame = client.getWidget(
            InterfaceID.Bankmain.FRAME);
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bankFrame == null || bankFrame.isHidden() || bank == null)
        {
            throw new IllegalStateException(
                "Open your bank before capturing bank and gear evidence.");
        }

        Map<String, ObservedItem> items = new LinkedHashMap<>();
        addItems(client.getItemContainer(InventoryID.WORN),
            EvidenceSource.EQUIPMENT, items);
        addItems(client.getItemContainer(InventoryID.INV),
            EvidenceSource.INVENTORY, items);
        addItems(bank, EvidenceSource.BANK, items);

        return snapshot(player, new ArrayList<>(items.values()), true);
    }

    public VerificationSnapshot captureAccountEvidence()
    {
        Player player = requireLoggedInPlayer();
        return snapshot(player, Collections.emptyList(), false);
    }

    private VerificationSnapshot snapshot(Player player,
        List<ObservedItem> items, boolean bankCaptured)
    {
        return new VerificationSnapshot(
            player.getName(),
            client.getTotalLevel(),
            player.getCombatLevel(),
            items,
            bankCaptured,
            client.getVarbitValue(VarbitID.PRAYER_PIETY) == 1,
            client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1,
            client.getRealSkillLevel(Skill.HERBLORE),
            captureDiaryProgress(),
            com.clanhq.verifier.model.RaidKillCounts.unavailable("Not fetched"),
            client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
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

    private void addItems(
        ItemContainer container,
        EvidenceSource source,
        Map<String, ObservedItem> observedItems)
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

            String itemName = client.getItemDefinition(item.getId()).getName();
            if (source == EvidenceSource.BANK
                && !rankItemCatalog.isRelevant(item.getId(), itemName))
            {
                continue;
            }

            ObservedItem observedItem = new ObservedItem(
                item.getId(),
                itemName,
                item.getQuantity(),
                source);

            observedItems.put(
                source.name() + ':' + item.getId(),
                observedItem);
        }
    }

    private DiaryProgress captureDiaryProgress()
    {
        int[] hardFlags = {VarbitID.ARDOUGNE_DIARY_HARD_COMPLETE,
            VarbitID.FALADOR_DIARY_HARD_COMPLETE,
            VarbitID.WILDERNESS_DIARY_HARD_COMPLETE,
            VarbitID.WESTERN_DIARY_HARD_COMPLETE,
            VarbitID.KANDARIN_DIARY_HARD_COMPLETE,
            VarbitID.VARROCK_DIARY_HARD_COMPLETE,
            VarbitID.DESERT_DIARY_HARD_COMPLETE,
            VarbitID.MORYTANIA_DIARY_HARD_COMPLETE,
            VarbitID.FREMENNIK_DIARY_HARD_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_HARD_COMPLETE,
            VarbitID.KOUREND_DIARY_HARD_COMPLETE};
        int[] eliteFlags = {VarbitID.ARDOUGNE_DIARY_ELITE_COMPLETE,
            VarbitID.FALADOR_DIARY_ELITE_COMPLETE,
            VarbitID.WILDERNESS_DIARY_ELITE_COMPLETE,
            VarbitID.WESTERN_DIARY_ELITE_COMPLETE,
            VarbitID.KANDARIN_DIARY_ELITE_COMPLETE,
            VarbitID.VARROCK_DIARY_ELITE_COMPLETE,
            VarbitID.DESERT_DIARY_ELITE_COMPLETE,
            VarbitID.MORYTANIA_DIARY_ELITE_COMPLETE,
            VarbitID.FREMENNIK_DIARY_ELITE_COMPLETE,
            VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE,
            VarbitID.KOUREND_DIARY_ELITE_COMPLETE};
        int hard = 0;
        int elite = 0;
        for (int flag : hardFlags)
        {
            hard += completed(flag);
        }
        for (int flag : eliteFlags)
        {
            elite += completed(flag);
        }
        hard += client.getVarbitValue(VarbitID.KARAMJA_HARD_COUNT) >= 10 ? 1 : 0;
        elite += completed(VarbitID.KARAMJA_DIARY_ELITE_COMPLETE);
        return new DiaryProgress(hard, elite, 12);
    }

    private int completed(int varbitId)
    {
        return client.getVarbitValue(varbitId) > 0 ? 1 : 0;
    }

}
