package com.roll_54.roll_mod_client.compat.emi.mixin;

import com.roll_54.roll_mod_client.compat.emi.tree.RecipeTreePicker;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.bom.BoM;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every way EMI's recipe screen lets you pick a recipe while resolving (the default button, the
 * "resolve" and "set default" keys on a slot) ends in one of these {@link BoM} calls. While the roll_mod
 * recipe tree is picking, the recipe goes there and EMI's own tree is left alone.
 */
@Mixin(value = BoM.class, remap = false)
public abstract class BoMRecipeTreePickMixin {

    @Inject(method = "addResolution", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickResolution(EmiIngredient ingredient, EmiRecipe recipe, CallbackInfo ci) {
        if (RecipeTreePicker.offer(recipe)) ci.cancel();
    }

    @Inject(method = "addRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickDefault(EmiRecipe recipe, CallbackInfo ci) {
        if (RecipeTreePicker.offer(recipe)) ci.cancel();
    }

    @Inject(method = "addRecipe(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V",
            at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickStackDefault(EmiIngredient stack, EmiRecipe recipe, CallbackInfo ci) {
        if (RecipeTreePicker.offer(recipe)) ci.cancel();
    }

    // A recipe that already is EMI's default toggles off instead; picking it still means "use this one".
    @Inject(method = "removeRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;)V", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickRemovedDefault(EmiRecipe recipe, CallbackInfo ci) {
        if (RecipeTreePicker.offer(recipe)) ci.cancel();
    }

    @Inject(method = "removeRecipe(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiRecipe;)V",
            at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickRemovedStackDefault(EmiIngredient stack, EmiRecipe recipe, CallbackInfo ci) {
        if (RecipeTreePicker.offer(recipe)) ci.cancel();
    }
}
