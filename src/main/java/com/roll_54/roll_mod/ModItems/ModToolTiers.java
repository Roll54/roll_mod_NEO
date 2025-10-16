package com.roll_54.roll_mod.ModItems;

import com.roll_54.roll_mod.Util.ModTags;
import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public class ModToolTiers {
    public static final Tier METEORITE_METAL = new SimpleTier(
            ModTags.Blocks.INCORRECT_METEORITE_METAL_TOOL,
            2400, 10f, 5f, 30,
            () -> Ingredient.of(ItemRegistry.METEORITE_METAL_INGOT.get())
    );
}