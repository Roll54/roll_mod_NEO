package com.roll_54.roll_mod.minestar.hub.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * The raised near-white plate vanilla draws its own panels with, for a widget that wants one.
 *
 * <p>Drawn as flat fills rather than as a nine-sliced texture, and that is the point of it: the
 * colours are vanilla's own — the inventory panel's face, highlight and shade, read straight off
 * {@code textures/gui/container/inventory.png} — so it matches the screen it sits on at any GUI
 * scale, with no atlas, no sprite metadata and no second renderer involved.
 *
 * <p>An earlier version borrowed LowDragLib2's vanilla-style sprite, the one the hub's own bookmarks
 * are built from. It looked right in the hub and wrong in the inventory: that library batches its
 * quads through its own render type, so a plate drawn that way and an icon blitted the vanilla way
 * do not reliably land in the order they were asked for, and the icon went missing. Everything here
 * goes through the same {@code GuiGraphics} calls as whatever is drawn over it.
 */
public final class HubPlate {

    /** The panel face, and the two bevel tones vanilla shades it with. */
    private static final int FACE = 0xFFC6C6C6;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADE = 0xFF555555;

    private HubPlate() {}

    /** Draws the plate at {@code x, y}. Client-side, on the render thread. */
    public static void draw(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;

        graphics.fill(x, y, right, bottom, FACE);
        // Lit from the top left, as every vanilla panel is: the two near edges catch the light and
        // the two far ones fall away.
        graphics.fill(x, y, right - 1, y + 1, HIGHLIGHT);
        graphics.fill(x, y, x + 1, bottom - 1, HIGHLIGHT);
        graphics.fill(x + 1, bottom - 1, right, bottom, SHADE);
        graphics.fill(right - 1, y + 1, right, bottom, SHADE);
    }
}
