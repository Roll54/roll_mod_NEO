package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.fzzyui.RollConfigScreen;
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sends every Fzzy Config screen to the LdLib one ({@link RollConfigScreen}). Fzzy opens its screens
 * through these two methods only: {@code openScreen} behind {@code ConfigApi.openScreen} and the
 * {@code /configure} command, {@code provideScreen} behind the mod list's Config button.
 *
 * <p>Holding Shift while opening keeps Fzzy's own screen — the way back if a Fzzy update ever breaks
 * the LdLib one. So does a scope the LdLib screen has nothing to show for.
 *
 * <p>Both targets are Kotlin {@code internal} members, which compile to these suffixed names.
 */
@Mixin(value = ClientConfigRegistry.class, remap = false)
public abstract class FzzyConfigRegistryMixin {

    @Inject(method = "openScreen$fzzy_config", at = @At("HEAD"), cancellable = true, remap = false)
    private void roll_mod$openLdLibScreen(String scope, CallbackInfo ci) {
        if (Screen.hasShiftDown()) return;
        Minecraft mc = Minecraft.getInstance();
        Screen screen = RollConfigScreen.open(scope, mc.screen);
        if (screen == null) return;
        mc.setScreen(screen);
        ci.cancel();
    }

    @Inject(method = "provideScreen$fzzy_config", at = @At("HEAD"), cancellable = true, remap = false)
    private void roll_mod$provideLdLibScreen(String scope, CallbackInfoReturnable<Screen> cir) {
        if (Screen.hasShiftDown()) return;
        Screen screen = RollConfigScreen.open(scope, Minecraft.getInstance().screen);
        if (screen != null) cir.setReturnValue(screen);
    }
}
