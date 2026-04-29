package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import com.roll_54.roll_mod.registry.ItemRegistry;
import com.roll_54.roll_mod.items.electricItems.EnergyDrillItem;
import com.roll_54.roll_mod.items.electricItems.EnergySwordItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class RMMItemProperties {

    public static void addCustomProperties() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof EnergyDrillItem) {

                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "trans"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ComponentsRegistry.TRANS.get(), 0f)
                );

                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "cat"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ComponentsRegistry.CAT.get(), 0f)
                );

                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "meteorite_skin"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ComponentsRegistry.METEORITE_SKIN.get(), 0f)
                );
            }
        }

        for (Item item : BuiltInRegistries.ITEM){
            if (item instanceof EnergySwordItem){
                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "activated"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ComponentsRegistry.FIRST_STATE_ACTIVATED.get(), false) ? 1.0F : 0.0F
                );
                ItemProperties.register(
                        item,
                        ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "murasama"),
                        (stack, level, entity, seed) ->
                                stack.getOrDefault(ComponentsRegistry.MURASAMA.get(), 0f)
                );
            }
            ItemProperties.register(
                    item,
                    ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "lavender"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ComponentsRegistry.LAVENDER.get(), 0.0F)
            );

            ItemProperties.register(
                    item,
                    ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "flame"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ComponentsRegistry.FLAME.get(), 0.0F)
            );

            ItemProperties.register(
                    item,
                    ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "skin_rgb"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ComponentsRegistry.SKIN_RGB.get(), 0.0F)
            );

            ItemProperties.register(
                    item,
                    ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "lime"),
                    (stack, level, entity, seed) ->
                            stack.getOrDefault(ComponentsRegistry.LIME.get(), 0.0F)
            );
        }

        ItemProperties.register(
                ItemRegistry.SKIN_APPLICATOR.get(),
                ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "applicator_color"),
                (stack, level, entity, seed) ->
                        stack.getOrDefault(ComponentsRegistry.APPLICATOR_COLOR.get(), 0)
        );


    }
}
