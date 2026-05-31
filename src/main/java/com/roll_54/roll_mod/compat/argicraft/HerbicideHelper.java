package com.roll_54.roll_mod.compat.argicraft;

import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.world.item.ItemStack;

public final class HerbicideHelper {

    private HerbicideHelper() {
    }

    public static boolean applyHerbicide(ItemStack stack, CropBlockEntity crop) {
        if (!(crop instanceof AgriHerbicide herbicide)) {
            return false;
        }
        int amount = stack.getOrDefault(ComponentsRegistry.HERBICIDE.get(), 1);
        if (amount <= 0) {
            return false;
        }
        herbicide.roll_mod$addHerbicide(amount);
        return true;
    }
}

