package com.clanhq.verifier.daily;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * Resolves the XP-drop tile that completes each standard agility course.
 * Finish coordinates mirror RuneLite's built-in Agility Courses list.
 */
final class AgilityCourseCompletion
{
    private static final Map<WorldPoint, String> COURSE_ENDS;

    static
    {
        Map<WorldPoint, String> ends = new HashMap<>();
        add(ends, "Gnome", 2484, 3437, 0);
        add(ends, "Gnome", 2487, 3437, 0);
        add(ends, "Shayzien Basic", 1554, 3640, 0);
        add(ends, "Draynor Village", 3103, 3261, 0);
        add(ends, "Al Kharid", 3299, 3194, 0);
        add(ends, "Agility Pyramid", 3364, 2830, 0);
        add(ends, "Varrock", 3236, 3417, 0);
        add(ends, "Penguin", 2652, 4039, 1);
        add(ends, "Barbarian Outpost", 2543, 3553, 0);
        add(ends, "Canifis", 3510, 3485, 0);
        add(ends, "Ape Atoll", 2770, 2747, 0);
        add(ends, "Shayzien Advanced", 1522, 3625, 0);
        for (int y = 3332; y <= 3335; y++)
        {
            add(ends, "Falador", 3029, y, 0);
        }
        for (int x = 2993; x <= 2995; x++)
        {
            add(ends, "Wilderness", x, 3933, 0);
        }
        add(ends, "Werewolf", 3528, 9873, 0);
        add(ends, "Seers' Village", 2704, 3464, 0);
        add(ends, "Pollnivneach", 3363, 2998, 0);
        add(ends, "Rellekka", 2653, 3676, 0);
        add(ends, "Prifddinas", 3240, 6109, 0);
        add(ends, "Ardougne", 2668, 3297, 0);
        COURSE_ENDS = Collections.unmodifiableMap(ends);
    }

    private AgilityCourseCompletion()
    {
    }

    private static void add(Map<WorldPoint, String> ends, String course,
        int x, int y, int plane)
    {
        ends.put(new WorldPoint(x, y, plane), course);
    }

    static String courseAt(WorldPoint location)
    {
        return COURSE_ENDS.get(location);
    }
}
