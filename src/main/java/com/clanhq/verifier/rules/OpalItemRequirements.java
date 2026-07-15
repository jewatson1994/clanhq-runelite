package com.clanhq.verifier.rules;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.ItemID;

@SuppressWarnings("deprecation")
public final class OpalItemRequirements
{
    private static final Set<Integer> HILT_IDS = ids(
        ItemID.GHOMMALS_HILT_2,
        ItemID.GHOMMALS_HILT_3,
        ItemID.GHOMMALS_HILT_4,
        ItemID.GHOMMALS_HILT_5,
        ItemID.GHOMMALS_HILT_6);

    private static final Set<Integer> BARROWS_GLOVES_IDS = ids(
        ItemID.BARROWS_GLOVES,
        ItemID.BARROWS_GLOVES_WRAPPED);

    private static final Set<Integer> POWERED_STAFF_IDS = ids(
        ItemID.WARPED_SCEPTRE,
        ItemID.WARPED_SCEPTRE_UNCHARGED,
        ItemID.TRIDENT_OF_THE_SEAS,
        ItemID.TRIDENT_OF_THE_SEAS_FULL,
        ItemID.TRIDENT_OF_THE_SEAS_E,
        ItemID.TRIDENT_OF_THE_SWAMP,
        ItemID.TRIDENT_OF_THE_SWAMP_E,
        ItemID.SANGUINESTI_STAFF,
        ItemID.SANGUINESTI_STAFF_UNCHARGED,
        ItemID.HOLY_SANGUINESTI_STAFF,
        ItemID.HOLY_SANGUINESTI_STAFF_UNCHARGED,
        ItemID.TUMEKENS_SHADOW,
        ItemID.TUMEKENS_SHADOW_UNCHARGED);

    private static final Set<Integer> MELEE_WEAPON_IDS = ids(
        ItemID.ZOMBIE_AXE,
        ItemID.ABYSSAL_WHIP,
        ItemID.ABYSSAL_WHIP_OR,
        ItemID.ABYSSAL_TENTACLE,
        ItemID.ABYSSAL_TENTACLE_OR,
        ItemID.OSMUMTENS_FANG,
        ItemID.OSMUMTENS_FANG_OR,
        ItemID.GHRAZI_RAPIER,
        ItemID.HOLY_GHRAZI_RAPIER,
        ItemID.BLADE_OF_SAELDOR,
        ItemID.BLADE_OF_SAELDOR_INACTIVE,
        ItemID.BLADE_OF_SAELDOR_C,
        ItemID.SOULREAPER_AXE,
        ItemID.SOULREAPER_AXE_O,
        ItemID.SCYTHE_OF_VITUR,
        ItemID.SCYTHE_OF_VITUR_UNCHARGED,
        ItemID.HOLY_SCYTHE_OF_VITUR,
        ItemID.HOLY_SCYTHE_OF_VITUR_UNCHARGED,
        ItemID.SANGUINE_SCYTHE_OF_VITUR,
        ItemID.SANGUINE_SCYTHE_OF_VITUR_UNCHARGED);

    private static final Set<Integer> DEFENDER_IDS = ids(
        ItemID.DRAGON_DEFENDER,
        ItemID.DRAGON_DEFENDER_T,
        ItemID.DRAGON_DEFENDER_L,
        ItemID.DRAGON_DEFENDER_LT,
        ItemID.AVERNIC_DEFENDER,
        ItemID.AVERNIC_DEFENDER_L,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_5,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_5_L,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_6,
        ItemID.GHOMMALS_AVERNIC_DEFENDER_6_L);

    private static final Set<Integer> ALL_ITEM_IDS = buildAllItemIds();

    private OpalItemRequirements()
    {
    }

    public static Set<Integer> hiltIds()
    {
        return HILT_IDS;
    }

    public static Set<Integer> barrowsGlovesIds()
    {
        return BARROWS_GLOVES_IDS;
    }

    public static Set<Integer> poweredStaffIds()
    {
        return POWERED_STAFF_IDS;
    }

    public static Set<Integer> meleeWeaponIds()
    {
        return MELEE_WEAPON_IDS;
    }

    public static Set<Integer> defenderIds()
    {
        return DEFENDER_IDS;
    }

    public static Set<Integer> allItemIds()
    {
        return ALL_ITEM_IDS;
    }

    private static Set<Integer> buildAllItemIds()
    {
        Set<Integer> itemIds = new HashSet<>();
        itemIds.addAll(HILT_IDS);
        itemIds.addAll(BARROWS_GLOVES_IDS);
        itemIds.addAll(POWERED_STAFF_IDS);
        itemIds.addAll(MELEE_WEAPON_IDS);
        itemIds.addAll(DEFENDER_IDS);
        return Collections.unmodifiableSet(itemIds);
    }

    private static Set<Integer> ids(Integer... itemIds)
    {
        return Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(itemIds)));
    }
}
