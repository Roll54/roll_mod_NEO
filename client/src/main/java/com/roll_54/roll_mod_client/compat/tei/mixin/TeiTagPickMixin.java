package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod_client.compat.tei.tree.TeiTagPicker;
import dev.lorp.tei.client.gui.RecipeScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A click on an item in TEI opens its recipes through {@code RecipeScreen.open}. While the roll_mod handset tree is
 * choosing an item for a tag node on TEI's tag page, a click on one of that tag's items is the choice instead
 * ({@link TeiTagPicker}).
 */
@Mixin(value = RecipeScreen.class, remap = false)
public abstract class TeiTagPickMixin {

    @Inject(method = "open(Lnet/minecraft/world/item/ItemStack;Z)Z", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$pickTagItem(ItemStack stack, boolean uses, CallbackInfoReturnable<Boolean> cir) {
        if (TeiTagPicker.offer(stack)) cir.setReturnValue(true);
    }
}
