package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.datagen.py.OreDefinition;
import com.roll_54.roll_mod.data.datagen.py.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.py.PyTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.init.GeneratedOreRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class PYOreBlockTagsProvider extends BlockTagsProvider {
    public PYOreBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RollMod.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxeTag = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var needsIronTag = tag(BlockTags.NEEDS_IRON_TOOL); // Defaulting to Iron for now

        for (OreDefinition def : OreDefinitions.ALL) {
            for (BlockSubLayer base : def.bases()) {
                String blockId = base.id() + "_" + def.oreName();
                GeneratedOreRegistry.BLOCKS.getEntries().stream()
                        .filter(holder -> holder.getId().getPath().equals(blockId))
                        .findFirst()
                        .ifPresent(blockHolder -> {
                            pickaxeTag.add(blockHolder.get());
                            // You can adjust mining tiers here if needed based on OreDefinition properties
                            // For example if you add a mining tier to OreDefinition record.
                            needsIronTag.add(blockHolder.get());
                        });
            }
        }
    }
}
