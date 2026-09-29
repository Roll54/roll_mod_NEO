package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper.DragMove;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper.DragResize;
import com.lowdragmc.lowdraglib2.gui.util.WindowDragHelper.ResizeHandle;
import org.appliedenergistics.yoga.YogaPositionType;
import org.joml.Vector2f;
import org.joml.Vector4f;

/**
 * The hub's window: a panel floating on a full-screen host, moved by its header and resized from
 * any edge or corner, the way LdLib2's own editor windows are.
 *
 * <p>Built on the same pieces as LdLib2's {@code Dialog.windowMode} — {@link DragMove} and
 * {@link DragResize} drags, {@link WindowDragHelper#detectResizeHandle},
 * {@link WindowDragHelper#computeResizeRect} and {@link WindowDragHelper#drawResizeIcon} — but not
 * on {@code setDragMove}/{@code setBorderResize} themselves: those never clamp the position, so a
 * window could be dragged off screen, or grown past its edge from the left or top.
 *
 * <p>The geometry is layout only. Nothing here carries a sync value, and the server's copy of the
 * tree keeps the default rectangle forever — it is never initialised against a screen and never
 * receives a mouse event. {@code clientCopy} gates every read and write of
 * {@link HubWindowState}; it comes from the player's level, not the dist, because in single player
 * the integrated server builds its copy on a {@code Dist.CLIENT} JVM too.
 */
final class HubWindow extends UIElement {

    /** How far in from the frame's edge a press starts a resize. Inside the 6px padding. */
    static final float RESIZE_BORDER = 4;

    /** A press that moved less than this is still a click, so a double-click can follow it. */
    private static final float CLICK_SLOP = 3;

    private static final int DRAG_ICON = 12;

    private final boolean clientCopy;
    private final Vector2f minSize = new Vector2f(HubWindowState.MIN_W, HubWindowState.MIN_H);

    private int screenW = HubWindowState.DEFAULT_W;
    private int screenH = HubWindowState.DEFAULT_H;

    /** Whether the current header press has become a drag, which rules out a double-click. */
    private boolean moved;

    HubWindow(boolean clientCopy) {
        this.clientCopy = clientCopy;
        layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0)
                .width(HubWindowState.DEFAULT_W).height(HubWindowState.DEFAULT_H));

        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0 || maximized()) return;
            ResizeHandle handle = WindowDragHelper.detectResizeHandle(this, e.x, e.y, RESIZE_BORDER);
            if (handle == null) return;
            startDrag(new DragResize(getLayoutX(), getLayoutY(), getSizeWidth(), getSizeHeight(), handle),
                    handle.icon)
                    .setDragTexture(-handle.icon.spriteSize.width / 2f, -handle.icon.spriteSize.height / 2f,
                            handle.icon.spriteSize.width, handle.icon.spriteSize.height);
            e.stopPropagation();
        });
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof DragResize d)) return;
            Vector2f delta = getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
            // computeResizeRect only bounds the size. Bounding the size by the room left between the
            // fixed edge and the screen's edge is what keeps the moving edge on screen.
            Vector2f maxSize = new Vector2f(
                    grows(d.handle(), true) ? d.startX() + d.startW() : screenW - d.startX(),
                    grows(d.handle(), false) ? d.startY() + d.startH() : screenH - d.startY());
            Vector4f r = WindowDragHelper.computeResizeRect(d, delta.x, delta.y,
                    new Vector2f(Math.min(minSize.x, maxSize.x), Math.min(minSize.y, maxSize.y)), maxSize);
            layout(l -> l.left(r.x).top(r.y).width(r.z).height(r.w));
        });
        addEventListener(UIEvents.DRAG_END, e -> commit());
    }

    /** Whether this handle moves the left edge (horizontal) or the top edge (vertical). */
    private static boolean grows(ResizeHandle handle, boolean horizontal) {
        return horizontal
                ? handle == ResizeHandle.LEFT || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.BOTTOM_LEFT
                : handle == ResizeHandle.TOP || handle == ResizeHandle.TOP_LEFT || handle == ResizeHandle.TOP_RIGHT;
    }

    /**
     * Makes {@code handle} move the window. Only presses on the handle itself count — a press on a
     * bookmark inside it is a tab click, not a drag. Double-clicking it toggles maximize.
     */
    void dragBy(UIElement handle) {
        handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0 || e.target != handle) return;
            moved = false;
            if (maximized()) return;
            handle.startDrag(new DragMove(getLayoutX(), getLayoutY()), Icons.MOVE)
                    .setDragTexture(-DRAG_ICON / 2f, -DRAG_ICON / 2f, DRAG_ICON, DRAG_ICON);
            e.stopPropagation();
        });
        handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof DragMove m)) return;
            Vector2f delta = handle.getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
            if (Math.abs(delta.x) >= CLICK_SLOP || Math.abs(delta.y) >= CLICK_SLOP) moved = true;
            float x = clamp(m.startX() + delta.x, screenW - getSizeWidth());
            float y = clamp(m.startY() + delta.y, screenH - getSizeHeight());
            layout(l -> l.left(x).top(y));
        });
        handle.addEventListener(UIEvents.DRAG_END, e -> commit());
        handle.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (e.button == 0 && e.target == handle && !moved) toggleMaximize();
        });
    }

    /**
     * Places the window on a {@code screenW}x{@code screenH} screen. Called from the UI's size
     * provider, which LdLib runs on every screen init — opening, resizing the game window, changing
     * the GUI scale — so a window left off a now-smaller screen is always pulled back onto it.
     */
    void fit(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        HubWindowState.Rect r = clientCopy
                ? HubWindowState.get().applied(screenW, screenH)
                : new HubWindowState.Rect((screenW - HubWindowState.DEFAULT_W) / 2,
                        (screenH - HubWindowState.DEFAULT_H) / 2, HubWindowState.DEFAULT_W, HubWindowState.DEFAULT_H);
        layout(l -> l.left(r.x()).top(r.y()).width(r.w()).height(r.h()));
    }

    boolean maximized() {
        return clientCopy && HubWindowState.get().maximized();
    }

    void toggleMaximize() {
        if (!clientCopy) return;
        HubWindowState state = HubWindowState.get();
        state.setMaximized(!state.maximized());
        fit(screenW, screenH);
        state.save();
    }

    /** Back to the default size, centred, not maximized. */
    void reset() {
        if (!clientCopy) return;
        HubWindowState state = HubWindowState.get();
        state.reset();
        fit(screenW, screenH);
        state.save();
    }

    /** Remembers where a move or resize left the window. */
    private void commit() {
        if (!clientCopy || maximized()) return;
        HubWindowState state = HubWindowState.get();
        state.setRect(Math.round(getLayoutX()), Math.round(getLayoutY()),
                Math.round(getSizeWidth()), Math.round(getSizeHeight()));
        state.save();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (!clientCopy || maximized()) return;
        ModularUI ui = getModularUI();
        if (ui != null && !ui.getDragHandler().isDragging()) {
            WindowDragHelper.drawResizeIcon(guiContext, this, RESIZE_BORDER);
        }
    }

    private static float clamp(float v, float max) {
        return Math.max(0, Math.min(v, max));
    }
}
