package com.roll_54.roll_mod.data.datagen;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.mi.machines.CoilPyrolyseOven;
import com.roll_54.roll_mod.compat.mi.machines.FluidHatches;
import com.roll_54.roll_mod.compat.mi.machines.OreWashers;
import com.roll_54.roll_mod.compat.mi.machines.HighlyAdvancedDistillery;
import com.roll_54.roll_mod.compat.mi.machines.ParallelMultiblocks;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Blockstates and models for the MI machines registered under {@code roll_mod:}
 * ({@link ParallelMultiblocks}, {@link FluidHatches}).
 *
 * <p>MI's runtime datagen only writes models for machines in its own namespace (see
 * {@code MIMachineModels}), so these are written here, in the same shape MI ships for its own
 * machines: a {@code modern_industrialization:machine} model naming a casing and overlay textures.
 * The front overlays are borrowed from the machine each one is the big version of.
 */
public class RollMachineModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockstates;
    private final PackOutput.PathProvider models;

    public RollMachineModelProvider(PackOutput output) {
        this.blockstates = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        this.models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (ParallelMultiblocks.Spec spec : ParallelMultiblocks.ALL) {
            JsonObject overlays = new JsonObject();
            overlays.addProperty("fluid_auto", "modern_industrialization:block/overlays/fluid_auto");
            overlays.addProperty("front", spec.overlays() + "/overlay_front");
            overlays.addProperty("front_active", spec.overlays() + "/overlay_front_active");
            overlays.addProperty("item_auto", "modern_industrialization:block/overlays/item_auto");
            overlays.addProperty("output", "modern_industrialization:block/overlays/output");
            machine(cache, writes, spec.id(), spec.casing(), overlays);
        }
        {
            JsonObject overlays = new JsonObject();
            overlays.addProperty("fluid_auto", "modern_industrialization:block/overlays/fluid_auto");
            overlays.addProperty("front", CoilPyrolyseOven.OVERLAYS + "/overlay_front");
            overlays.addProperty("front_active", CoilPyrolyseOven.OVERLAYS + "/overlay_front_active");
            overlays.addProperty("item_auto", "modern_industrialization:block/overlays/item_auto");
            overlays.addProperty("output", "modern_industrialization:block/overlays/output");
            machine(cache, writes, CoilPyrolyseOven.ID, CoilPyrolyseOven.CASING, overlays);
        }
        frontMachine(cache, writes, OreWashers.ORE_WASHER, OreWashers.ORE_WASHER_CASING, OreWashers.ORE_WASHER_OVERLAYS);
        frontMachine(cache, writes, HighlyAdvancedDistillery.ID, HighlyAdvancedDistillery.CASING,
                HighlyAdvancedDistillery.OVERLAYS);
        frontMachine(cache, writes, OreWashers.STEAM_ORE_WASHER, OreWashers.STEAM_ORE_WASHER_CASING,
                OreWashers.STEAM_ORE_WASHER_OVERLAYS);
        for (FluidHatches.Spec spec : FluidHatches.ALL) {
            // Same overlays MI gives its own fluid hatches, input and output alike.
            JsonObject overlays = new JsonObject();
            overlays.addProperty("fluid_auto", "modern_industrialization:block/overlays/fluid_auto");
            overlays.addProperty("output", "modern_industrialization:block/overlays/output_fluid");
            overlays.addProperty("side", "modern_industrialization:block/machines/hatch_fluid/overlay_side");
            for (String id : spec.ids()) {
                machine(cache, writes, id, spec.casing(), overlays);
            }
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    /** A multiblock controller with a front overlay from {@code overlays}, as the ones above. */
    private void frontMachine(CachedOutput cache, List<CompletableFuture<?>> writes, String id, String casing,
                              ResourceLocation overlayFolder) {
        JsonObject overlays = new JsonObject();
        overlays.addProperty("fluid_auto", "modern_industrialization:block/overlays/fluid_auto");
        overlays.addProperty("front", overlayFolder + "/overlay_front");
        overlays.addProperty("front_active", overlayFolder + "/overlay_front_active");
        overlays.addProperty("item_auto", "modern_industrialization:block/overlays/item_auto");
        overlays.addProperty("output", "modern_industrialization:block/overlays/output");
        machine(cache, writes, id, casing, overlays);
    }

    /** Blockstate, block model and item model of one {@code modern_industrialization:machine} block. */
    private void machine(CachedOutput cache, List<CompletableFuture<?>> writes, String id, String casing,
                         JsonObject overlays) {
        String blockModel = RollMod.id("block/" + id).toString();

        JsonObject variant = new JsonObject();
        variant.addProperty("model", blockModel);
        JsonObject variants = new JsonObject();
        variants.add("", variant);
        JsonObject blockstate = new JsonObject();
        blockstate.add("variants", variants);

        JsonObject model = new JsonObject();
        model.addProperty("casing", casing);
        model.add("default_overlays", overlays);
        model.addProperty("loader", "modern_industrialization:machine");

        JsonObject item = new JsonObject();
        item.addProperty("parent", blockModel);

        writes.add(DataProvider.saveStable(cache, blockstate, blockstates.json(RollMod.id(id))));
        writes.add(DataProvider.saveStable(cache, model, models.json(RollMod.id("block/" + id))));
        writes.add(DataProvider.saveStable(cache, item, models.json(RollMod.id("item/" + id))));
    }

    @Override
    public String getName() {
        return "Machine Models: " + RollMod.MODID;
    }
}
