package com.roll_54.roll_mod_client.compat.tei.mixin;

import com.roll_54.roll_mod.minestar.hub.gui.HubScreens;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The hub is a container screen, so TEI drew its item list and sidebars over it. The hub covers the recipe browser
 * instead: TEI's overlay does not take the hub screen at all.
 */
@Mixin(targets = "dev.lorp.tei.client.gui.ItemListOverlay", remap = false)
public abstract class TeiHideOnHubMixin {

    @Inject(method = "supports", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$notOnHub(Screen screen, CallbackInfoReturnable<Boolean> cir) {
        if (HubScreens.isHub(screen)) cir.setReturnValue(false);
    }
}
