package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.kits.ClientKitCache;
import com.roll_54.roll_mod.minestar.kits.KitView;
import com.roll_54.roll_mod.minestar.op.ClientOperatorCache;
import com.roll_54.roll_mod.minestar.op.OperatorEntry;
import com.roll_54.roll_mod.network.packet.OperatorTogglePacket;
import com.roll_54.roll_mod.network.packet.KitActionPacket;
import com.roll_54.roll_mod.util.Durations;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The right-hand column of the hub's home tab: the player's kits.
 *
 * <p>The argument-less trips — random teleport, back, spawn — used to sit under the list here. They
 * are teleports, so they live with the other teleports now, under the requests panel in
 * {@link TpaTab}.
 *
 * <p>Nothing here is conditional on who is looking. The server sends only the kits a player may
 * claim, so the tree is the same on both sides and the tab's positional bindings stay put; a player
 * with no kits simply gets an empty list.
 *
 * <p>Below them sits the operator switchboard, which only an administrator is ever sent anything
 * for. It is built for everyone regardless: the hub addresses its bindings by position, so the tree
 * has to be the same on every client.
 *
 * <p>Cooldowns are aged client-side from the last sync ({@link ClientKitCache#remainingMillis}), so
 * the numbers move without the server sending a packet a tick.
 */
public final class KitsPanel {

    static final int WIDTH = 150;

    private static final int HEADER_H = 12;
    private static final int ROW_H = 18;
    private static final int ICON = 16;
    private static final int BUTTON_W = 54;
    private static final int BUTTON_H = 14;
    private static final int GAP = 4;
    private static final int SWITCH_W = 28;

    /** How much of the column the switchboard takes once there is one to show. */
    private static final int OPERATORS_H = 76;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;

    private KitsPanel() {}

    public static UIElement build() {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(WIDTH).heightPercent(100)
                .paddingAll(GAP).gapRow(GAP));
        root.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.kits.title"));
        title.layout(l -> l.widthPercent(100).height(HEADER_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));

        ScrollerView list = new ScrollerView();
        // flexBasis(0) as well as flexGrow: LdLib defaults flex-basis to auto and flex-shrink to 0,
        // so without it the scroller takes the height of the whole list and shoves the buttons off.
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        Label empty = new Label();
        empty.setText(Component.translatable("gui.roll_mod.hub.kits.none")
                .withStyle(ChatFormatting.GRAY));
        empty.layout(l -> l.widthPercent(100).height(ROW_H));
        empty.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        UIElement operators = operators();
        root.addChildren(title, list, empty, operators);

        // A signature over what is drawn, recomputed every frame but only acted on when it changes;
        // seconds rather than millis, so a running countdown rebuilds once a second and not 20
        // times. The WarpTab and auction grid do the same.
        String[] signature = {null};
        root.addEventListener(UIEvents.TICK, e -> {
            List<KitView> kits = ClientKitCache.KITS;

            StringBuilder builder = new StringBuilder();
            for (KitView kit : kits) {
                builder.append(kit.name()).append(':')
                        .append(ClientKitCache.remainingMillis(kit) / 1000L).append(';');
            }
            String current = builder.toString();
            if (current.equals(signature[0])) return;
            signature[0] = current;

            empty.setDisplay(kits.isEmpty());
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();
            for (KitView kit : kits) {
                list.addScrollViewChild(row(kit));
            }
        });

        return root;
    }

    /**
     * The operator switchboard, for the handful of people who may use it.
     *
     * <p>Built for everybody, because the hub's tree may not vary with permission — see
     * {@code HubUI}'s class javadoc. Everyone else is sent an empty roster, and an empty roster
     * collapses the section to nothing rather than hiding it: a hidden element stops ticking, and
     * this one has to keep watching for the day its owner is made an administrator.
     */
    private static UIElement operators() {
        UIElement section = new UIElement();
        section.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).height(0)
                .gapRow(2));
        section.setOverflowVisible(false);

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.op.title"));
        title.layout(l -> l.widthPercent(100).height(HEADER_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));

        ScrollerView list = new ScrollerView();
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        section.addChildren(title, list);

        String[] signature = {null};
        section.addEventListener(UIEvents.TICK, e -> {
            List<OperatorEntry> entries = ClientOperatorCache.ENTRIES;

            StringBuilder builder = new StringBuilder();
            for (OperatorEntry entry : entries) {
                builder.append(entry.name()).append(entry.op() ? '+' : '-');
            }
            String current = builder.toString();
            if (current.equals(signature[0])) return;
            signature[0] = current;

            section.layout(l -> l.height(entries.isEmpty() ? 0 : OPERATORS_H));
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();
            for (OperatorEntry entry : entries) {
                list.addScrollViewChild(operatorRow(entry));
            }
        });

        return section;
    }

    /** One player, and the switch that ops them. */
    private static UIElement operatorRow(OperatorEntry entry) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(BUTTON_H)
                .marginBottom(2).gapColumn(3));

        Label name = new Label();
        name.setText(Component.literal(entry.name()));
        name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        Switch toggle = new Switch();
        toggle.setOn(entry.op());
        toggle.layout(l -> l.width(SWITCH_W).height(BUTTON_H));
        toggle.setOnSwitchChanged(on -> PacketDistributor.sendToServer(
                OperatorTogglePacket.of(entry.name(), entry.id(), on)));

        row.addChildren(name, toggle);
        return row;
    }

    /** One kit: its first item as the icon, its name, and the claim button or the wait. */
    private static UIElement row(KitView kit) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(1).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        UIElement icon = new UIElement();
        icon.layout(l -> l.width(ICON).height(ICON));
        if (!kit.icon().isEmpty()) {
            icon.style(s -> s.background(new ItemStackTexture(kit.icon())));
        }

        Label name = new Label();
        name.setText(Component.literal(kit.name()));
        name.layout(l -> l.flexBasis(0).flexGrow(1).height(ICON));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        long remaining = ClientKitCache.remainingMillis(kit);

        Button claim = new Button();
        claim.setText(remaining > 0
                ? Component.literal(Durations.format(remaining)).withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.roll_mod.hub.kits.claim"));
        claim.layout(l -> l.width(BUTTON_W).height(BUTTON_H));
        if (remaining <= 0) {
            claim.setOnClick(e -> PacketDistributor.sendToServer(KitActionPacket.claim(kit.name())));
        }

        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.literal(kit.name()),
                        Component.translatable("gui.roll_mod.hub.kits.items", kit.itemCount())
                                .withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));

        row.addChildren(icon, name, claim);
        return row;
    }
}
