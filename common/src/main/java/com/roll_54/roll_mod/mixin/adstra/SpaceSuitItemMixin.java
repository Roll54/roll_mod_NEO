package com.roll_54.roll_mod.mixin.adstra;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Raises the oxygen tank of every Ad Astra suit to 10 buckets. The space suit (1 B), netherite space
 * suit (2 B) and jet suit all pass their size into this constructor, which stores it in
 * {@code tankSize} (millibuckets); {@code getFluids} and the tooltip read that field, so changing it
 * here covers capacity and display. Pieces that pass 0 (no tank) are left at 0.
 *
 * <p>Existing stacks keep the oxygen they hold; they just gain room.
 */
@Mixin(value = SpaceSuitItem.class, remap = false)
public abstract class SpaceSuitItemMixin {

    @Unique
    private static final long roll_mod$TANK_SIZE = 10_000L;

    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    target = "Learth/terrarium/adastra/common/items/armor/SpaceSuitItem;tankSize:J",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD
            )
    )
    private void roll_mod$biggerTank(SpaceSuitItem instance, long tankSize, Operation<Void> original) {
        original.call(instance, tankSize > 0 ? roll_mod$TANK_SIZE : tankSize);
    }
}
