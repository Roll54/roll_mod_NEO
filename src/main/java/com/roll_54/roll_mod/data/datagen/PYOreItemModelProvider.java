package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import net.minecraft.data.PackOutput;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.HashSet;
import java.util.Set;

public class PYOreItemModelProvider extends ItemModelProvider {
    private final Set<String> emitted = new HashSet<>();

    public PYOreItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RollMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (OreDefinition def : OreDefinitions.ALL) {
            String ore = def.oreName();
            emit("raw_" + ore);
            emit(ore + "_dust");
            emit("crushed_" + ore + "_ore");
            emit("refined_" + ore + "_ore");
            emit("purified_" + ore + "_ore");
            emit("pure_" + ore + "_dust");
            emit("impure_" + ore + "_dust");
        }
    }

    private void emit(String name) {
        if (emitted.add(name)) {
            existingFileHelper.trackGenerated(modLoc("item/" + name), PackType.CLIENT_RESOURCES, ".png", "textures");
            withExistingParent(name, "item/generated").texture("layer0", modLoc("item/" + name));
        }
    }

    @Override
    public String getName() {
        return "PY Ore Item Models: " + RollMod.MODID;
    }
}
