package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod.minestar.creative.CreativeScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TEI's item list in roll_mod's creative inventory. TEI only takes container screens, and {@code RollCreativeScreen}
 * is a plain LdLib screen, so the item list, its search and the sidebars were missing there. This lets TEI take it,
 * tells TEI when the creative search bar has focus (so typing there does not trigger TEI's keys — it only knows
 * vanilla's EditBox), and which item is under the mouse, so R / U / favorite work on the creative grid too.
 * The window's rectangle comes from {@code TeiLdlibBounds}.
 */
@Mixin(targets = "dev.lorp.tei.client.TeiClient", remap = false)
public abstract class TeiCreativeScreenMixin {

    @Inject(method = "isTyping", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$creativeSearch(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (CreativeScreens.isTyping(screen)) cir.setReturnValue(true);
    }

    @Inject(method = "hoveredInScreen", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$creativeHovered(Screen screen, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack hovered = CreativeScreens.hovered(screen);
        if (!hovered.isEmpty()) cir.setReturnValue(hovered);
    }
}
