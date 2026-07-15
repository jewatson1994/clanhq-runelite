package com.clanhq.verifier.service;

import com.clanhq.verifier.model.BoatEvidence;
import com.clanhq.verifier.model.BoatConfiguration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

public final class BoatCaptureService
{
    private static final int[] OWNED = {VarbitID.SAILING_BOAT_1_OWNED,
        VarbitID.SAILING_BOAT_2_OWNED, VarbitID.SAILING_BOAT_3_OWNED,
        VarbitID.SAILING_BOAT_4_OWNED, VarbitID.SAILING_BOAT_5_OWNED};
    private static final int[] TYPES = {VarbitID.SAILING_BOAT_1_TYPE,
        VarbitID.SAILING_BOAT_2_TYPE, VarbitID.SAILING_BOAT_3_TYPE,
        VarbitID.SAILING_BOAT_4_TYPE, VarbitID.SAILING_BOAT_5_TYPE};
    private static final int[] HULLS = {VarbitID.SAILING_BOAT_1_HULL,
        VarbitID.SAILING_BOAT_2_HULL, VarbitID.SAILING_BOAT_3_HULL,
        VarbitID.SAILING_BOAT_4_HULL, VarbitID.SAILING_BOAT_5_HULL};
    private static final int[] SAILS = {VarbitID.SAILING_BOAT_1_SAIL,
        VarbitID.SAILING_BOAT_2_SAIL, VarbitID.SAILING_BOAT_3_SAIL,
        VarbitID.SAILING_BOAT_4_SAIL, VarbitID.SAILING_BOAT_5_SAIL};
    private static final int[] STEERING = {VarbitID.SAILING_BOAT_1_STEERING,
        VarbitID.SAILING_BOAT_2_STEERING, VarbitID.SAILING_BOAT_3_STEERING,
        VarbitID.SAILING_BOAT_4_STEERING, VarbitID.SAILING_BOAT_5_STEERING};
    private static final int[] KEELS = {VarbitID.SAILING_BOAT_1_KEEL,
        VarbitID.SAILING_BOAT_2_KEEL, VarbitID.SAILING_BOAT_3_KEEL,
        VarbitID.SAILING_BOAT_4_KEEL, VarbitID.SAILING_BOAT_5_KEEL};
    private final Client client;

    @Inject
    public BoatCaptureService(Client client)
    {
        this.client = client;
    }

    public BoatEvidence captureVisiblePanel()
    {
        Set<String> text = new LinkedHashSet<>();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectVisibleInterface(
            client.getWidget(InterfaceID.SailingBoatSelection.FRAME),
            visited, text);
        collectVisibleInterface(
            client.getWidget(InterfaceID.SailingCustomisation.FRAME),
            visited, text);
        List<BoatConfiguration> configurations = captureOwnedBoats();
        if (text.isEmpty() && configurations.isEmpty())
        {
            throw new IllegalStateException(
                "No owned Sailing boat records were available. Open Boat "
                    + "Customisation and try again.");
        }

        return new BoatEvidence(inferBoatTypes(text), configurations,
            new ArrayList<>(text));
    }

    public BoatEvidence captureStoredEvidence()
    {
        List<BoatConfiguration> configurations = captureOwnedBoats();
        return configurations.isEmpty()
            ? BoatEvidence.notCaptured()
            : new BoatEvidence(Collections.emptySet(), configurations,
                Collections.emptyList());
    }

    private List<BoatConfiguration> captureOwnedBoats()
    {
        List<BoatConfiguration> boats = new ArrayList<>();
        for (int index = 0; index < OWNED.length; index++)
        {
            if (client.getVarbitValue(OWNED[index]) <= 0)
            {
                continue;
            }
            boats.add(new BoatConfiguration(index + 1,
                readBoatType(client.getVarbitValue(TYPES[index])),
                readComponent(client.getVarbitValue(HULLS[index]),
                    DBTableID.SailingBoatHull.ID,
                    DBTableID.SailingBoatHull.COL_NAME, "hull"),
                readComponent(client.getVarbitValue(SAILS[index]),
                    DBTableID.SailingBoatSail.ID,
                    DBTableID.SailingBoatSail.COL_NAME, "mast and sails"),
                readComponent(client.getVarbitValue(STEERING[index]),
                    DBTableID.SailingBoatSteering.ID,
                    DBTableID.SailingBoatSteering.COL_NAME, "helm"),
                readComponent(client.getVarbitValue(KEELS[index]),
                    DBTableID.SailingBoatKeel.ID,
                    DBTableID.SailingBoatKeel.COL_NAME, "keel")));
        }
        return boats;
    }

    private String readBoatType(int rowId)
    {
        List<Integer> rows;
        try
        {
            rows = client.getDBRowsByValue(DBTableID.SailingBoat.ID,
                DBTableID.SailingBoat.COL_TYPE_ID, 0, rowId);
        }
        catch (RuntimeException exception)
        {
            rows = Collections.emptyList();
        }
        int resolvedRow = rows == null || rows.isEmpty() ? rowId : rows.get(0);
        String name = readDatabaseName(resolvedRow,
            DBTableID.SailingBoat.COL_DISPLAYNAME);
        if (name == null)
        {
            name = readDatabaseName(resolvedRow,
                DBTableID.SailingBoat.COL_NAME);
        }
        return name == null ? "Unknown boat row " + rowId : name;
    }

    private String readComponent(int encodedValue, int tableId,
        int nameColumn, String label)
    {
        String name = null;
        List<Integer> rows;
        try
        {
            rows = client.getDBTableRows(tableId);
        }
        catch (RuntimeException exception)
        {
            rows = Collections.emptyList();
        }
        if (rows != null && encodedValue >= 0 && encodedValue < rows.size())
        {
            name = readDatabaseName(rows.get(encodedValue), nameColumn);
        }
        if (name == null)
        {
            name = readDatabaseName(encodedValue, nameColumn);
        }
        return name == null ? "Unknown " + label + " value " + encodedValue
            : name;
    }

    private String readDatabaseName(int rowId, int nameColumn)
    {
        Object[] values;
        try
        {
            values = client.getDBTableField(rowId, nameColumn, 0);
        }
        catch (RuntimeException exception)
        {
            return null;
        }
        if (values == null)
        {
            return null;
        }
        for (Object value : values)
        {
            if (value instanceof String && !((String) value).trim().isEmpty())
            {
                return ((String) value).replaceAll("<[^>]*>", "").trim();
            }
        }
        return null;
    }

    static Set<String> inferBoatTypes(Iterable<String> text)
    {
        Set<String> types = new LinkedHashSet<>();
        for (String line : text)
        {
            String normalized = line.toLowerCase(Locale.ENGLISH);
            if (normalized.contains("skiff")
                || normalized.matches(".*boat size:\\s*small(?:\\s|\\().*"))
            {
                types.add("Skiff");
            }
            if (normalized.contains("sloop")
                || normalized.matches(".*boat size:\\s*medium(?:\\s|\\().*"))
            {
                types.add("Sloop");
            }
        }
        return types;
    }

    private void collectVisibleInterface(Widget root, Set<Widget> visited,
        Set<String> text)
    {
        if (root == null || root.isHidden())
        {
            return;
        }
        collectText(root, visited, text);
    }

    private void collectText(Widget widget, Set<Widget> visited, Set<String> text)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        addText(text, widget.getText());
        addText(text, widget.getName());
        collectChildren(widget.getChildren(), visited, text);
        collectChildren(widget.getDynamicChildren(), visited, text);
        collectChildren(widget.getStaticChildren(), visited, text);
        collectChildren(widget.getNestedChildren(), visited, text);
    }

    private void collectChildren(Widget[] children, Set<Widget> visited,
        Set<String> text)
    {
        if (children == null)
        {
            return;
        }
        for (Widget child : children)
        {
            collectText(child, visited, text);
        }
    }

    private static void addText(Set<String> text, String value)
    {
        String clean = value == null ? ""
            : value.replaceAll("<[^>]*>", "").trim();
        if (!clean.isEmpty() && !clean.equalsIgnoreCase("null"))
        {
            text.add(clean);
        }
    }
}
