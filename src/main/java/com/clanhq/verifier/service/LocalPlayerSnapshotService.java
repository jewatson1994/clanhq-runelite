package com.clanhq.verifier.service;

import com.clanhq.verifier.model.EvidenceSource;
import com.clanhq.verifier.model.DiaryProgress;
import com.clanhq.verifier.model.ObservedItem;
import com.clanhq.verifier.model.VerificationSnapshot;
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
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

public final class LocalPlayerSnapshotService
{
    private final Client client;
    @Inject
    public LocalPlayerSnapshotService(Client client)
    {
        this.client = client;
    }

    public VerificationSnapshot captureCompleteItemsEvidence()
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
            isPietyUnlocked(),
            client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1,
            client.getRealSkillLevel(Skill.HERBLORE),
            captureDiaryProgress(),
            com.clanhq.verifier.model.RaidKillCounts.unavailable("Not fetched"),
            client.getVarpValue(VarPlayerID.COLLECTION_COUNT))
            .withGrandmasterCombatAchievements(client.getVarbitValue(
                VarbitID.CA_TIER_STATUS_GRANDMASTER) > 0)
            .withAccountMetrics(captureBingoMetrics());
    }

    private Map<String, Integer> captureBingoMetrics()
    {
        Map<String, Integer> metrics = new LinkedHashMap<>();
        metrics.put("wintertodt_reward_points",
            Math.max(0, client.getVarbitValue(VarbitID.WINT_REWARD_POOL)));
        metrics.put("tempoross_reward_permits",
            Math.max(0, client.getVarbitValue(
                VarbitID.TEMPOROSS_REWARDPERMITS)));
        int elemental = Math.max(0,
            client.getVarpValue(VarPlayerID.TOTE_PRIMARY));
        int catalytic = Math.max(0,
            client.getVarpValue(VarPlayerID.TOTE_SECONDARY));
        metrics.put("gotr_reward_searches", Math.min(elemental, catalytic));
        return metrics;
    }

    private boolean isPietyUnlocked()
    {
        return hasPietyUnlock(Quest.KINGS_RANSOM.getState(client),
            client.getVarbitValue(VarbitID.KR_KNIGHTWAVES_STATE));
    }

    static boolean hasPietyUnlock(QuestState kingsRansom, int knightWavesState)
    {
        return kingsRansom == QuestState.FINISHED && knightWavesState >= 8;
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
            ObservedItem observedItem = new ObservedItem(
                item.getId(),
                itemName,
                item.getQuantity(),
                source);

            String key = source.name() + ':' + item.getId();
            ObservedItem existing = observedItems.get(key);
            if (existing == null)
            {
                observedItems.put(key, observedItem);
            }
            else
            {
                observedItems.put(key, new ObservedItem(
                    item.getId(),
                    itemName,
                    existing.getQuantity() + item.getQuantity(),
                    source));
            }
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
