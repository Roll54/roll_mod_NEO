package com.roll_54.roll_mod.gtmogs.api.worldgen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.addon.AddonFinder;
import com.roll_54.roll_mod.gtmogs.api.addon.IGTAddon;
import com.roll_54.roll_mod.gtmogs.integration.kjs.GTMOGSStartupEvents;
import com.roll_54.roll_mod.gtmogs.integration.kjs.events.WorldGenLayerKubeEvent;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public enum WorldGenLayers implements IWorldGenLayer, StringRepresentable {

    STONE("stone", new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), Set.of(Level.OVERWORLD)),
    DEEPSLATE("deepslate", new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), Set.of(Level.OVERWORLD)),
    NETHERRACK("netherrack", new TagMatchTest(BlockTags.NETHER_CARVER_REPLACEABLES), Set.of(Level.NETHER)),
    ENDSTONE("endstone", WorldGeneratorUtils.END_ORE_REPLACEABLES, Set.of(Level.END)),
    MOON_STONE("moon_stone", WorldGeneratorUtils.MOON_ORE_REPLACEABLES,
            Set.of(AdAstraDimensions.DIMENSIONS.get("moon"))),
    MARS_STONE("mars_stone", WorldGeneratorUtils.MARS_ORE_REPLACEABLES,
            Set.of(AdAstraDimensions.DIMENSIONS.get("mars"))),
    MERCURY_STONE("mercury_stone", WorldGeneratorUtils.MERCURY_ORE_REPLACEABLES,
            Set.of(AdAstraDimensions.DIMENSIONS.get("mercury"))),
    VENUS_STONE("venus_stone", WorldGeneratorUtils.VENUS_ORE_REPLACEABLES,
            Set.of(AdAstraDimensions.DIMENSIONS.get("venus"))),
    GLACIO_STONE("glacio_stone", WorldGeneratorUtils.GLACIO_ORE_REPLACEABLES,
            Set.of(AdAstraDimensions.DIMENSIONS.get("glacio"))),
    MINING_OVERWORLD_STONE("mining_overworld_stone", new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
            Set.of(RollModDimensions.DIMENSIONS.get("mining_overworld"))),
    MINING_OVERWORLD_DEEPSLATE("mining_overworld_deepslate", new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES),
            Set.of(RollModDimensions.DIMENSIONS.get("mining_overworld")));

    private final String name;

    @SuppressWarnings("NonFinalFieldInEnum")
    @Getter
    @Setter
    private Set<ResourceKey<Level>> levels;

    @SuppressWarnings("NonFinalFieldInEnum")
    @Getter
    @Setter
    private RuleTest target;

    WorldGenLayers(String name, RuleTest target, Set<ResourceKey<Level>> levels) {
        this.name = name;
        this.target = target;
        this.levels = levels;
        WorldGeneratorUtils.WORLD_GEN_LAYERS.put(name, this);
    }

    public static void registerAll() {
        AddonFinder.getAddonList().forEach(IGTAddon::registerWorldgenLayers);
        if (GTMOGS.Mods.isKubeJSLoaded()) {
            KJSCallWrapper.postEvent();
        }
    }

    public static IWorldGenLayer getByName(String name) {
        return WorldGeneratorUtils.WORLD_GEN_LAYERS.get(name);
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return name;
    }

    @Override
    public boolean isApplicableForLevel(ResourceKey<Level> level) {
        return levels.contains(level);
    }

    private static final class KJSCallWrapper {

        private static void postEvent() {
            GTMOGSStartupEvents.WORLD_GEN_LAYERS.post(new WorldGenLayerKubeEvent());
        }
    }
}
