package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.datagen.model.MachineCasingsProvider;
import aztech.modern_industrialization.machines.models.MachineCasing;
import com.roll_54.roll_mod_client.compat.fusion.FusionConnectingLoaderBuilder;
import com.roll_54.roll_mod_client.compat.fusion.MiCasingConnectedTextures;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Machines show their casing through {@code <casing ns>:machine_casing/<casing>}, which MI's runtime
 * datagen writes as a {@code use_block_model} copy of the imitated block. For casings with connected
 * textures ({@link MiCasingConnectedTextures#casings()}) it is written as a Fusion connecting model on
 * the block's {@code _mct} texture instead, so a machine's casing faces join the casing blocks around it.
 * This covers every casing imitating such a block, e.g. MI's {@code steel} and EI's {@code steel_pipe}.
 */
@Mixin(MachineCasingsProvider.class)
public class MIMachineCasingsProviderMixin {

    @Inject(method = "imitateBlock", at = @At("HEAD"), cancellable = true)
    private void roll_mod$connectedCasing(MachineCasing casing, Block block, CallbackInfo ci) {
        if (!MiCasingConnectedTextures.enabled()) return;
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        if (!MiCasingConnectedTextures.casings().contains(blockId)) return;

        @SuppressWarnings("unchecked")
        ModelProvider<BlockModelBuilder> provider = (ModelProvider<BlockModelBuilder>) (Object) this;
        provider.getBuilder(casing.key.toString()).customLoader((parent, helper) -> new FusionConnectingLoaderBuilder(parent, helper, blockId));
        ci.cancel();
    }
}
