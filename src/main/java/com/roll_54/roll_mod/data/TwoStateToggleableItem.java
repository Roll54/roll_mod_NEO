package com.roll_54.roll_mod.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.roll_54.roll_mod.registry.ComponentsRegistry.FIRST_STATE_ACTIVATED;
import static com.roll_54.roll_mod.registry.ComponentsRegistry.SECOND_STATE_ACTIVATED;

@SuppressWarnings({"unchecked", "rawtypes"})
public interface TwoStateToggleableItem {

    default boolean getDefaultActivatedState() {
        return false;
    }


    default boolean isActivatedFirst(ItemStack stack) {
        return (Boolean)stack.getOrDefault((DataComponentType) FIRST_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
    }
    default boolean isActivatedSecond(ItemStack stack) {
        return (Boolean)stack.getOrDefault((DataComponentType) SECOND_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
    }

    default void setActivatedFirst(Player player, ItemStack stack, boolean activated) {
        stack.set((DataComponentType) FIRST_STATE_ACTIVATED.get(), activated);
    }

    default void setActivatedSecond(Player player, ItemStack stack, boolean activated) {
        stack.set((DataComponentType) SECOND_STATE_ACTIVATED.get(), activated);
    }

}
