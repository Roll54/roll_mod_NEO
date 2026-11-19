package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where a container screen writes its two labels — read by {@code ContainerSkin} to place its own. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("titleLabelX")
    int roll_mod$titleLabelX();

    @Accessor("titleLabelY")
    int roll_mod$titleLabelY();

    @Accessor("inventoryLabelX")
    int roll_mod$inventoryLabelX();

    @Accessor("inventoryLabelY")
    int roll_mod$inventoryLabelY();
}
