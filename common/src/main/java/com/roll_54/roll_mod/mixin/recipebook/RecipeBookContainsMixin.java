package com.roll_54.roll_mod.mixin.recipebook;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With no recipe book, every recipe counts as known on the server. Vanilla asks in two places: placing
 * a recipe into a crafting grid ({@code ServerPlaceRecipe}) — which TEI's grid transfer uses, so it keeps
 * working for every recipe — and the {@code doLimitedCrafting} game rule, which this makes a no-op.
 * The client's copy is left alone.
 */
@Mixin(RecipeBook.class)
public abstract class RecipeBookContainsMixin {

    @Inject(method = "contains(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("HEAD"), cancellable = true)
    private void roll_mod$allKnown(RecipeHolder<?> recipe, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerRecipeBook && recipe != null) cir.setReturnValue(true);
    }

    @Inject(method = "contains(Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("HEAD"), cancellable = true)
    private void roll_mod$allKnownById(ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerRecipeBook) cir.setReturnValue(true);
    }
}
