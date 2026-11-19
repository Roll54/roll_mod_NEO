package com.roll_54.roll_mod.mixin.client;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Forces the pack's own menu panorama. Vanilla builds one {@code CubeMap} for every screen —
 * {@code Screen.CUBE_MAP}, drawn by the title screen, by every menu background with no world loaded,
 * and preloaded by {@code TitleScreen.preloadResources} — from
 * {@code minecraft:textures/gui/title/background/panorama_0..5.png}, which any resource pack can
 * replace. This points it at {@code roll_mod:textures/gui/title/background/panorama_0..5.png}
 * instead, so the panorama is ours whatever packs are on. The overlay gradient stays vanilla's.
 */
@Mixin(Screen.class)
public abstract class ScreenPanoramaMixin {

    @ModifyArg(method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/CubeMap;<init>(Lnet/minecraft/resources/ResourceLocation;)V"))
    private static ResourceLocation roll_mod$forcePanorama(ResourceLocation base) {
        // Not RollMod.id(): Screen can load before RollMod is initialised; MODID is an inlined constant.
        return ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "textures/gui/title/background/panorama");
    }
}
