package com.roll_54.roll_mod.worldgen;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The mining world, {@code roll_mod:mining_overworld}: flat deepslate (Y −63…0) and stone (Y 1…64) under
 * grass at Y 68 ({@code data/roll_mod/dimension/mining_overworld.json}).
 *
 * <ul>
 *   <li>Always midday: its dimension type ({@code dimension_type/mining_overworld.json}) has a fixed time.</li>
 *   <li>No weather ({@code mixin.MiningWorldWeatherMixin}) and no clouds ({@code MiningWorldClient}).</li>
 *   <li>Ores: GTMOGS veins of its own ({@code ore_vein/mining_overworld/}), placed although the world is
 *       flat ({@code compat.gtmogs.mixin.OrePlacerMiningWorldMixin}).</li>
 * </ul>
 */
public final class MiningWorld {

    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, RollMod.id("mining_overworld"));

    private MiningWorld() {}
}
