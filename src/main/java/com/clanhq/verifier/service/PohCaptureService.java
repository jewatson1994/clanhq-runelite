package com.clanhq.verifier.service;

import com.clanhq.verifier.model.PohEvidence;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.VarbitID;

public final class PohCaptureService
{
    private final Client client;

    @Inject
    public PohCaptureService(Client client)
    {
        this.client = client;
    }

    public PohEvidence capture()
    {
        if (client.getVarbitValue(VarbitID.POH_BUILDING_MODE) != 1)
        {
            throw new IllegalStateException(
                "Enter your own POH in build mode before capturing.");
        }
        WorldView worldView = client.getTopLevelWorldView();
        if (worldView == null)
        {
            throw new IllegalStateException("The POH scene is not loaded.");
        }
        Scene scene = worldView.getScene();

        Set<String> facilities = new LinkedHashSet<>();
        for (Tile[][] plane : scene.getTiles())
        {
            for (Tile[] column : plane)
            {
                for (Tile tile : column)
                {
                    inspectTile(tile, facilities);
                }
            }
        }
        return new PohEvidence(true, facilities);
    }

    private void inspectTile(Tile tile, Set<String> facilities)
    {
        if (tile == null)
        {
            return;
        }
        for (GameObject object : tile.getGameObjects())
        {
            if (object != null) inspect(object.getId(), facilities);
        }
        DecorativeObject decorative = tile.getDecorativeObject();
        WallObject wall = tile.getWallObject();
        GroundObject ground = tile.getGroundObject();
        if (decorative != null) inspect(decorative.getId(), facilities);
        if (wall != null) inspect(wall.getId(), facilities);
        if (ground != null) inspect(ground.getId(), facilities);
    }

    private void inspect(int objectId, Set<String> facilities)
    {
        String name = client.getObjectDefinition(objectId).getName()
            .toLowerCase(Locale.ENGLISH);
        if (name.contains("spiritual fairy")) facilities.add(PohEvidence.SPIRITUAL_FAIRY);
        if (name.contains("ornate jewellery box")) facilities.add(PohEvidence.JEWELLERY_BOX);
        if (name.contains("occult altar")) facilities.add(PohEvidence.OCCULT_ALTAR);
        if (name.contains("crystalline portal nexus")) facilities.add(PohEvidence.PORTAL_NEXUS);
        if (name.contains("ornate rejuvenation pool")) facilities.add(PohEvidence.REJUVENATION_POOL);
    }
}
