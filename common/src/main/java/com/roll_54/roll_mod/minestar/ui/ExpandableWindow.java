package com.roll_54.roll_mod.minestar.ui;

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

import javax.annotation.Nullable;

/**
 * A panel floating on a full-screen host, moved by its header and resized from any edge or corner,
 * the way LdLib2's own editor windows are; double-clicking the header maximizes it. Where it was left
 * is remembered in a {@link WindowGeometry} — the hub and the config screens each keep their own.
 *
 * <p>Built on the same pieces as LdLib2's {@code Dialog.windowMode} — {@link DragMove} and
 * {@link DragResize} drags, {@link WindowDragHelper#detectResizeHandle},
 * {@link WindowDragHelper#computeResizeRect} and {@link WindowDragHelper#drawResizeIcon} — but not
 * on {@code setDragMove}/{@code setBorderResize} themselves: those never clamp the position, so a
 * window could be dragged off screen, or grown past its edge from the left or top.
 *
 * <p>The geometry is layout only. Nothing here carries a sync value. A window built for a menu exists
 * on the server too, where it has no {@code geometry}: that copy keeps the default rectangle forever —
 * it is never initialised against a screen and never receives a mouse event.
 */
public class ExpandableWindow extends UIElement {

    /** How far in from the frame's edge a press starts a resize. Inside a 6px padding. */
    public static final float RESIZE_BORDER = 4;

    /** A press that moved less than this is still a click, so a double-click can follow it. */
    private static final float CLICK_SLOP = 3;

    private static final int DRAG_ICON = 12;

    /** The remembered rectangle; null on a server copy, which never reads or writes one. */
    @Nullable private final WindowGeometry geometry;
    private final int defaultW;
    private final int defaultH;
    private final Vector2f minSize;

    private int screenW;
    private int screenH;

    /** Whether the current header press has become a drag, which rules out a double-click. */
    private boolean moved;

    /**
     * @param geometry where the player left this kind of window; null for a server copy
     */
    public ExpandableWindow(@Nullable WindowGeometry geometry, int defaultW, int defaultH, int minW, int minH) {
        this.geometry = geometry;
        this.defaultW = defaultW;
        this.defaultH = defaultH;
        this.minSize = new Vector2f(minW, minH);
        this.screenW = defaultW;
        this.screenH = defaultH;
        layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0).width(defaultW).height(defaultH));

        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0 || maximized() || locked()) return;
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
     * button inside it is that button's click, not a drag. Double-clicking it toggles maximize.
     */
    public void dragBy(UIElement handle) {
        handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0 || e.target != handle) return;
            moved = false;
            if (maximized()) return;
            // A window the player pinned in place stays where it is and keeps its size. Maximizing
            // is a separate gesture and still works.
            if (locked()) return;
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
     * Places the window on a {@code screenW}x{@code screenH} screen. Call it from the UI's size
     * provider, which LdLib runs on every screen init — opening, resizing the game window, changing
     * the GUI scale — so a window left off a now-smaller screen is always pulled back onto it.
     */
    public void fit(int screenW, int screenH) {
        this.screenW = screenW;
        this.screenH = screenH;
        WindowGeometry.Rect r = geometry != null
                ? geometry.applied(screenW, screenH)
                : new WindowGeometry.Rect((screenW - defaultW) / 2, (screenH - defaultH) / 2, defaultW, defaultH);
        layout(l -> l.left(r.x()).top(r.y()).width(r.w()).height(r.h()));
    }

    public boolean maximized() {
        return geometry != null && geometry.maximized();
    }

    /** The player turned moving off (e.g. the hub's "Allow moving the hub"): no moving, no resizing. */
    public boolean locked() {
        return geometry != null && !geometry.movable();
    }

    public void toggleMaximize() {
        if (geometry == null) return;
        geometry.setMaximized(!geometry.maximized());
        fit(screenW, screenH);
        geometry.save();
    }

    /** Back to the default size, centred, not maximized. */
    public void reset() {
        if (geometry == null) return;
        geometry.reset();
        fit(screenW, screenH);
        geometry.save();
    }

    /** Remembers where a move or resize left the window. */
    private void commit() {
        if (geometry == null || maximized()) return;
        geometry.setRect(Math.round(getLayoutX()), Math.round(getLayoutY()),
                Math.round(getSizeWidth()), Math.round(getSizeHeight()));
        geometry.save();
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (geometry == null || maximized() || locked()) return;
        ModularUI ui = getModularUI();
        if (ui != null && !ui.getDragHandler().isDragging()) {
            WindowDragHelper.drawResizeIcon(guiContext, this, RESIZE_BORDER);
        }
    }

    private static float clamp(float v, float max) {
        return Math.max(0, Math.min(v, max));
    }
}
