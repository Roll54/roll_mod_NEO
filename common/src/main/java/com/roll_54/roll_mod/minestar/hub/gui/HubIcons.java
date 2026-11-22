package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.roll_54.roll_mod.RollMod;

/**
 * The hub's small control icons — window buttons (settings, reset, maximize, restore) and the tab headers' history
 * and info — as white pixel art at their own size (textures/gui/hub/icons), in place of LdLib's smooth icons, which
 * stood out against the Minecraft-style hub. White, so {@code LightText.icon} can tint them on light themes.
 */
public final class HubIcons {

    static final IGuiTexture SETTINGS = icon("settings");
    static final IGuiTexture RESET = icon("reset");
    static final IGuiTexture MAXIMIZE = icon("maximize");
    static final IGuiTexture RESTORE = icon("restore");
    static final IGuiTexture HISTORY = icon("history");
    static final IGuiTexture INFO = icon("info");
    public static final IGuiTexture REFRESH = icon("refresh");
    /** The ✕ on popups' close buttons. */
    public static final IGuiTexture CLOSE = icon("close");

    /**
     * {@code button} showing {@code icon} alone, centred at its own pixel size ({@code size}), in the theme's text
     * colour on light themes. For the small refresh buttons, which drew a "⟳" glyph that matched nothing around it.
     */
    public static com.lowdragmc.lowdraglib2.gui.ui.elements.Button iconOnly(com.lowdragmc.lowdraglib2.gui.ui.elements.Button button,
                                                                    IGuiTexture icon, int size, float width, float height) {
        button.setText(net.minecraft.network.chat.Component.empty());
        com.lowdragmc.lowdraglib2.gui.ui.UIElement mark = new com.lowdragmc.lowdraglib2.gui.ui.UIElement();
        mark.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left((float) Math.floor((width - size) / 2)).top((float) Math.floor((height - size) / 2))
                .width(size).height(size));
        mark.style(s -> s.background(lifted(IGuiTexture.dynamic(() -> com.roll_54.roll_mod.minestar.theme.LightText.icon(icon)))));
        mark.setAllowHitTest(false);
        button.addChild(mark);
        return button;
    }

    /** Ore themes' selector chevron (LdLib's Ore sheet), down and turned up; tinted like the selectors' (#444). */
    // Only the chevron's own 7x4 pixels of the Ore sheet's 10x10 DOWN cell, which sits off-centre in it: drawn at
    // its own size and turned about its own middle, up and down land on the same pixels.
    private static final net.minecraft.resources.ResourceLocation ORE_SHEET =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ldlib2", "textures/gui/ore_styles.png");
    private static final IGuiTexture ORE_DOWN = SpriteTexture.of(ORE_SHEET).setSprite(63, 38, 7, 4).setColor(0xFF444444);
    private static final IGuiTexture ORE_UP = SpriteTexture.of(ORE_SHEET).setSprite(63, 38, 7, 4).rotate(180).setColor(0xFF444444);
    private static final int ORE_W = 7;
    private static final int ORE_H = 4;
    private static final IGuiTexture LD_DOWN = SpriteTexture.of("ldlib2:textures/gui/down.png");
    private static final IGuiTexture LD_UP = SpriteTexture.of("ldlib2:textures/gui/up.png");

    /**
     * A ▲/▼ order button drawn with the theme's own drop-down chevron (the Selector's), not a text glyph — the Ore
     * chevron on Ore themes, LdLib's arrow tinted like text elsewhere. Faded while {@code active} is false.
     */
    public static com.lowdragmc.lowdraglib2.gui.ui.elements.Button arrow(boolean up, boolean active, int size, Runnable action) {
        com.lowdragmc.lowdraglib2.gui.ui.elements.Button button = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
        button.setText(net.minecraft.network.chat.Component.empty());
        button.layout(l -> l.width(size).height(size).paddingAll(0).flexShrink(0));
        // Two marks, one shown per theme: the Ore chevron at its own pixel size, LdLib's arrow in a square box.
        int inner = Math.max(6, size - 6);
        com.lowdragmc.lowdraglib2.gui.ui.UIElement ore = new com.lowdragmc.lowdraglib2.gui.ui.UIElement();
        ore.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left((size - ORE_W) / 2).top((size - ORE_H) / 2).width(ORE_W).height(ORE_H));
        ore.style(s -> s.background(lifted(IGuiTexture.dynamic(() -> {
            // GDP's drop-down chevron in every theme now (the plain mark below); the Ore one is no longer drawn.
            if (true) return IGuiTexture.EMPTY;
            IGuiTexture icon = up ? ORE_UP : ORE_DOWN;
            return active ? icon : icon.copy().setColor(0x60444444);
        }))));
        com.lowdragmc.lowdraglib2.gui.ui.UIElement plain = new com.lowdragmc.lowdraglib2.gui.ui.UIElement();
        plain.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left((size - inner) / 2).top((size - inner) / 2).width(inner).height(inner));
        plain.style(s -> s.background(lifted(IGuiTexture.dynamic(() -> {
            IGuiTexture down = com.lowdragmc.lowdraglib2.gui.texture.Icons.DOWN_ARROW_NO_BAR;
            IGuiTexture icon = com.roll_54.roll_mod.minestar.theme.LightText.icon(up ? down.copy().rotate(180) : down);
            return active ? icon : icon.copy().setColor(0x60FFFFFF);
        }))));
        ore.setAllowHitTest(false);
        plain.setAllowHitTest(false);
        button.addChildren(ore, plain);
        button.setActive(active);
        button.setOnClick(e -> {
            e.stopPropagation();
            action.run();
        });
        return button;
    }

    /**
     * {@code icon} drawn 1px higher on the themes whose buttons have a heavier bottom bevel than top — AE2 (2px over
     * 4px) and the Ore art — so it sits in the middle of the face rather than of the whole box. Read every frame, so
     * a theme change applies at once.
     */
    static IGuiTexture lifted(IGuiTexture icon) {
        return new IGuiTexture() {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics graphics, float mouseX, float mouseY, float x, float y,
                             float width, float height, float partialTicks) {
                var theme = com.roll_54.roll_mod.minestar.theme.UiThemeState.get();
                float lift = theme == com.roll_54.roll_mod.minestar.theme.UiTheme.AE2 || theme.oreArt() ? 1 : 0;
                icon.draw(graphics, mouseX, mouseY, x, y - lift, width, height, partialTicks);
            }
        };
    }

    /**
     * A bare ▲/▼ for a list row — GDP's drop-down chevron, no button frame — {@code size} square, level with the row's
     * other marks (the star, the pearl). Faded while {@code active} is false; a click runs {@code action}.
     */
    public static com.lowdragmc.lowdraglib2.gui.ui.UIElement rowArrow(boolean up, boolean active, int size, Runnable action) {
        com.lowdragmc.lowdraglib2.gui.ui.UIElement mark = new com.lowdragmc.lowdraglib2.gui.ui.UIElement();
        mark.layout(l -> l.width(size).height(size).flexShrink(0).alignSelf(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        mark.style(s -> s.background(IGuiTexture.dynamic(() -> {
            // GDP's own drop-down chevron (LdLib's Icons.*_ARROW_NO_BAR), read every frame: LdLib swaps Icons at
            // resource load.
            // UP_ARROW_NO_BAR points down too in this LdLib, so up is the down chevron turned over. One colour for both,
            // active or not; only a hover lights it.
            IGuiTexture down = com.lowdragmc.lowdraglib2.gui.texture.Icons.DOWN_ARROW_NO_BAR;
            IGuiTexture icon = com.roll_54.roll_mod.minestar.theme.LightText.icon(up ? down.copy().rotate(180) : down);
            // No hover light: the arrows stay the same colour.
            return snapped(icon);
        })));
        if (active) {
            mark.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, e -> {
                action.run();
                e.stopPropagation();
            });
        }
        return mark;
    }

    /**
     * {@code texture} drawn on whole pixels: a box centred in a row can land half a pixel off, and pixel art drawn
     * there doubles a row or column (a star's extra pixel, a pearl a pixel too wide).
     */
    public static IGuiTexture snapped(IGuiTexture texture) {
        return new IGuiTexture() {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics g, float mx, float my, float x, float y, float w, float h,
                             float pt) {
                texture.draw(g, mx, my, (float) Math.floor(x), (float) Math.floor(y), Math.round(w), Math.round(h), pt);
            }
        };
    }

    private HubIcons() {}

    private static IGuiTexture icon(String name) {
        return SpriteTexture.of(RollMod.id("textures/gui/hub/icons/" + name + ".png"));
    }
}
