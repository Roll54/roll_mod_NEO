package com.roll_54.roll_mod.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * Simple JEI recipe holder for cauldron washing interactions.
 */
public final class CauldronWashingRecipe {
    private final ItemStack input;
    private final ItemStack output;
    private final float dustDropChance;

    public CauldronWashingRecipe(ItemStack input, ItemStack output, float dustDropChance) {
        this.input = input;
        this.output = output;
        this.dustDropChance = dustDropChance;
    }

    public ItemStack getInput() {
        return input;
    }

    public ItemStack getOutput() {
        return output;
    }

    public float getDustDropChance() {
        return dustDropChance;
    }
}
