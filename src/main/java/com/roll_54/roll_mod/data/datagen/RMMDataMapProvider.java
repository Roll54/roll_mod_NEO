package com.roll_54.roll_mod.data.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

public class RMMDataMapProvider extends DataMapProvider {
    protected RMMDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(packOutput, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider){

        this.builder(NeoForgeDataMaps.COMPOSTABLES)
                .add(ResourceLocation.parse("silentgear:netherwood_leaves"), new Compostable(0.25f), false);
    }
}
