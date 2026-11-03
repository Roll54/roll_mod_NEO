package com.roll_54.roll_mod.gtmogs.api.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;

public final class AdAstraDimensions {

    public static final Map<String, ResourceKey<Level>> DIMENSIONS = Map.of(
            "moon",
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("ad_astra", "moon")),
            "mars",
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("ad_astra", "mars")),
            "mercury",
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("ad_astra", "mercury")),
            "venus",
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("ad_astra", "venus")),
            "glacio",
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("ad_astra", "glacio")));
}
