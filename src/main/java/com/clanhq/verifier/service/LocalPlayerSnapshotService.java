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
import net.runelite.api.EquipmentInventorySlot;
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
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;

public final class LocalPlayerSnapshotService
{
    private final Client client;
    private final ItemManager itemManager;

    @Inject
    public LocalPlayerSnapshotService(Client client, ItemManager itemManager)
    {
        this.client = client;
        this.itemManager = itemManager;
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
        for (Skill skill : Skill.values())
        {
            metrics.put("skill_" + skill.name().toLowerCase(),
                Math.max(1, client.getRealSkillLevel(skill)));
        }
        for (Quest quest : Quest.values())
        {
            metrics.put("quest_" + quest.name().toLowerCase(),
                quest.getState(client) == QuestState.FINISHED ? 1 : 0);
        }
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
            Map<String, Object> equipment = equipmentMetadata(item.getId());
            ObservedItem observedItem = new ObservedItem(
                item.getId(),
                itemName,
                item.getQuantity(),
                source,
                equipment);

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
                    source,
                    existing.getEquipment().isEmpty()
                        ? equipment : existing.getEquipment()));
            }
        }
    }

    private Map<String, Object> equipmentMetadata(int itemId)
    {
        if (itemManager == null)
        {
            return Collections.emptyMap();
        }
        ItemStats stats;
        try
        {
            stats = itemManager.getItemStats(itemId);
        }
        catch (RuntimeException ignored)
        {
            return Collections.emptyMap();
        }
        if (stats == null || !stats.isEquipable() || stats.getEquipment() == null)
        {
            return Collections.emptyMap();
        }
        ItemEquipmentStats equipment = stats.getEquipment();
        String slot = equipmentSlot(equipment.getSlot());
        if (slot == null)
        {
            return Collections.emptyMap();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("slot", slot);
        values.put("two_handed", equipment.isTwoHanded());
        values.put("attack_stab", equipment.getAstab());
        values.put("attack_slash", equipment.getAslash());
        values.put("attack_crush", equipment.getAcrush());
        values.put("attack_magic", equipment.getAmagic());
        values.put("attack_ranged", equipment.getArange());
        values.put("defence_stab", equipment.getDstab());
        values.put("defence_slash", equipment.getDslash());
        values.put("defence_crush", equipment.getDcrush());
        values.put("defence_magic", equipment.getDmagic());
        values.put("defence_ranged", equipment.getDrange());
        values.put("strength", equipment.getStr());
        values.put("ranged_strength", equipment.getRstr());
        values.put("magic_damage", equipment.getMdmg());
        values.put("prayer", equipment.getPrayer());
        values.put("attack_speed", equipment.getAspeed());
        return values;
    }

    private static String equipmentSlot(int slot)
    {
        for (EquipmentInventorySlot value : EquipmentInventorySlot.values())
        {
            if (value.getSlotIdx() != slot)
            {
                continue;
            }
            switch (value)
            {
                case HEAD: return "HEAD";
                case CAPE: return "CAPE";
                case AMULET: return "NECK";
                case WEAPON: return "WEAPON";
                case BODY: return "BODY";
                case SHIELD: return "SHIELD";
                case LEGS: return "LEGS";
                case GLOVES: return "HANDS";
                case BOOTS: return "FEET";
                case RING: return "RING";
                case AMMO: return "AMMO";
                default: return null;
            }
        }
        return null;
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
