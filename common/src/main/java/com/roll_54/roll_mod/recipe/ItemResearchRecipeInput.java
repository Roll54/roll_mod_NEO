package com.roll_54.roll_mod.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record ItemResearchRecipeInput(ItemStack catalyst) implements RecipeInput {
    @Override
    public ItemStack getItem(int i) {
        return catalyst;
    }

    @Override
    public int size() {
        return 1;
    }
}
