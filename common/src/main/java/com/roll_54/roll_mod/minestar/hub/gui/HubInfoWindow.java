package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * A hub section's explanation in an operating-system window of its own.
 *
 * <p>Why a real window rather than a panel drawn inside the hub: the explanation and the section it
 * explains are read together, and no in-screen panel can get out of its own way well enough for that.
 * This one can be dragged anywhere the desktop goes — onto a second monitor, off the game window
 * entirely — and left open while the hub is used underneath. The UI debugger takes exactly this route
 * for the same reason; see {@link ModularUIWindow}.
 *
 * <p>Client-only, and reached only through {@link HubSection}'s dist check: the hub's element tree is
 * built on the dedicated server too, so the window classes must never be resolved there.
 *
 * <p><b>There is only ever one.</b> {@link #open} closes whatever it finds already up before putting
 * a new one there, and {@link #close} is called from {@code HubUI.select} on every tab change and
 * when the hub screen itself goes away. A stack of stale explanations for sections nobody is looking
 * at any more was the alternative, and an always-on-top window is exactly the wrong thing to leave
 * lying around.
 *
 * <p>The size is fixed rather than resizable, so the prose column is the one it was written for; the
 * body scrolls instead of growing. {@link ModularUIWindow#setResizable} is what turns the drag-edge
 * gesture off — the window is undecorated, so the platform offers no resize border of its own and
 * that gesture is the only way it could have been resized.
 *
 * <p>The body is filled <em>before</em> the window opens, not lazily afterwards. That is not a
 * micro-optimisation, it is the fix for the scrolling: {@code ScrollerView} sizes its scroll bars
 * from {@code LAYOUT_CHANGED} on its viewport, so lines added after the first layout pass leave it
 * still believing it has nothing to scroll. Building the content complete means the first layout is
 * also the correct one. Re-reading per open is what keeps {@code F3+T} live.
 */
@OnlyIn(Dist.CLIENT)
public final class HubInfoWindow {

    /** Fixed, not a starting point: a narrow column of prose, tall enough to be worth scrolling. */
    private static final int WIDTH = 250;
    private static final int HEIGHT = 400;

    private static final int TITLE_H = 14;
    private static final int TITLE_FILL = 0x30FFFFFF;
    private static final int PADDING = 8;

    /**
     * The one window, and the section it is explaining.
     *
     * <p>Static because there is one desktop and one local player: the field is only ever touched
     * from a click listener or from {@code HubUI.select}, both of which are client-side, and the
     * dedicated server never resolves this class at all.
     */
    @Nullable private static ModularUIWindow current;
    @Nullable private static String currentId;

    private HubInfoWindow() {}

    /**
     * Opens the window for one section, or returns {@code false} if the platform will not give us a
     * second window — in which case the caller keeps the in-hub panel it already has. Nothing is
     * half-done on that path: the window is only ever handed content once it really exists.
     *
     * <p>Asking for the section that is already up focuses that window rather than replacing it, so
     * a second click on the {@code I} button fetches a window that had wandered behind something
     * instead of throwing away its scroll position.
     */
    public static boolean open(String id, Component title) {
        if (!OsWindowManager.isAvailable()) return false;

        if (current != null && current.isOpen()) {
            if (id.equals(currentId)) {
                current.window().focus();
                return true;
            }
            close();
        }

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100));

        UIElement titleBar = new UIElement();
        titleBar.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .widthPercent(100).height(TITLE_H).paddingLeft(PADDING).paddingRight(2).gapColumn(2));
        titleBar.style(s -> s.background(new ColorRectTexture(TITLE_FILL)));

        Label caption = new Label();
        caption.setText(title);
        caption.layout(l -> l.flexGrow(1).height(TITLE_H));
        caption.textStyle(t -> t.font(HubInfo.FONT)
                .textAlignVertical(Vertical.CENTER).textShadow(true));

        // A Button, so ModularUIWindow.isDragBlocker keeps a press on it from moving the window.
        Button closeButton = new Button();
        closeButton.noText().addPreIcon(Icons.WINDOW_CLOSE)
                .layout(l -> l.width(TITLE_H).height(TITLE_H));

        titleBar.addChildren(caption, closeButton);

        ScrollerView body = new ScrollerView();
        // flexBasis(0) is what actually makes this scroll, and it is not optional. LdLib defaults
        // flex-shrink to 0 and flex-basis to auto, so with flexGrow alone the scroller's base size
        // is its own content height — the whole document — and there is no shrinking to pull it back
        // to the window. It then overflowed the window instead of scrolling inside it: the viewport
        // was as tall as the text, so ScrollerView measured no overflow and never showed a bar.
        // A zero basis makes the grow the only thing deciding the height, which is the window's.
        body.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        body.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        // COLUMN, or every line sits at y=0 and the measured content height never exceeds the
        // viewport — the scroller then concludes there is nothing to scroll.
        body.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .widthPercent(100).paddingAll(PADDING)));
        for (UIElement line : HubInfo.read(id)) {
            body.addScrollViewChild(line);
        }

        root.addChildren(titleBar, body);

        // The modern theme by name, as the debugger does: a window has no screen behind it to inherit
        // a stylesheet from, so without this the scroll bar and button would come up unstyled.
        ModularUIWindow window = new ModularUIWindow(
                ModularUI.of(UI.of(root,
                        StylesheetManager.INSTANCE.getStylesheet(StylesheetManager.MODERN))),
                title.getString());
        // The whole bar, label included — isOverDragArea walks up from whatever was hit.
        window.setDragArea(titleBar);
        // Both routes out go through our own close(), so the static below cannot be left pointing at
        // a window that is no longer there. onCloseRequested is what a platform close request lands
        // on; without a runnable it would take the window down behind our back.
        closeButton.setOnClick(e -> close());
        window.setOnCloseRequested(HubInfoWindow::close);

        // Undecorated: LdLib draws the bar and handles the move itself, which avoids the platform's
        // modal drag loop freezing the game while the window is being moved. MIN_VALUE for the
        // position lets the platform place it — with only ever one window there is nothing to
        // cascade away from.
        if (!window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, WIDTH, HEIGHT, false)) return false;

        // Both only bite on a window that is already up, which is why they follow the open rather
        // than preceding it. Always-on-top is a no-op on a platform that has no such attribute
        // (Wayland); the window is simply an ordinary one there.
        window.setResizable(false);
        window.setAlwaysOnTop(true);

        current = window;
        currentId = id;
        return true;
    }

    /**
     * Takes the window down, if there is one. Safe to call when there is not — which is most of the
     * time, since {@code HubUI.select} calls it on every tab change.
     */
    public static void close() {
        ModularUIWindow window = current;
        current = null;
        currentId = null;
        if (window != null && window.isOpen()) window.close();
    }
}
