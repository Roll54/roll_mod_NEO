package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
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

    /** Ids of the windows that mark themselves: an ME hatch's, the service block's. */
    private static final String[] WINDOW_IDS = {
            com.roll_54.roll_mod.compat.mi.machines.me.MeHatchUI.ROOT_ID,
            com.roll_54.roll_mod.economy.service.gui.ServiceUI.ROOT_ID};

    /**
     * {x, y, w, h} in GUI pixels: the UI root, or — when the root spans the whole screen — the union of its shown
     * children that do not. Null for anything that is not an LdLib screen (container or plain, like the creative
     * inventory), or has nothing laid out yet.
     */
    @Nullable
    public static int[] of(Object screen, int screenW, int screenH) {
        ModularUI ui = modularUI(screen);
        if (ui == null) return null;
        UIElement root = ui.ui.getRootElement();
        if (root == null) return null;
        // A window that names itself is the window, whatever else sits on the root (the ME hatches' screens had a
        // small element in a corner that stretched the union over the screen, so TEI drew over the window).
        for (String id : WINDOW_IDS) {
            UIElement window = ui.getElementById(id);
            if (window != null && window.isDisplayed()) {
                int[] r = rect(window);
                if (r != null) return r;
            }
        }
        if (!coversScreen(root, screenW, screenH)) return rect(root);
        float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        union(root, screenW, screenH, box, 0);
        if (box[0] == Float.MAX_VALUE) return null;
        return new int[] {(int) box[0], (int) box[1], (int) Math.ceil(box[2] - box[0]), (int) Math.ceil(box[3] - box[1])};
    }

    /**
     * The union of {@code parent}'s shown children, looking through any that span the whole screen: an MBD2 machine
     * (the Crop Manager) wraps its window in a screen-sized element of its own under the screen-sized root, so only
     * looking one level down found nothing but the settings panel beside the window, and TEI laid its sidebar over it.
     */
    private static void union(UIElement parent, int screenW, int screenH, float[] box, int depth) {
        for (UIElement child : parent.getChildren()) {
            if (!child.isDisplayed() || child.getSizeWidth() <= 0 || child.getSizeHeight() <= 0) continue;
            if (coversScreen(child, screenW, screenH)) {
                if (depth < 6) union(child, screenW, screenH, box, depth + 1);
                continue;
            }
            box[0] = Math.min(box[0], child.getPositionX());
            box[1] = Math.min(box[1], child.getPositionY());
            box[2] = Math.max(box[2], child.getPositionX() + child.getSizeWidth());
            box[3] = Math.max(box[3], child.getPositionY() + child.getSizeHeight());
        }
    }

    /** Whether {@code screen} is an LdLib screen, container or plain. */
    public static boolean isLdlib(Object screen) {
        return screen instanceof ModularUIContainerScreen || screen instanceof ModularUIScreen;
    }

    @Nullable
    private static ModularUI modularUI(Object screen) {
        if (screen instanceof ModularUIContainerScreen container) return container.getMenu().getModularUI();
        if (screen instanceof ModularUIScreen plain) return plain.getModularUI();
        return null;
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
