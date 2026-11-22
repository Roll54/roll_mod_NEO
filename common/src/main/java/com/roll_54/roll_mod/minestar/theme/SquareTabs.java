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
 * The creative inventory's tabs with square corners, for "Square corners" in the hub's settings. Vanilla's tab sprites
 * round each outer corner with a two-pixel step; here each one is rebuilt once from the game's own sprite file, each
 * corner carried on from the straight edges beside it ({@link #squareCorners}), and drawn in its place. The theme's recolour then tints it as it does vanilla's. Nothing of vanilla's is shipped in the mod.
 */
@OnlyIn(Dist.CLIENT)
public final class SquareTabs {

    private static final String PREFIX = "container/creative_inventory/tab_";
    private static final Map<ResourceLocation, ResourceLocation> SQUARE = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> FAILED = new HashMap<>();

    private SquareTabs() {}

    /** Whether tabs are squared right now: "Square corners" with "Use the hub theme everywhere", on any theme. */
    public static boolean active() {
        UiTheme theme = UiThemeState.get();
        // On every theme, the vanilla-looking ones (Minecraft, plain OreUI) too: squaring is its own setting.
        return UiThemeState.forceEverywhere() && UiThemeState.squareCorners()
                && Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
    }

    public static boolean isTab(ResourceLocation sprite) {
        return "minecraft".equals(sprite.getNamespace()) && sprite.getPath().startsWith(PREFIX);
    }

    /** The squared copy of a tab sprite, as a texture of the sprite's own size; null if the file could not be read. */
    @Nullable
    public static synchronized ResourceLocation square(ResourceLocation sprite) {
        ResourceLocation done = SQUARE.get(sprite);
        if (done != null || FAILED.containsKey(sprite)) return done;
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(),
                "textures/gui/sprites/" + sprite.getPath() + ".png");
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(file);
             NativeImage image = NativeImage.read(in)) {
            NativeImage out = new NativeImage(image.getWidth(), image.getHeight(), true);
            out.copyFrom(image);
            boolean bottom = sprite.getPath().contains("bottom");
            squareCorners(out, bottom);
            ResourceLocation id = RollMod.id("square_tab/" + sprite.getPath().substring(PREFIX.length()));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
            SQUARE.put(sprite, id);
            return id;
        } catch (Exception e) {
            FAILED.put(sprite, true);
            return null;
        }
    }

    /** How far a rounded corner reaches in from each edge; past it the edges run straight. */
    private static final int CORNER = 4;

    /**
     * Makes the two outer corners on one edge square (the top edge, or the bottom one when {@code bottom}). Each
     * corner's {@link #CORNER}-pixel square is rebuilt from the straight edges beside it: above its diagonal the
     * top (or bottom) edge carried on to the corner, below it the side edge carried on, so the outline and the bevel
     * (light on one side, dark on the other) meet the way a square corner of vanilla's art does.
     */
    public static void squareCorners(NativeImage img, boolean bottom) {
        int w = img.getWidth(), h = img.getHeight();
        int top = -1, end = -1, left = w, right = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (alpha(img.getPixelRGBA(x, y)) == 0) continue;
                if (top < 0) top = y;
                end = y;
                left = Math.min(left, x);
                right = Math.max(right, x);
            }
        }
        if (top < 0 || right - left < 2 * CORNER + 1 || end - top < 2 * CORNER + 1) return;
        int edge = bottom ? end : top, dy = bottom ? -1 : 1;
        corner(img, left, edge, 1, dy);
        corner(img, right, edge, -1, dy);
    }

    private static void corner(NativeImage img, int cx, int cy, int dx, int dy) {
        int[][] rebuilt = new int[CORNER][CORNER];
        for (int j = 0; j < CORNER; j++) {
            for (int i = 0; i < CORNER; i++) {
                int x = cx + dx * i, y = cy + dy * j;
                rebuilt[j][i] = i >= j
                        ? img.getPixelRGBA(cx + dx * CORNER, y)   // the edge along the corner's side, carried on
                        : img.getPixelRGBA(x, cy + dy * CORNER);  // the side edge, carried on
            }
        }
        for (int j = 0; j < CORNER; j++) {
            for (int i = 0; i < CORNER; i++) img.setPixelRGBA(cx + dx * i, cy + dy * j, rebuilt[j][i]);
        }
    }

    private static int alpha(int abgr) {
        return abgr >>> 24;
    }
}
