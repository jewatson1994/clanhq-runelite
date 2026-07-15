package com.clanhq.verifier.service;

import com.clanhq.verifier.model.CollectionLogEvidence;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

public final class CollectionLogCaptureService
{
    private final Client client;

    @Inject
    public CollectionLogCaptureService(Client client)
    {
        this.client = client;
    }

    public CollectionLogEvidence captureVisiblePage()
    {
        Widget frame = client.getWidget(InterfaceID.Collection.FRAME);
        Widget header = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
        Widget items = client.getWidget(InterfaceID.Collection.ITEMS);
        if (frame == null || frame.isHidden() || header == null || items == null)
        {
            throw new IllegalStateException(
                "Open the Collection Log to the required page before capturing.");
        }
        String pageTitle = clean(header.getText());
        if (!isKnownPageTitle(pageTitle))
        {
            throw new IllegalStateException(
                "Open a supported Doom or raid Collection Log page.");
        }

        PageScan scan = new PageScan();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        scan(items, visited, scan);
        if (scan.visibleItemCount == 0)
        {
            throw new IllegalStateException(
                "The Collection Log page has not finished loading.");
        }
        return new CollectionLogEvidence(Collections.singletonMap(
            pageTitle, scan.items));
    }

    private void scan(Widget widget, Set<Widget> visited, PageScan scan)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        if (widget.getItemId() > 0)
        {
            scan.visibleItemCount++;
            String name = client.getItemDefinition(widget.getItemId()).getName();
            int quantity = Math.max(widget.getItemQuantity(), 0);
            if (quantity > 0 && widget.getOpacity() < 100)
            {
                scan.items.merge(name, quantity, Math::max);
            }
        }
        scanChildren(widget.getChildren(), visited, scan);
        scanChildren(widget.getDynamicChildren(), visited, scan);
        scanChildren(widget.getStaticChildren(), visited, scan);
        scanChildren(widget.getNestedChildren(), visited, scan);
    }

    private void scanChildren(Widget[] children, Set<Widget> visited, PageScan scan)
    {
        if (children == null) return;
        for (Widget child : children) scan(child, visited, scan);
    }

    private static boolean isKnownPageTitle(String text)
    {
        String normalized = text.toLowerCase();
        return normalized.contains("doom")
            || normalized.equals("chambers of xeric")
            || normalized.equals("theatre of blood")
            || normalized.equals("tombs of amascut");
    }

    private static String clean(String text)
    {
        return text == null ? "" : text.replaceAll("<[^>]*>", "").trim();
    }

    private static final class PageScan
    {
        private int visibleItemCount;
        private final Map<String, Integer> items = new LinkedHashMap<>();
    }
}
