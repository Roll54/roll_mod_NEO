package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagRegistry {
    //or parse, I don't care much....

    public static final TagKey<Item> ROCKET_ITEM = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "rocket_item"));
    public static final TagKey<Item> ROCKET_FUEL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "rocket_fuel"));
    public static final TagKey<Block> ORE_BLOCKS  = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    public static final TagKey<Item> CRUSHED_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "crushed_ore"));
    public static final TagKey<Item> CRUSHED_REFINED_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "crushed_refined_ore"));
    public static final TagKey<Item> CRUSHED_PURIFIED_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "crushed_purified_ore"));
    public static final TagKey<Item> DUST_PURE_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "dust_pure_ore"));
    public static final TagKey<Item> DUST_IMPURE_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "dust_impure_ore"));
    public static final TagKey<Item> DUST_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "dust_ore"));
    public static final TagKey<Item> RAW_ORE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "raw_ore"));
    public static final TagKey<Item> C_DUSTSTAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "dusts"));

    // Radioactivity tiers — items with these tags irradiate the holder every tick
    public static final TagKey<Item> LOW_RADIOACTIVITY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "low_radioactivity"));
    public static final TagKey<Item> MEDIUM_RADIOACTIVITY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "medium_radioactivity"));
    public static final TagKey<Item> HIGH_RADIOACTIVITY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "high_radioactivity"));
    public static final TagKey<Item> EXTREME_RADIOACTIVITY = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "extreme_radioactivity"));


    // Which cosmetic skin slot an item occupies. These tags are consulted first by SkinCategory#of;
    // anything they do not mention falls back to an instanceof + ItemAbility probe, so a sword from
    // a mod nobody tagged is still skinnable. Tags stay the authority because EnergySwordItem and
    // ComponentEnergyDrill both extend plain Item and answer no ItemAbility — neither half of the
    // probe can see them. Being datapack JSON, they also let a server retag items live via /reload.
    public static final TagKey<Item> SKIN_SLOT_SWORD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_slot/sword"));
    public static final TagKey<Item> SKIN_SLOT_AXE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_slot/axe"));
    public static final TagKey<Item> SKIN_SLOT_PICKAXE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_slot/pickaxe"));
    public static final TagKey<Item> SKIN_SLOT_HELMET = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_slot/helmet"));

    /** Items that may never carry a cosmetic skin. Beats every slot tag and the type probe both. */
    public static final TagKey<Item> SKIN_BLACKLIST = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_blacklist"));

    /**
     * Items the Crop Manager Mk2 accepts in its herbicide slot. A tag rather than a list of items
     * because the MBD2 machine definition is built during mod construction, before items are
     * registered, so its slot filter cannot resolve a DeferredHolder.
     */
    public static final TagKey<Item> HERBICIDES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "herbicides"));

    //for blockogriz
    public static final TagKey<Block> MINEABLE_WITH_PAXEL = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("cucumber", "mineable/paxel"));

}
