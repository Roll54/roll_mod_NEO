package com.roll_54.roll_mod.mixin.adstra;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Raises the oxygen tank of every Ad Astra suit to 10 buckets. The space suit (1 B), netherite space
 * suit (2 B) and jet suit all pass their size into this constructor, which stores it in
 * {@code tankSize} (millibuckets); {@code getFluids} and the tooltip read that field, so changing it
 * here covers capacity and display. Pieces that pass 0 (no tank) are left at 0.
 *
 * <p>Existing stacks keep the oxygen they hold; they just gain room.
 */
@Mixin(targets = "earth.terrarium.adastra.common.items.armor.SpaceSuitItem", remap = false)
public abstract class SpaceSuitItemMixin {

    @Unique
    private static final long roll_mod$TANK_SIZE = 10_000L;

    // tankSize is final, so it can only be written from <init> itself; a @WrapOperation on the
    // PUTFIELD moves the write into a bridge method and the JVM rejects it. Instead, swap the value as
    // the constructor reads its tankSize argument (the only load of that long in <init>, right before
    // the field is stored), so the write stays where it is.
    @ModifyVariable(method = "<init>", at = @At("LOAD"), argsOnly = true)
    private long roll_mod$biggerTank(long tankSize) {
        return tankSize > 0 ? roll_mod$TANK_SIZE : tankSize;
    }
}
