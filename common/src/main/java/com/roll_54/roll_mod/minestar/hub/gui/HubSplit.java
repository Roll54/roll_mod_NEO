package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;

import java.util.List;

/**
 * The draggable divider between a tab's list and its info panel: drag it to make the list wider or
 * narrower, double-click it for the tab's default width again. The width is kept per tab in
 * {@link HubWindowState}, so it survives closing the hub and restarting.
 *
 * <p>Uses LdLib's own drag mechanism, like {@code HubSection}'s floating panels:
 * {@code DRAG_SOURCE_UPDATE} reaches the element that started the drag whatever the cursor is over,
 * so a fast drag cannot outrun it. The width is measured from where the drag started, not summed
 * frame by frame, so a dropped frame cannot make it drift.
 *
 * <p>Layout only — no sync value — so it is safe on both sides; only the client copy reads or
 * writes the saved width.
 */
public final class HubSplit {

    /** The divider's own width; the 1px line is drawn in its middle. */
    static final int WIDTH = 5;
    private static final int MIN_LIST = 80;
    /** What the info panel keeps however far the divider is dragged. */
    private static final int MIN_DETAIL = 160;

    private static final int COLOR_LINE = 0x30FFFFFF;
    private static final int COLOR_LINE_HOVER = 0x90FFFFFF;

    private HubSplit() {}

    /**
     * Sizes {@code list} from the saved width, and returns the divider to put right after it in the
     * same row.
     *
     * @param key      the tab's id, which the width is saved under
     * @param defaultW the list's width until the player drags it
     */
    static UIElement handle(String key, UIElement list, int defaultW, boolean clientCopy) {
        return handle(key, list, defaultW, clientCopy, false, MIN_LIST, MIN_DETAIL);
    }

    /**
     * The same divider for a row whose info panel is on the <em>right</em> and is the one with a
     * width of its own — daily tasks' bonus panel. Dragging left widens {@code panel}.
     *
     * @param minPanel the narrowest {@code panel} may get
     * @param minRest  what the rest of the row keeps however far the divider is dragged
     */
    public static UIElement handleRight(String key, UIElement panel, int defaultW, boolean clientCopy,
                                 int minPanel, int minRest) {
        return handle(key, panel, defaultW, clientCopy, true, minPanel, minRest);
    }

    /**
     * @param sized    the element the divider sets the width of
     * @param right    whether {@code sized} is right of the divider, so dragging left grows it
     * @param minSized the narrowest {@code sized} may get
     * @param minRest  what the rest of the row keeps
     */
    private static UIElement handle(String key, UIElement sized, int defaultW, boolean clientCopy,
                                    boolean right, int minSized, int minRest) {
        UIElement list = sized;
        int start = clientCopy ? HubWindowState.get().split(key, defaultW) : defaultW;
        width(list, start);

        UIElement handle = new UIElement();
        handle.layout(l -> l.width(WIDTH).heightPercent(100).flexShrink(0));
        UIElement line = new UIElement();
        line.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(WIDTH / 2).top(0).width(1).heightPercent(100));
        line.style(s -> s.background(new ColorRectTexture(COLOR_LINE)));
        handle.addChild(line);

        handle.addEventListener(UIEvents.MOUSE_ENTER, e ->
                line.style(s -> s.background(new ColorRectTexture(COLOR_LINE_HOVER))));
        handle.addEventListener(UIEvents.MOUSE_LEAVE, e ->
                line.style(s -> s.background(new ColorRectTexture(COLOR_LINE))));
        handle.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.split.tip")), null, null, ItemStack.EMPTY));

        boolean[] dragging = {false};
        // Two dividers may share a key (the travel tab's two views); each follows the width the
        // other saved, so both lists stay the same width.
        if (clientCopy) {
            handle.addEventListener(UIEvents.TICK, e -> {
                if (dragging[0]) return;
                int saved = HubWindowState.get().split(key, defaultW);
                if (Math.abs(list.getSizeWidth() - saved) > 0.5f && list.getSizeWidth() > 0) width(list, saved);
            });
        }

        handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button != 0) return;
            dragging[0] = true;
            handle.startDrag(new float[] {list.getSizeWidth()}, null);
            e.stopPropagation();
        });
        handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof float[] origin)) return;
            UIElement row = handle.getParent();
            float room = row == null ? Float.MAX_VALUE : row.getContentWidth() - WIDTH - minRest;
            float delta = e.x - e.dragStartX;
            int width = (int) Math.max(minSized, Math.min(room, origin[0] + (right ? -delta : delta)));
            width(list, width);
        });
        handle.addEventListener(UIEvents.DRAG_END, e -> {
            dragging[0] = false;
            if (!clientCopy) return;
            HubWindowState state = HubWindowState.get();
            state.setSplit(key, Math.round(list.getSizeWidth()));
            state.save();
        });
        handle.addEventListener(UIEvents.DOUBLE_CLICK, e -> {
            if (e.button != 0) return;
            width(list, defaultW);
            if (clientCopy) {
                HubWindowState state = HubWindowState.get();
                state.setSplit(key, -1);
                state.save();
            }
        });
        return handle;
    }

    /** A fixed width that neither grows nor shrinks with the row. */
    private static void width(UIElement list, float width) {
        list.layout(l -> l.width(width).flexBasis(width).flexGrow(0).flexShrink(0));
    }
}
