package com.roll_54.roll_mod.mixin.antidupe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes the renewable-lava ("lava dupe") exploit where a pointed dripstone with a lava
 * source above drips into a cauldron below, filling it with a bucket-able lava source while
 * the source above is never consumed.
 *
 * <p>We wrap the cauldron lookup inside {@code maybeTransferFluid} and report "no fillable
 * cauldron" whenever the dripping fluid is lava, so lava never fills a cauldron. Water
 * cauldron filling and the mud→clay conversion path are left untouched.
 */
@Mixin(PointedDripstoneBlock.class)
public class DripstoneLavaCauldronMixin {

    @WrapOperation(
            method = "maybeTransferFluid",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;findFillableCauldronBelowStalactiteTip(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;)Lnet/minecraft/core/BlockPos;"
            )
    )
    private static BlockPos roll_mod$blockLavaCauldronFill(Level level, BlockPos pos, Fluid fluid, Operation<BlockPos> original) {
        if (fluid == Fluids.LAVA) {
            return null;
        }
        return original.call(level, pos, fluid);
    }
}
