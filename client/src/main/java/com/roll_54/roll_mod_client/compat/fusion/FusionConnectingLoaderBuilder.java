package com.roll_54.roll_mod_client.compat.fusion;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Datagen builder for a casing's machine-face Fusion connecting model
 * ({@link MiCasingConnectedTextures#writeConnectingModel}). It writes the whole model, textures included,
 * so the builder never checks for textures that MI's runtime datagen has not generated yet.
 */
public final class FusionConnectingLoaderBuilder extends CustomLoaderBuilder<BlockModelBuilder> {
    private final ResourceLocation block;

    public FusionConnectingLoaderBuilder(BlockModelBuilder parent, ExistingFileHelper helper, ResourceLocation block){
        super(ResourceLocation.fromNamespaceAndPath("fusion", "model"), parent, helper, false);
        this.block = block;
    }

    @Override
    public JsonObject toJson(JsonObject json){
        json = super.toJson(json);
        MiCasingConnectedTextures.writeConnectingModel(json, block, true);
        return json;
    }
}
