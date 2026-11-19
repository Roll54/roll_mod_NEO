package com.roll_54.roll_mod.mixin.client;

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A furnace menu's synced data — 0 fuel ticks left, 1 fuel ticks total, 2 cooking ticks done, 3 cooking
 * ticks total — for the time-left tooltip on the arrow. Vanilla only exposes it as fractions.
 */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuAccessor {
    @Accessor("data")
    ContainerData roll_mod$data();
}
