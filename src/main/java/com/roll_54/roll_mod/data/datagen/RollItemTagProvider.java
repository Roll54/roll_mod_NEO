package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

import static com.roll_54.roll_mod.RollMod.MODID;
import static com.roll_54.roll_mod.registry.TagRegistry.*;


public class RollItemTagProvider extends ItemTagsProvider {

    public RollItemTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper) {
        super(
                output,
                lookupProvider,
                // parent item tags (none)
                CompletableFuture.completedFuture(TagLookup.empty()),
                MODID,
                existingFileHelper
        );
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

        var raw = tag(RAW_ORE);
        var crushed = tag(CRUSHED_ORE);
        var crushedRefined = tag(CRUSHED_REFINED_ORE);
        var crushedPurified = tag(CRUSHED_PURIFIED_ORE);
        var dust = tag(DUST_ORE);
        var dustPure = tag(DUST_PURE_ORE);
        var dustImpure = tag(DUST_IMPURE_ORE);
        var cDusts = tag(C_DUSTSTAG);

        for (OreDefinition definition : OreDefinitions.ALL) {

            String material = definition.id();

            ResourceLocation rawItem = ResourceLocation.fromNamespaceAndPath(MODID, "raw_" + material);
            ResourceLocation crushedItem = ResourceLocation.fromNamespaceAndPath(MODID, "crushed_" + material + "_ore");
            ResourceLocation refinedItem = ResourceLocation.fromNamespaceAndPath(MODID, "refined_" + material + "_ore");
            ResourceLocation purifiedItem = ResourceLocation.fromNamespaceAndPath(MODID, "purified_" + material + "_ore");
            ResourceLocation dustItem = ResourceLocation.fromNamespaceAndPath(MODID, material + "_dust");
            ResourceLocation dustPureItem = ResourceLocation.fromNamespaceAndPath(MODID, "pure_" + material + "_dust");
            ResourceLocation dustImpureItem = ResourceLocation.fromNamespaceAndPath(MODID, "impure_" + material + "_dust");


            raw.addOptional(rawItem);
            crushed.addOptional(crushedItem);
            crushedRefined.addOptional(refinedItem);
            crushedPurified.addOptional(purifiedItem);
            dust.addOptional(dustItem);
            dustPure.addOptional(dustPureItem);
            dustImpure.addOptional(dustImpureItem);
            cDusts.addOptional(dustItem);

            TagKey<Item> materialTag = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", material)
            );

            var materialAppender = tag(materialTag);

            materialAppender.addOptional(rawItem);
            materialAppender.addOptional(crushedItem);
            materialAppender.addOptional(refinedItem);
            materialAppender.addOptional(purifiedItem);
            materialAppender.addOptional(dustItem);
            materialAppender.addOptional(dustPureItem);
            materialAppender.addOptional(dustImpureItem);

            TagKey<Item> craftMaterial = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", "ores/" + material)
            );

            var craftMaterialAppender = tag(craftMaterial);

            craftMaterialAppender.addOptional(rawItem);

            for (OreTextureTemplates.BlockSubLayer layer : OreTextureTemplates.BlockSubLayer.values()) {

                ResourceLocation oreBlockItem = ResourceLocation.fromNamespaceAndPath(
                        MODID,
                        layer.id() + "_" + material
                );

                craftMaterialAppender.addOptional(oreBlockItem);
            }

            TagKey<Item> dustMaterial = TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath("c", "dusts/" + material)
            );

            var dustMaterialAppender = tag(dustMaterial);

            dustMaterialAppender.addOptional(dustItem);

        }


    }

}
