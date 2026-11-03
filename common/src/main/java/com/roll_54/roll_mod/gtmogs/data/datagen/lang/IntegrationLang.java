package com.roll_54.roll_mod.gtmogs.data.datagen.lang;



import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class IntegrationLang {

    public static void init(RegistrateLangProvider provider) {
        initRecipeViewerLang(provider);
        initMinimapLang(provider);
        initOwnershipLang(provider);
    }

    /** JEI, REI, EMI */
    private static void initRecipeViewerLang(RegistrateLangProvider provider) {
        provider.add("gtmogs.jei.ore_vein_diagram", "Ore Vein Diagram");
        provider.add("gtmogs.jei.ore_vein_diagram.chance", "§eChance: %s§r");
        provider.add("gtmogs.jei.ore_vein_diagram.spawn_range", "Spawn Range:");
        provider.add("gtmogs.jei.ore_vein_diagram.weight", "Weight: %s");
        provider.add("gtmogs.jei.ore_vein_diagram.dimensions", "Dimensions:");
        for (ResourceKey<OreVeinDefinition> key : GTOreVeins.ALL_KEYS) {
            ResourceLocation id = key.location();
            String name = id.getPath();
            provider.add(id.toLanguageKey("ore_vein"), RegistrateLangProvider.toEnglishName(name));
        }
    }

    private static void initMinimapLang(RegistrateLangProvider provider) {
        provider.add("gtmogs.minimap.ore_vein.depleted", "Depleted");

        provider.add("message.gtmogs.new_veins.amount", "Prospected %d new veins!");
        provider.add("message.gtmogs.new_veins.name", "Prospected %s!");
        provider.add("button.gtmogs.mark_as_depleted.name", "Mark as Depleted");
        provider.add("button.gtmogs.toggle_waypoint.name", "Toggle Waypoint");

        provider.add("gtmogs.journeymap.options.layers", "Prospection layers");
        provider.add("gtmogs.journeymap.options.layers.ore_veins", "Show Ore Veins");
        provider.add("gtmogs.journeymap.options.layers.bedrock_fluids", "Show Bedrock Fluid Veins");
        provider.add("gtmogs.journeymap.options.layers.hide_depleted", "Hide Depleted Veins");
    }

    private static void initOwnershipLang(RegistrateLangProvider provider) {
        provider.add("gtmogs.ownership.name.player", "Player");
        provider.add("gtmogs.ownership.name.ftb", "FTB Teams");
        provider.add("gtmogs.ownership.name.argonauts", "Argonauts Guild");
    }
}