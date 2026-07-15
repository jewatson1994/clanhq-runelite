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
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;

public final class LocalPlayerSnapshotService
{
    private final Client client;
    private final RankItemCatalog rankItemCatalog;
    private final Map<String, ObservedItem> observedItems =
        new LinkedHashMap<>();
    private boolean captureActive;
    private boolean bankEvidenceCaptured;
    private boolean pietyObserved;
    private boolean rigourObserved;
    private boolean deadeyeObserved;
    private boolean mysticVigourObserved;
    private String sessionRsn;

    @Inject
    public LocalPlayerSnapshotService(
        Client client,
        RankItemCatalog rankItemCatalog)
    {
        this.client = client;
        this.rankItemCatalog = rankItemCatalog;
    }

    public void startCaptureSession()
    {
        Player player = requireLoggedInPlayer();

        observedItems.clear();
        captureActive = true;
        bankEvidenceCaptured = false;
        pietyObserved = false;
        rigourObserved = false;
        deadeyeObserved = false;
        mysticVigourObserved = false;
        sessionRsn = player.getName();

        observeCurrentState();

        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank != null)
        {
            addItems(bank, EvidenceSource.BANK);
            bankEvidenceCaptured = true;
        }
    }

    public VerificationSnapshot captureAccountEvidence()
    {
        Player player = requireLoggedInPlayer();
        return new VerificationSnapshot(
            player.getName(),
            client.getTotalLevel(),
            player.getCombatLevel(),
            Collections.emptyList(),
            false,
            client.getVarbitValue(VarbitID.PRAYER_PIETY) == 1,
            client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1,
            client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1,
            client.getRealSkillLevel(Skill.HERBLORE),
            captureDiaryProgress(),
            com.clanhq.verifier.model.RaidKillCounts.unavailable("Not fetched"),
            client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
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
        rigourObserved = rigourObserved
            || client.getVarbitValue(VarbitID.PRAYER_RIGOUR_UNLOCKED) == 1;
        deadeyeObserved = deadeyeObserved
            || client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) == 1;
        mysticVigourObserved = mysticVigourObserved
            || client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) == 1;
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
            pietyObserved,
            rigourObserved,
            deadeyeObserved,
            mysticVigourObserved,
            client.getRealSkillLevel(Skill.HERBLORE),
            captureDiaryProgress(),
            com.clanhq.verifier.model.RaidKillCounts.unavailable("Not fetched"),
            client.getVarpValue(VarPlayerID.COLLECTION_COUNT));
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
        rigourObserved = false;
        deadeyeObserved = false;
        mysticVigourObserved = false;
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
