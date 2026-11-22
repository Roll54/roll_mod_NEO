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

/**
 * Vanilla's empty-slot icons ({@code container/slot/…}: the smithing template, armour, shield, fuel outlines) on the
 * dark themes. Vanilla draws them dark on its light slot; the recolour turned them dark on the theme's dark slot, where
 * they vanished. On a dark theme they are drawn in the theme's light tone instead, each pixel keeping its own alpha.
 */
@OnlyIn(Dist.CLIENT)
public final class SlotIcons {

    private record Key(ResourceLocation sprite, int color) {}

    private static final Map<Key, ResourceLocation> READY = new HashMap<>();
    private static final Map<Key, Boolean> FAILED = new HashMap<>();
    private static int counter;

    private SlotIcons() {}

    /** The light copy to draw in place of {@code sprite}, or null to draw it as usual. */
    @Nullable
    public static ResourceLocation replacement(ResourceLocation sprite) {
        if (!"minecraft".equals(sprite.getNamespace()) || !sprite.getPath().startsWith("container/slot/")) return null;
        if (!GuiRecolor.on()) return null;
        UiTheme.Recolor recolor = UiThemeState.get().recolor();
        if (recolor == null || !dark(recolor.surface())) return null;
        int color = recolor.highlight();
        Key key = new Key(sprite, color);
        ResourceLocation ready = READY.get(key);
        if (ready != null || FAILED.containsKey(key)) return ready;
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(),
                "textures/gui/sprites/" + sprite.getPath() + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(file);
             NativeImage image = NativeImage.read(in)) {
            NativeImage out = new NativeImage(image.getWidth(), image.getHeight(), true);
            // ABGR, the colour's RGB under each pixel's own alpha.
            int bgr = ((color & 0xFF) << 16) | (color & 0xFF00) | ((color >> 16) & 0xFF);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int a = image.getPixelRGBA(x, y) >>> 24;
                    out.setPixelRGBA(x, y, a == 0 ? 0 : (a << 24) | bgr);
                }
            }
            ResourceLocation id = RollMod.id("slot_icon/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
            READY.put(key, id);
            return id;
        } catch (Exception e) {
            FAILED.put(key, true);
            return null;
        }
    }

    private static boolean dark(int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000 < 128;
    }
}
