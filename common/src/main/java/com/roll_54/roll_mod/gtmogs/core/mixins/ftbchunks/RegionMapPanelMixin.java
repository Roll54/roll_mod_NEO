package com.roll_54.roll_mod.gtmogs.core.mixins.ftbchunks;

import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftblibrary.ui.Panel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin extends Panel {

    @Shadow
    @Final
    LargeMapScreen largeMap;

    @Shadow
    int regionMinX;

    @Shadow
    int regionMinZ;

    public RegionMapPanelMixin(Panel panel) {
        super(panel);
    }

    @Inject(method = "addWidgets",
            at = @At(value = "INVOKE", target = "Ldev/ftb/mods/ftbchunks/client/gui/RegionMapPanel;alignWidgets()V"))
    private void gtmogs$injectAddWidgets(CallbackInfo ci) {
        if (!ConfigHolder.INSTANCE.compat.minimap.toggle.ftbChunksIntegration) {}
    }
}
