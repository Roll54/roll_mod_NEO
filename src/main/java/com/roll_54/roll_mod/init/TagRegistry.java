package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagRegistry {
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


}
