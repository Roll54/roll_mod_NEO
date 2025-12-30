package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.ModComponents;
import com.roll_54.roll_mod.modItems.EnergyDrillItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModItemProperties {

    public static void addCustomProperties() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof EnergyDrillItem) {

                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "trans"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ModComponents.TRANS.get(), 0f)
                );

                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "cat"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ModComponents.CAT.get(), 0f)
                );
            }
        }
    }
}
