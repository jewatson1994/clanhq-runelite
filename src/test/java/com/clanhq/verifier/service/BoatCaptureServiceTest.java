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
        addBoat(varbits, 1, 1, 201, 301, 401, 501);
        addBoat(varbits, 2, 2, 202, 302, 402, 502);
        Map<Integer, String> rows = new HashMap<>();
        rows.put(101, "Skiff");
        rows.put(102, "Sloop");
        for (int row : Arrays.asList(201, 202, 301, 302))
        {
            rows.put(row, "Rosewood component");
        }
        for (int row : Arrays.asList(401, 402, 501, 502))
        {
            rows.put(row, "Dragon component");
        }
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
                if (method.getReturnType().equals(boolean.class)) return false;
                if (method.getReturnType().equals(int.class)) return 0;
                return null;
            });

        BoatEvidence evidence = new BoatCaptureService(client)
            .captureVisiblePanel();

        assertEquals(2, evidence.getConfigurations().size());
        assertTrue(evidence.hasMaxedBoat("Skiff"));
        assertTrue(evidence.hasMaxedBoat("Sloop"));
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
