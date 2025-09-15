package com.roll_54.roll_mod.mi;

import aztech.modern_industrialization.machines.recipe.condition.MachineProcessConditions;
import com.roll_54.roll_mod.Roll_mod;
import net.minecraft.resources.ResourceLocation;

public final class MIConditionsBootstrap {
    private MIConditionsBootstrap() {}

    public static void init() {
        Roll_mod.LOGGER.info("[MI] Starting process condition registration");

        var stormId = ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active");
        Roll_mod.LOGGER.info("[MI] Created id {}", stormId);

        var stormDimId = ResourceLocation.fromNamespaceAndPath("roll_mod", "nether_storm_active_in_dimension");
        Roll_mod.LOGGER.info("[MI] Created id {}", stormDimId);

        Roll_mod.LOGGER.info("[MI] Registering {}", stormId);
        MachineProcessConditions.register(
                stormId,
                CustomProcessCondition.CODEC,
                CustomProcessCondition.STREAM_CODEC
        );
        Roll_mod.LOGGER.info("[MI] Registered {}", stormId);

        Roll_mod.LOGGER.info("[MI] Registering {}", stormDimId);
        MachineProcessConditions.register(
                stormDimId,
                NetherStormInDimensionProcessCondition.CODEC,
                NetherStormInDimensionProcessCondition.STREAM_CODEC
        );
        Roll_mod.LOGGER.info("[MI] Registered {}", stormDimId);

        // sanity-check, що воно реально в реєстрі
        var check = MachineProcessConditions.getCodec(stormId);
        Roll_mod.LOGGER.info("[MI] Lookup '{}': {}", stormId, (check != null ? "OK" : "MISSING"));
    }
}
