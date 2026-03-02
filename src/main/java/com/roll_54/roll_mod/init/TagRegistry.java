package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagRegistry {
    public static final TagKey<Item> ROCKET_ITEM = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "rocket_item"));
    public static final TagKey<Item> ROCKET_FUEL = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "rocket_fuel"));
}
