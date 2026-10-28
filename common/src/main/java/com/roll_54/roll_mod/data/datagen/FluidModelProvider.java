package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.FluidRegistry;
import com.roll_54.roll_mod.registry.FluidRegistry.RollFluid;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Blockstates, block models and bucket models for {@link FluidRegistry}'s fluids.
 *
 * <p>A liquid block is drawn by the fluid renderer, so its model only carries a particle texture (the
 * still texture). Buckets with their own texture in {@code textures/item/<name>_bucket.png} get a
 * plain item model; the rest use NeoForge's dynamic bucket, which paints the fluid's still texture
 * into a vanilla bucket.
 */
public class FluidModelProvider extends BlockStateProvider {
    private final ExistingFileHelper existingFileHelper;

    public FluidModelProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RollMod.MODID, exFileHelper);
        this.existingFileHelper = exFileHelper;
    }

    @Override
    public String getName() {
        // DataGenerator keys providers by name; BlockStateProvider's default would collide.
        return "Fluid Block States: " + RollMod.MODID;
    }

    @Override
    protected void registerStatesAndModels() {
        for (RollFluid fluid : FluidRegistry.all()) {
            simpleBlock(fluid.block().get(), models().getBuilder(fluid.name())
                    .texture("particle", RollMod.id(fluid.stillTexture())));

            String bucket = fluid.name() + "_bucket";
            ResourceLocation bucketTexture = RollMod.id("item/" + bucket);
            if (existingFileHelper.exists(bucketTexture, ModelProvider.TEXTURE)) {
                itemModels().basicItem(fluid.bucket().get());
            } else {
                itemModels().withExistingParent(bucket, ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket"))
                        .customLoader(DynamicFluidContainerModelBuilder::begin)
                        .fluid(fluid.source().get());
            }
        }
    }
}
