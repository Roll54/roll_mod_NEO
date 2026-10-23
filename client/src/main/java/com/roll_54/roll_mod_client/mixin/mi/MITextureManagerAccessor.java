package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.textures.TextureManager;
import com.google.gson.JsonElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.BiConsumer;

/**
 * MI's runtime datagen JSON writer. It saves {@code <ns>:<path>} to {@code assets/<ns>/<path>} of the
 * generated pack ({@code TexturesProvider.customJsonSave}), so it takes any JSON, not just mcmeta.
 */
@Mixin(TextureManager.class)
public interface MITextureManagerAccessor {
    @Accessor("mcMetaWriter")
    BiConsumer<JsonElement, String> roll_mod$getJsonWriter();
}
