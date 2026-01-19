package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.worldgen.RMMBiomeModifiers;
import com.roll_54.roll_mod.worldgen.RMMConfiguredFeatures;
import com.roll_54.roll_mod.worldgen.RMMPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.roll_54.roll_mod.RollMod.MODID;

public class RMMDatapackProvider extends DatapackBuiltinEntriesProvider {

    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, RMMConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, RMMPlacedFeatures::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, RMMBiomeModifiers::bootstrap);
            ;





    public RMMDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(MODID));
    }
}
