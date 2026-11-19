package com.roll_54.roll_mod.mixin.recipebook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * No recipe book on the server: nothing is unlocked or locked (so no recipe-book packet, no "new
 * recipes" toast, no recipe list growing in player data), and the full book is never sent on join.
 * The client has no recipe book either — see {@code NoRecipeBook} and {@link RecipeBookPacketsMixin}.
 */
@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin {

    @Inject(method = "addRecipes", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noUnlock(Collection<RecipeHolder<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "removeRecipes", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noLock(Collection<RecipeHolder<?>> recipes, ServerPlayer player, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "sendInitialRecipeBook", at = @At("HEAD"), cancellable = true)
    private void roll_mod$noInitialBook(ServerPlayer player, CallbackInfo ci) {
        ci.cancel();
    }
}
