package com.roll_54.roll_mod_client.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client config for what the recipe viewers (EMI, JEI) list. Saved as
 * {@code config/roll_mod_client-client.toml}; EMI and JEI read it on their next reload.
 */
public final class RecipeViewerConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue SHOW_LAVA_SMELTERY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("recipe_viewer");

        SHOW_LAVA_SMELTERY = builder
                .comment("Show the example MBD2 'lava_smeltery' recipe type (EU + water -> lava) in EMI/JEI.",
                        "It only exists to demonstrate MBD2 recipes, so it is hidden by default.")
                .define("show_lava_smeltery", false);
        builder.pop();
        SPEC = builder.build();
    }

    private RecipeViewerConfig() {
    }
}
