package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * The title row every hub tab wears, and the information panel behind its {@code I} button.
 *
 * <p>Six tabs needed the same two things, so they live here once rather than six times. Two of them
 * ({@link com.roll_54.roll_mod.economy.vendingblock.gui.auction.AuctionUI} and
 * {@code PlotPurchaseUI}) lay their contents out absolutely and already have a title of their own,
 * so the button and the panel are handed out separately from the header — see {@link #info} and
 * {@link #header}.
 *
 * <p>Both pieces are added to a tab <em>unconditionally</em>. The hub's tree is built identically on
 * the client and the dedicated server and its bindings are positional, so an element that appears on
 * one side only would shift every binding after it — see {@link HubUI}'s class javadoc. Only the
 * panel's <em>contents</em> are client-side, which is safe because they contain no sync values.
 *
 * <p>The panel is a floating window rather than a sheet over the whole tab, and can be dragged by its
 * title bar — see {@link #drag}. Both follow from the same thing: the explanation and the section it
 * explains are read together, so covering one with the other was the wrong shape.
 *
 * <p>Only one explanation is ever open, by either route, and changing section closes it — see
 * {@link #closeInfo}, which {@code HubUI.select} calls on every tab change and when the hub screen
 * goes away. An explanation is about the section under it; once that section is gone it is no longer
 * an explanation of anything, and on the window route it would have been an always-on-top window
 * left behind over the game.
 */
public final class HubSection {

    /** The title row: tall enough for a line of text, with a gap under it. Matches DailyTasksUI. */
    private static final int HEADER_H = 12;
    private static final int HEADER_GAP = 4;

    /** The I button, and the close button on the panel. */
    private static final int BUTTON = 12;

    /** Breathing room between a tab's own header button and the I button it sits beside. */
    private static final int BUTTON_GAP = 4;
    private static final int CLOSE_W = 44;
    private static final int CLOSE_H = 14;

    private static final int PANEL_PADDING = 8;

    /**
     * The window's size, inside the {@link HubUI#CONTENT_W}x{@link HubUI#CONTENT_H} tab box.
     *
     * <p>Deliberately smaller than the tab it floats over. Covering everything meant the section
     * underneath was unusable while the explanation of it was open, which is backwards — the two are
     * read together. A narrow column is also easier to read prose in than a 460px line.
     */
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 190;

    /** The title bar doubles as the drag handle, so it is tinted to look like one. */
    private static final int TITLE_FILL = 0x30FFFFFF;

    /**
     * Nearly opaque, so the section behind it stops competing for attention while being read — the
     * panel is prose, and prose over a busy list is unreadable. The border keeps it from bleeding
     * into the hub's own dark panel, which is the same family of colour.
     */
    private static final int PANEL_FILL = 0xF0101010;
    private static final int PANEL_BORDER = 0xFF707070;

    /** Above the tab's own contents, including anything the tab itself layers. */
    private static final int PANEL_Z = 10;

    /**
     * The fallback panel that is currently up, if any — there is at most one across every tab.
     *
     * <p>Only ever touched behind a {@link Dist#CLIENT} check, so the dedicated server (which builds
     * this same tree for every player) never writes to it. On the client there is one screen and one
     * local player, so one field is the whole of the state.
     */
    @Nullable private static UIElement openPanel;

    private HubSection() {}

    /**
     * Closes whatever explanation is open, by whichever route it opened. Called when the hub changes
     * tab and when its screen is removed; safe to call when nothing is open, which is most of the
     * time.
     */
    public static void closeInfo() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        ClientWindow.closeInfo();
    }

    /** The fallback half of {@link #closeInfo}, which the panel's own close button also wants. */
    private static void closePanel() {
        UIElement panel = openPanel;
        openPanel = null;
        if (panel != null) panel.setDisplay(false);
    }

    /**
     * The {@code I} button and the panel it opens, already wired to each other. <b>Both</b> have to
     * be added to the tab: the button wherever the tab wants it, the panel to the tab's root and
     * last, so it covers everything.
     */
    public record Info(UIElement button, UIElement overlay) {}

    public static Info info(String id) {
        Panel panel = overlay(id);

        UIElement button = new UIElement();
        button.layout(l -> l.width(BUTTON).height(BUTTON));
        button.style(s -> s.background(Icons.INFORMATION));
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui." + RollMod.MODID + ".hub.info.tip")),
                null, null, ItemStack.EMPTY));
        button.addEventListener(UIEvents.CLICK, e -> {
            // A window of its own where the platform allows one: it can be dragged onto another
            // monitor, sized past the tab box, and left open while the section is used underneath.
            // The dist check keeps the window classes off a dedicated server, where this tree is
            // also built — the click itself cannot fire there, but the reference would still resolve.
            if (FMLEnvironment.dist == Dist.CLIENT && ClientWindow.open(id)) return;

            // Fallback for a platform that will not give us a second window. Already populated, so
            // this is only a visibility toggle — and it goes through closePanel first so a second
            // section's explanation replaces the first rather than stacking on top of it.
            boolean show = !panel.root().isDisplayed();
            closePanel();
            if (show) {
                panel.root().setDisplay(true);
                openPanel = panel.root();
            }
        });

        return new Info(button, panel.root());
    }

    /**
     * A tab's title row: the section's name on the left, the {@code I} button on the right. The name
     * is the tab's own hover caption, so a tab is called the same thing in both places and adding a
     * section needs no second lang key.
     */
    public static UIElement header(String id, Info info, UIElement... extras) {
        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(HEADER_H)
                .marginBottom(HEADER_GAP));

        Label title = new Label();
        title.setText(title(id));
        title.layout(l -> l.flexGrow(1).height(HEADER_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));

        header.addChild(title);
        // Anything the tab wants beside the I button goes to its left, so the I stays where it is
        // on every tab — it is the one control in this row that is in the same place everywhere.
        for (UIElement extra : extras) {
            header.addChild(extra);
        }
        header.addChild(info.button());
        return header;
    }

    /**
     * A second kind of floating panel: a caption, a body the caller fills, and a close button.
     *
     * <p>Shares {@link #info}'s one-panel-at-a-time state, so opening this closes an open
     * explanation and {@code HubUI.select} closes this on a tab change — both for free. Unlike
     * {@link #info} there is no separate-OS-window route: this holds live controls that send
     * packets, and it belongs over the tab it configures.
     *
     * <p>Both halves have to be added to the tab, the same way {@link #info}'s are.
     */
    public static Info panel(Component title, IGuiTexture icon, Component tip,
                             int width, int height, Consumer<UIElement> body) {
        Floating floating = floating(title, width, height, body);

        UIElement button = new UIElement();
        button.layout(l -> l.width(BUTTON).height(BUTTON).marginRight(BUTTON_GAP));
        button.style(s -> s.background(icon));
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(tip), null, null, ItemStack.EMPTY));
        button.addEventListener(UIEvents.CLICK, e -> floating.toggle());

        return new Info(button, floating.root());
    }

    /**
     * The same window as {@link #panel}, with no button of its own: the caller opens it, typically
     * under the cursor with {@link Floating#openAt} — the shape of FTB Quests' "add reward" menu.
     *
     * <p>{@link Floating#root()} still has to be added to the tab's root, last, like every overlay
     * here; it is positioned in that root's coordinates.
     */
    public static Floating popup(Component title, int width, int height, Consumer<UIElement> body) {
        return floating(title, width, height, body);
    }

    /**
     * A floating window over a tab, and the handle for showing it.
     *
     * <p>All of them share the single {@link #openPanel} slot, so opening one closes whichever other
     * is up. Only ever opened from a click, which never fires on a dedicated server, so that slot
     * stays client-only as its javadoc requires.
     */
    public static final class Floating {
        private final UIElement root;
        private final float[] pos;
        private final int width;
        private final int height;

        private Floating(UIElement root, float[] pos, int width, int height) {
            this.root = root;
            this.pos = pos;
            this.width = width;
            this.height = height;
        }

        /** Add this to the tab's root, last. */
        public UIElement root() {
            return root;
        }

        public boolean isOpen() {
            return root.isDisplayed();
        }

        /** Opens wherever it last was — centred, the first time. */
        public void open() {
            closePanel();
            root.setDisplay(true);
            openPanel = root;
        }

        /**
         * Opens with its corner at a click, in the screen coordinates {@code UIEvent.x/y} carry.
         *
         * <p>Converted into the root's frame by subtracting the parent's absolute position — the pair
         * {@code isMouseOver} compares — and clamped with the same bounds {@link #drag} uses, so a
         * popup opened near an edge can never land somewhere it cannot be dragged back from.
         */
        public void openAt(float screenX, float screenY) {
            UIElement host = root.getParent();
            float originX = host == null ? 0 : host.getPositionX();
            float originY = host == null ? 0 : host.getPositionY();
            pos[0] = Mth.clamp(screenX - originX, 0, HubUI.CONTENT_W - width);
            pos[1] = Mth.clamp(screenY - originY, 0, HubUI.CONTENT_H - height);
            root.layout(l -> l.left(pos[0]).top(pos[1]));
            open();
        }

        /** Moves it, in the tab root's own coordinates, clamped like everything else here. */
        public void place(float x, float y) {
            pos[0] = Mth.clamp(x, 0, HubUI.CONTENT_W - width);
            pos[1] = Mth.clamp(y, 0, HubUI.CONTENT_H - height);
            root.layout(l -> l.left(pos[0]).top(pos[1]));
        }

        /** Where it is now, in the tab root's coordinates. */
        public float x() {
            return pos[0];
        }

        public void toggle() {
            if (isOpen()) close(); else open();
        }

        /** Closes this one; leaves any other window alone. */
        public void close() {
            if (openPanel == root) {
                closePanel();
            } else {
                root.setDisplay(false);
            }
        }
    }

    private static Floating floating(Component title, int width, int height, Consumer<UIElement> body) {
        float[] pos = {(HubUI.CONTENT_W - width) / 2f, (HubUI.CONTENT_H - height) / 2f};

        UIElement panel = new UIElement();
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pos[0]).top(pos[1]).width(width).height(height)
                .flexDirection(FlexDirection.COLUMN).paddingAll(PANEL_PADDING));
        panel.style(s -> s.background(new GuiTextureGroup(
                new ColorRectTexture(PANEL_FILL), new ColorBorderTexture(1, PANEL_BORDER)))
                .zIndex(PANEL_Z));
        panel.setDisplay(false);
        // Swallows clicks landing on the panel itself, so a press meant for it cannot fall through
        // to the tab behind.
        panel.addEventListener(UIEvents.CLICK, e -> {});

        Label caption = new Label();
        caption.setText(title);
        caption.layout(l -> l.widthPercent(100).height(HEADER_H).marginBottom(HEADER_GAP));
        caption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        caption.style(s -> s.background(new ColorRectTexture(TITLE_FILL)));
        drag(panel, caption, pos, width, height);

        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .flexBasis(0).flexGrow(1).gapRow(2));
        body.accept(content);

        Button close = new Button();
        close.setText(Component.translatable("gui." + RollMod.MODID + ".hub.info.close"));
        close.layout(l -> l.width(CLOSE_W).height(CLOSE_H).marginTop(HEADER_GAP).marginLeftAuto());
        close.setOnClick(e -> closePanel());

        panel.addChildren(caption, content, close);
        return new Floating(panel, pos, width, height);
    }

    /* -------------------------------------------- the panel ------------------------------------------- */

    /** The fallback panel, wrapped only so {@link #info} can tell it apart from the window path. */
    private record Panel(UIElement root) {}

    private static Panel overlay(String id) {
        // Where the window sits, in tab coordinates. Kept in an array because the drag listeners
        // below close over it. Centred to start, and centred again next time the hub is opened: the
        // tree is rebuilt per HubUI.createUI, so a window shoved into a corner during one session is
        // not still hiding there in the next.
        float[] pos = {(HubUI.CONTENT_W - PANEL_W) / 2f, (HubUI.CONTENT_H - PANEL_H) / 2f};

        UIElement panel = new UIElement();
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pos[0]).top(pos[1]).width(PANEL_W).height(PANEL_H)
                .flexDirection(FlexDirection.COLUMN).paddingAll(PANEL_PADDING));
        panel.style(s -> s.background(new GuiTextureGroup(
                new ColorRectTexture(PANEL_FILL), new ColorBorderTexture(1, PANEL_BORDER)))
                .zIndex(PANEL_Z));
        panel.setDisplay(false);
        // Swallows clicks landing on the window itself, so a press meant for it cannot fall through
        // to the list behind. Clicks outside it now reach the tab, which is the point of the window
        // being a window: the section stays usable while its explanation is open.
        panel.addEventListener(UIEvents.CLICK, e -> {});

        Label title = new Label();
        title.setText(title(id));
        title.layout(l -> l.widthPercent(100).height(HEADER_H).marginBottom(HEADER_GAP));
        title.textStyle(t -> t.font(HubInfo.FONT)
                .textAlignVertical(Vertical.CENTER).textShadow(true));
        // Tinted because it is the grab handle, and a handle nobody can see is a handle nobody uses.
        title.style(s -> s.background(new ColorRectTexture(TITLE_FILL)));
        drag(panel, title, pos, PANEL_W, PANEL_H);

        ScrollerView body = new ScrollerView();
        // flexBasis(0), for the reason HubInfoWindow gives: flex-shrink defaults to 0 and flex-basis
        // to auto here, so a grow on its own leaves the scroller as tall as the text it holds and
        // nothing ever overflows its viewport.
        body.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        body.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        // COLUMN explicitly: getContainerHeight() measures the lowest child edge, and in a row every
        // line sits at y=0, so the measured height never exceeds the viewport and the scroller
        // concludes there is nothing to scroll. AuctionUI and VendorTradeUI set this; this did not.
        body.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        // The constructor pads the viewport, which would inset the prose a second time inside the
        // panel's own padding. Same override DailyTasksUI makes.
        body.viewPort(v -> v.layout(l -> l.paddingAll(0)));

        Button close = new Button();
        close.setText(Component.translatable("gui." + RollMod.MODID + ".hub.info.close"));
        close.layout(l -> l.width(CLOSE_W).height(CLOSE_H).marginTop(HEADER_GAP).marginLeftAuto());
        close.setOnClick(e -> closePanel());

        panel.addChildren(title, body, close);

        // Filled now, not on first open. ScrollerView sizes its bars from LAYOUT_CHANGED on its
        // viewport, so lines added after the tree has already been laid out leave it still believing
        // there is nothing to scroll — the content has to be there for the first layout pass. On a
        // dedicated server HubInfo reads nothing and this adds no children at all.
        for (UIElement line : HubInfo.read(id)) {
            body.addScrollViewChild(line);
        }

        return new Panel(panel);
    }

    /**
     * Makes {@code handle} drag {@code panel} around inside the tab.
     *
     * <p>Uses the library's own drag mechanism rather than a hand-rolled cursor poll, and the choice
     * matters: {@code MOUSE_MOVE} is dispatched to whatever element is under the cursor
     * ({@code ModularUI.mouseMoved}), so a drag quick enough to outrun the pointer off the title bar
     * would simply stop being told about it and the window would stick. {@code DRAG_SOURCE_UPDATE}
     * is the one event delivered to the element that <em>started</em> the drag, whatever the cursor
     * is over by then. It is how {@code Scroller} drags its own scroll bar.
     *
     * <p>The payload is the window's corner at grab time and the offset is measured from the
     * mouse-down point, rather than accumulating each frame's delta, so a dropped frame cannot make
     * the window drift away from the cursor.
     *
     * <p>Safe on a dedicated server, where this whole tree is also built: nothing here touches a
     * client-only type, and mouse events never fire there — so unlike {@link HubInfo} and
     * {@code PlayerPreviewElement} this needs no client-only holder class.
     */
    private static void drag(UIElement panel, UIElement handle, float[] pos, int width, int height) {
        handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            handle.startDrag(new float[] {pos[0], pos[1]}, null);
            // As Scroller does: the grab must not also read as a press on the panel behind it.
            e.stopPropagation();
        });

        handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof float[] origin)) return;
            // Clamped to the tab box, so the window can never be shoved somewhere it cannot be
            // grabbed back from.
            pos[0] = Mth.clamp(origin[0] + (e.x - e.dragStartX), 0, HubUI.CONTENT_W - width);
            pos[1] = Mth.clamp(origin[1] + (e.y - e.dragStartY), 0, HubUI.CONTENT_H - height);
            panel.layout(l -> l.left(pos[0]).top(pos[1]));
        });
    }

    /** Isolated holder for the client-only window, as {@link HubInfo} does for its resource read. */
    private static final class ClientWindow {
        static boolean open(String id) {
            return HubInfoWindow.open(id, title(id));
        }

        /**
         * Closing, hopped onto the render thread if it is not already there.
         *
         * <p>{@code Dist.CLIENT} is not the same as "the client thread": in single player the
         * integrated server builds and ticks its own copy of the hub tree in the same JVM, so
         * {@code select} and the tree's {@code REMOVED} both reach this from the server thread.
         * Destroying a GLFW window from there is a crash waiting to happen, and hiding a live
         * element races the frame being drawn.
         */
        static void closeInfo() {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) return;
            if (mc.isSameThread()) {
                closeNow();
            } else {
                mc.execute(ClientWindow::closeNow);
            }
        }

        private static void closeNow() {
            closePanel();
            HubInfoWindow.close();
        }
    }

    /** {@code gui.roll_mod.hub.tab.<id>} — the caption the bookmark already uses. */
    private static Component title(String id) {
        return Component.translatable("gui." + RollMod.MODID + ".hub.tab." + id);
    }
}
