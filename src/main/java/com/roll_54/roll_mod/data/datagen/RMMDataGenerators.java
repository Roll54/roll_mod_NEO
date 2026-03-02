package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;


@EventBusSubscriber(modid = RollMod.MODID)
public class RMMDataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {

        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(), new PYOreTextureProvider(packOutput));

        generator.addProvider(event.includeServer(), new RMMDataMapProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeClient(), new RMMItemModelProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeClient(), new PYOreBlockStateProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeServer(), new PYOreLootTableProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeServer(), new RMMDatapackProvider(packOutput, lookupProvider));


        generator.addProvider(event.includeClient(), new PYOreItemModelProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeServer(), new PYOreBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));

    }
}
