package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import javax.annotation.Nullable;

/**
 * Where an LdLib screen's window actually is, for recipe viewers (TEI) that lay their sidebars out around a
 * container screen's rectangle. LdLib's container screens do not report a usable one, so TEI drew its item list,
 * page arrows and pins inside the window (the Crop Manager, every machine screen). Client only.
 */
public final class LdlibScreenBounds {

    private LdlibScreenBounds() {}

    /**
     * {x, y, w, h} in GUI pixels: the UI root, or — when the root spans the whole screen — the union of its shown
     * children that do not. Null for anything that is not an LdLib container screen, or has nothing laid out yet.
     */
    @Nullable
    public static int[] of(Object screen, int screenW, int screenH) {
        if (!(screen instanceof ModularUIContainerScreen container)) return null;
        ModularUI ui = container.getMenu().getModularUI();
        if (ui == null) return null;
        UIElement root = ui.ui.getRootElement();
        if (root == null) return null;
        if (!coversScreen(root, screenW, screenH)) return rect(root);
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (UIElement child : root.getChildren()) {
            if (!child.isDisplayed() || child.getSizeWidth() <= 0 || child.getSizeHeight() <= 0
                    || coversScreen(child, screenW, screenH)) continue;
            x0 = Math.min(x0, child.getPositionX());
            y0 = Math.min(y0, child.getPositionY());
            x1 = Math.max(x1, child.getPositionX() + child.getSizeWidth());
            y1 = Math.max(y1, child.getPositionY() + child.getSizeHeight());
        }
        if (x0 == Float.MAX_VALUE) return null;
        return new int[] {(int) x0, (int) y0, (int) Math.ceil(x1 - x0), (int) Math.ceil(y1 - y0)};
    }

    /** Whether {@code screen} is an LdLib container screen. */
    public static boolean isLdlib(Object screen) {
        return screen instanceof ModularUIContainerScreen;
    }

    private static boolean coversScreen(UIElement element, int screenW, int screenH) {
        return element.getSizeWidth() >= screenW - 2 && element.getSizeHeight() >= screenH - 2;
    }

    @Nullable
    private static int[] rect(UIElement element) {
        if (element.getSizeWidth() <= 0 || element.getSizeHeight() <= 0) return null;
        return new int[] {(int) element.getPositionX(), (int) element.getPositionY(),
                (int) Math.ceil(element.getSizeWidth()), (int) Math.ceil(element.getSizeHeight())};
    }
}
