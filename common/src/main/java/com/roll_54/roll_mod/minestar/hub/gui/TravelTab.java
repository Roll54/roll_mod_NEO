package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The hub's travel tab: the player's homes and the server's warps — every place there is to go —
 * behind one Homes | Warps switch in the title row.
 *
 * <p>Each view is the old tab's own body ({@link PlayerHomesTab#content}, {@link WarpTab#content}),
 * so nothing about either changed but where it is. Both are built, always, and the one not shown is
 * collapsed to no height rather than hidden: a hidden element stops ticking, and the views rebuild
 * from their client caches in their tick — the same reason {@code HubUI.select} collapses tabs.
 *
 * <p>The view picked last is remembered with the hub's other layout state ({@link HubWindowState}).
 */
public final class TravelTab {

    private static final String VIEW_KEY = "travel.view";

    /**
     * One list width for both views, and one divider remembering it, so flipping Homes | Warps does
     * not make the list jump wider or narrower.
     */
    static final int LIST_W = 145;
    static final String SPLIT_KEY = "travel";
    private static final int SWITCH_W = 54;
    private static final int SWITCH_H = 14;

    private TravelTab() {}

    /** Which side has the centre panel: 0 nothing picked yet, 1 a warp, 2 a home. Shared by the two lists. */
    public static final class Pick {
        public int active;
    }

    /** A list column and its detail panel, handed over by {@link WarpTab#parts} and {@link PlayerHomesTab#parts}. */
    public record Parts(UIElement list, UIElement detail) {}

    /**
     * Warps on the left, homes on the right and, in the middle, whichever the player picked last — its description,
     * where it is and its buttons — the way the Friends tab lays out its people. No Homes | Warps switch.
     */
    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));
        HubSection.Info section = HubSection.info("travel");

        Pick pick = new Pick();
        Parts[] warps = new Parts[1];
        Parts[] homes = new Parts[1];
        UIElement warpOverlay = WarpTab.parts(player, pick, 1, warps);
        UIElement homeOverlay = PlayerHomesTab.parts(player, pick, 2, homes);

        com.lowdragmc.lowdraglib2.gui.ui.elements.Label none = new com.lowdragmc.lowdraglib2.gui.ui.elements.Label();
        none.setText(Component.translatable("gui.roll_mod.hub.travel.none").withStyle(ChatFormatting.GRAY));
        none.textStyle(t -> t.textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER));
        UIElement noneSlot = slot(none);
        // Wraps onto the next line instead of running off the panel.
        none.layout(l -> l.widthPercent(100).marginTop(6).paddingHorizontal(4));
        none.textStyle(t -> t.textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.WRAP).adaptiveHeight(true));
        // Each panel scrolls: a warp's details (status, visits, price, description, buttons) outgrow the column.
        UIElement warpSlot = slot(scrolling(warps[0].detail()));
        UIElement homeSlot = slot(scrolling(homes[0].detail()));

        UIElement centre = new UIElement();
        centre.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).heightPercent(100));
        centre.addChildren(noneSlot, warpSlot, homeSlot);
        List<UIElement> slots = List.of(noneSlot, warpSlot, homeSlot);
        int[] shown = {-1};
        centre.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, e -> {
            if (shown[0] == pick.active) return;
            shown[0] = pick.active;
            for (int i = 0; i < slots.size(); i++) {
                boolean on = i == pick.active;
                slots.get(i).layout(l -> {
                    if (on) l.heightPercent(100);
                    else l.height(0);
                });
            }
        });

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1).gapColumn(4));
        boolean client = player.level().isClientSide();
        // Homes on the left, warps on the right; each list folds away to a thin bar, as the Friends tab's do.
        body.addChildren(foldable(homes[0].list(), true, "travel.fold.homes", client), centre,
                foldable(warps[0].list(), false, "travel.fold.warps", client));

        // The I 3px further right, as on the Friends tab: in line with the fold bar's outer edge.
        UIElement header = HubSection.header("travel", section);
        section.nudge(3);
        root.addChildren(header, body, warpOverlay, homeOverlay, section.overlay());
        return root;
    }

    /** {@code detail} in a vertical scroller filling the centre, the panel as tall as its content. */
    private static UIElement scrolling(UIElement detail) {
        com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView scroll = new com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView();
        scroll.viewPort(v -> v.layout(l -> l.paddingAll(0)));
        scroll.layout(l -> l.widthPercent(100).heightPercent(100));
        scroll.scrollerStyle(st -> st.mode(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode.VERTICAL)
                .verticalScrollDisplay(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay.AUTO)
                .horizontalScrollDisplay(com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay.NEVER));
        scroll.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        // 4px under the last button, so scrolled to the end it shows whole (AE2 and Ore draw a pixel below it too).
        detail.layout(l -> l.widthPercent(100).heightAuto().minHeight(0).paddingBottom(4));
        scroll.addScrollViewChild(detail);
        return scroll;
    }

    /** {@code list} with a fold bar on its inner side; folded, only the bar stays. Remembered per list. */
    private static UIElement foldable(UIElement list, boolean panelLeft, String key, boolean client) {
        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.ROW).heightPercent(100).flexShrink(0).gapColumn(2));
        UIElement slot = new UIElement();
        slot.layout(l -> l.heightPercent(100));
        slot.setOverflowVisible(false);
        list.layout(l -> l.width(LIST_W).flexShrink(0));
        // The list column clips by default too; its Create button sits on its top edge, and on AE2 and Ore that
        // button's face is drawn a pixel above its box.
        list.setOverflowVisible(true);
        column.setOverflowVisible(true);
        slot.addChild(list);
        boolean[] folded = {client && HubWindowState.get().split(key, 0) == 1};
        Runnable apply = () -> {
            column.layout(l -> l.width(folded[0] ? HubFoldBar.WIDTH : LIST_W + HubFoldBar.WIDTH + 2));
            slot.layout(l -> l.width(folded[0] ? 0 : LIST_W));
            // Clipped only while folded: open, the Create button's bevelled top edge (drawn a pixel above its box on
            // AE2 and Ore) must not be cut off.
            slot.setOverflowVisible(!folded[0]);
        };
        UIElement bar = HubFoldBar.create(() -> folded[0], panelLeft, () -> {
            folded[0] = !folded[0];
            apply.run();
            if (!client) return;
            HubWindowState state = HubWindowState.get();
            state.setSplit(key, folded[0] ? 1 : 0);
            state.save();
        }, "gui.roll_mod.hub.home.side.hide", "gui.roll_mod.hub.home.side.show");
        if (panelLeft) column.addChildren(slot, bar);
        else column.addChildren(bar, slot);
        apply.run();
        return column;
    }

    /**
     * The Homes | Warps switch layout this tab had before the three columns.
     * @deprecated Replaced by {@link #build}: warps and homes side by side.
     */
    @Deprecated
    public static UIElement buildSwitched(Player player) {
        boolean clientCopy = player.level().isClientSide();

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));

        HubSection.Info section = HubSection.info("travel");

        List<UIElement> panes = List.of(slot(PlayerHomesTab.content(player)), slot(WarpTab.content(player)));
        UIElement stack = new UIElement();
        // flexBasis(0) for the reason every tab gives: the panes' scrollers must overflow inside the
        // space under the title, not size the stack to their whole content.
        stack.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1));
        stack.addChildren(panes.toArray(UIElement[]::new));

        Button homes = toggle("gui.roll_mod.hub.travel.homes", clientCopy);
        Button warps = toggle("gui.roll_mod.hub.travel.warps", clientCopy);
        List<Button> toggles = List.of(homes, warps);

        int[] view = {clientCopy ? Math.floorMod(HubWindowState.get().split(VIEW_KEY, 0), panes.size()) : 0};
        Runnable apply = () -> {
            for (int i = 0; i < panes.size(); i++) {
                boolean shown = i == view[0];
                panes.get(i).layout(l -> {
                    if (shown) l.heightPercent(100);
                    else l.height(0);
                });
                // The chosen view reads as pressed: bold and yellow, the other plain.
                setCaption(toggles.get(i), Component.translatable(i == 0 ? "gui.roll_mod.hub.travel.homes"
                                : "gui.roll_mod.hub.travel.warps")
                        .withStyle(shown ? new ChatFormatting[] {ChatFormatting.YELLOW, ChatFormatting.BOLD}
                                : new ChatFormatting[] {ChatFormatting.GRAY}));
            }
        };
        for (int i = 0; i < toggles.size(); i++) {
            int index = i;
            toggles.get(i).setOnClick(e -> {
                view[0] = index;
                apply.run();
                // Client state, client file: the server's copy is never clicked.
                HubWindowState state = HubWindowState.get();
                state.setSplit(VIEW_KEY, index);
                state.save();
            });
        }
        apply.run();

        // The title names the view that is open: Homes or Warps.
        root.addChildren(HubSection.header(() -> Component.translatable(view[0] == 0
                        ? "gui.roll_mod.hub.travel.homes" : "gui.roll_mod.hub.travel.warps"), section, homes, warps),
                stack, section.overlay());
        return root;
    }

    /** One view's slot: clipped, because a collapsed slot's child keeps its own height. */
    private static UIElement slot(UIElement content) {
        UIElement slot = new UIElement();
        slot.layout(l -> l.widthPercent(100));
        slot.setOverflowVisible(false);
        slot.addChild(content);
        return slot;
    }

    private static Button toggle(String key, boolean clientCopy) {
        Button button = new Button();
        button.setText(Component.empty());
        button.layout(l -> l.width(SWITCH_W).height(SWITCH_H).marginRight(2).paddingAll(0));
        // The caption is a label of its own laid over the whole button, centred both ways: the button's own text
        // takes the theme sheets' padding and alignment, which pushed it down (cut at the bottom) or to the left.
        // Ore's thick top bevel takes it 1px lower.
        int nudge = clientCopy && com.roll_54.roll_mod.minestar.theme.UiThemeState.get().oreArt() ? 1 : 0;
        com.lowdragmc.lowdraglib2.gui.ui.elements.Label caption = new com.lowdragmc.lowdraglib2.gui.ui.elements.Label();
        caption.setText(Component.translatable(key));
        caption.layout(l -> l.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE)
                .left(0).top(nudge).width(SWITCH_W).height(SWITCH_H));
        caption.textStyle(t -> t.textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER)
                .textAlignVertical(com.lowdragmc.lowdraglib2.gui.ui.data.Vertical.CENTER));
        caption.setAllowHitTest(false);
        button.addChild(caption);
        return button;
    }

    /** The caption {@link #toggle} laid over {@code button}. */
    private static void setCaption(Button button, Component text) {
        var children = button.getChildren();
        if (!children.isEmpty() && children.get(children.size() - 1) instanceof com.lowdragmc.lowdraglib2.gui.ui.elements.Label label) {
            label.setText(text);
        }
    }
}
