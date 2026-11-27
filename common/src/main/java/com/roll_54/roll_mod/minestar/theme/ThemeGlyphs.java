package com.roll_54.roll_mod.minestar.theme;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Icons drawn in black on another mod's grey buttons. The recolour maps the button into the theme's tones, and on a
 * dark theme the black icon on it went with it into the dark — Extreme Sound Muffler's speaker, bin, play and mute
 * buttons vanished. So such a texture is drawn recoloured as usual, and then this copy over it: only the icons' dark
 * pixels, in the theme's text colour, everything else see-through. Each sheet lists the regions that hold its icons,
 * so the black of its lists and wells, which are no icons, stays the theme's dark.
 */
@OnlyIn(Dist.CLIENT)
public final class ThemeGlyphs {

    /** A region of a sheet, in its pixels, from (x0, y0) inclusive to (x1, y1) exclusive. */
    private record Box(int x0, int y0, int x1, int y1) {
        boolean contains(int x, int y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** Extreme Sound Muffler's sheet: the bin and speaker on its window, and the button icons under it. */
    private static final List<Box> ESM = List.of(new Box(205, 178, 248, 199), new Box(0, 200, 70, 234));

    private static final Map<ResourceLocation, List<Box>> SHEETS = Map.of(
            ResourceLocation.fromNamespaceAndPath("extremesoundmuffler", "textures/gui/sm_gui.png"), ESM,
            ResourceLocation.fromNamespaceAndPath("extremesoundmuffler", "textures/gui/sm_gui_dark.png"), ESM);

    /** Per sheet and text colour, its icon overlay; {@code null} once it could not be built. */
    private static final Map<String, ResourceLocation> BUILT = new HashMap<>();

    private ThemeGlyphs() {}

    /** Whether {@code texture} has icons to redraw over its recoloured self. */
    public static boolean has(@Nullable ResourceLocation texture) {
        return texture != null && SHEETS.containsKey(texture);
    }

    /** The overlay of {@code texture}'s icons in {@code rgb}, or null. Render thread. */
    @Nullable
    public static ResourceLocation overlay(ResourceLocation texture, int rgb) {
        String key = texture + "#" + Integer.toHexString(rgb & 0xFFFFFF);
        if (BUILT.containsKey(key)) return BUILT.get(key);
        ResourceLocation built = null;
        try {
            built = build(texture, SHEETS.get(texture), rgb & 0xFFFFFF);
        } catch (Exception e) {
            RollMod.LOGGER.warn("[Theme] could not separate the icons of {}", texture, e);
        }
        BUILT.put(key, built);
        return built;
    }

    private static ResourceLocation build(ResourceLocation texture, List<Box> boxes, int rgb) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        NativeImage image;
        try (InputStream in = mc.getResourceManager().getResourceOrThrow(texture).open()) {
            image = NativeImage.read(in);
        }
        // NativeImage keeps ABGR.
        int glyph = 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = (abgr >>> 24), b = (abgr >> 16) & 0xFF, g = (abgr >> 8) & 0xFF, r = abgr & 0xFF;
                int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
                // An icon's stroke: opaque, grey, darker than the buttons' shadow (0x34).
                boolean stroke = a == 0xFF && hi - lo <= 10 && hi < 0x30 && inside(boxes, x, y);
                image.setPixelRGBA(x, y, stroke ? glyph : 0);
            }
        }
        ResourceLocation id = RollMod.id("dynamic/glyphs/" + texture.getNamespace() + "/"
                + texture.getPath().replace(".png", "") + "_" + Integer.toHexString(rgb));
        mc.getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    private static boolean inside(List<Box> boxes, int x, int y) {
        for (Box box : boxes) {
            if (box.contains(x, y)) return true;
        }
        return false;
    }
}
