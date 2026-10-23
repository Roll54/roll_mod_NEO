package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.api.config.IrrigationConfig;
import com.agricraft.agricraft.common.block.entity.SprinklerBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Caps AgriCraft's (4.0.24+) sprinkler growth boost at 2x: a watered block gets about as many extra random ticks
 * from the sprinkler as vanilla random ticking gives it.
 *
 * <p>Upstream a sprinkler sweeps its 7x7 area one column per tick (49 ticks a sweep, a new sweep every
 * {@code sprinkler_interval} ticks, whichever is longer) and gives the first bonemealable block of each column a
 * full random tick with {@code sprinkler_growth_chance} (0.2): about one extra tick per 245 ticks against vanilla's
 * one per ~1365 at randomTickSpeed 3, so crops grew ~6.6x as fast — and cross crop sticks spread their
 * neighbours just as fast, the same forced spreading the fertilizer rule in CropBlockEntityMixin removes.
 *
 * <p>Vanilla ticks a given block with probability {@code randomTickSpeed / 4096} per game tick, so per sweep
 * period the matching chance is {@code period * randomTickSpeed / 4096} (≈ 0.036 at defaults). The config value
 * still applies when it is lower, and randomTickSpeed 0 turns the sprinkler boost off like vanilla growth.
 */
@Mixin(SprinklerBlockEntity.class)
public class SprinklerGrowthMixin {

    /** Columns in a sweep, one per tick (7x7). */
    @Unique
    private static final int roll_mod$SWEEP_TICKS = 49;

    @ModifyExpressionValue(method = "irrigateColumn",
            at = @At(value = "FIELD", target = "Lcom/agricraft/agricraft/api/config/IrrigationConfig;sprinklerGrowthChance:D"))
    private double roll_mod$vanillaRateGrowthChance(double configured, @Local(argsOnly = true) Level level) {
        int period = Math.max(roll_mod$SWEEP_TICKS, IrrigationConfig.sprinklerInterval);
        double vanillaRate = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) / 4096.0;
        return Math.min(configured, period * vanillaRate);
    }
}
