package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Locale;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

final class DailyTaskInfoBox extends InfoBox
{
    private static final NumberFormat NUMBERS =
        NumberFormat.getIntegerInstance(Locale.US);

    private DailyTaskSummary task;
    private int progress;

    DailyTaskInfoBox(DailyTaskSummary task, int progress, Plugin plugin,
        ItemManager itemManager, SkillIconManager skillIconManager,
        SpriteManager spriteManager, InfoBoxManager infoBoxManager)
    {
        super(DailyTaskIconFactory.getInfoBoxIcon(task, itemManager,
            skillIconManager), plugin);
        update(task, progress);
        DailyTaskIconFactory.loadInfoBoxIcon(task, this, infoBoxManager,
            spriteManager);
    }

    void update(DailyTaskSummary task, int progress)
    {
        this.task = task;
        this.progress = progress;
        setTooltip(tooltip(task, progress));
    }

    @Override
    public String getText()
    {
        return isComplete() ? "\u2713" : compact(progress);
    }

    @Override
    public Color getTextColor()
    {
        return isComplete() ? new Color(67, 190, 117) : Color.WHITE;
    }

    private boolean isComplete()
    {
        return task.isCompleted() || progress >= task.getTarget();
    }

    private static String tooltip(DailyTaskSummary task, int progress)
    {
        String name = task.getName() == null ? "" : task.getName();
        String category = titleCase(task.getCategory());
        String tier = task.isDailyDrop() && task.getTier() != null
            && !task.getTier().trim().isEmpty() ? titleCase(task.getTier()) : "";
        String status = task.isCompleted() || progress >= task.getTarget()
            ? "Complete" : NUMBERS.format(progress) + " / "
                + NUMBERS.format(task.getTarget());
        String prefix = tier.isEmpty() ? category : tier + " " + category;
        return prefix + " - " + name + " - " + status;
    }

    private static String compact(int value)
    {
        if (value >= 1_000_000)
        {
            return String.format(Locale.US, "%.1fM", value / 1_000_000.0)
                .replace(".0M", "M");
        }
        if (value >= 1_000)
        {
            return String.format(Locale.US, "%.1fK", value / 1_000.0)
                .replace(".0K", "K");
        }
        return NUMBERS.format(value);
    }

    private static String titleCase(String value)
    {
        if (value == null || value.isEmpty())
        {
            return "Task";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

}
