package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaMode;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import com.roll_54.roll_mod.network.packet.TeleportActionPacket;
import com.roll_54.roll_mod.network.packet.TpaActionPacket;
import com.roll_54.roll_mod.network.packet.TpaModePacket;
import com.roll_54.roll_mod.util.Durations;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The hub's teleport tab: who is online down the left, your requests on the right.
 *
 * <p>Reads {@link ClientTpaCache}, which {@code SyncTpaPacket} keeps current — a variable-length
 * list, so a client cache rather than LdLib2's positional bindings, the same shape as the warp tab.
 *
 * <p>The roster is the server's, not the client's player list: this class is common code and has no
 * business reaching for a client-only type.
 *
 * <p>Under the requests sit the trips that take no argument — random teleport, back, spawn. They
 * used to hang off the kit column on the home tab, which was only ever where there happened to be
 * room for them.
 */
public final class TpaTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 150;
    private static final int ROW_H = 16;
    private static final int ANSWER_W = 52;

    /**
     * The trip buttons under the requests panel. A row's height rather than less, because they size
     * their icons off the button's content box: at 14 the button's own padding would leave an 8px
     * icon beside the caption.
     */
    private static final int TRIP_H = ROW_H;

    /** Stands in for the artwork random teleport and spawn have yet to be given. */
    private static final String TRIP_PLACEHOLDER = "textures/gui/hub/teleportation/tp_icon.png";

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;

    /** Four rows, a caption and a close button — no more than that has to fit. */
    private static final int SETTINGS_W = 220;
    private static final int SETTINGS_H = 130;
    private static final int MODE_ROW_H = 16;

    private TpaTab() {}

    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("tpa");

        // Built unconditionally, like everything else in the hub: the rows are the same on both
        // sides, and which one is marked comes from the server in the tick below.
        Toggle[] modes = new Toggle[TpaMode.values().length];
        HubSection.Info settings = HubSection.panel(
                Component.translatable("gui.roll_mod.hub.tpa.settings.title"),
                SpriteTexture.of(RollMod.id("textures/gui/hub/teleportation/settings.png")),
                Component.translatable("gui.roll_mod.hub.tpa.settings.tip"),
                SETTINGS_W, SETTINGS_H,
                content -> buildModes(content, modes));

        UIElement body = new UIElement();
        // flexBasis(0), not flexGrow alone: LdLib defaults flex-shrink to 0 and flex-basis to auto,
        // so the body's base size would be its own content — the whole roster, however long — and
        // nothing would pull it back to the leftover space under the header. Without the zero basis
        // the scrollers inside never overflow, so the online list cannot scroll.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(PADDING));

        // The filter is per-viewer client state, so it lives in the tree rather than on the wire.
        String[] filter = {""};
        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H));
        search.setTextResponder(text ->
                filter[0] = text == null ? "" : text.strip().toLowerCase(Locale.ROOT));

        ScrollerView online = new ScrollerView();
        online.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        online.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        online.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100)
                .gapRow(PADDING));
        left.addChildren(search, online);

        ScrollerView requests = new ScrollerView();
        requests.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        requests.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        requests.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1)
                .heightPercent(100).paddingAll(PADDING).gapRow(2));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        // The trips sit under the requests, inside the same panel: they are teleports the player
        // takes alone, which is the other half of what this tab is for. They keep a fixed height,
        // so the requests scroller above them still takes everything that is left.
        detail.addChildren(requests, trips());

        body.addChildren(left, detail);
        // Both panels last, and the settings one after the explanation: whichever is open sits over
        // the tab, and only one of them ever is.
        root.addChildren(HubSection.header("tpa", section, settings.button()), body,
                section.overlay(), settings.overlay());

        String[] rosterSignature = {null};
        String[] requestSignature = {null};
        TpaMode[] shownMode = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            // From the tab root, not from inside the panel: a panel that is hidden stops ticking,
            // and the mark has to be right by the time it is opened.
            TpaMode mode = ClientTpaCache.MODE;
            if (mode != shownMode[0]) {
                shownMode[0] = mode;
                for (int i = 0; i < modes.length; i++) {
                    if (modes[i] != null) modes[i].setOn(TpaMode.values()[i] == mode, false);
                }
            }

            Map<UUID, String> roster = ClientTpaCache.ONLINE;
            List<Map.Entry<UUID, String>> shown = matching(roster, filter[0]);

            String roster0 = shown.size() + ":" + shown.stream()
                    .map(Map.Entry::getValue).reduce("", String::concat);
            if (!roster0.equals(rosterSignature[0])) {
                rosterSignature[0] = roster0;
                online.clearAllScrollViewChildren();
                online.clearLayoutCache();
                for (Map.Entry<UUID, String> entry : shown) {
                    online.addScrollViewChild(playerRow(entry.getKey(), entry.getValue()));
                }
            }

            List<TpaRequest> incoming = ClientTpaCache.INCOMING;
            List<TpaRequest> outgoing = ClientTpaCache.OUTGOING;

            // Seconds, not millis: the countdown then rebuilds once a second instead of every frame.
            long now = System.currentTimeMillis();
            StringBuilder builder = new StringBuilder();
            for (TpaRequest request : incoming) {
                builder.append('i').append(request.id()).append(request.remainingMillis(now) / 1000);
            }
            for (TpaRequest request : outgoing) {
                builder.append('o').append(request.id()).append(request.remainingMillis(now) / 1000);
            }
            String current = builder.toString();
            if (current.equals(requestSignature[0])) return;
            requestSignature[0] = current;

            requests.clearAllScrollViewChildren();
            requests.clearLayoutCache();
            if (incoming.isEmpty() && outgoing.isEmpty()) {
                requests.addScrollViewChild(note("gui.roll_mod.hub.tpa.empty"));
            }
            if (!incoming.isEmpty()) {
                requests.addScrollViewChild(note("gui.roll_mod.hub.tpa.incoming"));
                for (TpaRequest request : incoming) {
                    requests.addScrollViewChild(incomingRow(request, now));
                }
            }
            if (!outgoing.isEmpty()) {
                requests.addScrollViewChild(note("gui.roll_mod.hub.tpa.outgoing"));
                for (TpaRequest request : outgoing) {
                    requests.addScrollViewChild(outgoingRow(request, now));
                }
            }
        });

        return root;
    }

    /* ------------------------------------------- settings ------------------------------------------- */

    /**
     * One row per mode, in a group so exactly one is ever marked.
     *
     * <p>A click only sends the packet; the mark moves when the server says so, so a refused change
     * never leaves the window claiming something that is not true.
     */
    private static void buildModes(UIElement content, Toggle[] rows) {
        Toggle.ToggleGroup group = new Toggle.ToggleGroup().setAllowEmpty(false);

        TpaMode[] values = TpaMode.values();
        for (int i = 0; i < values.length; i++) {
            TpaMode mode = values[i];

            Toggle row = new Toggle();
            row.setToggleGroup(group);
            row.setText(mode.title());
            row.layout(l -> l.widthPercent(100).height(MODE_ROW_H));
            row.setOnToggleChanged(on -> {
                if (on) PacketDistributor.sendToServer(TpaModePacket.of(mode));
            });
            row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    tooltip(mode), null, null, ItemStack.EMPTY));

            rows[i] = row;
            content.addChild(row);
        }
    }

    /**
     * What a mode means, plus a warning when it cannot mean anything.
     *
     * <p>The FTB Teams check is inside the listener rather than in the builder on purpose: the tree
     * is built on both sides and must not vary, but a hover only ever happens on a client.
     */
    private static List<Component> tooltip(TpaMode mode) {
        if (mode.needsTeams() && !ModList.get().isLoaded("ftbteams")) {
            return List.of(mode.description(),
                    Component.translatable("gui.roll_mod.hub.tpa.settings.needsTeams")
                            .withStyle(ChatFormatting.RED));
        }
        return List.of(mode.description());
    }

    /* --------------------------------------------- rows --------------------------------------------- */

    /** An online player, with the two ways of asking them. */
    private static UIElement playerRow(UUID id, String name) {
        UIElement row = row();

        Label label = new Label();
        label.setText(Component.literal(name));
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        row.addChildren(label, ask(id, false), ask(id, true));
        return row;
    }

    /**
     * One of the two request buttons: the blue figure walks to them, the red one calls them over.
     *
     * <p>The icon is the whole label, so the wording moves to the tooltip — attached to the button
     * rather than to the icon child, because tooltips are collected by walking up from whatever the
     * cursor is over.
     *
     * <p>A {@link SpriteTexture} and not an {@code ItemStackTexture}: these rows live in a scroller,
     * and an item texture draws outside the UI's batch, so it would keep painting over the list
     * after its row had scrolled out of sight.
     */
    private static Button ask(UUID id, boolean here) {
        Button button = new Button();
        button.text.setDisplay(false);
        // paddingAll(1), not LdLib's default 3: at this row height the default would shrink a
        // 16x16 icon to 10x10 and leave it swimming in the button.
        button.layout(l -> l.width(ROW_H).height(ROW_H).paddingAll(1));
        button.setOnClick(e -> PacketDistributor.sendToServer(TpaActionPacket.request(id, here)));

        UIElement icon = new UIElement();
        icon.layout(l -> l.widthPercent(100).heightPercent(100));
        icon.style(s -> s.background(SpriteTexture.of(RollMod.id(
                "textures/gui/hub/teleportation/" + (here ? "tpahere" : "tpa") + ".png"))));
        button.addChild(icon);

        hint(button, here ? "gui.roll_mod.hub.tpa.here" : "gui.roll_mod.hub.tpa.to");
        return button;
    }

    /** A hover explanation, the same helper every other tab keeps. */
    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }

    private static UIElement incomingRow(TpaRequest request, long now) {
        UIElement row = row();

        Label label = new Label();
        label.setText(Component.translatable(request.kind() == TpaRequest.Kind.TO
                        ? "gui.roll_mod.hub.tpa.wantsIn"
                        : "gui.roll_mod.hub.tpa.wantsOut",
                request.fromName(), Durations.format(request.remainingMillis(now))));
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        Button accept = new Button();
        accept.setText(Component.translatable("gui.roll_mod.hub.tpa.accept"));
        accept.layout(l -> l.width(ANSWER_W).heightPercent(100));
        accept.setOnClick(e ->
                PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), true)));

        Button deny = new Button();
        deny.setText(Component.translatable("gui.roll_mod.hub.tpa.deny"));
        deny.layout(l -> l.width(ANSWER_W).heightPercent(100));
        deny.setOnClick(e ->
                PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), false)));

        row.addChildren(label, accept, deny);
        return row;
    }

    private static UIElement outgoingRow(TpaRequest request, long now) {
        UIElement row = row();

        Label label = new Label();
        label.setText(Component.translatable("gui.roll_mod.hub.tpa.waiting",
                request.toName(), Durations.format(request.remainingMillis(now))));
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        Button cancel = new Button();
        cancel.setText(Component.translatable("gui.roll_mod.hub.tpa.cancel"));
        cancel.layout(l -> l.width(ANSWER_W).heightPercent(100));
        cancel.setOnClick(e -> PacketDistributor.sendToServer(TpaActionPacket.cancel(request.id())));

        row.addChildren(label, cancel);
        return row;
    }

    private static UIElement row() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(1).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        return row;
    }

    /** A heading inside the scroller, or the line that stands in for an empty one. */
    private static UIElement note(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }

    private static List<Map.Entry<UUID, String>> matching(Map<UUID, String> roster, String filter) {
        List<Map.Entry<UUID, String>> shown = new ArrayList<>(roster.entrySet());
        if (!filter.isEmpty()) {
            shown.removeIf(entry -> !entry.getValue().toLowerCase(Locale.ROOT).contains(filter));
        }
        shown.sort(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER));
        return shown;
    }

    /* --------------------------------------------- trips -------------------------------------------- */

    /**
     * The argument-less trips, sharing one payload, under a heading of their own.
     *
     * <p>The heading is {@link #note}, the same gray line the requests above use for "asked of you"
     * and "asked by you": the panel now holds two different things, and without it the buttons read
     * as part of the request list rather than as their own thing.
     */
    private static UIElement trips() {
        UIElement column = new UIElement();
        // An explicit height rather than letting it measure: LdLib defaults flex-shrink to 0, so a
        // content-sized sibling of the requests scroller would be free to push it about.
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .height(ROW_H + 2 + TRIP_H).marginTop(2).gapRow(2));

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(TRIP_H)
                .gapColumn(3));
        row.addChildren(
                // PLACEHOLDER: random and spawn wear the tab's own icon until each has artwork of
                // its own. Back is the only one drawing the file it is actually named after.
                trip("gui.roll_mod.hub.trip.rtp", TeleportActionPacket.Action.RTP, TRIP_PLACEHOLDER),
                trip("gui.roll_mod.hub.trip.back", TeleportActionPacket.Action.BACK,
                        "textures/gui/hub/teleportation/back.png"),
                trip("gui.roll_mod.hub.trip.spawn", TeleportActionPacket.Action.SPAWN,
                        TRIP_PLACEHOLDER));

        column.addChildren(note("gui.roll_mod.hub.trip.title"), row);
        return column;
    }

    /**
     * One trip. Every one keeps its caption, and its texture is drawn after the words rather than
     * in place of them.
     *
     * <p>{@link Button#addPostIcon} and not a child of our own: it gives the icon
     * {@code heightPercent(100)} with an aspect ratio of 1, so it stays square at whatever height
     * the button has. A child sized {@code widthPercent(100).heightPercent(100)} — which is what
     * this did while the icon was the whole label — stretches a 16x16 file across the button's
     * full box, and these buttons are much wider than they are tall.
     */
    private static Button trip(String key, TeleportActionPacket.Action action, String texture) {
        Button button = new Button();
        button.layout(l -> l.flexBasis(0).flexGrow(1).height(TRIP_H));
        button.setText(Component.translatable(key));
        button.addPostIcon(SpriteTexture.of(RollMod.id(texture)));
        button.setOnClick(e -> PacketDistributor.sendToServer(TeleportActionPacket.of(action)));
        return button;
    }
}
