package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public final class MIConditionsBootstrap {
    private static final Logger LOGGER = RollMod.LOGGER;

    private MIConditionsBootstrap() {}

    public static void init() {
        LOGGER.debug("[MI] Starting condition registration");

        var STORM = ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active");
        LOGGER.debug("[MI] Created id for nether storm condition: {}", STORM);
        var STORM_IN_DIM = ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active_in_dimension");
        LOGGER.debug("[MI] Created id for nether storm in dimension condition: {}", STORM_IN_DIM);

        LOGGER.debug("[MI] Registering {}", STORM);
        MachineProcessConditions.register(
                STORM,
                CustomProcessCondition.CODEC,
                CustomProcessCondition.STREAM_CODEC
        );
        LOGGER.debug("[MI] Registered {}", STORM);

        LOGGER.debug("[MI] Registering {}", STORM_IN_DIM);
        MachineProcessConditions.register(
                STORM_IN_DIM,
                NetherStormInDimensionProcessCondition.CODEC,
                NetherStormInDimensionProcessCondition.STREAM_CODEC
        );
        LOGGER.debug("[MI] Registered {}", STORM_IN_DIM);

        // sanity-check, що воно реально в реєстрі
        LOGGER.debug("[MI] Performing lookup check for {}", STORM);
        var check = MachineProcessConditions.getCodec(STORM);
        LOGGER.debug("[MI] Lookup '{}': {}", STORM, (check != null ? "OK" : "MISSING"));
    }
}