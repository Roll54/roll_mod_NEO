package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.OreSampleBlock;
import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.minecraft.server.packs.PackType;

public class OreBlockStateProvider extends BlockStateProvider {
    public OreBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RollMod.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            for (var base : def.bases()) {
                String blockId = base.id() + "_" + def.id();
                GeneratedOreRegistry.BLOCKS.getEntries().stream()
                        .filter(holder -> holder.getId().getPath().equals(blockId))
                        .findFirst()
                        .ifPresent(blockHolder -> {
                            // Track the texture as generated so the model builder doesn't complain
                            models().existingFileHelper.trackGenerated(modLoc("block/" + blockId), PackType.CLIENT_RESOURCES, ".png", "textures");
                            simpleBlockWithItem(blockHolder.get(), cubeAll(blockHolder.get()));
                        });

                // Ore sample: same texture as the ore block above, on the shared outcrop geometry.
                String sampleId = GeneratedOreRegistry.oreSampleId(base, def.id());
                GeneratedOreRegistry.BLOCKS.getEntries().stream()
                        .filter(holder -> holder.getId().getPath().equals(sampleId))
                        .findFirst()
                        .ifPresent(blockHolder -> oreSample(blockHolder.get(), sampleId, blockId));
            }

            String rawBlockId = "raw_" + def.id() + "_block";
            GeneratedOreRegistry.BLOCKS.getEntries().stream()
                    .filter(holder -> holder.getId().getPath().equals(rawBlockId))
                    .findFirst()
                    .ifPresent(blockHolder -> {
                        models().existingFileHelper.trackGenerated(modLoc("block/" + rawBlockId), PackType.CLIENT_RESOURCES, ".png", "textures");
                        simpleBlockWithItem(blockHolder.get(), cubeAll(blockHolder.get()));
                    });
        }
    }

    /**
     * Emits the sample model (hand-written {@code block/ore_sample} geometry textured with the ore
     * block's texture) plus a two-state blockstate: upright on the floor, flipped on the ceiling.
     */
    private void oreSample(Block sampleBlock, String sampleId, String oreBlockId) {
        models().existingFileHelper.trackGenerated(modLoc("block/" + oreBlockId), PackType.CLIENT_RESOURCES, ".png", "textures");

        ModelFile model = models().withExistingParent(sampleId, modLoc("block/ore_sample"))
                .texture("particle", modLoc("block/" + oreBlockId))
                .texture("ore", modLoc("block/" + oreBlockId));

        getVariantBuilder(sampleBlock)
                .partialState().with(OreSampleBlock.VERTICAL_DIRECTION, Direction.UP)
                .modelForState().modelFile(model).addModel()
                .partialState().with(OreSampleBlock.VERTICAL_DIRECTION, Direction.DOWN)
                .modelForState().modelFile(model).rotationX(180).addModel();

        itemModels().withExistingParent(sampleId, modLoc("block/" + sampleId));
    }
}
