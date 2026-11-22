package com.roll_54.roll_mod.minestar.theme;

import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A container's background texture, made ready for the recolour shader ({@link GuiRecolor}) instead of redrawn: the
 * screen still draws its own art, and the shader turns its greys into the theme's tones, as it does for the player's
 * inventory. Two things the shader cannot do are done here on a copy of the texture, once per texture:
 * <ul>
 *   <li>dull colours (the loom's and the enchanting table's brown and tan wells, the anvil's wood) become greys of the
 *       same lightness, so they land in the theme's tones too; vivid colours stay as they are;</li>
 *   <li>with "Square corners" on, the stepped corners of the panel are filled ({@link SquareTabs#squareCorners}).</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class ThemedBackgrounds {

    private record Key(ResourceLocation texture, boolean dull, boolean squareTop, boolean squareBottom, int u, int v,
                       int w, int h) {}

    /**
     * Pictures left exactly as vanilla draws them, not recoloured: the hammers of the anvil and the smithing table, as
     * {x0, y0, x1, y1} inclusive in the texture's 256 grid. Taken out of the prepared copy (the panel's colour there)
     * and drawn on top of it with the recolour off.
     */
    private static final Map<ResourceLocation, int[]> KEEP = Map.of(
            ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png"), new int[]{17, 7, 46, 36},
            ResourceLocation.withDefaultNamespace("textures/gui/container/smithing.png"), new int[]{7, 7, 36, 37});
    /**
     * Fire drawn as a piece of a GUI texture (not a sprite): kept in its own colours, its whites and greys being part
     * of the flame. Farmer's Delight's cooking pot heat (u 176, v 0 of its texture). Vanilla's furnace fire is a sprite,
     * handled by the client's {@code GuiGraphicsRecolorMixin}.
     */
    private static final Map<ResourceLocation, int[]> FIRE = Map.of(
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "textures/gui/cooking_pot.png"), new int[]{176, 0});

    /** Whether this blit draws one of the {@link #FIRE} pieces. */
    public static boolean isFire(ResourceLocation texture, float u, float v) {
        int[] at = FIRE.get(texture);
        return at != null && (int) u == at[0] && (int) v == at[1];
    }

    private static final ResourceLocation ANVIL = ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png");

    /** The vanilla texture -> a copy holding only its kept picture. */
    private static final Map<ResourceLocation, ResourceLocation> KEPT = new HashMap<>();

    private static final Map<Key, ResourceLocation> READY = new HashMap<>();
    private static final Map<Key, Boolean> FAILED = new HashMap<>();
    private static int counter;

    /** Set while the prepared copy is blitted, so that blit is not taken for a background again. */
    private static boolean drawing;

    private ThemedBackgrounds() {}

    public static boolean drawing() {
        return drawing;
    }

    /**
     * The texture to blit instead of {@code texture}, or null to draw it as is: only a container texture drawn at the
     * screen's own rectangle, while the recolour is on.
     */
    @Nullable
    public static ResourceLocation replacement(ResourceLocation texture, int x, int y, float u, float v, int w, int h,
                                               int texW, int texH) {
        if (drawing || !texture.getPath().startsWith("textures/gui/container/")) return null;
        // Dulling is for the recolour; squaring is its own setting and works on every theme, the vanilla-looking
        // ones (Minecraft, plain OreUI) included.
        boolean recolour = GuiRecolor.on();
        boolean square = UiThemeState.forceEverywhere() && UiThemeState.squareCorners();
        if (!recolour && !square) return null;
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return null;
        int left = screen.getGuiLeft(), top = screen.getGuiTop();
        // Only the panel itself (its full width at the screen's corner), not the small pieces drawn over it.
        if (x != left || w != screen.getXSize() || u != 0) return null;
        boolean squareTop = square && y == top && v == 0;
        boolean squareBottom = square && y + h == top + screen.getYSize();
        if (!recolour && !squareTop && !squareBottom) return null;
        Key key = new Key(texture, recolour, squareTop, squareBottom, (int) u, (int) v, w, h);
        ResourceLocation ready = READY.get(key);
        if (ready != null || FAILED.containsKey(key)) return ready;
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(texture);
             NativeImage image = NativeImage.read(in)) {
            NativeImage out = new NativeImage(image.getWidth(), image.getHeight(), true);
            out.copyFrom(image);
            int s = Math.max(1, image.getWidth() / Math.max(1, texW));
            if (recolour) {
                for (int py = 0; py < out.getHeight(); py++) {
                    for (int px = 0; px < out.getWidth(); px++) out.setPixelRGBA(px, py, dull(out.getPixelRGBA(px, py)));
                }
            }
            if (recolour && texture.equals(ANVIL)) {
                // The red strip under the anvil's name field: vanilla's field covered it, the theme's (drawn over
                // the panel's colour) is smaller, so it showed as a red frame. It is the panel here.
                int surface = image.getPixelRGBA(Math.min(image.getWidth() - 1, 6 * s), Math.min(image.getHeight() - 1, 6 * s));
                for (int py = 0; py < out.getHeight(); py++) {
                    for (int px = 0; px < out.getWidth(); px++) {
                        int c = out.getPixelRGBA(px, py);
                        int r = c & 0xFF, g = (c >> 8) & 0xFF, b = (c >> 16) & 0xFF;
                        if ((c >>> 24) != 0 && r > 150 && g < 80 && b < 80) out.setPixelRGBA(px, py, surface);
                    }
                }
            }
            int[] keep = recolour ? KEEP.get(texture) : null;
            if (keep != null) {
                // The kept picture: lifted into a copy of its own, and painted over with the panel in this one.
                NativeImage kept = new NativeImage(image.getWidth(), image.getHeight(), true);
                int surface = image.getPixelRGBA(Math.min(image.getWidth() - 1, 6 * s), Math.min(image.getHeight() - 1, 6 * s));
                for (int py = keep[1] * s; py < (keep[3] + 1) * s && py < image.getHeight(); py++) {
                    for (int px = keep[0] * s; px < (keep[2] + 1) * s && px < image.getWidth(); px++) {
                        int c = image.getPixelRGBA(px, py);
                        if ((c >>> 24) == 0 || c == surface) continue;
                        kept.setPixelRGBA(px, py, c);
                        out.setPixelRGBA(px, py, surface);
                    }
                }
                if (!KEPT.containsKey(texture)) {
                    ResourceLocation keptId = RollMod.id("themed_background/kept_" + (counter++));
                    Minecraft.getInstance().getTextureManager().register(keptId, new DynamicTexture(kept));
                    KEPT.put(texture, keptId);
                } else {
                    kept.close();
                }
            }
            if (squareTop || squareBottom) {
                // Square the drawn region only, in a cut-out, and put it back.
                int rx = (int) u * s, ry = (int) v * s, rw = Math.min(w * s, out.getWidth() - rx), rh = Math.min(h * s, out.getHeight() - ry);
                try (NativeImage region = new NativeImage(rw, rh, true)) {
                    for (int py = 0; py < rh; py++) for (int px = 0; px < rw; px++) region.setPixelRGBA(px, py, out.getPixelRGBA(rx + px, ry + py));
                    if (squareTop) SquareTabs.squareCorners(region, false);
                    if (squareBottom) SquareTabs.squareCorners(region, true);
                    for (int py = 0; py < rh; py++) for (int px = 0; px < rw; px++) out.setPixelRGBA(rx + px, ry + py, region.getPixelRGBA(px, py));
                }
            }
            ResourceLocation id = RollMod.id("themed_background/" + (counter++));
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(out));
            READY.put(key, id);
            return id;
        } catch (Exception e) {
            FAILED.put(key, true);
            return null;
        }
    }

    /** Draws the prepared copy of {@code original}, then any picture of it kept as vanilla draws it. */
    public static void blit(net.minecraft.client.gui.GuiGraphics graphics, ResourceLocation original, ResourceLocation texture,
                            int x, int y, float u, float v, int w, int h, int texW, int texH) {
        drawing = true;
        try {
            graphics.blit(texture, x, y, u, v, w, h, texW, texH);
            ResourceLocation kept = KEPT.get(original);
            if (kept != null) GuiRecolor.withoutRecolor(() -> graphics.blit(kept, x, y, u, v, w, h, texW, texH));
        } finally {
            drawing = false;
        }
    }

    /** A dull colour as the grey of its lightness; greys and vivid colours unchanged. NativeImage's ABGR. */
    private static int dull(int abgr) {
        int a = abgr >>> 24;
        if (a == 0) return abgr;
        int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
        int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
        if (hi - lo <= 10 || (hi - lo) > 90 || (hi - lo) * 100 > hi * 45) return abgr;
        int lum = (r * 299 + g * 587 + b * 114) / 1000;
        return (a << 24) | (lum << 16) | (lum << 8) | lum;
    }
}
