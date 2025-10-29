package com.roll_54.roll_mod.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import static com.roll_54.roll_mod.RollMod.MODID;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_METEORITE_METAL_TOOL = createTag("needs_meteorite_metal_tool");
        public static final TagKey<Block> INCORRECT_METEORITE_METAL_TOOL = createTag("incorrect_for_meteorite_metal_tool");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(MODID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items"); //example

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(MODID, name));
        }
    }
}