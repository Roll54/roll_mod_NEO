package com.roll_54.roll_mod.worldgen;

import com.roll_54.roll_mod.block.SulfurBerryBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RandomizedIntStateProvider;

import java.util.List;

import static com.roll_54.roll_mod.RollMod.MODID;
import static com.roll_54.roll_mod.init.BlockRegistry.SULFUR_BERRY_BLOCK;

public class RMMConfiguredFeatures {
    // CF -> PF -> BM

    public static final ResourceKey<ConfiguredFeature<?, ?>> SULFUR_BERRY_BUSH_KEY = registerKey("sulfur_berry_bush");


    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, SULFUR_BERRY_BUSH_KEY, Feature.RANDOM_PATCH,
                FeatureUtils.simplePatchConfiguration(
                        Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(
                                new RandomizedIntStateProvider(
                                        BlockStateProvider.simple(
                                                SULFUR_BERRY_BLOCK.get().defaultBlockState()
                                        ),
                                        SulfurBerryBlock.AGE,
                                        UniformInt.of(0, 3)
                                )
                        ),
                        List.of(Blocks.SOUL_SAND, Blocks.SOUL_SOIL)
                )
        );
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(MODID, name));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(BootstrapContext<ConfiguredFeature<?, ?>> context,
                                                                                          ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
