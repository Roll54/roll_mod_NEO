package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.PYDatagen.PYOreDataGen;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

public class PYOreBlockStateProvider extends BlockStateProvider {
    public PYOreBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, RollMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (DeferredHolder<Block, ? extends Block> entry : PYOreDataGen.ORE_BLOCKS.getEntries()) {
            Block block = entry.get();
            // cube_all model for ore texture and item uses the same model
            ModelFile model = cubeAll(block);
            simpleBlockWithItem(block, model);
        }
    }
}
