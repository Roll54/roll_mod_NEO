package com.roll_54.roll_mod.mixin.recipebook;

import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The recipe-book packets a client can still send (an unmodded client, or another mod) are ignored:
 * book open/filter settings and "recipe seen" marks. The place-recipe packet is kept — TEI's grid
 * transfer uses it.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RecipeBookPacketsMixin {

    @Inject(method = "handleRecipeBookChangeSettingsPacket", at = @At("HEAD"), cancellable = true)
    private void roll_mod$ignoreSettings(ServerboundRecipeBookChangeSettingsPacket packet, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "handleRecipeBookSeenRecipePacket", at = @At("HEAD"), cancellable = true)
    private void roll_mod$ignoreSeen(ServerboundRecipeBookSeenRecipePacket packet, CallbackInfo ci) {
        ci.cancel();
    }
}
