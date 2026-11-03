package com.roll_54.roll_mod.gtmogs.data.datagen;

import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.data.worldgen.*;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTDensityFunctions;
import com.roll_54.roll_mod.gtmogs.data.worldgen.GTOreVeins;

import java.util.Set;

/**
 * Not hooked into roll_mod's datagen: GTMOGS's own bootstrap veins and density functions are not part of
 * the pack, whose veins are hand-written under {@code data/roll_mod/roll_mod/ore_vein/}.
 */
public class GTVanillaDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        var registries = event.getLookupProvider();
        if (event.includeServer()) {
            var set = Set.of(GTMOGS.MOD_ID);
            DatapackBuiltinEntriesProvider provider = generator.addProvider(true, new DatapackBuiltinEntriesProvider(
                    packOutput, registries, new RegistrySetBuilder()
                            .add(Registries.DENSITY_FUNCTION, GTDensityFunctions::bootstrap)
                            .add(GTRegistries.ORE_VEIN_REGISTRY, GTOreVeins::bootstrap),
                    set));
        }
    }
}
