package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GTMOGS surface indicators count water as replaceable and write the indicator's default state straight into the
 * chunk section, so an indicator generated underwater deleted its water source. If the replaced block is a water
 * source and the indicator can be waterlogged (our {@code OreSampleBlock}s can), place it waterlogged instead.
 *
 * <p>Targets GTMOGS by name, so no compile dependency; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.api.worldgen.generator.indicators.SurfaceIndicatorGenerator")
public abstract class SurfaceIndicatorWaterlogMixin {

    @WrapOperation(
            method = "placeIndicator",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState roll_mod$waterlogIndicator(LevelChunkSection section, int x, int y, int z, BlockState state,
                                                         boolean useLocks, Operation<BlockState> original) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && section.getBlockState(x, y, z).getFluidState().getType() == Fluids.WATER) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        return original.call(section, x, y, z, state, useLocks);
    }
}
