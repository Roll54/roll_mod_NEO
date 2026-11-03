package com.roll_54.roll_mod.gtmogs.common;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.roll_54.roll_mod.gtmogs.api.addon.AddonFinder;
import com.roll_54.roll_mod.gtmogs.api.addon.IGTAddon;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.WorldGenLayers;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.VeinGenerators;
import com.roll_54.roll_mod.gtmogs.api.worldgen.generator.indicators.IndicatorGenerators;
import com.roll_54.roll_mod.gtmogs.common.pack.GTDynamicDataPack;
import com.roll_54.roll_mod.gtmogs.common.pack.GTDynamicResourcePack;
import com.roll_54.roll_mod.gtmogs.common.pack.GTPackSource;
import com.roll_54.roll_mod.gtmogs.data.command.GTCommandArguments;
import com.roll_54.roll_mod.gtmogs.data.datagen.GTRegistrateDatagen;
import com.roll_54.roll_mod.gtmogs.data.misc.GTDimensionMarkers;
import com.roll_54.roll_mod.gtmogs.integration.map.WaypointManager;
import com.roll_54.roll_mod.gtmogs.utils.input.KeyBind;

public class CommonInit {

    private static IEventBus modBus;

    public static void init(final IEventBus modBus) {
        CommonInit.modBus = modBus;
        modBus.register(CommonInit.class);

        GTRegistrateDatagen.initPre();
        GTRegistries.init(modBus);

        AddonFinder.getAddonList().forEach(IGTAddon::gtInitComplete);
    }

    // Only register everything once.
    private static boolean didRunRegistration = false;

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (didRunRegistration) {
            return;
        }
        didRunRegistration = true;

        GTDimensionMarkers.init();

        GTCommandArguments.COMMAND_ARGUMENT_TYPES.register(modBus);
        WorldGenLayers.registerAll();
        VeinGenerators.registerAddonGenerators();
        IndicatorGenerators.registerAddonGenerators();
        WaypointManager.init();
        GTRegistrateDatagen.initPost();
        KeyBind.init();
    }

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        GTRegistries.getRegistries().forEach(event::register);
    }

    @SubscribeEvent
    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(GTRegistries.ORE_VEIN_REGISTRY,
                OreVeinDefinition.DIRECT_CODEC, OreVeinDefinition.DIRECT_CODEC);
    }

    @SubscribeEvent
    public static void loadComplete(FMLLoadCompleteEvent event) {}

    @SubscribeEvent
    public static void registerPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            // Clear old data
            GTDynamicResourcePack.clearClient();

            event.addRepositorySource(new GTPackSource("gtmogs:dynamic_assets",
                    event.getPackType(),
                    Pack.Position.BOTTOM,
                    GTDynamicResourcePack::new));
        } else if (event.getPackType() == PackType.SERVER_DATA) {
            // Clear old data
            GTDynamicDataPack.clearServer();

            // LOADING MOVED TO ReloadableServerResourcesMixin

            event.addRepositorySource(new GTPackSource("gtmogs:dynamic_data",
                    event.getPackType(),
                    Pack.Position.BOTTOM,
                    GTDynamicDataPack::new));
        }
    }
}
