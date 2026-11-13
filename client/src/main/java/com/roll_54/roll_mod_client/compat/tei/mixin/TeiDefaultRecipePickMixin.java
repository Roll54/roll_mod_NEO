package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreePicker;
import dev.lorp.tei.api.TeiDisplay;
import dev.lorp.tei.client.DefaultRecipes;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In resolve mode TEI's heart button and "default stack" key also pick a recipe: they set it as the default
 * recipe and go back to the tree. While the roll_mod handset tree is picking, that recipe goes there instead and
 * TEI's default recipes stay as they are (like EMI's {@code BoM.addRecipe} in {@code BoMRecipeTreePickMixin}).
 * Outside a handset pick these calls are untouched.
 */
@Mixin(value = DefaultRecipes.class, remap = false)
public abstract class TeiDefaultRecipePickMixin {

    @Inject(method = "toggle", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickDefault(TeiDisplay recipe, CallbackInfo ci) {
        if (TeiRecipeTreePicker.offer(recipe)) ci.cancel();
    }

    @Inject(method = "toggleStack", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickStackDefault(TeiDisplay recipe, Item forItem, CallbackInfo ci) {
        if (TeiRecipeTreePicker.offer(recipe)) ci.cancel();
    }
}
