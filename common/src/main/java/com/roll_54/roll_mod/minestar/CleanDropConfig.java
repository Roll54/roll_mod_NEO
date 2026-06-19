package com.roll_54.roll_mod.minestar;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@SuppressWarnings("all")
public class CleanDropConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> REMOVE_ITEMS;
    public static final ModConfigSpec.DoubleValue RADIUS_CLEAN;
    public static final ModConfigSpec.IntValue MIN_ITEMS;

    static {
        BUILDER.push("CLEAN MINER SHIT");

        BUILDER.push("Radius clean drops");
        RADIUS_CLEAN = BUILDER.defineInRange("radius_clean", 10.0, 10, 20);
        BUILDER.pop();

        BUILDER.push("Clean items in tag's format");
        REMOVE_ITEMS = BUILDER.defineList("tags", List.of("minecraft:cobblestone", "minecraft:deepslate"), obj -> obj instanceof String);
        BUILDER.pop();

        BUILDER.push("Radius clean drops");
        MIN_ITEMS = BUILDER.defineInRange("min items for clean", (int) 45, 40, 100);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
