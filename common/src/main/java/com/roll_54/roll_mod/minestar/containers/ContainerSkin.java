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

    private static final Set<Class<?>> SKINNED = Set.of(ContainerScreen.class, ShulkerBoxScreen.class,
            InventoryScreen.class, CraftingScreen.class, FurnaceScreen.class, BlastFurnaceScreen.class,
            SmokerScreen.class);

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

    /** Vanilla's label grey, for themes without a palette (the vanilla-looking MC theme). */
    private static final int VANILLA_LABEL = 0xFF404040;

    private static final Map<Screen, Skin> SKINS = new WeakHashMap<>();

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
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !SKINNED.contains(screen.getClass()) && !OVERLAY_SKINNED.contains(screen.getClass())
                && !CURIOS_SCREEN.equals(screen.getClass().getName())) return;
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
        SKINS.remove(event.getScreen());
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
            this.overlay = OVERLAY_SKINNED.contains(screen.getClass());
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
            ui.getWidget().render(graphics, mouseX, mouseY, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
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
                addClass("item-slot_bg");
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
                tint(0.3f);
                g.blitSprite(sprite, x, y, 14, 14);
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

        private static void tint(float alpha) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }
    }
}
