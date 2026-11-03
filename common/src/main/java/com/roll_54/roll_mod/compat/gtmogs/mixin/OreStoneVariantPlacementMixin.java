package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.gtmogs.OreStoneVariants;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every GTMOGS vein generator writes its ores straight into the chunk section; here each ore it writes is
 * swapped for the same material's ore in the rock it replaces ({@link OreStoneVariants}), so a stone
 * hematite vein running into granite comes out as granite hematite.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = {
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.CuboidVeinGenerator",
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.ClassicVeinGenerator$Layer",
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.StandardVeinGenerator",
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.LayeredVeinGenerator",
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.VeinedVeinGenerator",
        "com.quantumgarbage.gtmogs.api.worldgen.generator.veins.DikeVeinGenerator"
})
public abstract class OreStoneVariantPlacementMixin {

    @WrapOperation(method = "*",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState roll_mod$oreInItsRock(LevelChunkSection section, int x, int y, int z, BlockState state,
                                            boolean useLocks, Operation<BlockState> original) {
        return original.call(section, x, y, z, OreStoneVariants.forRock(state, section.getBlockState(x, y, z)), useLocks);
    }
}
