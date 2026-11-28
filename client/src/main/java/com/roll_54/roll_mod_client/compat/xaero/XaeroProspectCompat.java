package com.roll_54.roll_mod_client.compat.xaero;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Wires prospected ores into Xaero's maps when they are installed. Called from client setup; touches no Xaero class
 * unless that mod is loaded ({@link WorldMapProspects} and {@link MinimapProspects} are only loaded behind
 * {@code ModList.isLoaded}).
 *
 * <p>The World Map screen gets, without any mixin, a button that shows or hides the ores and — in ORE_LAYER mode —
 * the height buttons for the ore layer.
 */
@OnlyIn(Dist.CLIENT)
public final class XaeroProspectCompat {

    private static final String WORLD_MAP_SCREEN = "xaero.map.gui.GuiMap";

    private XaeroProspectCompat() {}

    public static void init() {
        boolean worldMap = ModList.get().isLoaded("xaeroworldmap");
        boolean minimap = ModList.get().isLoaded("xaerominimap");
        if (!worldMap && !minimap) return;
        // The maps build their handlers late (World Map) or per session (Minimap): registered once they exist.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (worldMap) WorldMapProspects.tryRegister();
            if (minimap) MinimapProspects.tryRegister();
        });
        if (worldMap) {
            NeoForge.EVENT_BUS.addListener(XaeroProspectCompat::onScreenInit);
            NeoForge.EVENT_BUS.addListener(XaeroProspectCompat::onScreenRender);
            NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onMouseClicked(e);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onMouseReleased(e);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseDragged.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onMouseDragged(e);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onMouseScrolled(e);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onKeyPressed(e);
            });
            NeoForge.EVENT_BUS.addListener((ScreenEvent.CharacterTyped.Pre e) -> {
                if (isWorldMap(e.getScreen())) ProspectVeinPanel.onCharTyped(e);
            });
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!WORLD_MAP_SCREEN.equals(screen.getClass().getName())) return;
        if (!ProspectMapState.config().minimapButton) return;
        // The toggle: an icon button in the free slot of Xaero's left column (see freeLeftSlot).
        int[] slot = freeLeftSlot(event, screen);
        event.addListener(new OreToggleButton(slot[0], slot[1]));
        // The vein filter panel opens beside that column.
        ProspectVeinPanel.layout(screen, slot[0] + SLOT);
        int y = screen.height - 22;
        int x = screen.width / 2 - 60;
        if (!ProspectMapState.oreLayer()) return;
        // The ore layer's height: down, the height itself (click: back to where the player stands), up.
        Button height = Button.builder(heightText(), b -> {
            ProspectMapState.resetLayer();
            b.setMessage(heightText());
        }).bounds(x + 124 + 20, y, 44, 18).build();
        event.addListener(Button.builder(Component.literal("−"), b -> {
            ProspectMapState.moveLayer(Screen.hasShiftDown() ? -16 : -4);
            height.setMessage(heightText());
        }).bounds(x + 124, y, 18, 18).build());
        event.addListener(height);
        event.addListener(Button.builder(Component.literal("+"), b -> {
            ProspectMapState.moveLayer(Screen.hasShiftDown() ? 16 : 4);
            height.setMessage(heightText());
        }).bounds(x + 124 + 66, y, 18, 18).build());
    }

    /**
     * Where the toggle goes: Xaero's left column of 20 px icon buttons has an empty slot between them; the first gap
     * one step wide or more, from the bottom up. When the column cannot be read, the slot four steps above the bottom.
     */
    private static int[] freeLeftSlot(ScreenEvent.Init.Post event, Screen screen) {
        java.util.List<net.minecraft.client.gui.components.AbstractWidget> column = new java.util.ArrayList<>();
        for (var listener : event.getListenersList()) {
            if (listener instanceof net.minecraft.client.gui.components.AbstractWidget w
                    && w.getX() < 12 && w.getWidth() <= 30 && w.getHeight() <= 30) column.add(w);
        }
        column.sort(java.util.Comparator.comparingInt(net.minecraft.client.gui.components.AbstractWidget::getY));
        int step = 20;
        for (int i = 1; i < column.size(); i++) {
            int d = column.get(i).getY() - column.get(i - 1).getY();
            if (d > 0) step = Math.min(step, d);
        }
        for (int i = column.size() - 1; i > 0; i--) {
            var below = column.get(i);
            var above = column.get(i - 1);
            if (below.getY() - above.getY() >= 2 * step) return new int[] { below.getX(), below.getY() - step };
        }
        int x = column.isEmpty() ? 0 : column.getLast().getX();
        return new int[] { x, screen.height - 2 - 4 * step };
    }

    /**
     * The ore button, drawn as its icon alone (17×15, centred in the column's 20×20 slot on whole pixels), white while
     * ores are shown and grey
     * while hidden, like Xaero's own. Left click: opens or closes the vein filter ({@link ProspectVeinPanel}); right click:
     * shows or hides the ores altogether. It sits centred in its 20 px slot of Xaero's left column.
     */
    private static final class OreToggleButton extends Button {
        private static final net.minecraft.resources.ResourceLocation ICON =
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/gui/xaero/prospect_toggle.png");
        static final int ICON_W = 17, ICON_H = 15;

        OreToggleButton(int slotX, int slotY) {
            super(slotX, slotY, SLOT, SLOT, Component.empty(),
                    b -> ProspectVeinPanel.toggle(), DEFAULT_NARRATION);
            refresh();
        }

        private void refresh() {
            setMessage(toggleText());
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(toggleText().copy().append("\n")
                    .append(Component.translatable("gui.roll_mod.prospect_map.toggle.tip"))));
        }

        @Override
        protected boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && active && visible && isMouseOver(mouseX, mouseY)) {
                playDownSound(net.minecraft.client.Minecraft.getInstance().getSoundManager());
                ProspectMapState.toggle();
                refresh();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            float c = ProspectMapState.visible() ? (isHoveredOrFocused() ? 1f : 0.9f) : (isHoveredOrFocused() ? 0.6f : 0.45f);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            graphics.setColor(c, c, c, 1f);
            // At its own size, centred in the slot, whole pixels.
            graphics.blit(ICON, getX() + (width - ICON_W) / 2, getY() + (height - ICON_H) / 2, 0, 0,
                    ICON_W, ICON_H, ICON_W, ICON_H);
            graphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    /** The height of one slot in Xaero's left button column. */
    private static final int SLOT = 20;

    private static boolean isWorldMap(Screen screen) {
        return screen != null && WORLD_MAP_SCREEN.equals(screen.getClass().getName());
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!isWorldMap(screen)) return;
        int mx = event.getMouseX(), my = event.getMouseY();
        ProspectVeinPanel.render(screen, event.getGuiGraphics(), mx, my, event.getPartialTick());
        // The vein under the mouse, unless the mouse is over the panel.
        if (ProspectVeinPanel.covers(screen, mx, my)) {
            WorldMapProspects.renderHoveredTooltip(event.getGuiGraphics(), Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            WorldMapProspects.renderHoveredTooltip(event.getGuiGraphics(), mx, my);
        }
    }

    private static Component toggleText() {
        return Component.translatable(ProspectMapState.visible()
                ? "gui.roll_mod.prospect_map.toggle.on" : "gui.roll_mod.prospect_map.toggle.off");
    }

    private static Component heightText() {
        return Component.literal("Y " + ProspectMapState.layerY());
    }
}
