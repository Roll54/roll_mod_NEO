package com.roll_54.roll_mod.сonfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Set;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public final class GeneralConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder()
            .comment("roll_mod common config").push("general");

    public static final ModConfigSpec.BooleanValue IS_DEBUG = BUILDER
            .comment("Enable extra debug logging")
            .define("debug", false);

    public static final ModConfigSpec.BooleanValue CUSTOM_PVP = BUILDER
            .comment("Replace vanilla armor absorption formula")
            .define("custom_pvp", true);

    // приклад списку предметів (залишив як заготовку, не обов’язково)
    // private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS =
    //        BUILDER.comment("Items example list").defineListAllowEmpty("items",
    //                List.of("minecraft:iron_ingot"), GeneralConfig::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.pop().build();

    /** КЕШ — лише його читаємо в ігрових івентах */
    public static volatile boolean isDebug = false;
    public static volatile boolean isCustomPvP = true;
    public static volatile Set<Item> items = Set.of(); // якщо будете використовувати

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String s && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(s));
    }

    /** Викликається коли конфіг завантажено вперше */
    public static void onLoad(final ModConfigEvent.Loading e) {
        if (e.getConfig().getSpec() == SPEC) {
            isDebug = IS_DEBUG.get();
            isCustomPvP = CUSTOM_PVP.get();
            // items = ITEM_STRINGS.get().stream()
            //        .map(s -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(s)))
            //        .collect(Collectors.toSet());
        }
    }

    /** Викликається при перезавантаженні */
    public static void onReload(final ModConfigEvent.Reloading e) {
        if (e.getConfig().getSpec() == SPEC) {
            isDebug = IS_DEBUG.get();
            isCustomPvP = CUSTOM_PVP.get();
        }
    }
}
