package com.roll_54.roll_mod.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.roll_54.roll_mod.data.RMMComponents.ACTIVATED;

public interface ToggleableItem {
    default boolean getDefaultActivatedState() {
        return false;
    }


    default boolean isActivated(ItemStack stack) {
        return (Boolean)stack.getOrDefault((DataComponentType) ACTIVATED.get(), this.getDefaultActivatedState());
    }
    default void setActivated(Player player, ItemStack stack, boolean activated) {
        stack.set((DataComponentType) ACTIVATED.get(), activated);
    }

}
