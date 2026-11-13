package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod_client.compat.tei.tree.TeiRecipeTreePicker;
import dev.lorp.tei.api.TeiDisplay;
import dev.lorp.tei.client.gui.RecipeTreeScreen;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TEI's recipe screen in resolve mode hands the chosen recipe (or "no recipe", null) to its own tree through
 * {@code RecipeTreeScreen.setResolution}. While the roll_mod handset tree is picking, the recipe goes there instead
 * and TEI's tree is left alone. Counterpart of the EMI tree's {@code BoMRecipeTreePickMixin} /
 * {@code ResolutionButtonPickMixin}.
 */
@Mixin(value = RecipeTreeScreen.class, remap = false)
public abstract class TeiResolvePickMixin {

    @Inject(method = "setResolution", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pick(Item item, TeiDisplay recipe, CallbackInfo ci) {
        if (TeiRecipeTreePicker.offer(recipe)) ci.cancel();
    }
}
