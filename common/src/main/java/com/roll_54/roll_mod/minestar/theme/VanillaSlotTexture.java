package com.roll_54.roll_mod.minestar.theme;

import com.lowdragmc.lowdraglib2.gui.texture.ITextureSize;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

/**
 * Vanilla's slot — dark bevel top and left, light bevel bottom and right, the slot grey inside — in a theme's
 * recolour, so an LdLib slot looks exactly like the slots of the player's own (vanilla, recoloured) inventory.
 *
 * <p>Built in memory as a 3x3 image, one pixel per part of the bevel, and drawn nine-sliced with 1px borders, so it
 * fits any slot or tank size. No PNG ships for it: the colours come from {@link UiTheme.Recolor}, the same palette
 * the recolour shader maps vanilla's greys through.
 *
 * <p>LdLib reads a sprite's image size only from textures that implement {@link ITextureSize} (it mixes that into
 * {@code SimpleTexture}); a plain {@link DynamicTexture} would read as 1x1 and the UVs would be wrong.
 */
@OnlyIn(Dist.CLIENT)
public final class VanillaSlotTexture extends DynamicTexture implements ITextureSize {

    private static final Map<UiTheme, ResourceLocation> REGISTERED = new EnumMap<>(UiTheme.class);

    private VanillaSlotTexture(NativeImage image) {
        super(image);
    }

    @Override
    public int ldlib2$getImageWidth() {
        return 3;
    }

    @Override
    public int ldlib2$getImageHeight() {
        return 3;
    }

    /** The texture's id for {@code theme}, registered on first use. */
    public static synchronized ResourceLocation of(UiTheme theme, UiTheme.Recolor recolor) {
        ResourceLocation id = REGISTERED.get(theme);
        if (id != null) return id;
        ResourceLocation newId = RollMod.id("dynamic/vanilla_slot_" + theme.name().toLowerCase(java.util.Locale.ROOT));
        REGISTERED.put(theme, newId);
        Runnable register = () -> Minecraft.getInstance().getTextureManager().register(newId, build(recolor));
        if (RenderSystem.isOnRenderThread()) register.run();
        else Minecraft.getInstance().execute(register);
        return newId;
    }

    private static VanillaSlotTexture build(UiTheme.Recolor recolor) {
        int dark = recolor(recolor, 0x37);
        int well = recolor(recolor, 0x8B);
        int light = recolor(recolor, 0xFF);
        int[][] rows = {
                {dark, dark, well},
                {dark, well, light},
                {well, light, light},
        };
        NativeImage image = new NativeImage(3, 3, false);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(0xFF000000 | rows[y][x]));
            }
        }
        return new VanillaSlotTexture(image);
    }

    /** One vanilla grey through the theme's tones, as {@code shaders/core/gui_recolor.fsh} maps it. */
    static int recolor(UiTheme.Recolor r, int grey) {
        float g = grey / 255f;
        if (g < 0.333f) return mix(r.outline(), r.shadow(), g / 0.333f);
        if (g < 0.545f) return mix(r.shadow(), r.well(), (g - 0.333f) / 0.212f);
        if (g < 0.776f) return mix(r.well(), r.surface(), (g - 0.545f) / 0.231f);
        return mix(r.surface(), r.highlight(), (g - 0.776f) / 0.224f);
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(FastColor.ARGB32.red(a) + (FastColor.ARGB32.red(b) - FastColor.ARGB32.red(a)) * t);
        int g = Math.round(FastColor.ARGB32.green(a) + (FastColor.ARGB32.green(b) - FastColor.ARGB32.green(a)) * t);
        int bl = Math.round(FastColor.ARGB32.blue(a) + (FastColor.ARGB32.blue(b) - FastColor.ARGB32.blue(a)) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
