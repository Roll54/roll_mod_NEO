package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.textures.MITextures;
import aztech.modern_industrialization.client.textures.TextureManager;
import aztech.modern_industrialization.client.textures.coloramp.Coloramp;
import aztech.modern_industrialization.materials.Material;
import aztech.modern_industrialization.materials.part.MaterialItemPart;
import aztech.modern_industrialization.materials.part.PartKeyProvider;
import aztech.modern_industrialization.materials.property.MaterialProperty;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_client.compat.fusion.MiCasingConnectedTextures;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.function.BiConsumer;

/**
 * Generates Fusion connected textures next to every MI material casing texture. MI tints
 * {@code materialsets/<set>/<part>.png} into {@code block/<id>.png}; here the same coloramp tints the
 * {@code <part>_ct} template (a 4x4 Fusion "simple" sheet, served from roll_mod's override folder by
 * {@link MITextureManagerOverrideMixin}) into {@code block/<id>_ct.png} and {@code block/<id>_mct.png}, and
 * the mcmetas, connected block model and Fusion model modifier are written into the same generated pack.
 * The pattern itself lives in {@link MiCasingConnectedTextures}; the machine casing models are done in
 * {@link MIMachineCasingsProviderMixin}.
 */
@Mixin(targets = "aztech.modern_industrialization.client.textures.PartTextureGenerator")
public class MICasingConnectedTexturesMixin {
    @Shadow
    @Final
    private Coloramp coloramp;
    @Shadow
    @Final
    private TextureManager mtm;
    @Shadow
    @Final
    private Material material;

    @Inject(method = "processCasing", at = @At("TAIL"))
    private void roll_mod$connectedTextures(PartKeyProvider part, CallbackInfo ci) throws IOException {
        if (!MiCasingConnectedTextures.enabled() || !(part instanceof MaterialItemPart itemPart)) return;

        String setName = material.get(MaterialProperty.SET).name;
        String template = part.key().key + "_ct";
        if (!mtm.hasAsset("modern_industrialization:textures/materialsets/common/" + template + ".png")) return;

        // Same namespace MI writes the casing texture to (PartTextureGenerator keeps only the path).
        ResourceLocation block = ResourceLocation.fromNamespaceAndPath("modern_industrialization",
                ResourceLocation.parse(itemPart.getItemId()).getPath());
        String id = block.getPath();

        NativeImage image = MITextures.generateTexture(mtm, template, setName, coloramp);
        try {
            mtm.addTexture("modern_industrialization:textures/block/" + id + "_ct.png", image);
            mtm.addTexture("modern_industrialization:textures/block/" + id + "_mct.png", image);
        } finally {
            image.close();
        }

        BiConsumer<JsonElement, String> json = ((MITextureManagerAccessor) mtm).roll_mod$getJsonWriter();
        json.accept(MiCasingConnectedTextures.textureMeta(block, false), "modern_industrialization:textures/block/" + id + "_ct.png.mcmeta");
        json.accept(MiCasingConnectedTextures.textureMeta(block, true), "modern_industrialization:textures/block/" + id + "_mct.png.mcmeta");
        json.accept(MiCasingConnectedTextures.connectedBlockModel(block), "modern_industrialization:models/block/" + id + "_connected.json");
        json.accept(MiCasingConnectedTextures.modelModifier(block), "modern_industrialization:fusion/model_modifiers/blocks/" + id + ".json");
        RollMod.LOGGER.debug("[{}] Generated connected textures for MI casing {}", RollMod.MODID, block);
    }
}
