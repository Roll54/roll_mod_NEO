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
import java.util.Map;
import java.util.Set;

/**
 * AE2's GUI art (and its addons', drawn from AE2's or their own sheets) is not vanilla's greys but a pale blue-grey
 * family. The recolour only maps true greys, so outside AE2's own screens — an Extended AE recipe in TEI, say — that art
 * stayed light on a dark theme. Here such a sheet gets a copy with its blue-greys turned into the vanilla greys of the
 * same role (light bevel, panel, slot, dark bevel, outline), which the recolour then maps like vanilla's own art.
 * Coloured pixels (icons, the purple of a cell) are left as they are. AE2's own screens are not recoloured at all, so
 * they never see this copy.
 */
@OnlyIn(Dist.CLIENT)
public final class Ae2Greys {

    private static final Set<String> NAMESPACES = Set.of("ae2", "extendedae", "advanced_ae", "megacells", "ae2wtlib");

    /** AE2's tones by brightness and the vanilla grey each stands for. */
    private static final int[] FROM = { 0x00, 0x41, 0x87, 0xAD, 0xCB, 0xF2, 0xFF };
    private static final int[] TO = { 0x00, 0x37, 0x55, 0x8B, 0xC6, 0xFF, 0xFF };

    /** Per sheet, its greyed copy; {@code null} once it could not be built. */
    private static final Map<ResourceLocation, ResourceLocation> BUILT = new HashMap<>();

    private Ae2Greys() {}

    /** Whether {@code texture} is one of AE2's (or an addon's) GUI sheets. */
    public static boolean applies(ResourceLocation texture) {
        return NAMESPACES.contains(texture.getNamespace()) && texture.getPath().startsWith("textures/guis/");
    }

    /** The greyed copy of {@code texture}, or {@code texture} itself if it could not be made. Render thread. */
    public static ResourceLocation greyed(ResourceLocation texture) {
        ResourceLocation built;
        if (BUILT.containsKey(texture)) {
            built = BUILT.get(texture);
        } else {
            built = null;
            try {
                built = build(texture);
            } catch (Exception e) {
                RollMod.LOGGER.warn("[Theme] could not grey AE2 sheet {}", texture, e);
            }
            BUILT.put(texture, built);
        }
        return built != null ? built : texture;
    }

    @Nullable
    private static ResourceLocation build(ResourceLocation texture) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        var resource = mc.getResourceManager().getResource(texture);
        if (resource.isEmpty()) return null;
        NativeImage image;
        try (InputStream in = resource.get().open()) {
            image = NativeImage.read(in);
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = abgr >>> 24, b = (abgr >> 16) & 0xFF, g = (abgr >> 8) & 0xFF, r = abgr & 0xFF;
                int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
                // AE2's frame tones lean blue by up to ~0x20; anything more coloured is a picture.
                if (a == 0 || hi - lo > 0x22) continue;
                int grey = map((r + g + b) / 3);
                image.setPixelRGBA(x, y, (a << 24) | (grey << 16) | (grey << 8) | grey);
            }
        }
        ResourceLocation id = RollMod.id("dynamic/ae2_greys/" + texture.getNamespace() + "/"
                + texture.getPath().replace(".png", ""));
        mc.getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    private static int map(int v) {
        for (int i = 1; i < FROM.length; i++) {
            if (v <= FROM[i]) {
                float t = (v - FROM[i - 1]) / (float) (FROM[i] - FROM[i - 1]);
                return Math.round(TO[i - 1] + (TO[i] - TO[i - 1]) * t);
            }
        }
        return 0xFF;
    }
}
