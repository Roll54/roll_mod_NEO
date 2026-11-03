package com.roll_54.roll_mod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.gtmogs.OreStoneVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/**
 * The "swallow" half of ores in their rock (the vein half is {@code OreStoneVariantPlacementMixin}):
 * vanilla's rock blobs are ore features too, and one placed by a neighbouring chunk after this chunk's
 * veins only replaced plain stone, leaving granite patches between stone ores. Here a rock blob that
 * reaches one of roll_mod's ores turns it into that ore in its rock (stone hematite → granite hematite)
 * instead of skipping it.
 */
@Mixin(OreFeature.class)
public abstract class OreFeatureRockOverOreMixin {

    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/OreFeature;canPlaceOre(Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Function;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration$TargetBlockState;Lnet/minecraft/core/BlockPos$MutableBlockPos;)Z"))
    private boolean roll_mod$rockReachesOre(BlockState current, Function<BlockPos, BlockState> getter, RandomSource random,
                                            OreConfiguration config, OreConfiguration.TargetBlockState target,
                                            BlockPos.MutableBlockPos pos, Operation<Boolean> original) {
        return original.call(current, getter, random, config, target, pos)
                || OreStoneVariants.rockOverOre(target.state, current) != null;
    }

    @WrapOperation(method = "doPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState roll_mod$oreInRock(LevelChunkSection section, int x, int y, int z, BlockState state,
                                          boolean useLocks, Operation<BlockState> original) {
        BlockState inRock = OreStoneVariants.rockOverOre(state, section.getBlockState(x, y, z));
        return original.call(section, x, y, z, inRock != null ? inRock : state, useLocks);
    }
}
