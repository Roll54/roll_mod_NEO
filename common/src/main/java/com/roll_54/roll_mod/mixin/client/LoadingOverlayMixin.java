package com.roll_54.roll_mod.mixin.client;

import com.mojang.blaze3d.platform.Window;
import com.roll_54.roll_mod.minestar.transition.LoadingWipe;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Vanilla's Mojang loading screen — every resource reload (F3+T, resource packs), and the startup when
 * NeoForge's early window is off — is replaced whole by {@link LoadingWipe}: it sweeps over what was on
 * screen, holds its interstitial while loading goes on, and reveals what comes next. The Mojang screen
 * is never drawn.
 *
 * <p>Vanilla's end of loading (checking for errors, running its callback, laying out the screen) is
 * done here, once the wipe has covered the window. NeoForge's startup overlay overrides this method
 * without calling it, so this does not touch it. Without the wipe's shader, vanilla runs as it is.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private Consumer<Optional<Throwable>> onFinish;
    @Shadow private long fadeOutStart;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void roll_mod$wipe(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!LoadingWipe.beginWholeLoading(this)) return;

        if (fadeOutStart == -1L && reload.isDone() && LoadingWipe.isCovered(this)) {
            // Vanilla's end of loading, as in its render; fadeOutStart first, as there, so it runs once.
            fadeOutStart = Util.getMillis();
            try {
                reload.checkExceptions();
                onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                onFinish.accept(Optional.of(throwable));
            }
            if (minecraft.screen != null) {
                Window window = minecraft.getWindow();
                minecraft.screen.init(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            }
            LoadingWipe.loadingDone(this);
        }

        switch (LoadingWipe.takeOver(graphics, this, mouseX, mouseY, partialTick, reload.getActualProgress())) {
            case DRAWN -> ci.cancel();
            case FINISHED -> {
                minecraft.setOverlay(null);
                ci.cancel();
            }
            // The wipe gave up (its shader went away in this reload): vanilla carries on.
            case OVERLAY -> {
            }
        }
    }
}
