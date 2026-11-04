package com.roll_54.roll_mod.gtmogs.integration.kjs;

import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.GTMOGSAPI;
import com.roll_54.roll_mod.gtmogs.api.GTValues;
import com.roll_54.roll_mod.gtmogs.api.gui.GuiTextures;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.*;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.DikeVeinGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.veins.NoopVeinGenerator;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;
import com.roll_54.roll_mod.gtmogs.integration.kjs.builders.worldgen.DimensionMarkerBuilder;
import com.roll_54.roll_mod.gtmogs.integration.kjs.builders.worldgen.OreVeinDefinitionBuilder;
import com.roll_54.roll_mod.gtmogs.integration.kjs.helpers.GTResourceLocation;
import dev.latvian.mods.kubejs.block.state.BlockStatePredicate;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.registry.ServerRegistryRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.TypeWrapperRegistry;
import dev.latvian.mods.rhino.Wrapper;

public class GTKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.addDefault(GTRegistries.DIMENSION_MARKER_REGISTRY, DimensionMarkerBuilder.class,
                DimensionMarkerBuilder::new);
        registry.addDefault(GTRegistries.ORE_VEIN_REGISTRY, OreVeinDefinitionBuilder.class,
                OreVeinDefinitionBuilder::new);
    }

    @Override
    public void registerServerRegistries(ServerRegistryRegistry registry) {
        registry.register(GTRegistries.ORE_VEIN_REGISTRY, OreVeinDefinition.DIRECT_CODEC, OreVeinDefinition.class);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GTMOGSStartupEvents.GROUP);
        registry.register(GTMOGSServerEvents.GROUP);
    }

    @Override
    public void registerClasses(ClassFilter filter) {
        // allow user to access all gtmogs classes by importing them.
        filter.allow("com.roll_54.roll_mod.gtmogs");
        filter.deny("com.quanntumgarbage.gtmogs.core");
        filter.deny("com.roll_54.roll_mod.gtmogs.common.network");
    }

    @Override
    public void registerBindings(BindingRegistry event) {
        // Mod related
        event.add("GTMOGS", GTMOGS.class);
        event.add("GTMOGSAPI", GTMOGSAPI.class);
        event.add("GTRegistries", GTRegistries.class);
        event.add("GTValues", GTValues.class);

        // GUI related
        event.add("GuiTextures", GuiTextures.class);

        // World Gen Related
        event.add("GTOreVein", OreVeinDefinition.class);
        event.add("OreVeinDefinition", OreVeinDefinition.class);
        event.add("GTLayerPattern", GTLayerPattern.class);
        event.add("GTDikeBlockDefinition", DikeVeinGenerator.DikeBlockDefinition.class);
        event.add("GTOres", GTOreVeins.class);
        event.add("GTOreVeins", GTOreVeins.class);
        event.add("GTWorldGenLayers", WorldGenLayers.class);
    }

    @Override
    public void registerTypeWrappers(TypeWrapperRegistry registry) {
        registry.register(GTResourceLocation.class, GTResourceLocation::wrap);
        registry.register(IWorldGenLayer.class, o -> {
            o = Wrapper.unwrapped(o);
            if (o instanceof IWorldGenLayer layer) return layer;
            if (o instanceof CharSequence chars) return WorldGenLayers.getByName(chars.toString());
            return null;
        });
        // A vanilla class, so KubeJS may wrap it itself: newer builds (2101.7.2-build.374) do, and a second
        // registration throws "Wrapper for class ... already exists!" and takes mod loading down with it.
        if (!registry.hasTypeWrapper(HeightRangePlacement.class)) {
            registry.registerMapCodec(HeightRangePlacement.class, HeightRangePlacement.CODEC);
        }
        registry.registerCodec(BiomeWeightModifier.class, BiomeWeightModifier.CODEC, BiomeWeightModifier.EMPTY);
        registry.registerCodec(VeinGenerator.class, VeinGenerator.DIRECT_CODEC, NoopVeinGenerator.INSTANCE);

        registry.register(IWorldGenLayer.RuleTestSupplier.class, (cx, o, t) -> {
            if (o instanceof IWorldGenLayer.RuleTestSupplier supplier) return supplier;
            return () -> BlockStatePredicate.wrapRuleTest(cx, o);
        });
    }
}
