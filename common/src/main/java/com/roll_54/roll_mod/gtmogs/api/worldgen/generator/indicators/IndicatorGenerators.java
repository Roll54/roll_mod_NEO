package com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.addon.AddonFinder;
import com.roll_54.roll_mod.gtmogs.api.addon.IGTAddon;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class IndicatorGenerators {

    public static final MapCodec<SurfaceIndicatorGenerator> SURFACE = register(GTMOGS.id("surface"),
            SurfaceIndicatorGenerator.CODEC, SurfaceIndicatorGenerator::new);

    public static <
            T extends IndicatorGenerator> MapCodec<T> register(ResourceLocation id, MapCodec<T> codec,
                                                               Supplier<? extends IndicatorGenerator> function) {
        WorldGeneratorUtils.INDICATOR_GENERATORS.put(id, codec);
        WorldGeneratorUtils.INDICATOR_GENERATOR_FUNCTIONS.put(id, function);
        return codec;
    }

    public static void registerAddonGenerators() {
        AddonFinder.getAddonList().forEach(IGTAddon::registerIndicatorGenerators);
    }
}
