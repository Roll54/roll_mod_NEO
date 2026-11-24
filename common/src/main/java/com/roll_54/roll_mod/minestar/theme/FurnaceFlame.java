package com.roll_54.roll_mod.minestar.theme;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The furnaces' fire ({@code …/lit_progress}) under the theme's recolour. Vanilla's sprite is opaque: its grey
 * background (the panel's 198 grey and the slot's 139) around a flame with white cores. Drawn raw, the background stayed
 * light grey on a dark theme; recoloured, the flame's whites became theme greys. So the sprite is drawn recoloured (the
 * background takes the theme) and then this flame-only copy, its greys made transparent, over it as it is.
 */
@OnlyIn(Dist.CLIENT)
public final class FurnaceFlame {

    /** Per sprite, its flame-only texture; {@code null} once it could not be built. */
    private static final Map<ResourceLocation, ResourceLocation> FLAMES = new HashMap<>();

    private FurnaceFlame() {}

    /** Draws the flame alone over what the recoloured sprite left, with the recolour off. Render thread. */
    public static void drawFlame(GuiGraphics g, ResourceLocation sprite, int texW, int texH, int u, int v,
                                 int x, int y, int w, int h) {
        ResourceLocation flame = flame(sprite);
        if (flame == null || w <= 0 || h <= 0) return;
        // Its cleared background must stay see-through.
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        GuiRecolor.withoutRecolor(() -> g.blit(flame, x, y, u, v, w, h, texW, texH));
    }

    @Nullable
    private static ResourceLocation flame(ResourceLocation sprite) {
        if (FLAMES.containsKey(sprite)) return FLAMES.get(sprite);
        ResourceLocation built = null;
        try {
            built = build(sprite);
        } catch (Exception e) {
            RollMod.LOGGER.warn("[Theme] could not separate the flame of {}", sprite, e);
        }
        FLAMES.put(sprite, built);
        return built;
    }

    private static ResourceLocation build(ResourceLocation sprite) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation file = sprite.withPath("textures/gui/sprites/" + sprite.getPath() + ".png");
        NativeImage image;
        try (InputStream in = mc.getResourceManager().getResourceOrThrow(file).open()) {
            image = NativeImage.read(in);
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y); // NativeImage keeps ABGR
                int b = (abgr >> 16) & 0xFF, g = (abgr >> 8) & 0xFF, r = abgr & 0xFF;
                // The background is plain grey; the flame is coloured, its cores pure white.
                boolean grey = Math.abs(r - g) < 6 && Math.abs(g - b) < 6 && Math.abs(r - b) < 6;
                if (grey && r < 250) image.setPixelRGBA(x, y, 0);
            }
        }
        ResourceLocation id = RollMod.id("dynamic/flame/" + sprite.getNamespace() + "/" + sprite.getPath());
        mc.getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }
}
