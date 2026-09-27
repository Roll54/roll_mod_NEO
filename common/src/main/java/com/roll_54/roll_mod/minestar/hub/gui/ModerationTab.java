package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.letters.ClientLetterCache;
import com.roll_54.roll_mod.minestar.letters.LetterView;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.minestar.moderation.ModerationRow;
import com.roll_54.roll_mod.minestar.moderation.ModerationService;
import com.roll_54.roll_mod.minestar.moderation.Rule;
import com.roll_54.roll_mod.minestar.moderation.RulesStore;
import com.roll_54.roll_mod.minestar.moderation.TpsMonitor;
import com.roll_54.roll_mod.network.packet.ModerationActionPacket;
import com.roll_54.roll_mod.network.packet.ModerationActionPacket.Action;
import com.roll_54.roll_mod.network.packet.WarpActionPacket;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The staff tab: players down the left, and on the right either the selected player's standing or
 * the warp queue, switched by the one mode button beside the live TPS readout.
 *
 * <p><b>Built for everyone.</b> {@code HubUI}'s tree has to be the same for every player on both
 * sides, so a non-moderator gets this tab too; they simply never see it, because {@code HubUI} hides
 * its bookmark while {@link ClientModerationCache#ALLOWED} is false, and every action is re-checked
 * on the server by {@code ModerationActionPacket} regardless.
 *
 * <p><b>Exactly one sync value</b>, the TPS on {@code modeRow}. Everything else comes through
 * {@code SyncModerationPacket} and {@code ClientWarpCache}. Sync ids are handed out in attachment
 * order, so a value created anywhere inside a rebuild would get an id the other side never
 * allocates and silently blank some other tab's binding — keep it that way.
 *
 * <p>Every rebuild is driven from the one TICK on the tab root and guarded by a signature string,
 * the {@link WarpTab} idiom. The two panes are stacked absolutely and switched by height, not
 * {@code setDisplay}, so nothing in them ever stops ticking.
 */
public final class ModerationTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 130;
    private static final int ROW_H = 16;
    private static final int LINE_H = 12;
    private static final int MARK = 16;
    private static final int GAP = 3;

    private static final int POPUP_W = 250;
    private static final int POPUP_H = 214;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private static final int MODE_PLAYERS = 0;
    private static final int MODE_WARPS = 1;
    private static final int MODE_LETTERS = 2;
    private static final String[] MODE_KEYS = {
            "gui.roll_mod.hub.moderation.mode.players",
            "gui.roll_mod.hub.moderation.mode.warps",
            "gui.roll_mod.hub.moderation.mode.letters"};
    private static final int MODE_W = 110;
    private static final String[] SEARCH_KEYS = {
            "gui.roll_mod.hub.moderation.search.tip",
            "gui.roll_mod.hub.moderation.search.warps",
            "gui.roll_mod.hub.moderation.search.letters"};

    private ModerationTab() {}

    /** What a punishment popup is for. */
    private enum Kind { MUTE, WARN, BAN }

    public static UIElement build(Player player, Consumer<Boolean> inventory) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("moderation");

        // Per-viewer client state.
        int[] mode = {MODE_PLAYERS};
        UUID[] selectedPlayer = {null};
        UUID[] selectedWarp = {null};
        String[] filter = {""};

        /* ---------------------------------------- left: players ---------------------------------------- */

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100)
                .gapRow(GAP));

        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H));
        search.setTextResponder(text -> filter[0] = text == null ? "" : text.strip().toLowerCase());
        // The hint follows the mode: what the box searches is whatever the list is showing.
        search.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(SEARCH_KEYS[mode[0]])), null, null, ItemStack.EMPTY));

        ScrollerView players = scroller();
        left.addChildren(search, players);

        /* ---------------------------------------- right: modes ----------------------------------------- */

        UIElement right = new UIElement();
        right.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .gapRow(GAP));

        // The tab's one sync value: hundredths of a TPS, zero for anyone who may not moderate so the
        // number is not handed to players who cannot see it anyway.
        SimpleBinding<Integer> tps = DataBindingBuilder.intValS2C((Supplier<Integer>) () ->
                        server == null || !ModerationPermissions.canModerate(server)
                                ? 0 : TpsMonitor.tpsHundredths())
                .initialValue(2000).build();

        UIElement modeRow = new UIElement();
        modeRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .gapColumn(GAP));
        modeRow.addSyncValue(tps.getSyncValue());

        // One button that steps through the modes. Letters is skipped for anyone who may not write
        // them, so the cycle is only ever as long as what the viewer can actually do.
        Button modeButton = new Button();
        modeButton.layout(l -> l.width(MODE_W).height(ROW_H));
        modeButton.setOnClick(e -> mode[0] = nextMode(mode[0]));
        hint(modeButton, "gui.roll_mod.hub.moderation.mode.tip");

        Label tpsLabel = new Label();
        tpsLabel.layout(l -> l.flexGrow(1).height(ROW_H));
        tpsLabel.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER).textShadow(true));
        hint(tpsLabel, "gui.roll_mod.hub.moderation.tps.tip");

        modeRow.addChildren(modeButton, tpsLabel);

        // Both panes live in one box and are stacked on top of each other; the inactive one is
        // squashed to nothing. See the class javadoc for why not setDisplay.
        UIElement stack = new UIElement();
        stack.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        stack.setOverflowVisible(false);

        UIElement playerPane = pane();
        UIElement warpPane = pane();
        LetterComposer composer = new LetterComposer(inventory);
        stack.addChildren(playerPane, warpPane, composer.pane);

        right.addChildren(modeRow, stack);

        /* ------------------------------------------ player pane ---------------------------------------- */

        // Rebuilt on change: who is selected and what they are serving.
        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .flexBasis(0).flexGrow(1).paddingAll(PADDING));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        // Built once, so a message being typed survives the detail's once-a-second rebuilds.
        UIElement whitelist = new UIElement();
        whitelist.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .gapColumn(GAP));

        Switch whitelistSwitch = new Switch();
        whitelistSwitch.layout(l -> l.width(26).height(ROW_H));
        whitelistSwitch.setOnSwitchChanged(on ->
                PacketDistributor.sendToServer(ModerationActionPacket.whitelist(on)));
        hint(whitelistSwitch, "gui.roll_mod.hub.moderation.whitelist.tip");

        Label whitelistLabel = new Label();
        whitelistLabel.layout(l -> l.width(70).height(ROW_H));
        whitelistLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        TextField whitelistMessage = new TextField();
        whitelistMessage.setText("");
        whitelistMessage.layout(l -> l.flexGrow(1).height(ROW_H));
        hint(whitelistMessage, "gui.roll_mod.hub.moderation.whitelist.message.tip");

        Button whitelistSave = new Button();
        whitelistSave.setText(Component.translatable("gui.roll_mod.hub.moderation.save"));
        whitelistSave.layout(l -> l.width(34).height(ROW_H));
        whitelistSave.setOnClick(e -> PacketDistributor.sendToServer(
                ModerationActionPacket.whitelistMessage(whitelistMessage.getText())));

        whitelist.addChildren(whitelistSwitch, whitelistLabel, whitelistMessage, whitelistSave);
        playerPane.addChildren(detail, whitelist);

        /* ------------------------------------------- warp pane ----------------------------------------- */

        // The warps themselves are listed on the left; this is the selected one.
        UIElement warpDetail = new UIElement();
        warpDetail.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .flexBasis(0).flexGrow(1).paddingAll(PADDING));
        warpDetail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement warpActions = new UIElement();
        warpActions.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(GAP));

        warpPane.addChildren(warpDetail, warpActions);

        /* -------------------------------------------- popups ------------------------------------------- */

        Punish mute = punishPopup(Kind.MUTE);
        Punish warn = punishPopup(Kind.WARN);
        Punish ban = punishPopup(Kind.BAN);

        /* --------------------------------------------- tree -------------------------------------------- */

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0)
                .flexGrow(1).gapColumn(PADDING));
        body.addChildren(left, right);

        // Overlays last, so they cover everything.
        root.addChildren(HubSection.header("moderation", section), body,
                section.overlay(), mute.floating.root(), warn.floating.root(), ban.floating.root(),
                composer.popup.root());

        /* --------------------------------------------- tick -------------------------------------------- */

        String[] listSig = {null};
        String[] detailSig = {null};
        String[] warpSig = {null};
        int[] leftMode = {-1};
        int[] shownMode = {-1};
        int[] whitelistVersion = {-1};

        root.addEventListener(UIEvents.TICK, e -> {
            long now = System.currentTimeMillis();
            List<ClientModerationCache.Row> rows = ClientModerationCache.ROWS;

            // TPS, colour decided here from the same thresholds the server alerts on.
            Integer hundredths = tps.getSyncValue().getValue();
            double value = hundredths == null ? 20.0 : hundredths / 100.0;
            tpsLabel.setText(Component.translatable("gui.roll_mod.hub.moderation.tps",
                    Component.literal(TpsMonitor.format(value)).withStyle(TpsMonitor.colour(value))));

            // Lost the letters permission while in letters mode: fall back rather than strand them.
            if (!ClientModerationCache.MAY_WRITE_LETTERS && mode[0] == MODE_LETTERS) mode[0] = MODE_PLAYERS;

            if (shownMode[0] != mode[0]) {
                // A new mode starts clean: the search and the selections belonged to the old list.
                if (shownMode[0] != -1) {
                    search.setText("");
                    filter[0] = "";
                    selectedPlayer[0] = null;
                    selectedWarp[0] = null;
                }
                shownMode[0] = mode[0];
                show(playerPane, mode[0] == MODE_PLAYERS);
                show(warpPane, mode[0] == MODE_WARPS);
                show(composer.pane, mode[0] == MODE_LETTERS);
                modeButton.setText(Component.translatable("gui.roll_mod.hub.moderation.mode",
                        Component.translatable(MODE_KEYS[mode[0]])));
            }
            composer.tick(mode[0] == MODE_LETTERS);

            // Left list: whatever the mode is about.
            List<Warp> allWarps = ClientWarpCache.WARPS;
            StringBuilder sig = new StringBuilder().append(mode[0]).append('|').append(filter[0]).append('|');
            switch (mode[0]) {
                case MODE_PLAYERS -> {
                    List<ClientModerationCache.Row> shown = matching(rows, filter[0]);
                    sig.append(selectedPlayer[0]).append('|');
                    for (ClientModerationCache.Row row : shown) {
                        sig.append(row.row().id()).append(row.row().online() ? '+' : '-')
                                .append(row.row().warns()).append(row.muted(now) ? 'm' : '_')
                                .append(row.banned(now) ? 'b' : '_').append(';');
                    }
                    if (!sig.toString().equals(listSig[0])) {
                        listSig[0] = sig.toString();
                        rebuildPlayers(players, shown, now, selectedPlayer);
                    }
                }
                case MODE_WARPS -> {
                    List<Warp> shown = matchingWarps(allWarps, filter[0]);
                    sig.append(selectedWarp[0]).append('|');
                    for (Warp w : shown) {
                        sig.append(w.id()).append('/').append(w.approval().id()).append('/')
                                .append(w.reports().size()).append(';');
                    }
                    if (!sig.toString().equals(listSig[0])) {
                        listSig[0] = sig.toString();
                        rebuildWarps(players, shown, selectedWarp);
                    }
                }
                default -> {
                    // Minutes, not seconds: rows show times of day, and the only thing the clock
                    // changes on its own is a letter crossing into "expired".
                    sig.append(ClientLetterCache.VERSION).append('|').append(composer.editingId())
                            .append('|').append(now / 60_000L);
                    if (!sig.toString().equals(listSig[0])) {
                        listSig[0] = sig.toString();
                        rebuildLetters(players, ClientLetterCache.LETTERS, filter[0], now, composer);
                    }
                }
            }

            // Player detail. Seconds, not millis, in the signature: a running countdown rebuilds
            // once a second rather than every frame.
            ClientModerationCache.Row current = find(rows, selectedPlayer[0]);
            String dSig = ClientModerationCache.VERSION + ":" + selectedPlayer[0] + ":"
                    + (current == null ? "" : now / 1000L) + ":" + ClientModerationCache.MAY_BAN;
            if (!dSig.equals(detailSig[0])) {
                detailSig[0] = dSig;
                detail.clearAllChildren();
                buildDetail(detail, current, now, mute, warn, ban);
            }

            // Whitelist row: state follows the server, the message only when nobody is typing it.
            if (whitelistVersion[0] != ClientModerationCache.VERSION) {
                whitelistVersion[0] = ClientModerationCache.VERSION;
                boolean mayBan = ClientModerationCache.MAY_BAN;
                // height(0) rather than setDisplay: see the class javadoc.
                whitelist.layout(l -> l.height(mayBan ? ROW_H : 0));
                whitelist.setOverflowVisible(false);
                whitelistSwitch.setOn(ClientModerationCache.WHITELIST, false);
                whitelistLabel.setText(Component.translatable(ClientModerationCache.WHITELIST
                        ? "gui.roll_mod.hub.moderation.whitelist.on"
                        : "gui.roll_mod.hub.moderation.whitelist.off")
                        .withStyle(ClientModerationCache.WHITELIST
                                ? ChatFormatting.RED : ChatFormatting.GREEN));
                if (!whitelistMessage.isFocused()) {
                    whitelistMessage.setText(ClientModerationCache.WHITELIST_MESSAGE);
                }
            }

            // Warp detail and actions.
            Warp currentWarp = findWarp(allWarps, selectedWarp[0]);
            String wSig = selectedWarp[0] + ":" + (currentWarp == null ? ""
                    : currentWarp.approval().id() + "/" + currentWarp.reports().size() + "/"
                            + currentWarp.visitorsToday() + "/" + currentWarp.visitsTotal());
            if (!wSig.equals(warpSig[0])) {
                warpSig[0] = wSig;
                warpDetail.clearAllChildren();
                buildWarpDetail(warpDetail, currentWarp);
                rebuildWarpActions(warpActions, currentWarp);
            }
        });

        return root;
    }

    /** Players → warps → letters → players, skipping letters for anyone who may not write them. */
    private static int nextMode(int current) {
        int next = (current + 1) % MODE_KEYS.length;
        if (next == MODE_LETTERS && !ClientModerationCache.MAY_WRITE_LETTERS) {
            next = (next + 1) % MODE_KEYS.length;
        }
        return next;
    }

    /* ------------------------------------------ player list ------------------------------------------- */

    private static List<ClientModerationCache.Row> matching(List<ClientModerationCache.Row> rows,
                                                            String filter) {
        if (filter.isEmpty()) return rows;
        List<ClientModerationCache.Row> matches = new ArrayList<>();
        for (ClientModerationCache.Row row : rows) {
            if (row.row().name().toLowerCase().contains(filter)) matches.add(row);
        }
        return matches;
    }

    private static void rebuildPlayers(ScrollerView list, List<ClientModerationCache.Row> rows,
                                       long now, UUID[] selected) {
        list.clearAllScrollViewChildren();
        for (ClientModerationCache.Row entry : rows) {
            ModerationRow row = entry.row();
            boolean isSelected = row.id().equals(selected[0]);

            UIElement item = new UIElement();
            item.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                    .marginBottom(2).paddingHorizontal(3));
            item.style(s -> s.background(new ColorRectTexture(
                    isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            // Clicking the selected row again clears it, which is also how the warp filter is lifted.
            item.addEventListener(UIEvents.CLICK, e ->
                    selected[0] = row.id().equals(selected[0]) ? null : row.id());

            MutableComponent text = Component.literal(row.name())
                    .withStyle(row.online() ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY);
            if (row.warns() > 0) {
                text.append(Component.literal(" ⚠" + row.warns()).withStyle(
                        row.warns() >= ModerationService.WARNS_BEFORE_BAN - 1
                                ? ChatFormatting.RED : ChatFormatting.GOLD));
            }
            if (entry.muted(now)) text.append(Component.literal(" M").withStyle(ChatFormatting.YELLOW));
            if (entry.banned(now)) text.append(Component.literal(" B").withStyle(ChatFormatting.DARK_RED));

            Label name = new Label();
            name.setText(text);
            name.layout(l -> l.flexGrow(1).height(ROW_H));
            name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            item.addChild(name);

            list.addScrollViewChild(item);
        }
    }

    /* ----------------------------------------- player detail ------------------------------------------ */

    private static void buildDetail(UIElement detail, @Nullable ClientModerationCache.Row entry, long now,
                                    Punish mute, Punish warn, Punish ban) {
        if (entry == null) {
            Label empty = new Label();
            empty.setText(Component.translatable(ClientModerationCache.ROWS.isEmpty()
                            ? "gui.roll_mod.hub.moderation.empty"
                            : "gui.roll_mod.hub.moderation.pick")
                    .withStyle(ChatFormatting.GRAY));
            empty.layout(l -> l.widthPercent(100).height(LINE_H));
            empty.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
            detail.addChild(empty);
            return;
        }
        ModerationRow row = entry.row();

        Label title = new Label();
        title.setText(Component.literal(row.name()).append(Component.literal("  ")).append(
                Component.translatable(row.online()
                                ? "gui.roll_mod.hub.moderation.online"
                                : "gui.roll_mod.hub.moderation.offline")
                        .withStyle(row.online() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY)));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
        detail.addChild(title);

        detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.warns",
                Component.literal(String.valueOf(row.warns())).withStyle(row.warns() > 0
                        ? ChatFormatting.GOLD : ChatFormatting.WHITE),
                ModerationService.WARNS_BEFORE_BAN), 4));

        long muteLeft = ClientModerationCache.Row.left(entry.muteUntil(), now);
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.mute",
                !entry.muted(now)
                        ? Component.translatable("gui.roll_mod.hub.moderation.none")
                                .withStyle(ChatFormatting.WHITE)
                        : state(muteLeft, ChatFormatting.YELLOW)), 0));

        long banLeft = ClientModerationCache.Row.left(entry.banUntil(), now);
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.ban",
                !entry.banned(now)
                        ? Component.translatable("gui.roll_mod.hub.moderation.none")
                                .withStyle(ChatFormatting.WHITE)
                        : state(banLeft, ChatFormatting.RED)), 0));
        if (entry.banned(now) && (!row.banRule().isBlank() || !row.banReason().isBlank())) {
            Label reason = new Label();
            MutableComponent why = RulesStore.label(row.banRule()).copy();
            if (!row.banRule().isBlank() && !row.banReason().isBlank()) why.append(" — ");
            reason.setText(why.append(row.banReason()).withStyle(ChatFormatting.GRAY));
            reason.layout(l -> l.widthPercent(100).marginLeft(6));
            reason.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            detail.addChild(reason);
        }

        // Actions, pinned to the bottom. Mute and warn need the player here to hear it — the server
        // refuses them otherwise — so they are only offered when that is true.
        UIElement actions = new UIElement();
        actions.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginTopAuto().gapColumn(GAP));

        if (row.online()) {
            if (entry.muted(now)) {
                actions.addChild(action("gui.roll_mod.hub.moderation.unmute", () ->
                        PacketDistributor.sendToServer(ModerationActionPacket.of(
                                Action.UNMUTE, row.id(), 0L, "", ""))));
            } else {
                actions.addChild(action("gui.roll_mod.hub.moderation.mute.button",
                        () -> mute.open(row)));
            }
            actions.addChild(action("gui.roll_mod.hub.moderation.warn.button", () -> warn.open(row)));
        }
        // Built only for those who may: the rebuilt detail carries no sync values, so varying it by
        // permission is safe here in a way it would not be in the tab's fixed skeleton.
        if (ClientModerationCache.MAY_BAN) {
            if (entry.banned(now)) {
                actions.addChild(action("gui.roll_mod.hub.moderation.unban", () ->
                        PacketDistributor.sendToServer(ModerationActionPacket.of(
                                Action.UNBAN, row.id(), 0L, "", ""))));
            } else {
                actions.addChild(action("gui.roll_mod.hub.moderation.ban.button", () -> ban.open(row)));
            }
        }
        detail.addChild(actions);
    }

    /** "for 1:23:45" or "no end", in the given colour. */
    private static Component state(long left, ChatFormatting color) {
        return left < 0L
                ? Component.translatable("gui.roll_mod.hub.moderation.forever").withStyle(color)
                : Component.literal(Durations.format(left)).withStyle(color);
    }

    /* ------------------------------------------- warp pane -------------------------------------------- */

    private static void rebuildWarps(ScrollerView list, List<Warp> warps, UUID[] selected) {
        list.clearAllScrollViewChildren();
        for (Warp warp : warps) {
            boolean isSelected = warp.id().equals(selected[0]);

            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(MARK + 4)
                    .marginBottom(2).paddingAll(2).gapColumn(GAP));
            row.style(s -> s.background(new ColorRectTexture(
                    isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            row.addEventListener(UIEvents.CLICK, e -> selected[0] = warp.id());

            UIElement badge = new UIElement();
            badge.layout(l -> l.width(MARK).height(MARK));
            badge.style(s -> s.background(warp.approval().icon()));
            badge.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    List.of(warp.approval().line()), null, null, ItemStack.EMPTY));

            MutableComponent text = Component.empty().append(LegacyText.display(warp.name()))
                    .append(Component.literal("  " + warp.ownerName()).withStyle(ChatFormatting.GRAY));
            if (!warp.reports().isEmpty()) {
                text.append(Component.literal("  ⚑" + warp.reports().size())
                        .withStyle(ChatFormatting.RED));
            }
            Label name = new Label();
            name.setText(text);
            name.layout(l -> l.flexGrow(1).height(MARK));
            name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

            row.addChildren(badge, name);
            list.addScrollViewChild(row);
        }
    }

    /**
     * The verdicts on one row, then Teleport and Delete — or a hint when nothing is selected. The
     * verdict the warp already has is greyed: nothing to press.
     */
    private static void rebuildWarpActions(UIElement bar, @Nullable Warp warp) {
        bar.clearAllChildren();
        if (warp == null) return;
        UIElement verdicts = actionRow();
        for (WarpApproval approval : List.of(WarpApproval.APPROVED, WarpApproval.DISAPPROVED,
                WarpApproval.ADMIN, WarpApproval.NOT_YET)) {
            Button b = action("gui.roll_mod.hub.moderation.warps." + approval.id(), () ->
                    PacketDistributor.sendToServer(WarpActionPacket.setApproval(warp.id(), approval)));
            b.setActive(warp.approval() != approval);
            verdicts.addChild(b);
        }
        UIElement other = actionRow();
        other.addChildren(
                action("gui.roll_mod.hub.moderation.warps.teleport", () ->
                        PacketDistributor.sendToServer(WarpActionPacket.teleport(warp.id()))),
                action("gui.roll_mod.hub.moderation.warps.delete", () ->
                        PacketDistributor.sendToServer(WarpActionPacket.delete(warp.id()))));
        bar.addChildren(verdicts, other);
    }

    private static UIElement actionRow() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP));
        return row;
    }

    /** Everything a moderator needs to judge a warp without going there. */
    private static void buildWarpDetail(UIElement detail, @Nullable Warp warp) {
        if (warp == null) {
            Label hint = new Label();
            hint.setText(Component.translatable("gui.roll_mod.hub.moderation.warps.pick")
                    .withStyle(ChatFormatting.GRAY));
            hint.layout(l -> l.widthPercent(100).height(LINE_H));
            hint.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
            detail.addChild(hint);
            return;
        }
        Label title = new Label();
        title.setText(LegacyText.display(warp.name()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
        detail.addChild(title);

        Label verdict = new Label();
        verdict.setText(Component.empty()
                .append(warp.approval().line().copy().withStyle(ChatFormatting.BOLD))
                .append(" — ").append(warp.approval().note())
                .withStyle(style -> style.withColor(warp.approval().color())));
        verdict.layout(l -> l.widthPercent(100).marginTop(2));
        verdict.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        detail.addChild(verdict);
        if (!warp.moderator().isEmpty()) {
            detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.warps.by",
                    Component.literal(warp.moderator()).withStyle(ChatFormatting.WHITE)), 0));
        }

        detail.addChild(line(Component.translatable("gui.roll_mod.hub.warp.owner",
                Component.literal(warp.ownerName()).withStyle(ChatFormatting.WHITE)), 4));
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.warp.coordinates",
                Component.literal(String.valueOf((int) warp.x())).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf((int) warp.y())).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf((int) warp.z())).withStyle(ChatFormatting.AQUA)), 0));
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.warp.world",
                Component.literal(warp.dimension().toString()).withStyle(ChatFormatting.AQUA)), 0));
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.warps.reports",
                Component.literal(String.valueOf(warp.reports().size())).withStyle(
                        warp.reports().isEmpty() ? ChatFormatting.WHITE : ChatFormatting.RED)), 4));
        detail.addChild(line(Component.translatable("gui.roll_mod.hub.moderation.warps.visits",
                Component.literal(String.valueOf(warp.visitorsToday())).withStyle(ChatFormatting.WHITE),
                Component.literal(String.valueOf(warp.visitsTotal())).withStyle(ChatFormatting.WHITE)), 0));

        if (!warp.description().isBlank()) {
            Label description = new Label();
            description.setText(LegacyText.display(warp.description()));
            description.layout(l -> l.widthPercent(100).marginTop(4));
            description.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            detail.addChild(description);
        }
    }

    /** Warps whose name or owner matches the search; an empty search matches all. */
    private static List<Warp> matchingWarps(List<Warp> warps, String filter) {
        if (filter.isEmpty()) return warps;
        List<Warp> out = new ArrayList<>();
        for (Warp warp : warps) {
            if (LegacyText.plain(warp.name()).toLowerCase().contains(filter)
                    || warp.ownerName().toLowerCase().contains(filter)) {
                out.add(warp);
            }
        }
        return out;
    }

    /* -------------------------------------------- letters --------------------------------------------- */

    /**
     * "+ New letter", then the letters grouped Due · Active · Expired. Picking one opens it in the
     * composer; the composer owns which one that is, so the highlight reads it back from there.
     */
    private static void rebuildLetters(ScrollerView list, List<LetterView> letters, String filter,
                                       long now, LetterComposer composer) {
        list.clearAllScrollViewChildren();
        UUID editing = composer.editingId();

        list.addScrollViewChild(letterRow(Component.translatable("gui.roll_mod.hub.moderation.letters.new")
                .withStyle(ChatFormatting.GREEN), editing == null, composer::clear, null));

        List<LetterView> due = new ArrayList<>();
        List<LetterView> active = new ArrayList<>();
        List<LetterView> expired = new ArrayList<>();
        for (LetterView letter : letters) {
            if (letter.personal()) continue; // a server notice to this moderator, not a letter they manage
            if (!filter.isEmpty() && !LegacyText.plain(letter.title()).toLowerCase().contains(filter)) continue;
            (letter.due(now) ? due : letter.expired(now) ? expired : active).add(letter);
        }
        SimpleDateFormat when = new SimpleDateFormat("dd.MM HH:mm");
        addGroup(list, "gui.roll_mod.hub.moderation.letters.group.due", due, editing, composer,
                v -> Component.literal("⏳ " + when.format(new Date(v.sendAt()))).withStyle(ChatFormatting.AQUA));
        addGroup(list, "gui.roll_mod.hub.moderation.letters.group.active", active, editing, composer,
                v -> Component.literal("✔" + v.acceptedCount()).withStyle(ChatFormatting.GREEN));
        addGroup(list, "gui.roll_mod.hub.moderation.letters.group.expired", expired, editing, composer,
                v -> Component.literal("✖").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void addGroup(ScrollerView list, String key, List<LetterView> letters, @Nullable UUID editing,
                                 LetterComposer composer,
                                 java.util.function.Function<LetterView, Component> marker) {
        if (letters.isEmpty()) return;
        Label header = new Label();
        header.setText(Component.translatable(key, letters.size()).withStyle(ChatFormatting.GRAY));
        header.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(3).paddingHorizontal(3));
        list.addScrollViewChild(header);
        for (LetterView letter : letters) {
            Component text = Component.empty().append(marker.apply(letter)).append(" ")
                    .append(LegacyText.display(letter.title()));
            list.addScrollViewChild(letterRow(text, letter.id().equals(editing), () -> composer.load(letter),
                    letter.icon()));
        }
    }

    /** {@code icon} is the letter's {@link LetterIcons icon}, or {@code null} for a row that is no letter. */
    private static UIElement letterRow(Component text, boolean selected, Runnable onClick, @Nullable String icon) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).marginBottom(2)
                .paddingHorizontal(icon == null ? 3 : 0).paddingRight(3).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(selected ? COLOR_ROW_SELECTED : COLOR_ROW)));
        row.addEventListener(UIEvents.CLICK, e -> onClick.run());
        // Staff see what readers see, but never the unread mark: it would be about the moderator.
        if (icon != null) row.addChild(LetterIcons.element(icon, false));
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.flexGrow(1).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        label.setAllowHitTest(false);
        row.addChild(label);
        return row;
    }

    /* -------------------------------------------- popups ---------------------------------------------- */

    /**
     * One punishment form: target line, duration (not for a warning), rule picker, note, confirm.
     * Built once with the tab; {@link #open} points it at a player and resets it.
     */
    private static final class Punish {
        final Kind kind;
        HubSection.Floating floating;
        final ModerationRow[] target = {null};
        /** The rules picked for this punishment, in the order they were clicked. Several allowed. */
        final Set<String> chosenRules = new LinkedHashSet<>();
        Label who;
        Label warnsLine;
        TextField duration;
        TextField note;
        ScrollerView rules;
        Button confirm;

        Punish(Kind kind) {
            this.kind = kind;
        }

        void open(ModerationRow row) {
            target[0] = row;
            chosenRules.clear();
            note.setText("");
            if (duration != null) duration.setText(kind == Kind.MUTE ? "30m" : "1d");
            who.setText(Component.translatable("gui.roll_mod.hub.moderation."
                    + kind.name().toLowerCase() + ".who", row.name()));
            if (warnsLine != null) {
                int next = row.warns() + 1;
                boolean bans = next >= ModerationService.WARNS_BEFORE_BAN;
                warnsLine.setText(Component.translatable("gui.roll_mod.hub.moderation.warn.count",
                        row.warns()).withStyle(bans ? ChatFormatting.RED : ChatFormatting.GRAY));
                confirm.setText(Component.translatable(bans
                        ? "gui.roll_mod.hub.moderation.warn.confirmBan"
                        : "gui.roll_mod.hub.moderation.confirm"));
            }
            rebuildRules();
            floating.open();
        }

        void rebuildRules() {
            rules.clearAllScrollViewChildren();
            for (Rule r : RulesStore.all()) {
                boolean chosen = chosenRules.contains(r.id());
                Label item = new Label();
                item.setText(r.label().withStyle(chosen ? ChatFormatting.YELLOW : ChatFormatting.WHITE)
                        .append(Component.literal(" [").append(r.severity().label()).append("]")));
                item.layout(l -> l.widthPercent(100).paddingHorizontal(2).marginBottom(1));
                item.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                item.style(s -> s.background(new ColorRectTexture(chosen ? COLOR_ROW_SELECTED : 0)));
                item.addEventListener(UIEvents.CLICK, e -> {
                    if (!chosenRules.remove(r.id())) chosenRules.add(r.id());
                    rebuildRules();
                });
                rules.addScrollViewChild(item);
            }
        }

        void submit() {
            ModerationRow row = target[0];
            if (row == null) return;
            long millis = 0L;
            if (duration != null) {
                millis = Durations.parse(duration.getText());
                if (millis < 0L) {
                    // Refused rather than guessed: a typo must never become a permanent ban.
                    duration.setText("");
                    return;
                }
            }
            Action action = switch (kind) {
                case MUTE -> Action.MUTE;
                case WARN -> Action.WARN;
                case BAN -> Action.BAN;
            };
            PacketDistributor.sendToServer(ModerationActionPacket.of(action, row.id(), millis,
                    RulesStore.join(chosenRules), note.getText()));
            floating.close();
        }
    }

    private static Punish punishPopup(Kind kind) {
        Punish p = new Punish(kind);
        p.floating = HubSection.popup(
                Component.translatable("gui.roll_mod.hub.moderation." + kind.name().toLowerCase()
                        + ".title"),
                POPUP_W, POPUP_H, body -> {
                    p.who = new Label();
                    p.who.layout(l -> l.widthPercent(100).height(LINE_H));
                    body.addChild(p.who);

                    if (kind == Kind.WARN) {
                        p.warnsLine = new Label();
                        p.warnsLine.layout(l -> l.widthPercent(100));
                        p.warnsLine.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                        body.addChild(p.warnsLine);
                    } else {
                        p.duration = new TextField();
                        p.duration.setText("");
                        p.duration.layout(l -> l.widthPercent(100).height(ROW_H));
                        hint(p.duration, "gui.roll_mod.hub.moderation.duration.tip");
                        body.addChild(p.duration);
                    }

                    Label ruleCaption = new Label();
                    ruleCaption.setText(Component.translatable("gui.roll_mod.hub.moderation.rule")
                            .withStyle(ChatFormatting.GRAY));
                    ruleCaption.layout(l -> l.widthPercent(100).height(LINE_H));
                    body.addChild(ruleCaption);

                    p.rules = scroller();
                    p.rules.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
                    body.addChild(p.rules);

                    p.note = new TextField();
                    p.note.setText("");
                    p.note.layout(l -> l.widthPercent(100).height(ROW_H));
                    hint(p.note, "gui.roll_mod.hub.moderation.note.tip");
                    body.addChild(p.note);

                    p.confirm = new Button();
                    p.confirm.setText(Component.translatable("gui.roll_mod.hub.moderation.confirm"));
                    p.confirm.layout(l -> l.widthPercent(100).height(ROW_H));
                    p.confirm.setOnClick(e -> p.submit());
                    body.addChild(p.confirm);
                });
        return p;
    }

    /* ------------------------------------------- plumbing --------------------------------------------- */

    /** One of the two stacked mode panes. */
    private static UIElement pane() {
        UIElement pane = new UIElement();
        pane.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0)
                .widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).gapRow(GAP));
        pane.setOverflowVisible(false);
        return pane;
    }

    private static void show(UIElement pane, boolean shown) {
        pane.layout(l -> l.heightPercent(shown ? 100 : 0));
    }

    /**
     * A vertical list that actually scrolls: {@code flexBasis(0)} so it takes the leftover space
     * rather than its content's height, and a COLUMN container so the content height is measured —
     * the two gotchas {@code HubSection.overlay} documents.
     */
    private static ScrollerView scroller() {
        ScrollerView view = new ScrollerView();
        view.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        return view;
    }

    private static Button action(String key, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexGrow(1).height(ROW_H));
        button.setOnClick(e -> onClick.run());
        return button;
    }

    private static Label line(Component text, int marginTop) {
        Label line = new Label();
        line.setText(text.copy().withStyle(ChatFormatting.GRAY));
        line.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(marginTop));
        return line;
    }

    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }

    @Nullable
    private static ClientModerationCache.Row find(List<ClientModerationCache.Row> rows, @Nullable UUID id) {
        if (id == null) return null;
        for (ClientModerationCache.Row row : rows) {
            if (row.row().id().equals(id)) return row;
        }
        return null;
    }

    @Nullable
    private static Warp findWarp(List<Warp> warps, @Nullable UUID id) {
        if (id == null) return null;
        for (Warp warp : warps) {
            if (warp.id().equals(id)) return warp;
        }
        return null;
    }
}
