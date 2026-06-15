package com.roll_54.roll_mod.compat.argicraft;

import com.agricraft.agricraft.api.crop.AgriCrop;
import com.agricraft.agricraft.api.plant.AgriPlant;
import com.agricraft.agricraft.common.block.entity.CropBlockEntity;
import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.world.item.ItemStack;

public final class HerbicideHelper {

    private HerbicideHelper() {
    }

    /**
     * @return {@code true} if the crop's plant declares {@code weeds_resistance} in its datapack JSON,
     * meaning weeds never spawn on it and herbicide should not be applied by the managers.
     */
    public static boolean isWeedResistant(AgriCrop crop) {
        if (crop == null) {
            return false;
        }
        AgriPlant plant = crop.getPlant();
        return plant instanceof WeedResistant resistant && resistant.roll_mod$isWeedsResistant();
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

