package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.minestar.hub.warp.WarpService;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.teleport.ClientTeleportCache;
import com.roll_54.roll_mod.network.packet.WarpActionPacket;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * The hub's warp tab: every warp on the server down the left, the selected one on the right.
 *
 * <p>Reads {@link ClientWarpCache}, which {@code SyncWarpsPacket} keeps current — the same
 * client-cache approach as the auction and plot screens, because a variable-length list does not fit
 * LdLib2's positional bindings. On the server the cache is empty and this tree is trivially small,
 * which is fine: nothing here is synced positionally.
 *
 * <p>Panels are rebuilt only when the thing they show changes, guarded by a signature string — the
 * idiom the vendor screens already use, since {@code UIEvents.TICK} fires every frame.
 */
public final class WarpTab {

    private static final int PADDING = 6;
    /** The same as the homes list — see {@link TravelTab#LIST_W}. */
    private static final int LIST_W = TravelTab.LIST_W;
    private static final int ROW_H = 16;
    private static final int LINE_H = 12;

    /**
     * The status badge opening a list row, and the height the row's name lines up to.
     *
     * <p>16 because that is the artwork's own resolution: the badges are 16x16 pixel art (admin.png
     * is 32x32, an exact 2:1), and drawing them at 12 resampled every sprite onto a half-pixel grid,
     * which is what made them look smeared next to the crisp text beside them.
     */
    private static final int MARK = 16;

    /**
     * A list row, tall enough to hold {@link #MARK} at its native size plus the row's own padding.
     *
     * <p>Separate from {@link #ROW_H} on purpose: that one sizes the buttons and text fields all over
     * this tab, and widening those to fit a badge would be the tail wagging the dog.
     */
    private static final int LIST_ROW_H = MARK + 4;

    /** The description's indent from the panel edge, as asked for. */
    private static final int DESCRIPTION_INSET = 5;

    /**
     * The space opening each group of the detail panel: where it is, how visited it is, what it
     * costs.
     *
     * <p>Deliberately small. The panel has no scroller, and at the hub window's minimum height
     * ({@code HubWindowState.MIN_H}) the gaps come out of the same budget as a blocked warp's appeal paragraph and a full-length
     * description; at that extreme the teleport button's {@code marginTopAuto} has no slack left to
     * give and the buttons below it would be pushed off the bottom. Four also happens to be the
     * margin the owner line already carried, so the first group costs nothing new.
     */
    private static final int GROUP_GAP = 4;

    /** The Change window: a title, the pin toggle and the owner's delete. */
    private static final int CHANGE_W = 200;
    private static final int CHANGE_H = 110;
    private static final int COUNTDOWN_W = 200;
    private static final int COUNTDOWN_H = 90;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private WarpTab() {}

    public static UIElement build(Player player) {
        return build(player, true);
    }

    /**
     * The list and the detail panel without the section's own title row and padding — what the
     * travel tab shows under its Homes | Warps switch.
     */
    public static UIElement content(Player player) {
        return build(player, false);
    }

    private static UIElement build(Player player, boolean titled) {
        // A column now, so the section title can sit above the list and detail panel. The old row
        // lives on as `body`, unchanged apart from taking its height from the leftover space.
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(titled ? PADDING : 0));

        HubSection.Info section = HubSection.info("warps");

        UIElement body = new UIElement();
        // flexBasis(0), not flexGrow alone: with LdLib's auto basis the body (and the list in it)
        // would be as tall as every warp together, so the scroller would never overflow or scroll.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(1));

        // Selection and mode are per-viewer client state, so they live in the tree, not the packet.
        UUID[] selected = {null};
        boolean[] creating = {false};

        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .paddingAll(PADDING));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100)
                .gapRow(PADDING));

        Button create = new Button();
        create.setText(Component.translatable("gui.roll_mod.hub.warp.create"));
        create.layout(l -> l.widthPercent(100).height(ROW_H));
        create.setOnClick(e -> {
            creating[0] = true;
            selected[0] = null;
        });

        // Matches on name and owner: with every player's warps in one list, "whose is this" is as
        // common a question as "what is it called".
        String[] filter = {""};
        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H));
        search.setTextResponder(text -> filter[0] = text == null ? "" : text.strip().toLowerCase());
        hint(search, "gui.roll_mod.hub.warp.search.tip");

        ScrollerView list = new ScrollerView();
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        left.addChildren(create, search, list);
        // The divider between the list and the panel: drag to resize, double-click to reset.
        body.addChildren(left, HubSplit.handle(titled ? "warps" : TravelTab.SPLIT_KEY, left, LIST_W, player.level().isClientSide()), detail);
        // The panel last, so it covers the body when opened.
        // Deleting a warp cannot be undone, so it asks first.
        HubConfirm confirmer = new HubConfirm();
        // A locked warp answers in a window rather than in chat, which the hub covers.
        HubNotice notice = new HubNotice(Component.translatable("gui.roll_mod.hub.warp.locked.title"));

        // Pin and, for the owner, delete: what changes a warp rather than travelling to it. Filled
        // for the selected warp by the tick below while it is open.
        UIElement[] changeBody = {null};
        HubSection.Floating changeWindow = HubSection.popup(
                Component.translatable("gui.roll_mod.hub.change.title"), CHANGE_W, CHANGE_H, content -> {
                    content.layout(l -> l.flexDirection(FlexDirection.COLUMN));
                    changeBody[0] = content;
                }).windowed();

        // The warm-up, counted down where the player is looking: the server keeps the hub open
        // through it now (see WarpService.begin) and closes it on arrival.
        Label countdownText = new Label();
        HubSection.Floating countdown = HubSection.popup(
                Component.translatable("gui.roll_mod.hub.warp.countdown.title"), COUNTDOWN_W, COUNTDOWN_H, content -> {
                    content.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(4));
                    countdownText.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
                    countdownText.textStyle(t -> t.textWrap(TextWrap.WRAP).textAlignHorizontal(Horizontal.CENTER));
                    Button cancel = new Button();
                    cancel.setText(Component.translatable("gui.roll_mod.hub.warp.countdown.cancel"));
                    cancel.layout(l -> l.widthPercent(100).height(ROW_H).flexShrink(0));
                    cancel.setOnClick(e -> PacketDistributor.sendToServer(WarpActionPacket.cancel()));
                    content.addChildren(countdownText, cancel);
                }).windowed().noCloseButton();

        if (titled) {
            root.addChildren(HubSection.header("warps", section), body, section.overlay());
        } else {
            root.addChildren(body);
        }
        root.addChildren(changeWindow.root(), countdown.root(), notice.root(), confirmer.root());

        String[] listSignature = {null};
        String[] detailSignature = {null};
        String[] changeSignature = {null};
        boolean[] countingDown = {false};

        root.addEventListener(UIEvents.TICK, e -> {
            List<Warp> warps = ClientWarpCache.WARPS;

            // Opened when a warm-up starts and closed when it ends — arrival, Cancel, or a refusal
            // on arrival — but only ever by this listener, so it never closes a window it did not open.
            if (ClientTeleportCache.active()) {
                if (!countingDown[0]) {
                    countingDown[0] = true;
                    countdown.open();
                }
                countdownText.setText(Component.translatable("gui.roll_mod.hub.warp.countdown.text",
                        LegacyText.display(ClientTeleportCache.DESTINATION),
                        Component.literal(String.valueOf(ClientTeleportCache.secondsLeft()))
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)));
            } else if (countingDown[0]) {
                countingDown[0] = false;
                countdown.close();
            }

            // The server refuses a warp over the cap anyway; hiding the button means the player is
            // told before they fill the form in rather than after.
            long owned = warps.stream().filter(w -> w.owner().equals(player.getUUID())).count();
            create.setDisplay(owned < ClientWarpCache.LIMIT);

            List<Warp> shown = matching(warps, filter[0]);

            // Approval and the visitor count are in the signature because both are visible on a
            // row: approval as its dot, visitors as the order the server sorted the list into.
            // Keyed on ids alone, a moderator's verdict and a new arrival both went unnoticed.
            // The name and the lock too: a moderator can rename a warp or put it behind a quest, and
            // the player's team finishing that quest opens it.
            String signature = shown.size() + ":" + shown.stream()
                    .map(w -> w.id() + "/" + w.approval().id() + "/" + w.visitorsToday() + "/" + w.name()
                            + "/" + w.questLocked() + "/" + locked(player, w))
                    .reduce("", String::concat)
                    + ":" + selected[0] + ":" + HubPins.version(player);
            if (!signature.equals(listSignature[0])) {
                listSignature[0] = signature;
                rebuildList(player, list, shown, selected, creating);
            }

            Warp current = find(warps, selected[0]);
            // The price and the owner are absent on purpose: neither can change for a given warp id
            // — Warp offers withApproval, withReports and withVisit and nothing that rewrites a
            // price — so the price row and its free/paid split are settled by the selection alone.
            // The approval is what makes the teleport button read "instant" rather than a five
            // second countdown, and it is already here, so promoting a warp to official while the
            // panel is open relabels the button on the next frame.
            String mode = creating[0] ? "create" : selected[0] + ":"
                    + (current == null ? "" : current.approval().id() + "/"
                            + current.visitorsToday() + "/" + current.visitsTotal() + "/"
                            // Moderators can now edit these, and gate the warp behind a quest.
                            + current.name() + "/" + current.description() + "/" + current.price() + "/"
                            + current.x() + "," + current.y() + "," + current.z() + "/"
                            + current.requiredQuest() + "/" + locked(player, current));
            if (!mode.equals(detailSignature[0])) {
                detailSignature[0] = mode;
                detail.clearAllChildren();
                if (creating[0]) {
                    buildCreateForm(player, detail, creating);
                } else {
                    buildDetail(player, detail, current, selected, creating, notice, () -> {
                        changeSignature[0] = null;
                        changeWindow.open();
                    });
                }
            }

            // The Change window follows the selection, and goes with the warp.
            if (changeWindow.isOpen()) {
                if (current == null || creating[0]) {
                    changeWindow.close();
                } else {
                    String change = current.id() + ":" + current.name() + ":" + HubPins.version(player);
                    if (!change.equals(changeSignature[0])) {
                        changeSignature[0] = change;
                        changeBody[0].clearAllChildren();
                        buildChange(player, changeBody[0], current, selected, confirmer);
                    }
                }
            }
        });

        return root;
    }

    /** The badge of every warp behind a quest. */
    static final com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture LOCKED_ICON = SpriteTexture.of(
            RollMod.id("textures/gui/hub/teleportation/warp/locked_warp_by_quest.png"));

    /** "Requires the quest: <title>". */
    static Component questLine(Warp warp) {
        return Component.translatable("gui.roll_mod.hub.warp.quest",
                QuestGate.title(true, warp.requiredQuest()).copy().withStyle(ChatFormatting.YELLOW));
    }

    /** The warps a search matches, by name or owner. An empty search matches everything. */
    private static List<Warp> matching(List<Warp> warps, String filter) {
        if (filter.isEmpty()) return warps;
        List<Warp> matches = new java.util.ArrayList<>();
        for (Warp warp : warps) {
            if (LegacyText.plain(warp.name()).toLowerCase().contains(filter)
                    || warp.ownerName().toLowerCase().contains(filter)) {
                matches.add(warp);
            }
        }
        return matches;
    }

    /* --------------------------------------------- list --------------------------------------------- */

    /**
     * Whether {@code player} is kept out by the warp's quest. Display only — the server decides
     * ({@code WarpService.questUnlocked}). Moderators pass, as they do there.
     */
    static boolean locked(Player player, Warp warp) {
        return warp.questLocked() && !ClientModerationCache.ALLOWED
                && !QuestGate.isCompleted(player, warp.requiredQuest());
    }

    private static void rebuildList(Player player, ScrollerView list, List<Warp> warps,
                                    UUID[] selected, boolean[] creating) {
        list.clearAllScrollViewChildren();
        // Rendered in the order it arrived. Official first and then the busiest is decided by
        // WarpData.sorted() on the server, which is the side that owns the visit counts — sorting
        // again here would only be a second opinion that could disagree with the detail panel.
        // The viewer's own pins go above all of it, each shown once.
        List<Warp> rest = new java.util.ArrayList<>(warps);
        List<Warp> pinned = HubPins.takePinned(player, HubWindowState.PinKind.WARPS, rest, Warp::id);
        if (!pinned.isEmpty()) {
            list.addScrollViewChild(heading("gui.roll_mod.hub.pinned"));
            for (Warp warp : pinned) list.addScrollViewChild(warpRow(warp, selected, creating));
            if (!rest.isEmpty()) list.addScrollViewChild(heading("gui.roll_mod.hub.warp.section.all"));
        }
        for (Warp warp : rest) list.addScrollViewChild(warpRow(warp, selected, creating));
    }

    private static Label heading(String key) {
        Label header = new Label();
        header.setText(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        header.layout(l -> l.widthPercent(100).height(LINE_H).marginBottom(1));
        header.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return header;
    }

    private static UIElement warpRow(Warp warp, UUID[] selected, boolean[] creating) {
        boolean isSelected = warp.id().equals(selected[0]);

        // A row is a container rather than a bare Label so the status badge has somewhere to sit:
        // the badge opens the row at a fixed width and the name takes the slack, which is what
        // keeps every name starting in the same column.
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(LIST_ROW_H)
                .marginBottom(2).paddingAll(2).paddingHorizontal(HubSection.ROW_INSET).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(
                isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
        // On the container, not the name: a click on the mark still selects the row, because
        // LdLib walks up from whatever was hit.
        row.addEventListener(UIEvents.CLICK, e -> {
            selected[0] = warp.id();
            creating[0] = false;
        });

        // Moderation at a glance, so a player picking somewhere to go does not have to open each
        // one to find out — and, for an Official warp, so that being pinned to the top is not the
        // only thing saying so, which says nothing once the list is scrolled. One badge per row
        // rather than a badge here and a colour chip at the far end: the status is one fact, and
        // splitting it across the row made the chip read as decoration.
        UIElement badge = new UIElement();
        badge.layout(l -> l.width(MARK).height(MARK));
        // A warp behind a quest always wears the lock, finished or not, and says which quest.
        boolean gated = warp.questLocked();
        badge.style(s -> s.background(gated ? LOCKED_ICON : warp.approval().icon()));
        List<Component> tip = gated
                ? List.of(warp.approval().line(), questLine(warp))
                : List.of(warp.approval().line());
        badge.addEventListener(UIEvents.HOVER_TOOLTIPS,
                e -> e.hoverTooltips = new HoverTooltips(tip, null, null, ItemStack.EMPTY));
        row.addChild(badge);

        Label name = new Label();
        name.setText(LegacyText.display(warp.name()));
        name.layout(l -> l.flexGrow(1).height(MARK));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        row.addChild(name);

        return row;
    }

    /* -------------------------------------------- detail -------------------------------------------- */

    private static void buildDetail(Player player, UIElement detail, @Nullable Warp warp,
                                    UUID[] selected, boolean[] creating, HubNotice notice,
                                    Runnable openChange) {
        if (warp == null) {
            Label empty = new Label();
            empty.setText(Component.translatable("gui.roll_mod.hub.warp.none")
                    .withStyle(ChatFormatting.GRAY));
            empty.layout(l -> l.widthPercent(100).height(LINE_H));
            empty.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
            detail.addChild(empty);
            return;
        }

        Label title = new Label();
        title.setText(LegacyText.display(warp.name()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));

        detail.addChild(title);

        // Directly under the name, because it is the thing worth knowing before deciding to go —
        // buried under the coordinates it read as a footnote.
        addApprovalLine(player, detail, warp);
        boolean locked = locked(player, warp);
        if (warp.questLocked()) {
            Label quest = new Label();
            quest.setText(questLine(warp).copy().withStyle(locked ? ChatFormatting.RED : ChatFormatting.GREEN));
            quest.layout(l -> l.widthPercent(100).marginBottom(2));
            quest.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            detail.addChild(quest);
        }

        // Where this warp is. The gap goes on whichever line opens the group rather than on `owner`
        // itself: an official warp names no creator — it belongs to the server, and who typed the
        // command is not the visitor's business — so on those the coordinates open the group and
        // hanging the spacing off the owner line would leave the approval note running straight
        // into them.
        if (!warp.isAdmin()) {
            detail.addChild(infoLine(Component.translatable("gui.roll_mod.hub.warp.owner",
                    value(warp.ownerName(), ChatFormatting.WHITE)), GROUP_GAP));
        }

        detail.addChild(infoLine(Component.translatable("gui.roll_mod.hub.warp.coordinates",
                        value(String.valueOf((int) warp.x()), ChatFormatting.AQUA),
                        value(String.valueOf((int) warp.y()), ChatFormatting.AQUA),
                        value(String.valueOf((int) warp.z()), ChatFormatting.AQUA)),
                warp.isAdmin() ? GROUP_GAP : 0));

        detail.addChild(infoLine(Component.translatable("gui.roll_mod.hub.warp.world",
                value(warp.dimension().toString(), ChatFormatting.AQUA)), 0));

        addVisitorLines(detail, warp);
        detail.addChild(priceRow(player, warp));

        Label description = new Label();
        description.setText(LegacyText.display(warp.description()));
        description.layout(l -> l.widthPercent(100).marginTop(6).marginLeft(DESCRIPTION_INSET));
        description.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        Button teleport = new Button();
        // Official warps travel at once, so the label must not promise a countdown that never runs.
        teleport.setText(warp.isAdmin()
                ? Component.translatable("gui.roll_mod.hub.warp.teleport.instant")
                : Component.translatable("gui.roll_mod.hub.warp.teleport",
                        WarpService.WARMUP_SECONDS));
        teleport.layout(l -> l.widthPercent(100).height(ROW_H).marginTopAuto());
        // Clickable even when locked: a locked warp answers with the server's own chat line in a
        // window, rather than a greyed button that does not say why. The server still decides.
        teleport.setOnClick(e -> {
            Component refusal = refusal(player, warp);
            if (refusal != null) {
                notice.show(refusal);
            } else {
                PacketDistributor.sendToServer(WarpActionPacket.teleport(warp.id()));
            }
        });

        detail.addChildren(description, teleport);

        Button change = new Button();
        change.setText(Component.translatable("gui.roll_mod.hub.change"));
        change.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        change.setOnClick(e -> openChange.run());
        detail.addChild(change);

        // Last, because `teleport` pins itself to the bottom with marginTopAuto and everything
        // added after it stacks below. Clearing the selection is all this does: the TICK guard sees
        // both signatures change on the next frame and rebuilds the placeholder and the row
        // highlight by itself.
        Button close = new Button();
        close.setText(Component.translatable("gui.roll_mod.hub.warp.close"));
        close.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        close.setOnClick(e -> {
            selected[0] = null;
            creating[0] = false;
        });
        detail.addChild(close);
    }

    /**
     * What the server would refuse this teleport with, word for word as its chat line
     * ({@code WarpService.requestTeleport}), or null when it should go through. Only the two locks
     * the client can see; balance and a missing dimension are still answered in chat.
     */
    @Nullable
    private static Component refusal(Player player, Warp warp) {
        if (!warp.canTeleport(player.getUUID())) {
            return Component.translatable("msg.roll_mod.warp.blocked").withStyle(ChatFormatting.RED);
        }
        if (locked(player, warp)) {
            return Component.translatable("msg.roll_mod.warp.questLocked",
                    QuestGate.title(false, warp.requiredQuest())).withStyle(ChatFormatting.RED);
        }
        return null;
    }

    /** The Change window's body: the pin toggle, and the owner's confirmed delete. */
    private static void buildChange(Player player, UIElement body, Warp warp, UUID[] selected,
                                    HubConfirm confirmer) {
        Label title = new Label();
        title.setText(LegacyText.display(warp.name()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
        body.addChild(title);

        Button pin = HubPins.toggle(player, HubWindowState.PinKind.WARPS, warp.id());
        pin.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        body.addChild(pin);

        if (warp.owner().equals(player.getUUID())) {
            Button delete = new Button();
            delete.setText(Component.translatable("gui.roll_mod.hub.warp.delete"));
            // Clear of the harmless toggle above it, as on homes.
            delete.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(12));
            delete.setOnClick(e -> confirmer.ask(
                    Component.translatable("gui.roll_mod.hub.confirm.warpDelete", LegacyText.display(warp.name())),
                    Component.translatable("gui.roll_mod.hub.confirm.warpDelete.button"),
                    () -> {
                        PacketDistributor.sendToServer(WarpActionPacket.delete(warp.id()));
                        if (warp.id().equals(selected[0])) selected[0] = null;
                    }));
            body.addChild(delete);
        }
    }

    /**
     * The moderation verdict. A blocked warp shows its owner the appeal text instead of a bare "no":
     * being stopped without being told why or who to ask would be the worst version of this.
     */
    private static void addApprovalLine(Player player, UIElement detail, Warp warp) {
        boolean blockedAndMine = warp.approval() == WarpApproval.DISAPPROVED
                && warp.owner().equals(player.getUUID());

        Label approval = new Label();
        // The owner of a blocked warp gets the appeal text instead of the general note: they need
        // the name to argue with, why it likely happened, and who to escalate to — not a description
        // of what "Rejected" means. It runs to several sentences, hence the wrapping label below.
        Component text = blockedAndMine
                ? Component.translatable("msg.roll_mod.warp.disapproved",
                        warp.moderator().isEmpty() ? "?" : warp.moderator())
                        .withStyle(style -> style.withColor(WarpService.APPEAL_COLOR))
                : Component.empty()
                        .append(warp.approval().line().copy().withStyle(ChatFormatting.BOLD))
                        .append(Component.literal(" — "))
                        .append(warp.approval().note())
                        .withStyle(style -> style.withColor(warp.approval().color()));

        approval.setText(text);
        approval.layout(l -> l.widthPercent(100).marginTop(4).marginBottom(2));
        approval.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        detail.addChild(approval);

        // Who vouched for it, shown to everyone rather than just the owner: the name is the point of
        // the badge. A warp approved before the moderator was recorded has nothing to name, so it
        // keeps the plain badge instead of claiming "?" reviewed it.
        if (warp.approval() == WarpApproval.APPROVED && !warp.moderator().isEmpty()) {
            Label vouched = new Label();
            // The approved green, taken from the state itself rather than hardcoded, so this line,
            // the badge and the note beside it can never drift apart. It reads as a vouch, which is
            // the opposite of the appeal line's warning pink directly above it in the rejected case.
            vouched.setText(Component.translatable("msg.roll_mod.warp.approvedBy", warp.moderator())
                    .withStyle(style -> style.withColor(warp.approval().color())));
            vouched.layout(l -> l.widthPercent(100).marginBottom(2));
            vouched.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            detail.addChild(vouched);
        }

        if (!warp.reports().isEmpty()) {
            Label reports = new Label();
            reports.setText(Component.translatable("gui.roll_mod.hub.warp.reports",
                    warp.reports().size()).withStyle(ChatFormatting.GRAY));
            reports.layout(l -> l.widthPercent(100).height(LINE_H));
            detail.addChild(reports);
        }
    }

    /**
     * How much use the warp gets. Two different measures deliberately: today's figure counts each
     * player once, so it cannot be inflated by warping in a loop and is what the list is ordered by,
     * while the total counts every trip ever made. An owner watching their own warp would read
     * either as broken without the tooltip, since their own visits are never counted.
     */
    private static void addVisitorLines(UIElement detail, Warp warp) {
        Label today = infoLine(Component.translatable("gui.roll_mod.hub.warp.visitors.today",
                value(String.valueOf(warp.visitorsToday()), ChatFormatting.WHITE)), GROUP_GAP);
        hint(today, "gui.roll_mod.hub.warp.visitors.today.tip");

        Label total = infoLine(Component.translatable("gui.roll_mod.hub.warp.visitors.total",
                value(String.valueOf(warp.visitsTotal()), ChatFormatting.WHITE)), 0);

        detail.addChildren(today, total);
    }

    /* --------------------------------------------- price -------------------------------------------- */

    /**
     * What the trip costs, opening the last group of the panel.
     *
     * <p>Always drawn, even when there is nothing to pay: "free" is worth saying out loud, and a row
     * that comes and goes would shuffle everything below it as the selection moves. Returned rather
     * than added so {@code buildDetail} can place it itself — the teleport button below pins itself
     * with {@code marginTopAuto}, and anything added after that lands under the button instead.
     *
     * <p>The amount stays a raw number, unlike the balance in {@code HomeTab}, which is abbreviated
     * through {@code EnergyFormatUtils}. A price is typed in exactly on the create form and paid
     * exactly on arrival, so showing the owner "1.5K" for what they entered as 1500 would be a
     * worse kind of tidy.
     */
    private static UIElement priceRow(Player player, Warp warp) {
        long price = warp.priceFor(player.getUUID());

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100)
                .height(LINE_H).marginTop(GROUP_GAP).gapColumn(3));

        UIElement icon = new UIElement();
        icon.layout(l -> l.width(LINE_H).height(LINE_H));
        icon.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/item/lp.png"))));

        Label label = new Label();
        label.setText(Component.translatable("gui.roll_mod.hub.warp.price", price > 0
                        ? value(String.valueOf(price), ChatFormatting.YELLOW)
                        : Component.translatable("gui.roll_mod.hub.warp.price.free")
                                .withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.flexGrow(1).height(LINE_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        row.addChildren(icon, label);
        return row;
    }

    /* ------------------------------------------- info lines ----------------------------------------- */

    /**
     * One fact about the warp: a gray label carrying a colored value.
     *
     * <p>The color split is done with the substitutions rather than by rewriting the lang keys —
     * a style set on a child wins over the one it inherits, so the translated text around the
     * argument stays gray while the argument itself keeps whatever {@link #value} gave it.
     *
     * @param marginTop the gap opening a group, or 0 for a line continuing the one above
     */
    private static Label infoLine(Component text, int marginTop) {
        Label line = new Label();
        line.setText(text.copy().withStyle(ChatFormatting.GRAY));
        line.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(marginTop));
        return line;
    }

    /** A value to drop into an {@link #infoLine}, in its own color. */
    private static Component value(String text, ChatFormatting color) {
        return Component.literal(text).withStyle(color);
    }

    /* ------------------------------------------ create form ----------------------------------------- */

    private static void buildCreateForm(Player player, UIElement detail, boolean[] creating) {
        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.warp.create"));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));

        // Empty fields say nothing about what belongs in them, so each one explains itself on hover.
        TextField name = new TextField();
        name.setText("");
        name.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(4));
        hint(name, "gui.roll_mod.hub.warp.field.name");

        TextField description = new TextField();
        description.setText("");
        description.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        hint(description, "gui.roll_mod.hub.warp.field.description");

        TextField price = new TextField();
        price.setNumbersOnlyLong(0, Long.MAX_VALUE);
        price.setText("0");
        price.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        hint(price, "gui.roll_mod.hub.warp.field.price");

        // Always the creator's current position: a warp you cannot stand in is not worth making, so
        // there is nothing to type here and nothing to get wrong.
        Label here = new Label();
        here.setText(Component.translatable("gui.roll_mod.hub.warp.here",
                        (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                        player.level().dimension().location().toString())
                .withStyle(ChatFormatting.GRAY));
        here.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(6));

        Button confirm = new Button();
        confirm.setText(Component.translatable("gui.roll_mod.hub.warp.confirm"));
        confirm.layout(l -> l.widthPercent(100).height(ROW_H).marginTopAuto());
        confirm.setOnClick(e -> {
            PacketDistributor.sendToServer(WarpActionPacket.create(
                    name.getText(), description.getText(), parse(price.getText())));
            creating[0] = false;
        });

        Button cancel = new Button();
        cancel.setText(Component.translatable("gui.roll_mod.hub.warp.cancel"));
        cancel.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        cancel.setOnClick(e -> creating[0] = false);

        detail.addChildren(title, name, description, price, here, confirm, cancel);
    }

    /* ------------------------------------------- plumbing ------------------------------------------- */

    /** Attaches a one-line explanation shown while the cursor is over the field. */
    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }

    @Nullable
    private static Warp find(List<Warp> warps, @Nullable UUID id) {
        if (id == null) return null;
        for (Warp warp : warps) {
            if (warp.id().equals(id)) return warp;
        }
        return null;
    }

    private static long parse(String text) {
        try {
            return Math.max(0, Long.parseLong(text.strip()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
