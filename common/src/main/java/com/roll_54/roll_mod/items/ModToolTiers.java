package com.roll_54.roll_mod.items;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.ModTags;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    public static final Tier METEORITE_METAL = new SimpleTier(
            ModTags.Blocks.INCORRECT_METEORITE_METAL_TOOL,
            2400, 10f, 5f, 30,
            () -> Ingredient.of(ItemRegistry.METEORITE_METAL_INGOT.get())
    );

    /**
     * The bronze alloys come from Modern Industrialization materials registered by KubeJS at runtime, so
     * the repair ingredient has to go through the material's {@code c:ingots/...} tag rather than an item
     * reference that would not resolve at class-load time.
     */
    public static final Tier BLACK_BRONZE = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            550, 6.5f, 2.5f, 18,
            () -> Ingredient.of(ingotTag("black_bronze"))
    );

    public static final Tier BISMUTH_BRONZE = new SimpleTier(
            BlockTags.INCORRECT_FOR_IRON_TOOL,
            400, 6.0f, 2.0f, 16,
            () -> Ingredient.of(ingotTag("bismuth_bronze"))
    );

    /** Diamond's mining level and speed, on a much shorter-lived head. */
    public static final Tier BLACK_STEEL = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1500, 8.0f, 3.0f, 10,
            () -> Ingredient.of(ingotTag("black_steel"))
    );

    public static final Tier DIAMOND_ALLOY = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            3000, 8.0f, 2.5f, 10,
            () -> Ingredient.of(ItemRegistry.DIAMOND_ALLOY_INGOT.get())
    );


    public static final Tier STEEL = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            2000, 8.0f, 2.5f, 10,
            () -> Ingredient.of(ingotTag("steel"))
    );


    private static TagKey<Item> ingotTag(String material) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/" + material));
    }
}
