package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roll_54.roll_mod.worldgen.MiningWorld;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * GTMOGS places neither veins nor their surface samples in a superflat world unless its developer option
 * {@code doSuperflatOres} is on — and the mining world, {@code roll_mod:mining_overworld}, is a flat world.
 * So its veins ({@code ore_vein/mining_overworld/}) and samples never generated there. Here that option
 * reads as on for the mining world, and only for it: other flat worlds stay empty as GTMOGS intends.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.api.worldgen.ores.OrePlacer")
public abstract class OrePlacerMiningWorldMixin {

    @ModifyExpressionValue(method = {"placeOres", "placeIndicators"},
            at = @At(value = "FIELD",
                    target = "Lcom/quantumgarbage/gtmogs/config/ConfigHolder$DeveloperConfigs;doSuperflatOres:Z"))
    private boolean roll_mod$oresInMiningWorld(boolean doSuperflatOres, WorldGenLevel level, ChunkGenerator generator,
                                               ChunkAccess chunk) {
        return doSuperflatOres || level.getLevel().dimension() == MiningWorld.LEVEL;
    }
}
