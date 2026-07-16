package com.clanhq.verifier.service;

import com.clanhq.verifier.model.CollectionLogEvidence;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

public final class CollectionLogCaptureService
{
    private static final Pattern SLOT_FRACTION = Pattern.compile(
        "([0-9][0-9,]*)\\s*(?:/|of)\\s*[0-9][0-9,]*",
        Pattern.CASE_INSENSITIVE);
    private final Client client;

    @Inject
    public CollectionLogCaptureService(Client client)
    {
        this.client = client;
    }

    public CollectionLogEvidence capturePage(String expectedPageTitle)
    {
        Widget frame = client.getWidget(InterfaceID.Collection.FRAME);
        Widget header = client.getWidget(InterfaceID.Collection.HEADER_TEXT);
        Widget headerContainer = client.getWidget(InterfaceID.Collection.HEADER);
        Widget main = client.getWidget(InterfaceID.Collection.MAIN);
        Widget itemsContainer = client.getWidget(
            InterfaceID.Collection.ITEMS_CONTENTS);
        Widget items = client.getWidget(InterfaceID.Collection.ITEMS);
        if (frame == null || frame.isHidden()
            || (itemsContainer == null && items == null))
        {
            throw new IllegalStateException(
                "Open the Collection Log to the required page before capturing.");
        }
        String pageTitle = firstKnownPageTitle(header, headerContainer, main);
        if (pageTitle == null)
        {
            throw new IllegalStateException(
                "Open a supported Doom or raid Collection Log page.");
        }
        if (!pageTitle.equals(expectedPageTitle))
        {
            throw new IllegalStateException("Open the " + expectedPageTitle
                + " Collection Log page before capturing.");
        }

        PageScan scan = new PageScan();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        scan(itemsContainer, visited, scan);
        scan(items, visited, scan);
        if (scan.visibleItemCount == 0)
        {
            throw new IllegalStateException(
                "The Collection Log page has not finished loading.");
        }
        CollectionLogEvidence page = CollectionLogEvidence.page(pageTitle,
            scan.items, scan.acquiredSlotCount, scan.visibleItemCount);
        Integer obtainedSlots = parseObtainedSlots(readVisibleText(frame));
        return obtainedSlots == null ? page : page.merge(
            CollectionLogEvidence.slotCount(obtainedSlots));
    }

    private String firstKnownPageTitle(Widget... candidates)
    {
        for (Widget candidate : candidates)
        {
            if (candidate == null || candidate.isHidden())
            {
                continue;
            }
            String title = knownPageTitle(readVisibleText(candidate));
            if (title != null)
            {
                return title;
            }
        }
        return null;
    }

    private String readVisibleText(Widget root)
    {
        StringBuilder text = new StringBuilder();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        appendVisibleText(root, visited, text);
        return text.toString();
    }

    private void appendVisibleText(Widget widget, Set<Widget> visited,
        StringBuilder text)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        text.append(' ').append(clean(widget.getText()));
        text.append(' ').append(clean(widget.getName()));
        appendChildrenText(widget.getChildren(), visited, text);
        appendChildrenText(widget.getDynamicChildren(), visited, text);
        appendChildrenText(widget.getStaticChildren(), visited, text);
        appendChildrenText(widget.getNestedChildren(), visited, text);
    }

    private void appendChildrenText(Widget[] children, Set<Widget> visited,
        StringBuilder text)
    {
        if (children == null)
        {
            return;
        }
        for (Widget child : children)
        {
            appendVisibleText(child, visited, text);
        }
    }

    private void scan(Widget widget, Set<Widget> visited, PageScan scan)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        if (widget.getItemId() > 0)
        {
            String name = client.getItemDefinition(widget.getItemId()).getName();
            if (!isExcludedGreenLogSlot(name))
            {
                scan.visibleItemCount++;
                int quantity = Math.max(widget.getItemQuantity(), 0);
                if (quantity > 0 && widget.getOpacity() < 100)
                {
                    scan.acquiredSlotCount++;
                    scan.items.merge(name, quantity, Math::max);
                }
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

    static String knownPageTitle(String text)
    {
        String normalized = text.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("doom of mokhaiotl"))
        {
            return "Doom of Mokhaiotl";
        }
        if (normalized.contains("yama"))
        {
            return "Yama";
        }
        if (normalized.contains("chambers of xeric"))
        {
            return "Chambers of Xeric";
        }
        if (normalized.contains("theatre of blood"))
        {
            return "Theatre of Blood";
        }
        if (normalized.contains("tombs of amascut"))
        {
            return "Tombs of Amascut";
        }
        return null;
    }

    static Integer parseObtainedSlots(String text)
    {
        Matcher matcher = SLOT_FRACTION.matcher(text);
        Integer largest = null;
        while (matcher.find())
        {
            int value = Integer.parseInt(matcher.group(1).replace(",", ""));
            if (largest == null || value > largest)
            {
                largest = value;
            }
        }
        return largest;
    }

    static boolean isExcludedGreenLogSlot(String itemName)
    {
        String normalized = itemName.toLowerCase(Locale.ENGLISH);
        return normalized.contains("shroud")
            || normalized.startsWith("xeric's ");
    }

    private static String clean(String text)
    {
        return text == null ? "" : text.replaceAll("<[^>]*>", "").trim();
    }

    private static final class PageScan
    {
        private int visibleItemCount;
        private int acquiredSlotCount;
        private final Map<String, Integer> items = new LinkedHashMap<>();
    }
}
