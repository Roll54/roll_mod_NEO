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
import com.roll_54.roll_mod.minestar.theme.UiTheme;
import com.roll_54.roll_mod.minestar.theme.UiThemeState;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
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
    /**
     * The inset every tab's root takes inside the hub's content box. Shared so the title row — and
     * the I button at its end — sits in the same place on every tab.
     */
    public static final int TAB_PADDING = 6;

    /**
     * The left and right inset inside a tinted list row, so its first and last words — a rank, a
     * name, a value — do not sit on the row's edge. Every hub list row takes it.
     */
    public static final int ROW_INSET = 5;

    private static final int HEADER_H = 12;
    private static final int HEADER_GAP = 4;

    /** The I button, and the close button on the panel. */
    private static final int BUTTON = 12;

    /** Breathing room between a tab's own header button and the I button it sits beside. */
    private static final int BUTTON_GAP = 4;
    private static final int CLOSE_W = 44;
    private static final int CLOSE_H = 14;

    /**
     * The frame is the drag handle, so the padding is where a press grabs the window — wide enough
     * to hit without aiming.
     */
    private static final int PANEL_PADDING = 8;
    /** The frame's drawn border: the visible part of the handle. */
    private static final int FRAME = 2;

    /**
     * The window's size, inside the tab box — {@link HubUI#DEFAULT_CONTENT_W}x
     * {@link HubUI#DEFAULT_CONTENT_H} at the default hub size, and whatever the hub window has been
     * resized to otherwise.
     *
     * <p>Deliberately smaller than the tab it floats over. Covering everything meant the section
     * underneath was unusable while the explanation of it was open, which is backwards — the two are
     * read together. A narrow column is also easier to read prose in than a 460px line.
     */
    private static final int PANEL_W = 280;
    private static final int PANEL_H = 190;

    /** The mark before a popup's caption, when it has one: {@code exclamation_mark.png} is 7x7. */
    private static final int TITLE_ICON = 7;

    /** A hairline under the caption — no longer a handle, just a heading. */
    private static final int TITLE_LINE = 0x40FFFFFF;

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

    /**
     * Centres a button's caption both ways. A short button with no padding otherwise draws its text
     * at the top of the box rather than in its middle.
     */
    public static Button centreCaption(Button button) {
        button.text.layout(l -> l.widthPercent(100).heightPercent(100));
        button.text.textStyle(t -> t.textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));
        return button;
    }

    /** Hides whichever in-hub floating panel is up — for a desktop window taking over. */
    static void closeInHubPanel() {
        closePanel();
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
                panel.show();
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
        return header(title(id), info, extras);
    }

    /**
     * The same title row with a caption of the tab's own, for a section whose heading reads
     * differently from its bookmark's hover name. Everything else — height, gap, the I button's
     * place — is the shared row, which is the point.
     */
    public static UIElement header(Component caption, Info info, UIElement... extras) {
        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(HEADER_H)
                .marginBottom(HEADER_GAP));

        Label title = new Label();
        title.setText(caption);
        title.layout(l -> l.flexGrow(1).height(HEADER_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));

        header.addChild(title);
        // Unclipped: a control taller than the 12px row — a themed button wants 16 — overhangs it
        // evenly instead of being cut off at the top. The row's own height, and with it every tab's
        // alignment, stays the same. This also lets clicks reach the overhang.
        header.setOverflowVisible(true);
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
        return panel(title, icon, tip, width, height, false, body);
    }

    /** As above; {@code windowed} opens it as a desktop window of its own, see {@link Floating#windowed}. */
    public static Info panel(Component title, IGuiTexture icon, Component tip,
                             int width, int height, boolean windowed, Consumer<UIElement> body) {
        Floating floating = floating(title, null, width, height, body);
        if (windowed) floating.windowed();

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
        return floating(title, null, width, height, body);
    }

    /** As {@link #popup}, with {@code icon} before the caption — the warning mark of {@link HubConfirm}. */
    public static Floating popup(Component title, IGuiTexture icon, int width, int height, Consumer<UIElement> body) {
        return floating(title, icon, width, height, body);
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
        final Component title;
        /** The mark before the caption, if any — carried into the desktop window's title bar too. */
        @Nullable final IGuiTexture icon;
        /** What the caller filled; moved into the desktop window while it is up, then back. */
        final UIElement content;
        private final Button closeButton;
        /** Whether it has been positioned yet. Until then it is centred on first open. */
        private boolean placed;
        /** Opens as a desktop window of its own where the platform allows one — see {@link #windowed}. */
        private boolean windowed;
        /** Whether the window offers a close of its own; off when the body has a Cancel. */
        boolean closable = true;

        private Floating(UIElement root, float[] pos, int width, int height, Component title,
                         @Nullable IGuiTexture icon, UIElement content, Button closeButton) {
            this.icon = icon;
            this.root = root;
            this.pos = pos;
            this.width = width;
            this.height = height;
            this.title = title;
            this.content = content;
            this.closeButton = closeButton;
        }

        /**
         * Opens it the way the info panel opens: an operating-system window of its own, which can be
         * moved anywhere on the desktop and resized from its edges, with the in-hub panel as the
         * fallback on a platform that gives no second window. Client-only behaviour; the server's
         * copy of the tree never opens anything.
         */
        public Floating windowed() {
            windowed = true;
            return this;
        }

        /**
         * No Close button, in the panel or on the window: for a body with a Cancel of its own,
         * which does the same thing.
         */
        public Floating noCloseButton() {
            closable = false;
            closeButton.setDisplay(false);
            return this;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        /** Puts {@link #content} back in the in-hub panel, where it lives while no window has it. */
        void reclaim() {
            if (content.getParent() == root) return;
            content.removeSelf();
            root.addChildAt(content, 1);
        }

        /** Add this to the tab's root, last. */
        public UIElement root() {
            return root;
        }

        public boolean isOpen() {
            return root.isDisplayed() || (windowed && FMLEnvironment.dist == Dist.CLIENT && ClientPopup.isOpen(this));
        }

        /** Opens wherever it last was — centred, the first time. */
        public void open() {
            if (windowed && FMLEnvironment.dist == Dist.CLIENT && ClientPopup.open(this)) return;
            if (!placed) {
                centre(root, pos, width, height);
                placed = true;
            } else {
                keepInside(root, pos, width, height);
            }
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
            pos[0] = clampTo(screenX - originX, width, boundW(root));
            pos[1] = clampTo(screenY - originY, height, boundH(root));
            root.layout(l -> l.left(pos[0]).top(pos[1]));
            placed = true;
            open();
        }

        /** Moves it, in the tab root's own coordinates, clamped like everything else here. */
        public void place(float x, float y) {
            pos[0] = clampTo(x, width, boundW(root));
            pos[1] = clampTo(y, height, boundH(root));
            root.layout(l -> l.left(pos[0]).top(pos[1]));
            placed = true;
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
            if (windowed && FMLEnvironment.dist == Dist.CLIENT) ClientPopup.close(this);
            if (openPanel == root) {
                closePanel();
            } else {
                root.setDisplay(false);
            }
        }
    }

    private static Floating floating(Component title, @Nullable IGuiTexture icon, int width, int height,
                                     Consumer<UIElement> body) {
        // The default-size centre, which the server's copy keeps; the client re-centres against the
        // live tab size when it first opens. See Floating.open.
        float[] pos = {(HubUI.DEFAULT_CONTENT_W - width) / 2f, (HubUI.DEFAULT_CONTENT_H - height) / 2f};

        UIElement panel = new UIElement();
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pos[0]).top(pos[1]).width(width).height(height)
                .flexDirection(FlexDirection.COLUMN).paddingAll(PANEL_PADDING));
        panel.style(s -> s.background(frame(panel)).zIndex(PANEL_Z));
        panel.setDisplay(false);
        // Swallows clicks landing on the panel itself, so a press meant for it cannot fall through
        // to the tab behind.
        panel.addEventListener(UIEvents.CLICK, e -> {});

        UIElement caption = new UIElement();
        caption.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(HEADER_H)
                .marginBottom(HEADER_GAP).gapColumn(3));
        caption.style(s -> s.background(underline()));
        if (icon != null) {
            UIElement mark = new UIElement();
            mark.layout(l -> l.width(TITLE_ICON).height(TITLE_ICON).flexShrink(0).marginTop((HEADER_H - TITLE_ICON) / 2f));
            mark.style(s -> s.background(icon));
            caption.addChild(mark);
        }
        Label captionText = new Label();
        captionText.setText(title);
        captionText.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        captionText.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        caption.addChild(captionText);
        drag(panel, pos, width, height);
        keepInsideWhileOpen(panel, pos, width, height);

        UIElement content = new UIElement();
        content.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .flexBasis(0).flexGrow(1).gapRow(2));
        body.accept(content);

        Button close = new Button();
        close.setText(Component.translatable("gui." + RollMod.MODID + ".hub.info.close"));
        close.layout(l -> l.width(CLOSE_W).height(CLOSE_H).marginTop(HEADER_GAP).marginLeftAuto());
        close.setOnClick(e -> closePanel());

        panel.addChildren(caption, content, close);
        Floating floating = new Floating(panel, pos, width, height, title, icon, content, close);
        // The panel's Close closes this floating, whichever way it is shown.
        close.setOnClick(e -> floating.close());
        return floating;
    }

    /* -------------------------------------------- the panel ------------------------------------------- */

    /**
     * The fallback panel, wrapped so {@link #info} can tell it apart from the window path, and can
     * centre it against the live tab size the first time it opens.
     */
    private record Panel(UIElement root, float[] pos, boolean[] placed) {
        void show() {
            if (placed[0]) {
                keepInside(root, pos, PANEL_W, PANEL_H);
            } else {
                centre(root, pos, PANEL_W, PANEL_H);
                placed[0] = true;
            }
        }
    }

    private static Panel overlay(String id) {
        // Where the window sits, in tab coordinates. Kept in an array because the drag listeners
        // below close over it. Centred to start, and centred again next time the hub is opened: the
        // tree is rebuilt per HubUI.createUI, so a window shoved into a corner during one session is
        // not still hiding there in the next.
        float[] pos = {(HubUI.DEFAULT_CONTENT_W - PANEL_W) / 2f, (HubUI.DEFAULT_CONTENT_H - PANEL_H) / 2f};

        UIElement panel = new UIElement();
        panel.layout(l -> l.positionType(YogaPositionType.ABSOLUTE)
                .left(pos[0]).top(pos[1]).width(PANEL_W).height(PANEL_H)
                .flexDirection(FlexDirection.COLUMN).paddingAll(PANEL_PADDING));
        panel.style(s -> s.background(frame(panel)).zIndex(PANEL_Z));
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
        title.style(s -> s.background(underline()));
        drag(panel, pos, PANEL_W, PANEL_H);
        keepInsideWhileOpen(panel, pos, PANEL_W, PANEL_H);

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

        return new Panel(panel, pos, new boolean[1]);
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
    private static void drag(UIElement panel, float[] pos, int width, int height) {
        UIElement handle = panel;
        handle.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            // The frame only: a press on anything inside — a field, a button, the caption — is that
            // child's, and only one landing on the panel's own padding grabs the window.
            if (e.target != panel) return;
            handle.startDrag(new float[] {pos[0], pos[1]}, null);
            // As Scroller does: the grab must not also read as a press on the panel behind it.
            e.stopPropagation();
        });

        handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof float[] origin)) return;
            // Clamped to the tab box, so the window can never be shoved somewhere it cannot be
            // grabbed back from.
            pos[0] = clampTo(origin[0] + (e.x - e.dragStartX), width, boundW(panel));
            pos[1] = clampTo(origin[1] + (e.y - e.dragStartY), height, boundH(panel));
            panel.layout(l -> l.left(pos[0]).top(pos[1]));
        });
    }

    /* ------------------------------------------ theme ------------------------------------------- */

    /**
     * The window's fill and frame, in the theme picked in the hub's settings, re-read every frame so
     * a theme change shows at once. The frame is the drag handle: it lights up in the theme's accent
     * while the cursor is on it. The vanilla-looking themes, which have no palette, keep the grey.
     */
    private static IGuiTexture frame(UIElement panel) {
        return IGuiTexture.dynamic(() -> {
            UiTheme.Palette palette = UiThemeState.get().palette();
            if (palette == null) {
                return new GuiTextureGroup(new ColorRectTexture(PANEL_FILL),
                        new ColorBorderTexture(FRAME, panel.isHover() ? 0xFFB0B0B0 : PANEL_BORDER));
            }
            int fill = (palette.raised() & 0x00FFFFFF) | 0xF0000000;
            int border = panel.isHover() ? palette.accent() | 0xFF000000
                    : (palette.accent() & 0x00FFFFFF) | 0x80000000;
            return new GuiTextureGroup(new ColorRectTexture(fill), new ColorBorderTexture(FRAME, border));
        });
    }

    /** The caption's hairline underneath, in the theme's accent. */
    private static IGuiTexture underline() {
        return IGuiTexture.dynamic(() -> {
            UiTheme.Palette palette = UiThemeState.get().palette();
            int color = palette == null ? TITLE_LINE : (palette.accent() & 0x00FFFFFF) | 0xA0000000;
            return new UnderlineTexture(color);
        });
    }

    /** A 1px line along the bottom edge of whatever it is drawn on. */
    private record UnderlineTexture(int color) implements IGuiTexture {
        @Override
        public void draw(net.minecraft.client.gui.GuiGraphics graphics, float mouseX, float mouseY, float x, float y,
                         float width, float height, float partialTicks) {
            new ColorRectTexture(color).draw(graphics, mouseX, mouseY, x, y + height - 1, width, 1, partialTicks);
        }
    }

    /* ------------------------------------------ bounds ------------------------------------------ */

    /**
     * The tab box a floating panel lives in: its parent's live size, which follows the hub window as
     * it is resized. Falls back to the default box before the first layout, and on the server's copy
     * of the tree, which is never laid out against a screen.
     */
    private static float boundW(UIElement panel) {
        UIElement parent = panel.getParent();
        float w = parent == null ? 0 : parent.getSizeWidth();
        return w > 0 ? w : HubUI.DEFAULT_CONTENT_W;
    }

    private static float boundH(UIElement panel) {
        UIElement parent = panel.getParent();
        float h = parent == null ? 0 : parent.getSizeHeight();
        return h > 0 ? h : HubUI.DEFAULT_CONTENT_H;
    }

    /**
     * {@code v} kept so {@code [v, v + size]} stays inside {@code [0, bound]}. A panel bigger than
     * its box pins to the top-left rather than going negative, so its title bar stays reachable.
     */
    private static float clampTo(float v, float size, float bound) {
        return Math.max(0, Math.min(v, bound - size));
    }

    private static void centre(UIElement panel, float[] pos, int width, int height) {
        pos[0] = clampTo((boundW(panel) - width) / 2f, width, boundW(panel));
        pos[1] = clampTo((boundH(panel) - height) / 2f, height, boundH(panel));
        panel.layout(l -> l.left(pos[0]).top(pos[1]));
    }

    /** Pulls the panel back inside its box, if the box has shrunk under it. */
    private static void keepInside(UIElement panel, float[] pos, int width, int height) {
        float x = clampTo(pos[0], width, boundW(panel));
        float y = clampTo(pos[1], height, boundH(panel));
        if (x == pos[0] && y == pos[1]) return;
        pos[0] = x;
        pos[1] = y;
        panel.layout(l -> l.left(x).top(y));
    }

    /**
     * Keeps an open panel inside its tab while the hub window is resized under it. A hidden element
     * does not tick, so this costs nothing while the panel is closed.
     */
    private static void keepInsideWhileOpen(UIElement panel, float[] pos, int width, int height) {
        panel.addEventListener(UIEvents.TICK, e -> keepInside(panel, pos, width, height));
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
            HubPopupWindow.close();
        }
    }

    /** Isolated holder for the desktop-window route of {@link Floating#windowed}, as above. */
    private static final class ClientPopup {
        static boolean open(Floating floating) {
            return HubPopupWindow.open(floating);
        }

        static boolean isOpen(Floating floating) {
            return HubPopupWindow.isOpenFor(floating);
        }

        static void close(Floating floating) {
            HubPopupWindow.closeIf(floating);
        }
    }

    /** The themed fill and frame of a floating window, for the desktop window to wear too. */
    static IGuiTexture frameFor(UIElement panel) {
        return frame(panel);
    }

    /** {@code gui.roll_mod.hub.tab.<id>} — the caption the bookmark already uses. */
    private static Component title(String id) {
        return Component.translatable("gui." + RollMod.MODID + ".hub.tab." + id);
    }
}
