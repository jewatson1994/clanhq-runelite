package com.clanhq.verifier.rules;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;

public final class RankItemCatalog
{
    private final Set<Integer> relevantItemIds = new HashSet<>();
    private final List<String> relevantNameFragments = Arrays.asList(
        "ghommal's hilt", "barrows gloves", "warped sceptre", "trident",
        "sanguinesti", "tumeken's shadow", "zombie axe", "abyssal whip",
        "osmumten's fang", "ghrazi rapier", "blade of saeldor", "soulreaper axe",
        "scythe of vitur", "defender", "god cape", "fire cape", "infernal cape",
        "ava's assembler", "dizana's quiver", "fighter torso", "void knight",
        "void ranger helm", "elite void",
        "moon helm", "moon chestplate", "moon tassets", "moon spear",
        "dual macuahuitl", "eclipse atlatl", "imbued god cape", "imbued saradomin cape",
        "imbued zamorak cape", "imbued guthix cape", "zenyte", "anguish", "tormented bracelet",
        "suffering", "torture", "occult necklace", "primordial boots", "avernic treads",
        "bandos godsword", "dragon warhammer", "elder maul", "berserker ring",
        "archers ring", "seers ring", "warrior ring", "crystal", "bow of faerdhinen",
        "zulrah", "magic fang", "serpentine", "tanzanite fang", "quest point cape",
        "tormented synapse", "burning claws", "dragon claws", "voidwaker",
        "dragon hunter lance", "venator bow", "pegasian boots", "eternal boots",
        "amulet of rancour", "virtus", "ultor ring", "bellator ring", "magus ring",
        "venator ring", "godsword", "max cape", "ancestral", "masori", "torva",
        "oathplate", "twisted bow", "zaryte crossbow", "kodai", "nightmare staff",
        "inquisitor", "justiciar", "dinh's bulwark", "dragon hunter crossbow",
        "hill giant club", "prayer scroll", "twisted buckler", "lightbearer",
        "elidinis' ward", "zaryte vambraces", "nihil horn",
        "metamorphic dust", "sanguine dust", "sanguine ornament kit",
        "holy ornament kit", "saturated heart", "twisted ancestral colour kit",
        "champion's cape", "expert dragon archer");

    @Inject
    public RankItemCatalog()
    {
        relevantItemIds.addAll(OpalItemRequirements.allItemIds());
    }

    public boolean isRelevant(int itemId)
    {
        return relevantItemIds.contains(itemId);
    }

    public boolean isRelevant(int itemId, String itemName)
    {
        if (isRelevant(itemId))
        {
            return true;
        }
        String normalized = itemName.toLowerCase(Locale.ENGLISH);
        return relevantNameFragments.stream().anyMatch(normalized::contains);
    }
}
