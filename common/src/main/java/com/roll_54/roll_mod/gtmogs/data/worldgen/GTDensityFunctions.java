package com.roll_54.roll_mod.gtmogs.data.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import com.roll_54.roll_mod.gtmogs.GTMOGS;

public class GTDensityFunctions {

    public static final ResourceKey<DensityFunction> NEW_ORE_VEIN_TOGGLE = create("vein_toggle");
    public static final ResourceKey<DensityFunction> NEW_ORE_VEIN_RIDGED = create("vein_ridged");

    public static void bootstrap(BootstrapContext<DensityFunction> ctx) {
        HolderGetter<NormalNoise.NoiseParameters> noises = ctx.lookup(Registries.NOISE);

        ctx.register(NEW_ORE_VEIN_TOGGLE, veinToggle(noises::getOrThrow));
        ctx.register(NEW_ORE_VEIN_RIDGED, veinRidged(noises::getOrThrow));
    }

    /**
     * Definition of {@link #NEW_ORE_VEIN_TOGGLE}.
     *
     * <p>
     * Shared by datagen and by the runtime fallback in
     * {@link com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.VeinedVeinGenerator}, so veined veins keep
     * working in worlds where the datapack entry isn't present.
     */
    public static DensityFunction veinToggle(NoiseLookup noises) {
        Holder<NormalNoise.NoiseParameters> oreVeininess = noises.get(Noises.ORE_VEININESS);
        return DensityFunctions.interpolated(
                DensityFunctions.noise(oreVeininess, 1.5f, 1.5f));
    }

    /**
     * Definition of {@link #NEW_ORE_VEIN_RIDGED}. See {@link #veinToggle(NoiseLookup)}.
     */
    public static DensityFunction veinRidged(NoiseLookup noises) {
        Holder<NormalNoise.NoiseParameters> oreVeinA = noises.get(Noises.ORE_VEIN_A);
        Holder<NormalNoise.NoiseParameters> oreVeinB = noises.get(Noises.ORE_VEIN_B);
        return DensityFunctions.add(
                DensityFunctions.constant(-0.08f),
                DensityFunctions.max(
                        DensityFunctions.interpolated(
                                DensityFunctions.noise(oreVeinA, 4.0f, 4.0f)).abs(),
                        DensityFunctions.interpolated(
                                DensityFunctions.noise(oreVeinB, 4.0f, 4.0f)).abs()));
    }

    /**
     * Resolves a noise parameter holder, so the definitions above can be built either from a datagen
     * {@link HolderGetter} or from a live {@link net.minecraft.core.Registry}.
     */
    @FunctionalInterface
    public interface NoiseLookup {

        Holder<NormalNoise.NoiseParameters> get(ResourceKey<NormalNoise.NoiseParameters> key);
    }

    private static ResourceKey<DensityFunction> create(String path) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, GTMOGS.id(path));
    }
}
