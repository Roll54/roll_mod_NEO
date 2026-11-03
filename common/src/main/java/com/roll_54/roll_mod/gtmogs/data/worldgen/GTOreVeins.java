package com.roll_54.roll_mod.gtmogs.data.worldgen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.*;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.NoopVeinGenerator;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class GTOreVeins {

    @Getter
    private static final HashSet<Block> veinOres = new HashSet<>();

    public static void addVeinOre(Block b) {
        veinOres.add(b);
    }

    /**
     * The size of the largest registered vein.
     * This becomes available after all veins have been loaded.
     */
    @Getter
    private static int largestVeinSize = 0;

    public static Set<ResourceKey<OreVeinDefinition>> ALL_KEYS = new ReferenceOpenHashSet<>();

    public static void updateLargestVeinSize(Registry<OreVeinDefinition> registry) {
        // map to average of min & max values.
        GTOreVeins.largestVeinSize = registry.stream()
                .map(OreVeinDefinition::clusterSize)
                .mapToInt(intProvider -> (intProvider.getMinValue() + intProvider.getMaxValue()) / 2)
                .max()
                .orElse(0);
    }

    public static ResourceKey<OreVeinDefinition> create(ResourceLocation id) {
        var key = ResourceKey.create(GTRegistries.ORE_VEIN_REGISTRY, id);
        ALL_KEYS.add(key);
        return key;
    }

    public static OreVeinDefinition blankOreDefinition(HolderGetter<Biome> biomeLookup) {
        return new OreVeinDefinition(
                ConstantInt.ZERO, 0, 0, IWorldGenLayer.NOWHERE, Set.of(),
                HeightRangePlacement.uniform(VerticalAnchor.absolute(0), VerticalAnchor.absolute(0)),
                0, HolderSet.empty(), BiomeWeightModifier.EMPTY, new ArrayList<>(), NoopVeinGenerator.INSTANCE,
                biomeLookup);
    }

    private static void register(BootstrapContext<OreVeinDefinition> context,
                                 ResourceKey<OreVeinDefinition> key,
                                 Consumer<OreVeinDefinition> consumer) {
        OreVeinDefinition builder = blankOreDefinition(context.lookup(Registries.BIOME));
        consumer.accept(builder);
        context.register(key, builder);
    }

    public static void bootstrap(BootstrapContext<OreVeinDefinition> context) {
        final Supplier<Block> SLIME_BLOCK = () -> BuiltInRegistries.BLOCK
                .get(ResourceLocation.parse("minecraft:slime_block"));
        RuleTest[] endRules = new RuleTest[] { WorldGeneratorUtils.END_ORE_REPLACEABLES };
    }
}
