package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.minestar.transition.LoadingWipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.neoforged.fml.earlydisplay.DisplayWindow;
import net.neoforged.fml.loading.progress.ProgressMeter;
import net.neoforged.neoforge.client.loading.NeoForgeLoadingOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The game's startup loading screen ends with {@link LoadingWipe} instead of NeoForge's fade.
 *
 * <p>The wipe begins on the frame loading ends, before NeoForge's own end-of-loading step in it, so
 * the main menu that step opens already knows it is covered (and holds its intro for the reveal).
 */
@Mixin(NeoForgeLoadingOverlay.class)
public abstract class NeoForgeLoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow @Final private DisplayWindow displayWindow;
    @Shadow @Final private ProgressMeter progressMeter;
    @Shadow private long fadeOutStart;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void roll_mod$wipe(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (fadeOutStart == -1L && reload.isDone()) LoadingWipe.beginAfterLoading(this);
        switch (LoadingWipe.takeOver(graphics, this, mouseX, mouseY, partialTick, 1f)) {
            case DRAWN -> ci.cancel();
            case FINISHED -> {
                // What NeoForge does at the end of its fade.
                progressMeter.complete();
                minecraft.setOverlay(null);
                displayWindow.close();
                ci.cancel();
            }
            case OVERLAY -> {
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void roll_mod$wipeOver(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LoadingWipe.drawOverOverlay(graphics, this);
    }
}
