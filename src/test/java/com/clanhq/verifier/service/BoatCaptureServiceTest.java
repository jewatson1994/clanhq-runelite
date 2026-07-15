package com.clanhq.verifier.service;

import com.clanhq.verifier.model.BoatEvidence;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BoatCaptureServiceTest
{
    @Test
    public void identifiesBoatTypesFromSelectionPanelSizes()
    {
        Set<String> skiff = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Boat Customisation", "Boat Size: Small (2x5)"));
        Set<String> sloop = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Boat Customisation", "Boat Size: Medium (3x10)"));

        assertTrue(skiff.contains("Skiff"));
        assertFalse(skiff.contains("Sloop"));
        assertTrue(sloop.contains("Sloop"));
        assertFalse(sloop.contains("Skiff"));
    }

    @Test
    public void stillIdentifiesExplicitBoatNames()
    {
        Set<String> types = BoatCaptureService.inferBoatTypes(Arrays.asList(
            "Skiff", "Sloop"));

        assertTrue(types.contains("Skiff"));
        assertTrue(types.contains("Sloop"));
    }

    @Test
    public void capturesAuthoritativeOwnedBoatConfigurations()
    {
        Map<Integer, Integer> varbits = new HashMap<>();
        addBoat(varbits, 1, 1, 0, 0, 0, 0);
        addBoat(varbits, 2, 2, 6, 6, 6, 6);
        Map<Integer, String> rows = new HashMap<>();
        rows.put(101, "Skiff");
        rows.put(102, "Sloop");
        rows.put(200, "Wooden hull");
        rows.put(206, "Rosewood hull");
        rows.put(300, "Wooden mast and linen sails");
        rows.put(306, "Rosewood mast and cotton sails");
        rows.put(400, "Bronze helm");
        rows.put(406, "Dragon helm");
        rows.put(500, "Bronze keel");
        rows.put(506, "Dragon keel");
        Client client = (Client) Proxy.newProxyInstance(
            Client.class.getClassLoader(), new Class<?>[] {Client.class},
            (proxy, method, arguments) ->
            {
                if (method.getName().equals("getVarbitValue"))
                {
                    return varbits.getOrDefault((Integer) arguments[0], 0);
                }
                if (method.getName().equals("getDBTableField"))
                {
                    String value = rows.get((Integer) arguments[0]);
                    return value == null ? null : new Object[] {value};
                }
                if (method.getName().equals("getDBRowsByValue"))
                {
                    int type = (Integer) arguments[3];
                    return type == 1 ? Arrays.asList(101)
                        : type == 2 ? Arrays.asList(102)
                        : java.util.Collections.emptyList();
                }
                if (method.getName().equals("getDBTableRows"))
                {
                    int table = (Integer) arguments[0];
                    int start = table == net.runelite.api.gameval.DBTableID.SailingBoatHull.ID
                        ? 200
                        : table == net.runelite.api.gameval.DBTableID.SailingBoatSail.ID
                            ? 300
                            : table == net.runelite.api.gameval.DBTableID.SailingBoatSteering.ID
                                ? 400 : 500;
                    java.util.List<Integer> tableRows = new java.util.ArrayList<>();
                    for (int index = 0; index < 7; index++)
                    {
                        tableRows.add(start + index);
                    }
                    return tableRows;
                }
                if (method.getReturnType().equals(boolean.class)) return false;
                if (method.getReturnType().equals(int.class)) return 0;
                return null;
            });

        BoatEvidence evidence = new BoatCaptureService(client)
            .captureVisiblePanel();

        assertEquals(2, evidence.getConfigurations().size());
        assertFalse(evidence.hasMaxedBoat("Skiff"));
        assertTrue(evidence.hasMaxedBoat("Sloop"));
        assertEquals("Wooden hull",
            evidence.getConfigurations().get(0).getHull());
    }

    private static void addBoat(Map<Integer, Integer> values, int slot,
        int type, int hull, int sail, int steering, int keel)
    {
        int[][] ids = {
            {VarbitID.SAILING_BOAT_1_OWNED, VarbitID.SAILING_BOAT_1_TYPE,
                VarbitID.SAILING_BOAT_1_HULL, VarbitID.SAILING_BOAT_1_SAIL,
                VarbitID.SAILING_BOAT_1_STEERING, VarbitID.SAILING_BOAT_1_KEEL},
            {VarbitID.SAILING_BOAT_2_OWNED, VarbitID.SAILING_BOAT_2_TYPE,
                VarbitID.SAILING_BOAT_2_HULL, VarbitID.SAILING_BOAT_2_SAIL,
                VarbitID.SAILING_BOAT_2_STEERING, VarbitID.SAILING_BOAT_2_KEEL}
        };
        int[] target = ids[slot - 1];
        values.put(target[0], 1);
        values.put(target[1], type);
        values.put(target[2], hull);
        values.put(target[3], sail);
        values.put(target[4], steering);
        values.put(target[5], keel);
    }
}
