package com.roll_54.roll_mod.gtmogs.api.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;

public final class RollModDimensions {

    public static final Map<String, ResourceKey<Level>> DIMENSIONS = Map.of(
            "mining_overworld",
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath("roll_mod", "mining_overworld")));
}
