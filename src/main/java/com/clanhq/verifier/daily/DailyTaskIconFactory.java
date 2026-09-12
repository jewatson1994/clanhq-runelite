package com.clanhq.verifier.daily;

import com.clanhq.verifier.daily.model.DailyTaskSummary;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import net.runelite.api.Skill;
import net.runelite.api.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

final class DailyTaskIconFactory
{
    private static final int FALLBACK_ICON_SIZE = 15;

    private DailyTaskIconFactory()
    {
    }

    static BufferedImage get(DailyTaskSummary task, ItemManager itemManager,
        SkillIconManager skillIconManager, SpriteManager spriteManager)
    {
        String category = task.getCategory();
        if (spriteManager != null && "ACTIVITIES".equalsIgnoreCase(category))
        {
            BufferedImage image = spriteManager.getSprite(
                SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES, 0);
            if (image != null)
            {
                return image;
            }
        }
        if (spriteManager != null && "PVM".equalsIgnoreCase(category))
        {
            BufferedImage image = spriteManager.getSprite(SpriteID.TAB_COMBAT, 0);
            if (image != null)
            {
                return image;
            }
        }
        if (skillIconManager != null && "SKILLING".equalsIgnoreCase(category))
        {
            Skill skill = findSkill(task.getName());
            if (skill != null)
            {
                BufferedImage image = skillIconManager.getSkillImage(skill);
                if (image != null)
                {
                    return image;
                }
            }
        }
        Integer itemId = task.getVerificationItemId();
        if (itemId == null && task.getVerificationItemIds() != null
            && !task.getVerificationItemIds().isEmpty())
        {
            itemId = task.getVerificationItemIds().get(0);
        }
        if (itemManager != null && itemId != null)
        {
            BufferedImage image = itemManager.getImage(itemId);
            if (image != null)
            {
                return image;
            }
        }
        return fallbackIcon(category);
    }

    static BufferedImage getInfoBoxIcon(DailyTaskSummary task,
        ItemManager itemManager, SkillIconManager skillIconManager)
    {
        String category = task.getCategory();
        if (skillIconManager != null && "SKILLING".equalsIgnoreCase(category))
        {
            Skill skill = findSkill(task.getName());
            if (skill != null)
            {
                BufferedImage image = skillIconManager.getSkillImage(skill);
                if (image != null)
                {
                    return image;
                }
            }
        }

        Integer itemId = task.getVerificationItemId();
        if (itemId == null && task.getVerificationItemIds() != null
            && !task.getVerificationItemIds().isEmpty())
        {
            itemId = task.getVerificationItemIds().get(0);
        }
        if (itemManager != null && itemId != null)
        {
            BufferedImage image = itemManager.getImage(itemId);
            if (image != null)
            {
                return image;
            }
        }
        return fallbackIcon(category);
    }

    static void loadInfoBoxIcon(DailyTaskSummary task, InfoBox infoBox,
        InfoBoxManager infoBoxManager, SpriteManager spriteManager)
    {
        if (spriteManager == null || infoBoxManager == null)
        {
            return;
        }

        String category = task.getCategory();
        int spriteId;
        if ("ACTIVITIES".equalsIgnoreCase(category))
        {
            spriteId = SpriteID.QUESTS_PAGE_ICON_RED_MINIGAMES;
        }
        else if ("PVM".equalsIgnoreCase(category))
        {
            spriteId = SpriteID.TAB_COMBAT;
        }
        else
        {
            return;
        }

        spriteManager.getSpriteAsync(spriteId, 0, image ->
        {
            infoBox.setImage(image);
            infoBoxManager.updateInfoBoxImage(infoBox);
        });
    }

    private static BufferedImage fallbackIcon(String category)
    {
        BufferedImage image = new BufferedImage(FALLBACK_ICON_SIZE,
            FALLBACK_ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor("PVM".equalsIgnoreCase(category)
            ? new Color(220, 92, 92) : "ACTIVITIES".equalsIgnoreCase(category)
                ? new Color(230, 180, 70) : new Color(108, 190, 102));
        graphics.fillOval(3, 3, 9, 9);
        graphics.dispose();
        return image;
    }

    private static Skill findSkill(String taskName)
    {
        String normalized = taskName == null ? ""
            : taskName.toLowerCase(Locale.ROOT);
        for (Skill skill : Skill.values())
        {
            if (normalized.contains(skill.getName().toLowerCase(Locale.ROOT)))
            {
                return skill;
            }
        }
        return null;
    }
}
