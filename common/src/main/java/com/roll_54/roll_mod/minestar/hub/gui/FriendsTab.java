package com.roll_54.roll_mod.minestar.hub.gui;

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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.friends.FriendRequest;
import com.roll_54.roll_mod.minestar.friends.FriendSort;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.network.packet.FriendActionPacket;
import com.roll_54.roll_mod.network.packet.TpaActionPacket;
import com.roll_54.roll_mod.util.Durations;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The hub's friends tab: the player's Minestar friends down the left, sorted by the drop-down above
 * them, and the selected friend's Minestar status and rewards on the right, with the ways to reach
 * them — tpa, tpahere, and an invite into the player's FTB party.
 *
 * <p>Reads {@link ClientFriendsCache}, which {@code SyncFriendsPacket} and
 * {@code SyncFriendDetailPacket} keep current — a variable-length list, so a client cache rather than
 * LdLib2's positional bindings, the same shape as the tpa tab. Nothing here syncs, so the tree is the
 * same on both sides; every rebuild happens in the client-side tick.
 *
 * <p>Rewards are shown by name only, coloured by rarity, with the description on hover.
 */
public final class FriendsTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 150;
    private static final int ROW_H = 16;
    private static final int REFRESH_W = 16;
    /** Between the sort drop-down and the list under it. */
    private static final int LIST_GAP = 3;
    /** How long the remove button waits for its second, confirming click. */
    private static final long CONFIRM_MILLIS = 3000L;

    /** The top tile of the panel: the face, the level under it, and name and status beside them. */
    private static final int HEAD = 32;
    private static final int HEAD_COLUMN_W = 44;
    private static final int TILE_PADDING = 4;
    private static final int COLOR_TILE = 0x30FFFFFF;
    /** Minestar's level colour. */
    private static final int COLOR_LEVEL = 0xFFFF6EC7;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x70FFFFFF;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private static final ChatFormatting[] RARITY_COLORS = {
            ChatFormatting.WHITE, ChatFormatting.AQUA, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD};

    private FriendsTab() {}

    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("friends");

        // Per-viewer client state.
        FriendSort[] sort = {FriendSort.SERVER};
        UUID[] selected = {null};

        UIElement body = new UIElement();
        // flexBasis(0) for the reason TpaTab gives: an auto basis would be the whole list and the
        // scrollers inside would never overflow.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(1));

        /* ---------------------------------------- left: friends ---------------------------------------- */

        UIElement sortRow = new UIElement();
        // 3px under it, as a margin of its own: the column's gap did not show between the two.
        sortRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(2)
                .flexShrink(0).marginBottom(LIST_GAP));

        Selector<FriendSort> sorter = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        sorter.setCandidateUIProvider(UIElementProvider.text(s -> s == null ? Component.empty() : s.title()));
        sorter.setCandidates(List.of(FriendSort.values()));
        sorter.setSelected(FriendSort.SERVER, false);
        sorter.layout(l -> l.flexGrow(1).height(ROW_H));
        sorter.setOnValueChanged(s -> {
            if (s != null) sort[0] = s;
        });
        hint(sorter, "gui.roll_mod.hub.friends.sort.tip");

        Button refresh = new Button();
        refresh.setText(Component.literal("⟳"));
        refresh.layout(l -> l.width(REFRESH_W).height(ROW_H).paddingAll(0));
        refresh.setOnClick(e -> PacketDistributor.sendToServer(FriendActionPacket.refresh()));
        hint(refresh, "gui.roll_mod.hub.friends.refresh");

        sortRow.addChildren(sorter, refresh);

        ScrollerView list = scroller();

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100));
        left.addChildren(sortRow, list);

        /* ---------------------------------------- right: detail ---------------------------------------- */

        // One tile across the top: face and level on the left, name and status beside them. Rebuilt
        // with the panel; hidden while nobody is picked. Hiding it is safe — it holds no sync value
        // and no TICK of its own.
        UIElement tile = new UIElement();
        tile.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexShrink(0)
                .paddingAll(TILE_PADDING).gapColumn(6).marginBottom(4));
        tile.style(s -> s.background(new ColorRectTexture(COLOR_TILE)));
        tile.setDisplay(false);

        ScrollerView detail = scroller();

        Button tpa = action("gui.roll_mod.hub.friends.tpa");
        tpa.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(TpaActionPacket.request(selected[0], false));
        });
        Button tpaHere = action("gui.roll_mod.hub.friends.tpahere");
        tpaHere.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(TpaActionPacket.request(selected[0], true));
        });
        Button invite = action("gui.roll_mod.hub.friends.invite");
        invite.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(FriendActionPacket.invite(selected[0]));
        });
        // Add for someone who is not a friend yet, remove for someone who is. Removing takes a
        // second click within CONFIRM_MILLIS — a friendship is not something to lose to a misclick.
        long[] removeArmedAt = {0L};
        Button friendship = action("gui.roll_mod.hub.friends.add");
        friendship.setOnClick(e -> {
            UUID target = selected[0];
            if (target == null) return;
            if (!isFriend(target)) {
                PacketDistributor.sendToServer(FriendActionPacket.add(target));
            } else if (System.currentTimeMillis() - removeArmedAt[0] < CONFIRM_MILLIS) {
                removeArmedAt[0] = 0L;
                PacketDistributor.sendToServer(FriendActionPacket.remove(target));
            } else {
                removeArmedAt[0] = System.currentTimeMillis();
            }
        });
        hint(tpa, "gui.roll_mod.hub.friends.tpa.tip");
        hint(tpaHere, "gui.roll_mod.hub.friends.tpahere.tip");
        hint(invite, "gui.roll_mod.hub.friends.invite.tip");

        // Two rows: the teleports, then the team invite and the friendship button — four captions
        // side by side did not fit the panel.
        UIElement teleports = new UIElement();
        teleports.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
        teleports.addChildren(tpa, tpaHere);
        UIElement social = new UIElement();
        social.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
        social.addChildren(invite, friendship);

        UIElement actions = new UIElement();
        actions.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).height(2 * ROW_H + 3)
                .flexShrink(0).marginTop(2).gapRow(3));
        actions.addChildren(teleports, social);

        UIElement right = new UIElement();
        right.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1)
                .heightPercent(100).paddingAll(PADDING).gapRow(2));
        right.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        right.addChildren(tile, detail, actions);

        // The divider between the list and the panel: drag to resize, double-click to reset.
        body.addChildren(left, HubSplit.handle("friends", left, LIST_W, player.level().isClientSide()), right);
        root.addChildren(HubSection.header("friends", section), body, section.overlay());

        /* --------------------------------------------- tick -------------------------------------------- */

        String[] listSig = {null};
        String[] detailSig = {null};
        String[] friendshipShown = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            int version = ClientFriendsCache.VERSION;
            boolean onlineOnly = sort[0] == FriendSort.ONLINE_ONLY;
            boolean requestsView = sort[0] == FriendSort.REQUESTS;
            List<FriendEntry> entries = requestsView ? requestEntries()
                    : onlineOnly ? onlinePlayers() : new ArrayList<>(ClientFriendsCache.ENTRIES);
            // Requests keep their own order: incoming first, then outgoing, each newest first.
            if (!requestsView) entries.sort(sort[0].comparator());

            // A friend who dropped out of the list is no longer selectable.
            FriendEntry current = find(entries, selected[0]);
            if (current == null) selected[0] = null;

            // The roster is part of it in the online view, which lists whoever is on the server.
            String sig = version + "|" + sort[0] + "|" + selected[0]
                    + (onlineOnly ? "|" + ClientTpaCache.ONLINE.hashCode() : "");
            if (!sig.equals(listSig[0])) {
                listSig[0] = sig;
                list.clearAllScrollViewChildren();
                list.clearLayoutCache();
                FriendsService.Status status = ClientFriendsCache.STATUS;
                // The online view needs no Minestar to list anyone — only to add them.
                if (status != FriendsService.Status.OK && !onlineOnly) {
                    list.addScrollViewChild(note("gui.roll_mod.hub.friends.status." + status.name().toLowerCase(java.util.Locale.ROOT)));
                } else if (requestsView) {
                    fillRequests(list, selected[0], id -> {
                        selected[0] = id;
                        PacketDistributor.sendToServer(FriendActionPacket.select(id));
                    });
                } else if (entries.isEmpty()) {
                    list.addScrollViewChild(note(onlineOnly
                            ? "gui.roll_mod.hub.friends.online_only.empty" : "gui.roll_mod.hub.friends.empty"));
                } else {
                    String group = null;
                    for (FriendEntry entry : entries) {
                        // The server sort reads as groups, so each pack gets a heading.
                        if (sort[0] == FriendSort.SERVER) {
                            String heading = entry.here() ? "\u0001here" : entry.online() ? entry.pack() : "\u0001offline";
                            if (!heading.equals(group)) {
                                group = heading;
                                list.addScrollViewChild(groupHeading(entry));
                            }
                        }
                        list.addScrollViewChild(friendRow(entry, onlineOnly && isFriend(entry.id()),
                                entry.id().equals(selected[0]), () -> {
                            selected[0] = entry.id();
                            PacketDistributor.sendToServer(FriendActionPacket.select(entry.id()));
                        }));
                    }
                }
            }

            // Buttons: reachable only when the friend is on this server; invite also needs them to
            // be outside the party, and is hidden altogether in the team sort.
            boolean here = current != null && current.here();
            tpa.setActive(here);
            tpaHere.setActive(here);
            invite.setActive(here && !current.teammate());
            boolean showInvite = sort[0] != FriendSort.TEAM;
            if (invite.isDisplayed() != showInvite) invite.setDisplay(showInvite);

            boolean friend = current != null && isFriend(current.id());
            boolean armed = friend && System.currentTimeMillis() - removeArmedAt[0] < CONFIRM_MILLIS;
            String friendshipKey = !friend ? "gui.roll_mod.hub.friends.add"
                    : armed ? "gui.roll_mod.hub.friends.remove.confirm" : "gui.roll_mod.hub.friends.remove";
            if (!friendshipKey.equals(friendshipShown[0])) {
                friendshipShown[0] = friendshipKey;
                friendship.setText(Component.translatable(friendshipKey));
            }
            // Adding needs Minestar; removing a friend needs a friend.
            friendship.setActive(current != null
                    && (friend || ClientFriendsCache.STATUS != FriendsService.Status.UNAVAILABLE));

            String dSig = version + "|" + selected[0];
            if (!dSig.equals(detailSig[0])) {
                detailSig[0] = dSig;
                fillTile(tile, current);
                detail.clearAllScrollViewChildren();
                detail.clearLayoutCache();
                fillDetail(detail, current);
            }
        });

        return root;
    }

    /* -------------------------------------------- detail -------------------------------------------- */

    static void fillDetail(ScrollerView detail, @Nullable FriendEntry entry) {
        if (entry == null) {
            detail.addScrollViewChild(note("gui.roll_mod.hub.friends.pick"));
            return;
        }
        FriendDetail profile = ClientFriendsCache.DETAILS.get(entry.id());
        if (profile == null) {
            detail.addScrollViewChild(note(ClientFriendsCache.FAILED.contains(entry.id())
                    ? "gui.roll_mod.hub.friends.detail.failed" : "gui.roll_mod.hub.friends.detail.loading"));
            return;
        }

        if (!profile.groupName().isEmpty()) {
            MutableComponent group = Component.literal(profile.groupName());
            if (profile.groupColor() >= 0) group = group.withColor(profile.groupColor());
            detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.friends.group", group)));
        }
        detail.addScrollViewChild(line(profile.prime()
                ? Component.translatable("gui.roll_mod.hub.friends.prime.until",
                        DATE.format(Instant.ofEpochMilli(profile.primeUntilMillis()))).withStyle(ChatFormatting.GOLD)
                : Component.translatable("gui.roll_mod.hub.friends.prime.none").withStyle(ChatFormatting.GRAY)));
        detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.friends.playtime", profile.playtimeHours())));

        detail.addScrollViewChild(note("gui.roll_mod.hub.friends.rewards"));
        if (profile.rewards().isEmpty()) {
            detail.addScrollViewChild(note("gui.roll_mod.hub.friends.rewards.none"));
        }
        for (FriendDetail.Reward reward : profile.rewards()) {
            int rarity = Math.floorMod(reward.rarity(), RARITY_COLORS.length);
            Label label = line(Component.literal("• " + reward.title()).withStyle(RARITY_COLORS[rarity]));
            List<Component> tip = new ArrayList<>();
            tip.add(Component.translatable("gui.roll_mod.hub.friends.rarity." + rarity).withStyle(RARITY_COLORS[rarity]));
            if (!reward.description().isEmpty()) tip.add(Component.literal(reward.description()).withStyle(ChatFormatting.GRAY));
            label.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    tip, null, null, ItemStack.EMPTY));
            detail.addScrollViewChild(label);
        }
    }

    /**
     * The panel's top tile: the friend's face with their level under it in pink — hover it for the XP
     * still missing — and their name and status to the right.
     */
    private static void fillTile(UIElement tile, @Nullable FriendEntry entry) {
        tile.clearAllChildren();
        tile.setDisplay(entry != null);
        if (entry == null) return;
        FriendDetail profile = ClientFriendsCache.DETAILS.get(entry.id());

        UIElement headColumn = new UIElement();
        headColumn.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(HEAD_COLUMN_W).flexShrink(0)
                .alignItems(AlignItems.CENTER).gapRow(2));

        PlayerHeadElement head = new PlayerHeadElement(entry.id(), entry.name(), entry.skin(), entry.skinSignature());
        head.layout(l -> l.width(HEAD).height(HEAD));

        Label level = new Label();
        level.setText(profile == null ? Component.literal("…")
                : Component.translatable("gui.roll_mod.hub.friends.level", profile.level()));
        level.layout(l -> l.widthPercent(100));
        level.textStyle(t -> t.textColor(COLOR_LEVEL).textAlignHorizontal(Horizontal.CENTER)
                .textWrap(TextWrap.HIDE));
        if (profile != null) {
            level.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    Component.translatable("gui.roll_mod.hub.friends.xp.next", profile.xpToNext(), profile.level() + 1),
                    Component.translatable("gui.roll_mod.hub.friends.xp.progress",
                            profile.levelXp(), profile.levelXpNeeded()).withStyle(ChatFormatting.GRAY)),
                    null, null, ItemStack.EMPTY));
        }
        headColumn.addChild(head);
        headColumn.addChild(level);
        if (profile != null) {
            // Hours played, under the level.
            Label playtime = new Label();
            playtime.setText(Component.translatable("gui.roll_mod.hub.friends.playtime.short", profile.playtimeHours())
                    .withStyle(ChatFormatting.GRAY));
            playtime.layout(l -> l.widthPercent(100));
            playtime.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textWrap(TextWrap.HIDE));
            playtime.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    Component.translatable("gui.roll_mod.hub.friends.playtime", profile.playtimeHours())),
                    null, null, ItemStack.EMPTY));
            headColumn.addChild(playtime);
        }

        UIElement text = new UIElement();
        text.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).gapRow(2));
        text.addChildren(line(Component.literal(entry.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)),
                line(status(entry, System.currentTimeMillis())));

        tile.addChildren(headColumn, text);
    }

    /** Online here / online in a pack / offline, last seen some time ago. */
    static Component status(FriendEntry entry, long now) {
        if (entry.here()) {
            return Component.translatable("gui.roll_mod.hub.friends.online.here").withStyle(ChatFormatting.GREEN);
        }
        if (entry.online()) {
            return entry.pack().isEmpty()
                    ? Component.translatable("gui.roll_mod.hub.friends.online").withStyle(ChatFormatting.AQUA)
                    : Component.translatable("gui.roll_mod.hub.friends.online.pack",
                            String.join(", ", entry.playing())).withStyle(ChatFormatting.AQUA);
        }
        if (entry.lastSeenMillis() < 0) {
            return Component.translatable("gui.roll_mod.hub.friends.offline.never").withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("gui.roll_mod.hub.friends.offline.seen",
                Durations.format(Math.max(0, now - entry.lastSeenMillis()))).withStyle(ChatFormatting.GRAY);
    }

    /* --------------------------------------------- rows --------------------------------------------- */

    /** {@code markFriend}: the online view lists strangers too, so friends among them get a heart. */
    private static UIElement friendRow(FriendEntry entry, boolean markFriend, boolean isSelected, Runnable onClick) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(1).paddingHorizontal(HubSection.ROW_INSET).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));

        ChatFormatting color = entry.here() ? ChatFormatting.GREEN
                : entry.online() ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        MutableComponent text = Component.literal(entry.name()).withStyle(color);
        if (entry.teammate()) text.append(Component.literal(" ★").withStyle(ChatFormatting.GOLD));
        if (markFriend) text.append(Component.literal(" ❤").withStyle(ChatFormatting.LIGHT_PURPLE));

        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        row.addChild(label);

        row.addEventListener(UIEvents.CLICK, e -> onClick.run());
        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.literal(entry.name()), status(entry, System.currentTimeMillis())),
                null, null, ItemStack.EMPTY));
        return row;
    }

    /** The server sort's group heading: this server, a pack's name, or offline. */
    private static UIElement groupHeading(FriendEntry entry) {
        Component text = entry.here()
                ? Component.translatable("gui.roll_mod.hub.friends.group.here")
                : entry.online()
                ? (entry.pack().isEmpty()
                        ? Component.translatable("gui.roll_mod.hub.friends.group.unknown")
                        : Component.literal(entry.pack()))
                : Component.translatable("gui.roll_mod.hub.friends.group.offline");
        Label label = new Label();
        label.setText(text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        label.layout(l -> l.widthPercent(100).height(ROW_H - 4));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        return label;
    }

    /* -------------------------------------------- helpers ------------------------------------------- */

    static ScrollerView scroller() {
        ScrollerView view = new ScrollerView();
        view.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        // COLUMN, or every line sits at y=0 and the scroller never sees anything to scroll.
        view.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        return view;
    }

    static Button action(String key) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        button.setActive(false);
        return button;
    }

    /** A wrapping line of the detail panel. */
    static Label line(Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.widthPercent(100).marginBottom(2));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

    /** A gray line: a heading, or the text that stands in for an empty list. */
    static Label note(String key) {
        Label label = line(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.marginTop(2));
        return label;
    }

    static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }

    /**
     * The requests view: incoming requests with accept and decline, then outgoing ones with cancel,
     * each under a heading that counts them. The ✔ / ✖ buttons swallow their click, so answering a
     * request does not also select its row.
     */
    private static void fillRequests(ScrollerView list, @Nullable UUID selected, java.util.function.Consumer<UUID> onSelect) {
        List<FriendRequest> incoming = ClientFriendsCache.INCOMING;
        List<FriendRequest> outgoing = ClientFriendsCache.OUTGOING;
        if (incoming.isEmpty() && outgoing.isEmpty()) {
            list.addScrollViewChild(note("gui.roll_mod.hub.friends.requests.empty"));
            return;
        }
        long now = System.currentTimeMillis();
        if (!incoming.isEmpty()) {
            list.addScrollViewChild(heading(Component.translatable("gui.roll_mod.hub.friends.requests.incoming", incoming.size())));
            for (FriendRequest request : incoming) {
                list.addScrollViewChild(requestRow(request, now, request.id().equals(selected), onSelect,
                        small("✔", ChatFormatting.GREEN, "gui.roll_mod.hub.friends.requests.accept",
                                () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), true))),
                        small("✖", ChatFormatting.RED, "gui.roll_mod.hub.friends.requests.decline",
                                () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), false)))));
            }
        }
        if (!outgoing.isEmpty()) {
            list.addScrollViewChild(heading(Component.translatable("gui.roll_mod.hub.friends.requests.outgoing", outgoing.size())));
            for (FriendRequest request : outgoing) {
                list.addScrollViewChild(requestRow(request, now, request.id().equals(selected), onSelect,
                        small("✖", ChatFormatting.RED, "gui.roll_mod.hub.friends.requests.cancel",
                                () -> PacketDistributor.sendToServer(FriendActionPacket.cancel(request.id())))));
            }
        }
    }

    private static UIElement requestRow(FriendRequest request, long now, boolean isSelected,
                                        java.util.function.Consumer<UUID> onSelect, Button... buttons) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(1).paddingHorizontal(HubSection.ROW_INSET).gapColumn(2));
        row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));

        boolean here = ClientTpaCache.ONLINE.containsKey(request.id());
        Label label = new Label();
        label.setText(Component.literal(request.name()).withStyle(here ? ChatFormatting.GREEN : ChatFormatting.WHITE));
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        row.addChild(label);
        row.addChildren(buttons);

        row.addEventListener(UIEvents.CLICK, e -> onSelect.accept(request.id()));
        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                Component.literal(request.name()),
                Component.translatable("gui.roll_mod.hub.friends.requests.sent",
                        Durations.format(Math.max(0, now - request.sentAtMillis()))).withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));
        return row;
    }

    /** A square one-glyph button whose click stays its own. */
    static Button small(String glyph, ChatFormatting color, String tip, Runnable action) {
        Button button = new Button();
        button.setText(Component.literal(glyph).withStyle(color));
        button.layout(l -> l.width(ROW_H - 2).height(ROW_H - 2).paddingAll(0).flexShrink(0));
        button.setOnClick(e -> {
            action.run();
            e.stopPropagation();
        });
        hint(button, tip);
        return button;
    }

    static Label heading(Component text) {
        Label label = new Label();
        label.setText(text.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        label.layout(l -> l.widthPercent(100).height(ROW_H - 4));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        return label;
    }

    /** The requests as entries, so selecting one shows the same tile and buttons as a friend. */
    private static List<FriendEntry> requestEntries() {
        List<FriendEntry> entries = new ArrayList<>();
        for (List<FriendRequest> requests : List.of(ClientFriendsCache.INCOMING, ClientFriendsCache.OUTGOING)) {
            for (FriendRequest request : requests) {
                boolean here = ClientTpaCache.ONLINE.containsKey(request.id());
                entries.add(new FriendEntry(request.id(), request.name(), here, -1L, List.of())
                        .withServerView(here, false, "", ""));
            }
        }
        return entries;
    }

    /**
     * Everyone on this server, from the roster the tpa tab already keeps ({@link ClientTpaCache} —
     * the viewer excluded). A friend appears as their friend entry; anyone else as a bare one, online
     * here, whose face the client already has.
     */
    private static List<FriendEntry> onlinePlayers() {
        List<FriendEntry> players = new ArrayList<>();
        for (Map.Entry<UUID, String> player : ClientTpaCache.ONLINE.entrySet()) {
            FriendEntry friend = find(ClientFriendsCache.ENTRIES, player.getKey());
            players.add(friend != null ? friend
                    : new FriendEntry(player.getKey(), player.getValue(), true, -1L, List.of())
                            .withServerView(true, false, "", ""));
        }
        return players;
    }

    private static boolean isFriend(UUID id) {
        return find(ClientFriendsCache.ENTRIES, id) != null;
    }

    @Nullable
    private static FriendEntry find(List<FriendEntry> entries, @Nullable UUID id) {
        if (id == null) return null;
        for (FriendEntry entry : entries) {
            if (entry.id().equals(id)) return entry;
        }
        return null;
    }
}
