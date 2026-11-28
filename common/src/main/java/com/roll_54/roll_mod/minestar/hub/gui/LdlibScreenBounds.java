package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;

import javax.annotation.Nullable;
import org.joml.Vector2f;

/**
 * Where an LdLib screen's window actually is, for recipe viewers (TEI) that lay their sidebars out around a
 * container screen's rectangle. LdLib's container screens do not report a usable one, so TEI drew its item list,
 * page arrows and pins inside the window (the Crop Manager, every machine screen). Client only.
 */
public final class LdlibScreenBounds {

    private LdlibScreenBounds() {}

    /** Ids of the windows that mark themselves: an ME hatch's, the service block's, the Crop Manager's. */
    private static final String[] WINDOW_IDS = {
            com.roll_54.roll_mod.compat.mi.machines.me.MeHatchUI.ROOT_ID,
            com.roll_54.roll_mod.economy.service.gui.ServiceUI.ROOT_ID,
            com.roll_54.roll_mod.compat.MBD2.machine.CropManagerUI.ROOT_ID};

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
                int[] r = withShownChildren(window);
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
            float[] r = world(child);
            box[0] = Math.min(box[0], r[0]);
            box[1] = Math.min(box[1], r[1]);
            box[2] = Math.max(box[2], r[0] + r[2]);
            box[3] = Math.max(box[3], r[1] + r[3]);
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
        float[] r = world(element);
        return new int[] {Math.round(r[0]), Math.round(r[1]), (int) Math.ceil(r[2]), (int) Math.ceil(r[3])};
    }

    /**
     * A window and whatever of its shown children sticks out of it (the Crop Manager's settings panel and gear
     * beside its right edge), so the recipe viewer keeps clear of those too.
     */
    @Nullable
    private static int[] withShownChildren(UIElement window) {
        int[] r = rect(window);
        if (r == null) return null;
        int x0 = r[0], y0 = r[1], x1 = r[0] + r[2], y1 = r[1] + r[3];
        for (UIElement child : window.getChildren()) {
            if (!child.isDisplayed() || !child.isVisible()) continue;
            int[] c = rect(child);
            if (c == null) continue;
            x0 = Math.min(x0, c[0]);
            y0 = Math.min(y0, c[1]);
            x1 = Math.max(x1, c[0] + c[2]);
            y1 = Math.max(y1, c[1] + c[3]);
        }
        return new int[] {x0, y0, x1 - x0, y1 - y0};
    }

    /**
     * {x, y, w, h} on screen. Layout position and size are before the parents' transforms and the UI's draw pose
     * (MBD2 machine UIs are moved and scaled by them), so they go through {@code localToWorld} the way LdLib2's own
     * JEI/EMI bridge does; read raw, the box sat beside the real window and TEI laid its sidebars over the machine.
     */
    private static float[] world(UIElement element) {
        Vector2f pos = element.localToWorld(new Vector2f(element.getPositionX(), element.getPositionY()));
        Vector2f size = element.localToWorldNormal(new Vector2f(element.getSizeWidth(), element.getSizeHeight()));
        return new float[] {pos.x, pos.y, Math.abs(size.x), Math.abs(size.y)};
    }
}
