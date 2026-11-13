package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.roll_54.roll_mod.network.packet.TeleportActionPacket;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * The home tab's right-hand column: the quick teleports (random, back, spawn), with a tall bar on its left edge
 * ({@link HubFoldBar}) that folds the column away and brings it back. The kits that used to share this column
 * have a hub tab of their own now ({@link KitsTab}).
 *
 * <p>Holds no sync value, so it can be appended after the home tab's positional bindings without
 * moving them. Whether it is folded is remembered with the hub's layout ({@link HubWindowState}); the key is
 * the old Kits | Teleport | hidden switch's, read as folded only for "hidden".
 */
public final class HomeSidePanel {

    private static final String MODE_KEY = "home.side";
    /** The old switch's "hidden" value. */
    private static final int HIDDEN = 2;
    private static final int OPEN = 1;
    private static final int W = KitsPanel.WIDTH;
    private static final int GAP = 4;
    private static final int TRIP_H = 16;
    private static final int COLOR_PANEL = 0x40000000;

    private HomeSidePanel() {}

    public static UIElement build(Player player) {
        boolean clientCopy = player.level().isClientSide();

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.ROW).width(W + HubFoldBar.WIDTH + 2).heightPercent(100)
                .flexShrink(0).gapColumn(2));

        UIElement pane = teleports();
        UIElement slot = new UIElement();
        slot.layout(l -> l.width(W).heightPercent(100));
        slot.setOverflowVisible(false);
        slot.addChild(pane);

        boolean[] folded = {clientCopy && HubWindowState.get().split(MODE_KEY, OPEN) == HIDDEN};
        Runnable apply = () -> {
            column.layout(l -> l.width(folded[0] ? HubFoldBar.WIDTH : W + HubFoldBar.WIDTH + 2));
            slot.layout(l -> l.width(folded[0] ? 0 : W));
        };
        UIElement bar = HubFoldBar.create(() -> folded[0], false, () -> {
            folded[0] = !folded[0];
            apply.run();
            if (!clientCopy) return;
            HubWindowState state = HubWindowState.get();
            state.setSplit(MODE_KEY, folded[0] ? HIDDEN : OPEN);
            state.save();
        }, "gui.roll_mod.hub.home.side.hide", "gui.roll_mod.hub.home.side.show");

        column.addChildren(bar, slot);
        apply.run();
        return column;
    }

    /** The teleports alone, {@code width} wide and the full height: the home tab's left column. */
    public static UIElement column(int width) {
        UIElement pane = teleports();
        pane.layout(l -> l.width(width).flexShrink(0));
        return pane;
    }

    /** Random, back and spawn, one under another, each with what it does. */
    private static UIElement teleports() {
        UIElement pane = new UIElement();
        pane.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(W).heightPercent(100).paddingAll(GAP)
                .gapRow(2));
        pane.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.trip.title"));
        title.layout(l -> l.widthPercent(100).height(12));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));
        pane.addChild(title);

        for (Trip trip : List.of(
                new Trip("rtp", TeleportActionPacket.Action.RTP),
                new Trip("spawn", TeleportActionPacket.Action.SPAWN),
                // Always last, and only for those who may use /back (Prime and up, operators).
                new Trip("back", TeleportActionPacket.Action.BACK))) {
            Button button = TpaTab.trip("gui.roll_mod.hub.trip." + trip.id, trip.action,
                    "textures/gui/hub/teleportation/" + trip.id + ".png");
            button.layout(l -> l.widthPercent(100).height(TRIP_H).flexGrow(0).flexBasis(TRIP_H).flexShrink(0));
            Label about = new Label();
            about.setText(Component.translatable("gui.roll_mod.hub.home.side.trip." + trip.id)
                    .withStyle(ChatFormatting.GRAY));
            about.layout(l -> l.widthPercent(100).marginBottom(4));
            about.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            pane.addChildren(button, about);
            if (trip.action == TeleportActionPacket.Action.BACK) {
                // Hidden outright for everyone else; the status packet says who may (ClientPlayerStatusCache.MAY_BACK).
                Runnable apply = () -> {
                    boolean may = com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache.MAY_BACK;
                    if (button.isDisplayed() != may) button.setDisplay(may);
                    if (about.isDisplayed() != may) about.setDisplay(may);
                };
                apply.run();
                pane.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, e -> apply.run());
            }
        }
        return pane;
    }

    private record Trip(String id, TeleportActionPacket.Action action) {}
}
