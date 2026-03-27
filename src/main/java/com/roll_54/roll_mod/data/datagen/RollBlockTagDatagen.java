package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.registry.GeneratedOreRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static com.roll_54.roll_mod.registry.TagRegistry.*;

public class RollBlockTagDatagen extends BlockTagsProvider {
    public RollBlockTagDatagen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RollMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        var pickaxeTag = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var needsStoneTag = tag(BlockTags.NEEDS_STONE_TOOL);

        var commonOreBlocks = tag(ORE_BLOCKS);

        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {
            for (BlockSubLayer base : def.bases()) {

                String blockId = base.id() + "_" + def.id();

                GeneratedOreRegistry.BLOCKS.getEntries().stream()
                        .filter(holder -> holder.getId().getPath().equals(blockId))
                        .findFirst()
                        .ifPresent(blockHolder -> {

                            var block = blockHolder.get();

                            pickaxeTag.add(block);
                            needsStoneTag.add(block);

                            commonOreBlocks.add(block);
                        });
            }
        }
    }
}
