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
import com.roll_54.roll_mod.network.packet.KitWindowPackets;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
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
    private static final int TILE_H = ICON + BUTTON_H + 2 + 4;

    /** How much of the column the switchboard takes once there is one to show. */
    private static final int OPERATORS_H = 76;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;

    private KitsPanel() {}

    public static UIElement build() {
        return build(false);
    }

    /**
     * @param cards the Kits tab's look, as on Eden: one wide card per kit (icon, name, a coloured status, Preview
     *              and Claim on the right) instead of the narrow column's tiles
     */
    public static UIElement build(boolean cards) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(WIDTH).heightPercent(100)
                .paddingAll(GAP).gapRow(GAP));
        root.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.kits.title"));
        title.layout(l -> l.widthPercent(100).height(HEADER_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));

        ScrollerView list = new ScrollerView();

        // Kept off the list's themed frame, so the first line does not run into it.

        list.viewPort(v -> v.layout(l -> l.paddingAll(4)));
        // flexBasis(0) as well as flexGrow: LdLib defaults flex-basis to auto and flex-shrink to 0,
        // so without it the scroller takes the height of the whole list and shoves the buttons off.
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        // Tiles, one to a row.
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100).flexDirection(FlexDirection.COLUMN)
                .gapRow(GAP)));

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
                        .append(ClientKitCache.remainingMillis(kit) / 1000L).append(kit.locked() ? 'L' : '_')
                        .append(kit.cooldownTicks()).append(':').append(kit.description().hashCode()).append(';');
            }
            String current = builder.toString();
            if (current.equals(signature[0])) return;
            signature[0] = current;

            empty.setDisplay(kits.isEmpty());
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();
            for (KitView kit : kits) {
                list.addScrollViewChild(cards ? card(kit) : tile(kit));
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
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));

        ScrollerView list = new ScrollerView();

        // Kept off the list's themed frame, so the first line does not run into it.

        list.viewPort(v -> v.layout(l -> l.paddingAll(4)));
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
    /**
     * One kit as a tile, the full width of the column: its icon and name on the first line, from the
     * left, and the claim button under them — a fixed size, centred, not stretched across the tile.
     * The name is the one a moderator gave it ({@code Kit#title}), falling back to the id; the full
     * name and item count are on hover, since a long name is cut to the tile's width.
     */
    /* -------------------------------------------- cards -------------------------------------------- */

    private static final int CARD_ICON = 20;
    private static final int CARD_H = CARD_ICON + 10;
    private static final int COLOR_READY = 0xFF6BD66B;
    private static final int COLOR_WAIT = 0xFFE8A84B;

    /**
     * A kit as Eden's Kits tab draws it: the icon, the name over its status — "Ready" in green, or "Ready in 2h 5m"
     * counting down in amber — and Preview and Claim on the right; Claim greyed while the kit waits.
     */
    private static UIElement card(KitView kit) {
        long remaining = ClientKitCache.remainingMillis(kit);
        boolean ready = !kit.locked() && remaining == 0;

        UIElement card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexShrink(0)
                .alignItems(AlignItems.CENTER).paddingHorizontal(6).paddingVertical(5).gapColumn(6).marginBottom(3));
        card.style(s -> s.background(new ColorRectTexture(ready ? COLOR_ROW : 0x20FFFFFF)));

        UIElement icon = new UIElement();
        icon.layout(l -> l.width(CARD_ICON).height(CARD_ICON).flexShrink(0));
        if (!kit.icon().isEmpty()) icon.style(s -> s.background(new ItemStackTexture(kit.icon())));

        UIElement text = new UIElement();
        text.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).gapRow(2));
        Label name = new Label();
        name.setText(LegacyText.display(kit.title()));
        name.layout(l -> l.widthPercent(100).height(10));
        name.textStyle(t -> t.textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));
        Label status = new Label();
        status.setText(statusText(kit, remaining));
        status.layout(l -> l.widthPercent(100).height(10));
        status.textStyle(t -> t.textColor(statusColor(kit, remaining)).textWrap(TextWrap.HIDE)
                .textAlignVertical(Vertical.CENTER));
        text.addChildren(name, status);
        // The kit's description, as a moderator wrote it (colour codes allowed).
        if (!kit.description().isBlank()) {
            Label description = new Label();
            description.setText(LegacyText.display(kit.description()).copy().withStyle(ChatFormatting.GRAY));
            description.layout(l -> l.widthPercent(100));
            description.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            text.addChild(description);
        }

        // What the kit gives, laid out as an inventory (KitWindows).
        Button preview = new Button();
        preview.setText(Component.translatable("gui.roll_mod.hub.kits.preview"));
        preview.layout(l -> l.width(BUTTON_W).height(BUTTON_H).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(preview);
        preview.setOnClick(e -> PacketDistributor.sendToServer(new KitWindowPackets.Preview(kit.name())));

        Button claim = new Button();
        claim.setText(Component.translatable("gui.roll_mod.hub.kits.claim"));
        claim.layout(l -> l.width(BUTTON_W).height(BUTTON_H).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(claim);
        claim.setActive(ready);
        if (ready) {
            claim.setOnClick(e -> PacketDistributor.sendToServer(KitActionPacket.claim(kit.name())));
        } else {
            // Live: the wait counts down here too.
            claim.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    statusText(kit, ClientKitCache.remainingMillis(kit))), null, null, ItemStack.EMPTY));
        }

        card.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            if (e.target != card && e.target != text && e.target != icon) return;
            e.hoverTooltips = new HoverTooltips(List.of(LegacyText.display(kit.title()),
                    Component.translatable("gui.roll_mod.hub.kits.items", kit.itemCount()).withStyle(ChatFormatting.GRAY),
                    cooldownLine(kit).copy().withStyle(ChatFormatting.GRAY)), null, null, ItemStack.EMPTY);
        });
        card.addChildren(icon, text, preview, claim);
        return card;
    }

    /** As on Eden: "Ready · once every 1d", "Ready in 2h 5m", "Claimed: a one-time kit", or locked. */
    private static Component statusText(KitView kit, long remaining) {
        if (kit.locked()) return Component.translatable("gui.roll_mod.hub.kits.locked");
        if (remaining < 0) return Component.translatable("gui.roll_mod.hub.kits.claimed_once");
        if (remaining > 0) return Component.translatable("gui.roll_mod.hub.kits.ready_in", Durations.format(remaining));
        if (kit.cooldownTicks() < 0) return Component.translatable("gui.roll_mod.hub.kits.ready_once");
        if (kit.cooldownTicks() > 0) {
            return Component.translatable("gui.roll_mod.hub.kits.ready_every", Durations.format(kit.cooldownTicks() * 50L));
        }
        return Component.translatable("gui.roll_mod.hub.kits.ready");
    }

    private static int statusColor(KitView kit, long remaining) {
        if (kit.locked()) return COLOR_LOCKED;
        if (remaining < 0) return COLOR_DIM;
        return remaining > 0 ? COLOR_WAIT : COLOR_READY;
    }

    /** How often the kit can be had: once, every so long, or any time. */
    private static Component cooldownLine(KitView kit) {
        if (kit.cooldownTicks() < 0) return Component.translatable("gui.roll_mod.hub.kits.preview.once");
        if (kit.cooldownTicks() > 0) {
            return Component.translatable("gui.roll_mod.hub.kits.preview.every", Durations.format(kit.cooldownTicks() * 50L));
        }
        return Component.translatable("gui.roll_mod.hub.kits.preview.anytime");
    }

    private static final int COLOR_LOCKED = 0xFFE5484D;
    private static final int COLOR_DIM = 0xFF9A9A9A;

    private static UIElement tile(KitView kit) {
        UIElement tile = new UIElement();
        tile.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).height(TILE_H).flexShrink(0)
                .alignItems(AlignItems.CENTER).justifyContent(AlignContent.CENTER)
                .paddingHorizontal(HubSection.ROW_INSET).paddingVertical(2).gapRow(2));
        tile.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        UIElement head = new UIElement();
        // The icon where the name starts, both on the left; only the button below is centred.
        head.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ICON).gapColumn(3)
                .alignItems(AlignItems.CENTER));
        if (!kit.icon().isEmpty()) {
            UIElement icon = new UIElement();
            icon.layout(l -> l.width(ICON).height(ICON).flexShrink(0));
            icon.style(s -> s.background(new ItemStackTexture(kit.icon())));
            head.addChild(icon);
        }
        Label name = new Label();
        name.setText(LegacyText.display(kit.title()));
        name.layout(l -> l.flexBasis(0).flexGrow(1).height(ICON));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        head.addChild(name);

        long remaining = ClientKitCache.remainingMillis(kit);
        Button claim = new Button();
        claim.setText(remaining > 0
                ? Component.literal(Durations.format(remaining)).withStyle(ChatFormatting.GRAY)
                : Component.translatable("gui.roll_mod.hub.kits.claim"));
        claim.layout(l -> l.width(BUTTON_W).height(BUTTON_H).flexGrow(0).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(claim);
        if (remaining <= 0) {
            claim.setOnClick(e -> PacketDistributor.sendToServer(KitActionPacket.claim(kit.name())));
        }

        // What the kit gives, laid out as an inventory (KitWindows) — before claiming, or while waiting.
        Button contents = new Button();
        contents.setText(Component.translatable("gui.roll_mod.hub.kits.contents"));
        contents.layout(l -> l.width(BUTTON_W).height(BUTTON_H).flexGrow(0).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(contents);
        contents.setOnClick(e -> PacketDistributor.sendToServer(new KitWindowPackets.Preview(kit.name())));

        UIElement buttons = new UIElement();
        buttons.layout(l -> l.flexDirection(FlexDirection.ROW).height(BUTTON_H).gapColumn(GAP)
                .justifyContent(AlignContent.CENTER));
        buttons.addChildren(claim, contents);

        tile.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(LegacyText.display(kit.title()),
                        Component.translatable("gui.roll_mod.hub.kits.items", kit.itemCount())
                                .withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));

        tile.addChildren(head, buttons);
        return tile;
    }
}
