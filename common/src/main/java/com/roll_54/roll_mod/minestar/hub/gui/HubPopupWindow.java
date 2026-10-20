package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * A hub popup — the money window, "configure interaction", an "are you sure?" — in an
 * operating-system window of its own, the way {@link HubInfoWindow} shows a section's explanation:
 * movable anywhere on the desktop, resizable from its edges, always on top.
 *
 * <p>The popup's body is not rebuilt for the window. The very elements the tab made — with their
 * listeners, and the labels and switches the tab's own tick keeps updating — are moved into the
 * window while it is up and handed back to the in-hub panel when it closes ({@link
 * HubSection.Floating#reclaim}). So everything the popup does works the same either way, and the
 * tab never knows which one it got.
 *
 * <p>One at a time, like the info window: opening a second closes the first, and a tab change or the
 * hub closing ({@code HubSection.closeInfo}) closes it.
 *
 * <p>Client-only, reached only behind {@code HubSection}'s dist check.
 */
@OnlyIn(Dist.CLIENT)
public final class HubPopupWindow {

    private static final int TITLE_H = 14;
    private static final int TITLE_FILL = 0x30FFFFFF;
    private static final int PADDING = 8;
    /** {@code exclamation_mark.png}, at its own 7x7. */
    private static final int ICON = 7;
    /**
     * Desktop pixels per hub pixel. A little larger than the info window's proportion (280 → 400),
     * so a popup opens roomy and can be shrunk, rather than cramped and needing a drag.
     */
    private static final float SCALE = 1.6f;

    @Nullable private static ModularUIWindow current;
    @Nullable private static HubSection.Floating currentFloating;

    private HubPopupWindow() {}

    /** Opens {@code floating} as a window, or answers false so the caller shows the in-hub panel. */
    static boolean open(HubSection.Floating floating) {
        if (!OsWindowManager.isAvailable()) return false;
        if (current != null && current.isOpen() && currentFloating == floating) {
            current.window().focus();
            return true;
        }
        close();
        // Whatever in-hub panel is up gives way, as one popup replaces another inside the hub.
        HubSection.closeInHubPanel();

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100));
        root.style(s -> s.background(HubSection.frameFor(root)));

        UIElement titleBar = new UIElement();
        titleBar.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .widthPercent(100).height(TITLE_H).paddingLeft(PADDING).paddingRight(2).gapColumn(2));
        titleBar.style(s -> s.background(new ColorRectTexture(TITLE_FILL)));
        if (floating.icon != null) {
            UIElement mark = new UIElement();
            mark.layout(l -> l.width(ICON).height(ICON).flexShrink(0));
            mark.style(s -> s.background(floating.icon));
            titleBar.addChild(mark);
        }
        Label caption = new Label();
        caption.setText(floating.title);
        caption.layout(l -> l.flexGrow(1).height(TITLE_H));
        caption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        titleBar.addChild(caption);
        if (floating.closable) {
            // A Button, so ModularUIWindow.isDragBlocker keeps a press on it from moving the window.
            Button closeButton = new Button();
            closeButton.noText().addPreIcon(Icons.WINDOW_CLOSE).layout(l -> l.width(TITLE_H).height(TITLE_H));
            closeButton.setOnClick(e -> close());
            titleBar.addChild(closeButton);
        }

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1)
                .paddingAll(PADDING));
        floating.content.removeSelf();
        body.addChild(floating.content);
        root.addChildren(titleBar, body);

        ModularUIWindow window = new ModularUIWindow(ModularUI.of(UI.of(root, RollUi.sheets())),
                floating.title.getString());
        window.setDragArea(titleBar);
        window.setOnCloseRequested(HubPopupWindow::close);
        int w = Math.max(ModularUIWindow.MIN_WIDTH, Math.round(floating.width() * SCALE));
        int h = Math.max(ModularUIWindow.MIN_HEIGHT, Math.round(floating.height() * SCALE) + TITLE_H);
        if (!window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, w, h, false)) {
            // No window after all: the body goes home and the caller shows the panel instead.
            floating.reclaim();
            return false;
        }
        window.setResizable(true);
        window.setAlwaysOnTop(true);

        current = window;
        currentFloating = floating;
        return true;
    }

    static boolean isOpenFor(HubSection.Floating floating) {
        return currentFloating == floating && current != null && current.isOpen();
    }

    /** Closes the window if {@code floating} is the one in it. */
    static void closeIf(HubSection.Floating floating) {
        if (currentFloating == floating) close();
    }

    /** Takes the window down and hands the body back to its panel. Safe when there is none. */
    public static void close() {
        ModularUIWindow window = current;
        HubSection.Floating floating = currentFloating;
        current = null;
        currentFloating = null;
        if (window != null && window.isOpen()) window.close();
        if (floating != null) floating.reclaim();
    }
}
