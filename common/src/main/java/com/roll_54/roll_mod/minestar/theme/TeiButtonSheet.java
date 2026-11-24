package com.roll_54.roll_mod.minestar.theme;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.gui.HubNotifications;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.InputStream;

/**
 * TEI's recipe button sheet with its green "on" states (bookmarked, default recipe, tree goal) in the player's
 * notification colour (Settings → Notifications, the colour of the hub's "!" badge), for the theme's recolour, which
 * only maps greys and left the green as it was. Built from TEI's own sheet when first needed and again whenever the
 * colour changes; the light and dark of each pixel are kept, only the hue becomes the badge's.
 */
@OnlyIn(Dist.CLIENT)
public final class TeiButtonSheet {

    static final ResourceLocation ORIGINAL = ResourceLocation.fromNamespaceAndPath("tei", "textures/gui/buttons.png");
    /** The fixed amber copy, kept for when the sheet cannot be built. */
    private static final ResourceLocation FALLBACK = RollMod.id("textures/gui/tei/buttons.png");
    private static final ResourceLocation BUILT = RollMod.id("dynamic/tei_buttons");

    /** The recipe buttons' corner of the sheet; the globe further right keeps its green land. */
    private static final int AREA_W = 120;
    private static final int AREA_H = 96;

    private static int builtFor;
    private static boolean failed;

    private TeiButtonSheet() {}

    /** The sheet to draw in place of TEI's while the recolour is on. Render thread. */
    public static ResourceLocation texture() {
        if (failed) return FALLBACK;
        int argb = 0xFF000000 | HubNotifications.color().argb();
        if (argb != builtFor) {
            try {
                build(argb);
                builtFor = argb;
            } catch (Exception e) {
                failed = true;
                RollMod.LOGGER.warn("[Theme] could not recolour TEI's buttons; using the amber copy", e);
                return FALLBACK;
            }
        }
        return BUILT;
    }

    private static void build(int argb) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        NativeImage image;
        try (InputStream in = mc.getResourceManager().getResourceOrThrow(ORIGINAL).open()) {
            image = NativeImage.read(in);
        }
        int cr = (argb >> 16) & 0xFF, cg = (argb >> 8) & 0xFF, cb = argb & 0xFF;
        for (int y = 0; y < Math.min(AREA_H, image.getHeight()); y++) {
            for (int x = 0; x < Math.min(AREA_W, image.getWidth()); x++) {
                int abgr = image.getPixelRGBA(x, y); // NativeImage keeps ABGR
                int a = abgr >>> 24, b = (abgr >> 16) & 0xFF, g = (abgr >> 8) & 0xFF, r = abgr & 0xFF;
                if (a == 0 || g <= r + 30 || g <= b + 30) continue;
                // Its lightness on TEI's greens (about 60 dark to 210 light) onto the badge colour: darker below the
                // middle, towards white above it.
                float t = Math.max(0f, Math.min(1f, ((0.299f * r + 0.587f * g + 0.114f * b) - 60f) / 150f));
                int nr, ng, nb;
                if (t < 0.5f) {
                    float k = 0.35f + 1.3f * t;
                    nr = Math.round(cr * k);
                    ng = Math.round(cg * k);
                    nb = Math.round(cb * k);
                } else {
                    float k = (t - 0.5f) * 1.2f;
                    nr = Math.round(cr + (255 - cr) * k);
                    ng = Math.round(cg + (255 - cg) * k);
                    nb = Math.round(cb + (255 - cb) * k);
                }
                image.setPixelRGBA(x, y, (a << 24) | (clamp(nb) << 16) | (clamp(ng) << 8) | clamp(nr));
            }
        }
        // Registering under the same id again releases the previous colour's texture.
        mc.getTextureManager().register(BUILT, new DynamicTexture(image));
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
