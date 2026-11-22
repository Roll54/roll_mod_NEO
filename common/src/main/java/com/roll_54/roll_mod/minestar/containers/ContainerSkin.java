package com.roll_54.roll_mod.minestar.containers;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.mojang.blaze3d.systems.RenderSystem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import com.roll_54.roll_mod.mixin.client.AbstractContainerScreenAccessor;
import com.roll_54.roll_mod.mixin.client.AbstractFurnaceMenuAccessor;
import com.roll_54.roll_mod.mixin.client.AbstractFurnaceScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Vanilla's container screens drawn in LdLib, in the hub theme: the panel, the slot frames, the labels,
 * and for the furnaces their flame and arrow — with how long is left when you hover them.
 *
 * <p>Only the look is replaced. The screen stays vanilla's: slots, items, dragging, the carried stack,
 * item tooltips, the recipe book, server sync, and everything recipe viewers and other mods hang on it
 * keep working untouched. The skin is an LdLib {@link ModularUI} hosted inside the vanilla screen and
 * drawn in NeoForge's background pass, where vanilla's own background texture would have gone
 * ({@code ContainerSkinMixin} leaves that texture and the vanilla labels out). It has no clickable
 * parts, so every click falls through to vanilla.
 *
 * <p>Skinned: chests (and every other generic 9-wide container), shulker boxes, the player inventory,
 * crafting tables, furnaces, blast furnaces and smokers — vanilla's own classes only, so a mod's subclass
 * keeps its own art.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ContainerSkin {

    /**
     * The player's own inventory (vanilla's and Curios') is left vanilla, as it was before the skin: it is not in this
     * set, and the Curios screen is no longer matched in {@link #onInit}.
     */
    private static final Set<Class<?>> SKINNED = Set.of(ContainerScreen.class, ShulkerBoxScreen.class,
            CraftingScreen.class, FurnaceScreen.class, BlastFurnaceScreen.class, SmokerScreen.class);

    /**
     * Screens whose background pass draws real content over the texture — progress bars, the
     * enchanting book, lists, the horse, error marks. For these only the one full-size background
     * texture is replaced (by the panel and slot frames, drawn where it would have been); everything
     * vanilla draws after it, and its own labels, stay. See {@link #isOverlayBackground}.
     */
    private static final Set<Class<?>> OVERLAY_SKINNED = Set.of(
            net.minecraft.client.gui.screens.inventory.DispenserScreen.class,
            net.minecraft.client.gui.screens.inventory.HopperScreen.class,
            net.minecraft.client.gui.screens.inventory.CrafterScreen.class,
            net.minecraft.client.gui.screens.inventory.AnvilScreen.class,
            net.minecraft.client.gui.screens.inventory.SmithingScreen.class,
            net.minecraft.client.gui.screens.inventory.GrindstoneScreen.class,
            net.minecraft.client.gui.screens.inventory.EnchantmentScreen.class,
            net.minecraft.client.gui.screens.inventory.BrewingStandScreen.class,
            net.minecraft.client.gui.screens.inventory.BeaconScreen.class,
            net.minecraft.client.gui.screens.inventory.CartographyTableScreen.class,
            net.minecraft.client.gui.screens.inventory.LoomScreen.class,
            net.minecraft.client.gui.screens.inventory.StonecutterScreen.class,
            net.minecraft.client.gui.screens.inventory.MerchantScreen.class,
            net.minecraft.client.gui.screens.inventory.HorseInventoryScreen.class);

    /**
     * Curios' replacement for the player inventory. Matched by name, so nothing of Curios is loaded
     * when it is not installed; it is drawn like the vanilla inventory, plus its curio column.
     */
    private static final String CURIOS_SCREEN = "top.theillusivec4.curios.client.gui.CuriosScreen";

    /** The panel colour of vanilla's container textures (opaque, as NativeImage reads it). */
    private static final int VANILLA_SURFACE = 0xFFC6C6C6;

    /** Vanilla's label grey, for themes without a palette (the vanilla-looking MC theme). */
    private static final int VANILLA_LABEL = 0xFF404040;

    /** The packages whose container screens "Use the hub theme everywhere" overlay-skins; the rest are only recoloured. */
    private static final List<String> OVERLAY_PACKAGES = List.of("net.minecraft.", "aztech.modern_industrialization.");

    private static final Map<Screen, Skin> SKINS = new WeakHashMap<>();

    /** The LdLib skin is retired in favour of the recolour alone ({@link #onInit}). */
    private static final boolean SKIN_DEPRECATED = true;

    /**
     * Whether {@code screen} is one of the vanilla stations the theme covers (chests, furnaces, the anvil…): only with
     * "Use the hub theme everywhere" on, which switches all of this off, and not under the Minecraft theme.
     */
    public static boolean themedByDefault(Screen screen) {
        if (!UiThemeState.forceEverywhere() || UiThemeState.get().recolor() == null) return false;
        return SKINNED.contains(screen.getClass()) || OVERLAY_SKINNED.contains(screen.getClass());
    }

    private ContainerSkin() {}

    /** Whether {@code screen} is drawn by this skin — read by {@code ContainerSkinMixin} and {@code GuiRecolor}. */
    public static boolean isSkinned(Screen screen) {
        return SKINS.containsKey(screen);
    }

    /**
     * Whether the skin draws the whole look of {@code screen}, so vanilla's background pass and labels
     * are left out ({@code ContainerSkinMixin}). False for overlay-skinned screens.
     */
    public static boolean isFullSkin(Screen screen) {
        Skin skin = SKINS.get(screen);
        return skin != null && !skin.overlay;
    }

    /**
     * Called for every GUI blit from 0,0 (from {@code ContainerBackgroundMixin}): whether it is the
     * background texture of an overlay-skinned screen — drawn at the screen's corner at its full size —
     * which is then skipped; the skin was drawn just before ({@link #drawOverlaySkin}).
     */
    public static boolean isOverlayBackground(int x, int y, int w, int h) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return false;
        Skin skin = SKINS.get(screen);
        return skin != null && skin.overlay && x == screen.getGuiLeft() && y == screen.getGuiTop()
                && w == screen.getXSize() && h == screen.getYSize();
    }

    /** Set while {@link #drawDecor} blits, so its own blit is not taken for the background again. */
    private static boolean drawingDecor;

    public static boolean drawingDecor() {
        return drawingDecor;
    }

    /**
     * An overlay-skinned screen's background texture, with what the skin already draws taken out: the panel's own
     * colour, its frame (the outer 4px) and every slot's box. What is left are its pictures — arrows, plus signs,
     * the anvil's hammer, the grindstone, wells and boxes — drawn in its place over the skin, recoloured to the
     * theme like the rest of vanilla's drawing. Built once per screen, from the texture file.
     */
    public static void drawDecor(GuiGraphics graphics, ResourceLocation texture, int x, int y, int w, int h,
                                 int texW, int texH) {
        if (!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen)) return;
        Skin skin = SKINS.get(screen);
        if (skin == null) return;
        ResourceLocation decor = skin.decor(texture, w, h, texW, texH);
        if (decor == null) return;
        drawingDecor = true;
        try {
            // Already in the theme's colours: the recolour shader would shift them again.
            com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(() -> graphics.blit(decor, x, y, 0, 0, w, h, w, h));
        } finally {
            drawingDecor = false;
        }
    }

    /** Draws an overlay-skinned screen's skin; called just before its vanilla background pass. */
    public static void drawOverlaySkin(Screen screen, GuiGraphics graphics) {
        Skin skin = SKINS.get(screen);
        if (skin != null && skin.overlay) skin.render(graphics, -1, -1);
    }

    /** Vanilla's dark-grey label colour, as the theme's text colour on a skinned screen; otherwise as given. */
    public static int labelColor(int color) {
        if (color != 0x404040 && color != 0xFF404040) return color;
        Screen screen = Minecraft.getInstance().screen;
        return screen != null && isSkinned(screen) ? Skin.labelColor() : color;
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        // Deprecated: containers are no longer redrawn in LdLib (panel, slot frames, the background's pictures cut out
        // and redrawn). Vanilla's own art is recoloured by GuiRecolor instead, on the screens listed here as on any
        // other: one way for every screen, matching the player's inventory, with nothing per screen to keep up.
        // Kept, as the project keeps what it deprecates; nothing is skinned while this returns.
        if (SKIN_DEPRECATED) return;
        // The Minecraft theme is vanilla's own look: no container is skinned under it, the "everywhere" overlay
        // included.
        if (UiThemeState.get() == UiTheme.MC) return;
        if (!SKINNED.contains(screen.getClass()) && !OVERLAY_SKINNED.contains(screen.getClass())
                && !everywhere(screen)) return;
        Skin skin = new Skin(screen);
        SKINS.put(screen, skin);
        // A listener, not a renderable: it is drawn by hand in the background pass, under the slots,
        // and as a listener LdLib's screen hooks still tick it. Its elements handle no clicks.
        // NeoForge's addListener also puts anything Renderable into the draw list, which drew the skin
        // a second time over everything renderBg had drawn (the loom's pattern list, the enchanting
        // book...) — so it is taken back out of that list.
        event.addListener(skin.ui.getWidget());
        ((com.roll_54.roll_mod.mixin.client.ScreenRenderablesAccessor) screen).roll_mod$renderables()
                .remove(skin.ui.getWidget());
    }

    /**
     * With "Use the hub theme everywhere" on, every other container screen — the rest of vanilla's and other mods'
     * (Modern Industrialization's machines…) — is overlay-skinned too: its full-size background texture becomes the
     * theme's panel with framed slots, and whatever the screen draws over it stays its own. The player's inventory
     * (vanilla, creative, Curios) is left vanilla on purpose. A screen whose background is not one full-size texture
     * keeps it; the skin underneath is then simply covered.
     */
    private static boolean everywhere(AbstractContainerScreen<?> screen) {
        if (!com.roll_54.roll_mod.minestar.theme.UiThemeState.forceEverywhere()) return false;
        // LdLib's own screens (the hub, machines…) are themed already; skinned, the hub's full-screen host came out as
        // one opaque panel over the world.
        if (screen instanceof com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen) return false;
        // AE2 and its addons keep their own interfaces untouched.
        if (com.roll_54.roll_mod.minestar.theme.ThemeArt.isAe2(screen)) return false;
        // Only screens the skin is known to frame right: vanilla's, and Modern Industrialization's machines (EI's use
        // them too). Other mods' screens keep their own art, recoloured by GuiRecolor; skinned, some (Tide's rod
        // workbench) came out as an empty panel with no slots.
        String name = screen.getClass().getName();
        boolean known = false;
        for (String prefix : OVERLAY_PACKAGES) known |= name.startsWith(prefix);
        if (!known) return false;
        if (screen instanceof InventoryScreen
                || screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) return false;
        return !CURIOS_SCREEN.equals(screen.getClass().getName());
    }

    @SubscribeEvent
    public static void onBackground(ContainerScreenEvent.Render.Background event) {
        Skin skin = SKINS.get(event.getContainerScreen());
        // Overlay-skinned screens were drawn in place of their background texture already.
        if (skin != null && !skin.overlay) skin.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    /** The furnace timers, after everything else so no item draws over them. */
    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Skin skin = SKINS.get(event.getScreen());
        if (skin != null) skin.renderTooltips(event.getGuiGraphics(), event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing event) {
        Skin skin = SKINS.remove(event.getScreen());
        if (skin != null) skin.releaseDecor();
    }

    /* ======================================================== one screen ======================================================== */

    private static final class Skin {
        final AbstractContainerScreen<?> screen;
        final ModularUI ui;
        /** Everything is placed relative to this, the screen's gui rectangle; moved when the recipe book shifts it. */
        final UIElement frame = new UIElement();
        int left = Integer.MIN_VALUE;
        int top = Integer.MIN_VALUE;
        /** The player inventory or Curios' version of it: player model, small arrow, title only. */
        final boolean inventoryLike;
        /** Only the background texture is replaced; vanilla still draws everything over it, labels too. */
        final boolean overlay;
        /** Each slot's frame, shown only while its slot is active (Curios pages switch slots on and off). */
        final List<SlotFrame> slotFrames = new ArrayList<>();
        /** Behind slots that sit outside the main image — Curios' column; hidden when there are none. */
        final UIElement sidePanel = withClass(new UIElement(), "panel_bg");
        int sideX, sideY, sideW = -1, sideH;

        Skin(AbstractContainerScreen<?> screen) {
            this.screen = screen;
            this.inventoryLike = screen instanceof InventoryScreen || CURIOS_SCREEN.equals(screen.getClass().getName());
            // Everything not in SKINNED (the screens drawn whole) keeps its own drawing over the panel.
            this.overlay = !SKINNED.contains(screen.getClass());
            // Full-screen but invisible: it must not count as taken space. LdLib reports every element's
            // rectangle to recipe viewers as an exclusion area, so a full-screen root left TEI no room for
            // its item lists beside the inventory. Only the panels below it are reported.
            UIElement root = new UIElement() {
                @Override
                public void appendExtraAreas(List<net.minecraft.client.renderer.Rect2i> areas) {
                    for (UIElement child : getChildren()) child.appendExtraAreas(areas);
                }
            };
            root.layout(l -> l.widthPercent(100).heightPercent(100));
            root.setAllowHitTest(false);
            frame.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .width(screen.getXSize()).height(screen.getYSize()));
            frame.setAllowHitTest(false);
            root.addChild(frame);
            build();
            ui = ModularUI.of(UI.of(root, RollUi.sheets(), size -> size));
            ui.setScreenAndInit(screen);
            place();
        }

        private void build() {
            int w = screen.getXSize();
            int h = screen.getYSize();
            add(sidePanel, 0, 0, 0, 0);
            sidePanel.setDisplay(false);
            add(withClass(new UIElement(), "panel_bg"), 0, 0, w, h);

            if (inventoryLike) {
                add(new PlayerDoll(), 25, 7, 51, 72);
            }
            if (screen instanceof net.minecraft.client.gui.screens.inventory.LoomScreen) {
                // The pattern list's 4x4 grid of 14px buttons.
                add(new LoomWell(), 60, 13, 56, 56);
            }
            for (Slot slot : screen.getMenu().slots) {
                boolean big = slot instanceof ResultSlot || slot instanceof FurnaceResultSlot;
                SlotFrame frame = new SlotFrame(slot);
                slotFrames.add(frame);
                if (big) add(frame, slot.x - 5, slot.y - 5, 26, 26);
                else add(frame, slot.x - 1, slot.y - 1, 18, 18);
            }
            // Overlay screens draw their own progress, arrows and labels over the skin.
            if (overlay) return;
            if (screen instanceof CraftingScreen) add(new Arrow(null), 89, 34, 24, 16);
            // Between the 2x2 grid (ends at 132) and the output's frame (starts at 149).
            if (inventoryLike) add(new Arrow(null), 134, 31, 14, 10);
            if (screen instanceof AbstractFurnaceScreen<?> furnace) {
                AbstractFurnaceScreenAccessor sprites = (AbstractFurnaceScreenAccessor) furnace;
                add(new Flame(sprites.roll_mod$litProgressSprite()), 56, 36, 14, 14);
                add(new Arrow(sprites.roll_mod$burnProgressSprite()), 79, 34, 24, 16);
            }

            AbstractContainerScreenAccessor labels = (AbstractContainerScreenAccessor) screen;
            // Inventory-like screens write "Crafting" at vanilla's 97,6. Curios draws it there by hand and
            // leaves the label field at its default, so the field cannot be trusted for it.
            if (inventoryLike) label(screen.getTitle(), 97, 6);
            else label(screen.getTitle(), labels.roll_mod$titleLabelX(), labels.roll_mod$titleLabelY());
            // The player inventory writes only its "Crafting" title, as vanilla's does.
            if (!inventoryLike) {
                label(Component.translatable("container.inventory"), labels.roll_mod$inventoryLabelX(),
                        labels.roll_mod$inventoryLabelY());
            }
        }

        /** The background texture's pictures alone ({@link ContainerSkin#drawDecor}); null if it cannot be read. */
        @Nullable ResourceLocation decorId;
        @Nullable ResourceLocation decorSource;
        @Nullable UiTheme decorTheme;
        boolean decorFailed;

        @Nullable
        ResourceLocation decor(ResourceLocation texture, int w, int h, int texW, int texH) {
            UiTheme theme = UiThemeState.get();
            if (texture.equals(decorSource) && theme == decorTheme) return decorId;
            if (decorFailed) return null;
            releaseDecor();
            decorSource = texture;
            decorTheme = theme;
            UiTheme.Recolor palette = theme.recolor();
            try (java.io.InputStream in = Minecraft.getInstance().getResourceManager().open(texture);
                 com.mojang.blaze3d.platform.NativeImage image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
                // A resource pack may draw the texture at a higher resolution: s image pixels per GUI pixel.
                int s = Math.max(1, image.getWidth() / Math.max(1, texW));
                int iw = Math.min(w * s, image.getWidth()), ih = Math.min(h * s, image.getHeight());
                // The panel's colour: what fills the texture most, and what sits just inside its frame.
                Map<Integer, Integer> counts = new java.util.HashMap<>();
                for (int py = 0; py < ih; py++) {
                    for (int px = 0; px < iw; px++) {
                        int c = image.getPixelRGBA(px, py);
                        if ((c >>> 24) != 0) counts.merge(c, 1, Integer::sum);
                    }
                }
                int mode = counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
                com.mojang.blaze3d.platform.NativeImage out = new com.mojang.blaze3d.platform.NativeImage(iw, ih, true);
                boolean[] keep = new boolean[iw * ih];
                for (int py = 0; py < ih; py++) {
                    for (int px = 0; px < iw; px++) {
                        int c = image.getPixelRGBA(px, py);
                        int gx = px / s, gy = py / s;
                        keep[py * iw + px] = (c >>> 24) != 0 && c != mode && c != VANILLA_SURFACE
                                && gx >= 4 && gy >= 4 && gx < w - 4 && gy < h - 4 && !inSlotBox(gx, gy);
                    }
                }
                // Each picture (8-connected pixels) is kept whole or not at all. One that reaches a slot is that
                // slot's own frame or bracket (the grindstone's wheel, the loom's, a result slot's big box) and
                // would only show as broken lines around the skin's frame, so it goes. The rest are drawn flat, in
                // the theme shade of their most common colour: an even "+" or arrow, not vanilla's bevelled one.
                int[] stack = new int[iw * ih];
                boolean[] seen = new boolean[iw * ih];
                List<Integer> part = new ArrayList<>();
                for (int start = 0; start < iw * ih; start++) {
                    if (!keep[start] || seen[start]) continue;
                    part.clear();
                    int top = 0;
                    stack[top++] = start;
                    seen[start] = true;
                    boolean touchesSlot = false;
                    int minX = iw, minY = ih, maxX = -1, maxY = -1;
                    Map<Integer, Integer> colours = new java.util.HashMap<>();
                    while (top > 0) {
                        int i = stack[--top];
                        part.add(i);
                        int px = i % iw, py = i / iw;
                        minX = Math.min(minX, px);
                        minY = Math.min(minY, py);
                        maxX = Math.max(maxX, px);
                        maxY = Math.max(maxY, py);
                        if (nearSlot(px / s, py / s)) touchesSlot = true;
                        colours.merge(image.getPixelRGBA(px, py), 1, Integer::sum);
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                int nx = px + dx, ny = py + dy;
                                if (nx < 0 || ny < 0 || nx >= iw || ny >= ih) continue;
                                int n = ny * iw + nx;
                                if (keep[n] && !seen[n]) {
                                    seen[n] = true;
                                    stack[top++] = n;
                                }
                            }
                        }
                    }
                    // A small sign stays even beside a slot (the cartography table's "+"); frames and brackets are
                    // far larger.
                    boolean small = (maxX - minX + 1) <= 14 * s && (maxY - minY + 1) <= 14 * s;
                    // Art that frames a slot (the grindstone, the loom's brackets, a result slot's big box) goes with
                    // the slots, which are drawn as vanilla's recoloured: so it is too, pixel by pixel, as the recolour
                    // shader would draw it. The signs on their own (arrows, "+", the hammer) stay flat, in one shade.
                    boolean framing = touchesSlot && !small;
                    int common = colours.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(0);
                    for (int i : part) {
                        int px = i % iw, py = i / iw;
                        int src = image.getPixelRGBA(px, py);
                        int c = palette == null ? src : framing ? shaded(src, palette) : themed(common, palette);
                        out.setPixelRGBA(px, py, c);
                    }
                }
                decorId = RollMod.id("container_decor/" + Integer.toHexString(System.identityHashCode(this)));
                Minecraft.getInstance().getTextureManager().register(decorId,
                        new net.minecraft.client.renderer.texture.DynamicTexture(out));
                return decorId;
            } catch (Exception e) {
                decorFailed = true;
                decorId = null;
                return null;
            }
        }

        /**
         * A picture's pixel in the theme's own shades, by how light it is in vanilla's art: vanilla's outline, shadow,
         * well and highlight greys become the theme's, so a brown well or the anvil's hammer reads as part of the panel
         * instead of a recoloured photo. On themes that leave vanilla's art alone, the pixel as it was.
         */
        private static int themed(int abgr, @Nullable UiTheme.Recolor palette) {
            if (palette == null) return abgr;
            // One shade for every picture, the theme's well: the arrows and "+" read right in it, and the anvil's
            // hammer or the brewing stand's bubbles no longer come out in the bright text colour.
            int argb = palette.well();
            // ARGB to NativeImage's ABGR, keeping the pixel's own alpha.
            return (abgr & 0xFF000000) | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
        }


        /**
         * Within 4px of a slot's 18px box: where vanilla draws a slot's own larger frame or brackets (a result slot's
         * 26px box, the grindstone's wheel, the loom's frame). The pictures between slots (the anvil's "+", arrows)
         * keep further off.
         */
        private boolean nearSlot(int gx, int gy) {
            for (Slot slot : screen.getMenu().slots) {
                if (gx >= slot.x - 5 && gx < slot.x + 21 && gy >= slot.y - 5 && gy < slot.y + 21) return true;
            }
            return false;
        }

        /**
         * A pixel as the GUI recolour shader ({@code gui_recolor.fsh}) draws it: a grey placed among the theme's five
         * tones at vanilla's greys (black 0, #555555 a third, #8B8B8B 0.545, #C6C6C6 0.776, white 1); colour kept.
         */
        private static int shaded(int abgr, UiTheme.Recolor p) {
            int r = abgr & 0xFF, g = (abgr >> 8) & 0xFF, b = (abgr >> 16) & 0xFF;
            int hi = Math.max(r, Math.max(g, b)), lo = Math.min(r, Math.min(g, b));
            if (hi - lo > 10) return abgr;
            float v = hi / 255f;
            int argb;
            if (v < 0.333f) argb = mixArgb(p.outline(), p.shadow(), v / 0.333f);
            else if (v < 0.545f) argb = mixArgb(p.shadow(), p.well(), (v - 0.333f) / 0.212f);
            else if (v < 0.776f) argb = mixArgb(p.well(), p.surface(), (v - 0.545f) / 0.231f);
            else argb = mixArgb(p.surface(), p.highlight(), (v - 0.776f) / 0.224f);
            return (abgr & 0xFF000000) | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
        }

        private static int mixArgb(int a, int b, float t) {
            int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
            int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
            int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
            return 0xFF000000 | (r << 16) | (g << 8) | bl;
        }

        /** Whether a GUI pixel lies in a slot's box: the 18px around it, or 26px around a result slot. */
        private boolean inSlotBox(int gx, int gy) {
            for (Slot slot : screen.getMenu().slots) {
                boolean big = slot instanceof ResultSlot || slot instanceof FurnaceResultSlot;
                int pad = big ? 5 : 1, size = big ? 26 : 18;
                int sx = slot.x - pad, sy = slot.y - pad;
                if (gx >= sx && gx < sx + size && gy >= sy && gy < sy + size) return true;
            }
            return false;
        }

        void releaseDecor() {
            if (decorId != null) Minecraft.getInstance().getTextureManager().release(decorId);
            decorId = null;
            decorSource = null;
        }

        private void label(Component text, int x, int y) {
            Label label = new Label();
            label.setText(text);
            label.textStyle(t -> t.textColor(labelColor()).textShadow(false));
            int width = Minecraft.getInstance().font.width(text) + 2;
            add(label, x, y, width, 9);
        }

        private static int labelColor() {
            UiTheme.Palette palette = UiThemeState.get().palette();
            return palette != null ? palette.text() : VANILLA_LABEL;
        }

        private static UIElement withClass(UIElement element, String cssClass) {
            element.addClass(cssClass);
            return element;
        }

        private void add(UIElement element, int x, int y, int w, int h) {
            element.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                    .left(x).top(y).width(w).height(h));
            element.setAllowHitTest(false);
            frame.addChild(element);
        }

        /** Follows the gui rectangle, which the recipe book moves when it opens. */
        private void place() {
            int l = screen.getGuiLeft();
            int t = screen.getGuiTop();
            if (l == left && t == top) return;
            left = l;
            top = t;
            frame.layout(lay -> lay.left(l).top(t));
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            place();
            updateSlots();
            // The skin is the theme's own art: drawn with the recolour shader off (it recolours what vanilla draws next).
            com.roll_54.roll_mod.minestar.theme.GuiRecolor.withoutRecolor(() -> ui.getWidget().render(graphics, mouseX, mouseY,
                    Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
            // LdLib draws its layers forward in depth. Everything vanilla draws next — buttons (Curios',
            // other mods'), slot highlights — sits at depth 0 and would fail the depth test wherever the
            // panel was drawn, so it vanished or showed only in part. Clearing depth after the skin
            // makes the skin a pure background again.
            graphics.flush();
            RenderSystem.clear(org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            // Hand vanilla back the state it draws in, whatever the skin's textures left behind.
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
        }

        /**
         * Frames follow their slots' active state, and the side panel wraps whatever active slots lie
         * outside the main image — both change without the screen re-initialising (Curios pages).
         */
        private void updateSlots() {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            int w = screen.getXSize(), h = screen.getYSize();
            for (SlotFrame frame : slotFrames) {
                boolean active = frame.slot.isActive();
                if (frame.isDisplayed() != active) frame.setDisplay(active);
                Slot s = frame.slot;
                if (active && (s.x < 0 || s.y < 0 || s.x + 16 > w || s.y + 16 > h)) {
                    minX = Math.min(minX, s.x - 1);
                    minY = Math.min(minY, s.y - 1);
                    maxX = Math.max(maxX, s.x + 17);
                    maxY = Math.max(maxY, s.y + 17);
                }
            }
            if (minX == Integer.MAX_VALUE) {
                if (sidePanel.isDisplayed()) sidePanel.setDisplay(false);
                sideW = -1;
                return;
            }
            int pad = 7;
            int x = minX - pad, y = minY - pad, pw = maxX - minX + 2 * pad, ph = maxY - minY + 2 * pad;
            if (x != sideX || y != sideY || pw != sideW || ph != sideH) {
                sideX = x;
                sideY = y;
                sideW = pw;
                sideH = ph;
                sidePanel.layout(l -> l.left(x).top(y).width(pw).height(ph));
            }
            if (!sidePanel.isDisplayed()) sidePanel.setDisplay(true);
        }

        /** Over the furnace arrow: time left on the item; over the flame: fuel left. */
        void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!(screen.getMenu() instanceof AbstractFurnaceMenu menu)) return;
            ContainerData data = ((AbstractFurnaceMenuAccessor) menu).roll_mod$data();
            int x = mouseX - screen.getGuiLeft();
            int y = mouseY - screen.getGuiTop();
            List<Component> lines = new ArrayList<>();
            if (x >= 79 && x < 103 && y >= 34 && y < 50) {
                int done = data.get(2);
                int total = data.get(3);
                if (total > 0 && done > 0) {
                    lines.add(Component.translatable("gui.roll_mod.container.smelting", seconds(total - done), seconds(total)));
                } else {
                    lines.add(Component.translatable("gui.roll_mod.container.idle"));
                }
            } else if (x >= 56 && x < 70 && y >= 36 && y < 50) {
                int fuel = data.get(0);
                lines.add(fuel > 0
                        ? Component.translatable("gui.roll_mod.container.fuel", seconds(fuel))
                        : Component.translatable("gui.roll_mod.container.no_fuel"));
            }
            if (!lines.isEmpty()) graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
        }

        private static String seconds(int ticks) {
            return String.format(Locale.ROOT, "%.1f", ticks / 20f);
        }

        /* ------------------------------------------------------- pieces ------------------------------------------------------- */

        /**
         * An arrow drawn flat in the theme's text colour — shaft and head, at the element's size. With a
         * furnace behind it ({@code progress}), a faint track that fills solid as the item cooks; otherwise
         * (crafting table, inventory) solid.
         */
        private final class Arrow extends UIElement {
            private final boolean progress;

            Arrow(@Nullable ResourceLocation sprite) {
                this.progress = sprite != null;
            }

            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                super.drawBackgroundAdditional(context);
                int x = Math.round(getPositionX()), y = Math.round(getPositionY());
                int w = Math.round(getSizeWidth()), h = Math.round(getSizeHeight());
                int color = labelColor();
                GuiGraphics g = context.graphics;
                g.flush();
                if (progress) {
                    shape(g, x, y, w, h, w, (color & 0x00FFFFFF) | 0x50000000);
                    if (screen.getMenu() instanceof AbstractFurnaceMenu menu) {
                        int filled = Mth.ceil(menu.getBurnProgress() * w);
                        if (filled > 0) shape(g, x, y, w, h, filled, color);
                    }
                } else {
                    shape(g, x, y, w, h, w, color);
                }
                g.flush();
            }

            /** The arrow's first {@code columns} columns: a shaft a third of the height, then a tapering head. */
            private static void shape(GuiGraphics g, int x, int y, int w, int h, int columns, int color) {
                int mid = y + h / 2;
                int head = Math.max(1, h / 2);
                int headStart = w - head;
                int shaft = Math.max(1, h / 6);
                for (int c = 0; c < Math.min(columns, w); c++) {
                    int half = c < headStart ? shaft : Math.max(0, (int) Math.ceil(h / 2.0 * (w - c) / head));
                    g.fill(x + c, mid - half, x + c + 1, mid + half + (h % 2), color);
                }
            }
        }

        /** The loom's pattern list area: darkened while there is nothing to paint (no banner and dye). */
        private final class LoomWell extends UIElement {
            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                super.drawBackgroundAdditional(context);
                if (((com.roll_54.roll_mod.mixin.client.LoomScreenAccessor) screen).roll_mod$displayPatterns()) return;
                int x = Math.round(getPositionX()), y = Math.round(getPositionY());
                int w = Math.round(getSizeWidth()), h = Math.round(getSizeHeight());
                context.graphics.fill(x, y, x + w, y + h, 0x80000000);
            }
        }

        /** A slot's frame in the theme's slot style. */
        private static final class SlotFrame extends UIElement {
            final Slot slot;

            SlotFrame(Slot slot) {
                this.slot = slot;
                // On a recoloured theme the frame is only the vanilla-style slot drawn below; the theme's LdLib slot
                // (rounded on the round themes) drew over it and is not given at all.
                if (UiThemeState.get().recolor() == null) {
                    addClass("item-slot_bg");
                    slotBackground(this);
                }
            }

            /**
             * On a recoloured theme, the slot as the player's own inventory shows it (vanilla's slot through the
             * recolour shader): a dark line top and left, the well inside, the light bevel right and bottom, in the
             * shades the shader gives those greys. A block's slots then match the inventory under them on every
             * theme, instead of wearing the theme's LdLib slot (AE2's came out unlike the inventory's).
             */
            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                UiTheme.Recolor palette = UiThemeState.get().recolor();
                if (palette == null) {
                    super.drawBackgroundAdditional(context);
                    return;
                }
                int x = Math.round(getPositionX()), y = Math.round(getPositionY());
                int w = Math.round(getSizeWidth()), h = Math.round(getSizeHeight());
                // Vanilla's #373737 sits two thirds of the way from the outline tone to the shadow tone.
                int dark = mix(palette.outline(), palette.shadow(), 0.65f);
                GuiGraphics g = context.graphics;
                g.fill(x, y, x + w, y + h, palette.well());
                g.fill(x, y, x + w - 1, y + 1, dark);
                g.fill(x, y, x + 1, y + h - 1, dark);
                g.fill(x + 1, y + h - 1, x + w, y + h, palette.highlight());
                g.fill(x + w - 1, y + 1, x + w, y + h, palette.highlight());
            }

            private static int mix(int a, int b, float t) {
                int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
                int gg = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
                int bb = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
                return 0xFF000000 | (r << 16) | (gg << 8) | bb;
            }
        }

        /** The fuel flame: faint when out, burning down as fuel is used. */
        private final class Flame extends UIElement {
            private final ResourceLocation sprite;

            Flame(ResourceLocation sprite) {
                this.sprite = sprite;
            }

            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                super.drawBackgroundAdditional(context);
                int x = Math.round(getPositionX()), y = Math.round(getPositionY());
                GuiGraphics g = context.graphics;
                g.flush();
                // Out of fuel: nothing (a faint flame read as a stray square on the panel). Burning: vanilla's fire.
                tint(1f);
                if (screen.getMenu() instanceof AbstractFurnaceMenu menu && menu.isLit()) {
                    int l = Mth.ceil(menu.getLitProgress() * 13f) + 1;
                    g.blitSprite(sprite, 14, 14, 0, 14 - l, x, y + 14 - l, 14, l);
                }
                g.flush();
            }
        }

        /** The player model in its frame, following the mouse as in vanilla's inventory. */
        private final class PlayerDoll extends UIElement {
            PlayerDoll() {
                addClass("item-slot_bg");
                slotBackground(this);
            }

            @Override
            public void drawBackgroundAdditional(GUIContext context) {
                super.drawBackgroundAdditional(context);
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                int x = screen.getGuiLeft(), y = screen.getGuiTop();
                context.graphics.flush();
                tint(1f);
                InventoryScreen.renderEntityInInventoryFollowsMouse(context.graphics, x + 26, y + 8, x + 75, y + 78,
                        30, 0.0625f, context.mouseX, context.mouseY, mc.player);
                context.graphics.flush();
            }
        }

        /**
         * LdLib's own slot art as the frame's base look, as {@code ItemSlot} sets it on itself: a theme's
         * {@code .item-slot_bg} rule still draws over it, and a theme without one (GDP's sheet has none) no longer
         * leaves the frames blank — vanilla screens skinned on GDP showed no slots at all.
         */
        private static void slotBackground(UIElement element) {
            element.getStyle().background(com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot.ITEM_SLOT_TEXTURE);
        }

        private static void tint(float alpha) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }
    }
}
