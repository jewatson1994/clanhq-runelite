package com.clanhq.verifier.service;

import com.clanhq.verifier.model.BoatEvidence;
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
import net.runelite.api.widgets.Widget;

public final class BoatCaptureService
{
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
        if (text.isEmpty())
        {
            throw new IllegalStateException(
                "Open Sailing boat selection or customisation before capturing.");
        }

        return new BoatEvidence(inferBoatTypes(text), new ArrayList<>(text));
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
