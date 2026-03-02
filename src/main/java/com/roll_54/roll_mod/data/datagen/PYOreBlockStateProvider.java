package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.init.GeneratedOreRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.server.packs.PackType;

public class PYOreBlockStateProvider extends BlockStateProvider {
    public PYOreBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RollMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (OreDefinition def : OreDefinitions.ALL) {
            for (var base : def.bases()) {
                String blockId = base.id() + "_" + def.oreName();
                GeneratedOreRegistry.BLOCKS.getEntries().stream()
                        .filter(holder -> holder.getId().getPath().equals(blockId))
                        .findFirst()
                        .ifPresent(blockHolder -> {
                            // Track the texture as generated so the model builder doesn't complain
                            models().existingFileHelper.trackGenerated(modLoc("block/" + blockId), PackType.CLIENT_RESOURCES, ".png", "textures");
                            simpleBlockWithItem(blockHolder.get(), cubeAll(blockHolder.get()));
                        });
            }
        }
    }
}
