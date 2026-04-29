package com.roll_54.roll_mod.data;

import com.roll_54.roll_mod.registry.ComponentsRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

// todo зробити клятий тристейт...
@SuppressWarnings({"unchecked", "rawtypes"})
public interface TriStateToggleAbleItem {

    default boolean getDefaultActivatedState() {
        return false;
    }

    default boolean isActivatedById(ItemStack stack, int id) {
        switch (id) {
            case 0 -> {
                return (Boolean) stack.getOrDefault((DataComponentType) ComponentsRegistry.FIRST_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
            }
            case 1 -> {
                return (Boolean) stack.getOrDefault((DataComponentType) ComponentsRegistry.SECOND_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
            }
            case 2 -> {
                return (Boolean) stack.getOrDefault((DataComponentType) ComponentsRegistry.THIRD_STATE_ACTIVATED.get(), this.getDefaultActivatedState());
            }
        }
        return false;
    }

    default void setActivatedById(ItemStack stack, int id, boolean activated) {
        switch (id) {
            case 0 -> stack.set((DataComponentType) ComponentsRegistry.FIRST_STATE_ACTIVATED.get(), activated);
            case 1 -> stack.set((DataComponentType) ComponentsRegistry.SECOND_STATE_ACTIVATED.get(), activated);
            case 2 -> stack.set((DataComponentType) ComponentsRegistry.THIRD_STATE_ACTIVATED.get(), activated);
        }
    }
}


