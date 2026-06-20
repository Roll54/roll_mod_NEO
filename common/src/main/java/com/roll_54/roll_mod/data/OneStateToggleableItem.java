package com.roll_54.roll_mod.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.roll_54.roll_mod.registry.ComponentsRegistry.FIRST_STATE_ACTIVATED;

@SuppressWarnings({"unchecked", "rawtypes"})
public interface OneStateToggleableItem {
    default boolean getDefaultActivatedState() {
        return false;
    }


    default boolean isActivated(ItemStack stack) {
        return (Boolean)stack.getOrDefault((DataComponentType) FIRST_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
    }
    default void setActivated(Player player, ItemStack stack, boolean activated) {
        stack.set((DataComponentType) FIRST_STATE_ACTIVATED.get(), activated);
    }

}
