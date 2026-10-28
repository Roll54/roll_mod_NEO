package com.roll_54.roll_mod_client.compat.jei.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops JEI logging two "Received invalid gui properties ... screenWidth must be greater than 1" errors
 * every time an LdLib screen (the hub, vending and service blocks, ...) opens.
 *
 * <p>JEI reads a container screen's size when it is opened, but LdLib's screens are not sized until
 * they are initialised; JEI then reads them again on init, correctly. The open-time read only refreshes
 * the same two overlays the init-time one does, so skipping it for a screen with no size yet loses
 * nothing.
 *
 * <p>Targets JEI by name (only its API is on the compile classpath); the config is soft, so without JEI
 * this is skipped.
 */
@Mixin(targets = "mezz.jei.gui.events.GuiEventHandler", remap = false)
public abstract class JeiUnsizedScreenMixin {

    @Inject(method = "onGuiOpen(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"), cancellable = true)
    private void roll_mod$waitForInit(Screen screen, CallbackInfo ci) {
        if (screen != null && (screen.width <= 1 || screen.height <= 1)) ci.cancel();
    }
}
