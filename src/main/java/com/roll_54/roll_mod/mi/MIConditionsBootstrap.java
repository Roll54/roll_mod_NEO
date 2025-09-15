package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions;
import net.minecraft.resources.ResourceLocation;

public final class MIConditionsBootstrap {
    private MIConditionsBootstrap() {}

    public static void init() {
        var STORM = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active");
        var STORM_IN_DIM = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active_in_dimension");

        aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions.register(
                STORM,
                CustomProcessCondition.CODEC,
                CustomProcessCondition.STREAM_CODEC
        );
        com.roll_54.roll_mod.Roll_mod.LOGGER.info("[MI] Registered process condition {}", STORM);

        aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions.register(
                STORM_IN_DIM,
                NetherStormInDimensionProcessCondition.CODEC,
                NetherStormInDimensionProcessCondition.STREAM_CODEC
        );
        com.roll_54.roll_mod.Roll_mod.LOGGER.info("[MI] Registered process condition {}", STORM_IN_DIM);

        // sanity-check, що воно реально в реєстрі
        var check = aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions
                .getCodec(STORM);
        com.roll_54.roll_mod.Roll_mod.LOGGER.info("[MI] Lookup '{}': {}", STORM, (check != null ? "OK" : "MISSING"));
    }
}