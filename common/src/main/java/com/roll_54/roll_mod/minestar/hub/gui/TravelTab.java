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

    public static UIElement build(Player player) {
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

        Button homes = toggle("gui.roll_mod.hub.travel.homes");
        Button warps = toggle("gui.roll_mod.hub.travel.warps");
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
                toggles.get(i).setText(Component.translatable(i == 0 ? "gui.roll_mod.hub.travel.homes"
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

        root.addChildren(HubSection.header("travel", section, homes, warps), stack, section.overlay());
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

    private static Button toggle(String key) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.width(SWITCH_W).height(SWITCH_H).marginRight(2).paddingAll(0));
        return HubSection.centreCaption(button);
    }
}
