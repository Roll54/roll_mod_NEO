package com.roll_54.roll_mod.gtmogs.api.worldgen.generator;

import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.MapCodec;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.addon.AddonFinder;
import com.roll_54.roll_mod.gtmogs.api.addon.IGTAddon;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGeneratorUtils;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.*;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class VeinGenerators {

    public static final MapCodec<NoopVeinGenerator> NO_OP = register(GTMOGS.id("no_op"), NoopVeinGenerator.CODEC,
            () -> NoopVeinGenerator.INSTANCE);

    public static final MapCodec<StandardVeinGenerator> STANDARD = register(GTMOGS.id("standard"),
            StandardVeinGenerator.CODEC, StandardVeinGenerator::new);
    public static final MapCodec<LayeredVeinGenerator> LAYER = register(GTMOGS.id("layer"), LayeredVeinGenerator.CODEC,
            LayeredVeinGenerator::new);
    public static final MapCodec<GeodeVeinGenerator> GEODE = register(GTMOGS.id("geode"), GeodeVeinGenerator.CODEC,
            GeodeVeinGenerator::new);
    public static final MapCodec<DikeVeinGenerator> DIKE = register(GTMOGS.id("dike"), DikeVeinGenerator.CODEC,
            DikeVeinGenerator::new);
    public static final MapCodec<VeinedVeinGenerator> VEINED = register(GTMOGS.id("veined"), VeinedVeinGenerator.CODEC,
            VeinedVeinGenerator::new);
    public static final MapCodec<ClassicVeinGenerator> CLASSIC = register(GTMOGS.id("classic"),
            ClassicVeinGenerator.CODEC, ClassicVeinGenerator::new);
    public static final MapCodec<CuboidVeinGenerator> CUBOID = register(GTMOGS.id("cuboid"), CuboidVeinGenerator.CODEC,
            CuboidVeinGenerator::new);

    public static <
            T extends VeinGenerator> MapCodec<T> register(ResourceLocation id, MapCodec<T> codec,
                                                          Supplier<? extends VeinGenerator> function) {
        WorldGeneratorUtils.VEIN_GENERATORS.put(id, codec);
        WorldGeneratorUtils.VEIN_GENERATOR_FUNCTIONS.put(id, function);
        return codec;
    }

    public static void registerAddonGenerators() {
        AddonFinder.getAddonList().forEach(IGTAddon::registerVeinGenerators);
    }
}
