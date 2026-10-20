package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.network.packet.TeleportActionPacket;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The home tab's right-hand column: the kits, or the quick teleports (random, back, spawn) — or
 * nothing, folded away to a narrow strip with one button to bring it back.
 *
 * <p>Both views are built, always, and the one not shown is collapsed to no height: {@link
 * KitsPanel} rebuilds in its own tick, and a hidden element stops ticking. Folding works the same
 * way, so the kits stay current while the column is put away.
 *
 * <p>Holds no sync value, so it can be appended after the home tab's positional bindings without
 * moving them. What the player picked is remembered with the hub's layout ({@link HubWindowState}).
 */
public final class HomeSidePanel {

    private enum Mode { KITS, TELEPORT, HIDDEN }

    private static final String MODE_KEY = "home.side";
    private static final int W = KitsPanel.WIDTH;
    /** The folded strip: just wide enough for its one button. */
    private static final int FOLDED_W = 14;
    private static final int SWITCH_H = 14;
    private static final int GAP = 4;
    private static final int TRIP_H = 16;
    private static final int COLOR_PANEL = 0x40000000;

    private HomeSidePanel() {}

    public static UIElement build(Player player) {
        boolean clientCopy = player.level().isClientSide();

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(W).heightPercent(100).flexShrink(0)
                .gapRow(2));

        Button kits = switchButton("gui.roll_mod.hub.home.side.kits");
        Button teleport = switchButton("gui.roll_mod.hub.home.side.teleport");
        Button hide = new Button();
        hide.setText(Component.literal("»"));
        hide.layout(l -> l.width(FOLDED_W).height(SWITCH_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(hide);
        hint(hide, "gui.roll_mod.hub.home.side.hide");

        UIElement switches = new UIElement();
        switches.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(SWITCH_H).flexShrink(0)
                .gapColumn(2));
        switches.setOverflowVisible(false);
        switches.addChildren(kits, teleport, hide);

        Button show = new Button();
        show.setText(Component.literal("«"));
        show.layout(l -> l.width(FOLDED_W).height(SWITCH_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(show);
        hint(show, "gui.roll_mod.hub.home.side.show");

        UIElement kitsPane = slot(KitsPanel.build());
        UIElement teleportPane = slot(teleports());
        UIElement stack = new UIElement();
        stack.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1));
        stack.setOverflowVisible(false);
        stack.addChildren(kitsPane, teleportPane);

        column.addChildren(switches, show, stack);

        Mode[] mode = {Mode.KITS};
        if (clientCopy) {
            mode[0] = Mode.values()[Math.floorMod(HubWindowState.get().split(MODE_KEY, 0), Mode.values().length)];
        }
        Runnable apply = () -> {
            boolean folded = mode[0] == Mode.HIDDEN;
            column.layout(l -> l.width(folded ? FOLDED_W : W));
            switches.layout(l -> l.height(folded ? 0 : SWITCH_H));
            show.setDisplay(folded);
            show(kitsPane, mode[0] == Mode.KITS);
            show(teleportPane, mode[0] == Mode.TELEPORT);
            label(kits, "gui.roll_mod.hub.home.side.kits", mode[0] == Mode.KITS);
            label(teleport, "gui.roll_mod.hub.home.side.teleport", mode[0] == Mode.TELEPORT);
        };
        // The last visible view, so « opens what was there before it was folded.
        Mode[] lastShown = {mode[0] == Mode.HIDDEN ? Mode.KITS : mode[0]};
        java.util.function.Consumer<Mode> pick = next -> {
            if (next != Mode.HIDDEN) lastShown[0] = next;
            mode[0] = next;
            apply.run();
            HubWindowState state = HubWindowState.get();
            state.setSplit(MODE_KEY, next.ordinal());
            state.save();
        };
        kits.setOnClick(e -> pick.accept(Mode.KITS));
        teleport.setOnClick(e -> pick.accept(Mode.TELEPORT));
        hide.setOnClick(e -> pick.accept(Mode.HIDDEN));
        show.setOnClick(e -> pick.accept(lastShown[0]));
        apply.run();
        return column;
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
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        pane.addChild(title);

        for (Trip trip : List.of(
                new Trip("rtp", TeleportActionPacket.Action.RTP),
                new Trip("back", TeleportActionPacket.Action.BACK),
                new Trip("spawn", TeleportActionPacket.Action.SPAWN))) {
            Button button = TpaTab.trip("gui.roll_mod.hub.trip." + trip.id, trip.action,
                    "textures/gui/hub/teleportation/" + trip.id + ".png");
            button.layout(l -> l.widthPercent(100).height(TRIP_H).flexGrow(0).flexBasis(TRIP_H).flexShrink(0));
            Label about = new Label();
            about.setText(Component.translatable("gui.roll_mod.hub.home.side.trip." + trip.id)
                    .withStyle(ChatFormatting.GRAY));
            about.layout(l -> l.widthPercent(100).marginBottom(4));
            about.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            pane.addChildren(button, about);
        }
        return pane;
    }

    private record Trip(String id, TeleportActionPacket.Action action) {}

    /** One view's place in the stack: clipped, because a collapsed slot's child keeps its height. */
    private static UIElement slot(UIElement content) {
        UIElement slot = new UIElement();
        slot.layout(l -> l.widthPercent(100));
        slot.setOverflowVisible(false);
        slot.addChild(content);
        return slot;
    }

    private static void show(UIElement slot, boolean shown) {
        slot.layout(l -> {
            if (shown) l.heightPercent(100);
            else l.height(0);
        });
    }

    private static Button switchButton(String key) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexBasis(0).flexGrow(1).height(SWITCH_H).paddingAll(0));
        return HubSection.centreCaption(button);
    }

    /** The picked view reads as pressed. */
    private static void label(Button button, String key, boolean picked) {
        button.setText(picked
                ? Component.translatable(key).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
