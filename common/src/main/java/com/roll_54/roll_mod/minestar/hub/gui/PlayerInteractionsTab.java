package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.currency.model.CurrencyOffer;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.currency.model.MoneyRequest;
import com.roll_54.roll_mod.economy.network.client.ClientCurrencyHolder;
import com.roll_54.roll_mod.economy.network.client.ClientMoneyCache;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.friends.FriendRequest;
import com.roll_54.roll_mod.minestar.friends.FriendsService;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import com.roll_54.roll_mod.minestar.interactions.ClientInteractionCache;
import com.roll_54.roll_mod.minestar.interactions.InteractionSettings;
import com.roll_54.roll_mod.network.packet.FriendActionPacket;
import com.roll_54.roll_mod.network.packet.InteractionSettingPacket;
import com.roll_54.roll_mod.network.packet.MoneyActionPacket;
import com.roll_54.roll_mod.network.packet.TpaActionPacket;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The hub's player interactions tab — teleports and friends in one place.
 *
 * <p>Down the left: what is waiting for the viewer's answer (teleport, friend and money requests),
 * then the players the viewer pinned (Configure → Pin, kept client-side by {@link HubPins}), then
 * everyone online here, then friends elsewhere, with the argument-less trips (rtp, back, spawn)
 * under the list. On the right, for the chosen player: a full-width header with their face, name,
 * Minestar level and group, then a column of every action on them beside their Minestar profile.
 *
 * <p>Money opens a small window: an amount, and a switch for whether it is sent ({@link
 * com.roll_54.roll_mod.economy.api.CurrencyOfferService}, held until they accept) or asked for
 * ({@link com.roll_54.roll_mod.economy.api.MoneyRequestService}, taken only if they agree).
 *
 * <p>Like the tabs it replaces it reads client caches only — {@link ClientTpaCache}, {@link
 * ClientFriendsCache}, {@link ClientMoneyCache} — and holds no sync value, so the tree is the same on
 * both sides and every rebuild happens in the client tick.
 *
 * <p>{@link #focus} selects a player from elsewhere in the hub (a leaderboard row); someone who is in
 * none of the lists is shown under a "viewed" heading of their own.
 */
public final class PlayerInteractionsTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 150;
    private static final int ROW_H = 16;
    private static final int ACTIONS_W = 120;
    private static final int HEAD = 32;
    /** How long the remove button waits for its second, confirming click. */
    private static final long CONFIRM_MILLIS = 3000L;
    /** A profile that has not arrived after this long is asked for again. */
    private static final long RESELECT_MILLIS = 1500L;

    private static final int CONFIGURE_W = 280;
    private static final int CONFIGURE_H = 180;
    private static final int MONEY_W = 260;
    private static final int MONEY_H = 160;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x70FFFFFF;
    private static final int COLOR_LINE = 0x60FFFFFF;
    /** Minestar's level colour, as the friends tab has it. */
    private static final int COLOR_LEVEL = 0xFFFF6EC7;

    /**
     * A selection asked for from elsewhere in the hub, applied by the next tick. Client copy only —
     * written from a click, which never fires on a dedicated server.
     */
    private static UUID focusId;
    private static String focusName;

    private PlayerInteractionsTab() {}

    /** Selects {@code id} the next time this tab ticks, adding them to the list if they are not in it. */
    public static void focus(UUID id, String name) {
        focusName = name;
        focusId = id;
    }

    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("interactions");

        // Per-viewer client state.
        UUID[] selected = {null};
        FriendEntry[] viewed = {null};
        String[] filter = {""};
        long[] selectSentAt = {0L};

        java.util.function.Consumer<UUID> pick = id -> {
            selected[0] = id;
            selectSentAt[0] = System.currentTimeMillis();
            PacketDistributor.sendToServer(FriendActionPacket.select(id));
        };

        UIElement body = new UIElement();
        // flexBasis(0) for the reason TpaTab gives: an auto basis would be the whole list and the
        // scrollers inside would never overflow.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(1));

        /* ----------------------------------------- left: players ---------------------------------------- */

        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H).flexShrink(0).marginBottom(3));
        search.setTextResponder(text -> filter[0] = text == null ? "" : text.strip().toLowerCase(Locale.ROOT));

        ScrollerView list = FriendsTab.scroller();

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100));
        // Random, back and spawn moved to the home tab's side panel; the list has the full height.
        left.addChildren(search, list);

        /* ----------------------------------------- right: player ---------------------------------------- */

        // The header across the whole panel: face, then name with the level beside it, the group and
        // status under them — and a line under all of it, the full width of the panel.
        UIElement top = new UIElement();
        top.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexShrink(0).gapColumn(6)
                .alignItems(AlignItems.CENTER));
        UIElement rule = new UIElement();
        rule.layout(l -> l.widthPercent(100).height(1).flexShrink(0).marginTop(3).marginBottom(4));
        rule.style(s -> s.background(new ColorRectTexture(COLOR_LINE)));

        // The actions: fixed buttons, then whatever is pending between the two players.
        Button tpa = FriendsTab.action("gui.roll_mod.hub.interactions.tpa");
        tpa.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(TpaActionPacket.request(selected[0], false));
        });
        Button tpaHere = FriendsTab.action("gui.roll_mod.hub.interactions.tpahere");
        tpaHere.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(TpaActionPacket.request(selected[0], true));
        });
        long[] removeArmedAt = {0L};
        Button friendship = FriendsTab.action("gui.roll_mod.hub.friends.add");
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
        Button invite = FriendsTab.action("gui.roll_mod.hub.interactions.invite");
        invite.setOnClick(e -> {
            if (selected[0] != null) PacketDistributor.sendToServer(FriendActionPacket.invite(selected[0]));
        });
        Button money = FriendsTab.action("gui.roll_mod.hub.interactions.money");
        FriendsTab.hint(tpa, "gui.roll_mod.hub.friends.tpa.tip");
        FriendsTab.hint(tpaHere, "gui.roll_mod.hub.friends.tpahere.tip");
        FriendsTab.hint(invite, "gui.roll_mod.hub.friends.invite.tip");
        FriendsTab.hint(money, "gui.roll_mod.hub.interactions.money.tip");

        UIElement pending = new UIElement();
        pending.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(3));
        // Pending rows take whatever height the buttons leave and scroll, so a long list can never push
        // Configure off the bottom of the column.
        ScrollerView pendingScroll = FriendsTab.scroller();
        pendingScroll.layout(l -> l.marginTop(3));
        pendingScroll.addScrollViewChild(pending);

        UIElement actions = new UIElement();
        actions.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(ACTIONS_W).flexShrink(0).heightPercent(100)
                .gapRow(3));
        for (Button button : List.of(tpa, tpaHere, friendship, invite, money)) {
            // Stacked, so each one is a row tall and the column's width wide.
            button.layout(l -> l.widthPercent(100).height(ROW_H).flexGrow(0).flexShrink(0).flexBasis(ROW_H));
            actions.addChild(button);
        }
        actions.addChild(pendingScroll);

        // Always at the foot of the column, below the pending scroller: this viewer's standing choices
        // about the chosen player.
        Button configure = FriendsTab.action("gui.roll_mod.hub.interactions.configure");
        configure.layout(l -> l.widthPercent(100).height(ROW_H).flexGrow(0).flexShrink(0).flexBasis(ROW_H));
        FriendsTab.hint(configure, "gui.roll_mod.hub.interactions.configure.tip");
        actions.addChild(configure);

        ScrollerView info = FriendsTab.scroller();
        info.layout(l -> l.heightPercent(100));

        UIElement lower = new UIElement();
        lower.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1).gapColumn(6));
        lower.addChildren(actions, info);

        Label pickNote = FriendsTab.note("gui.roll_mod.hub.interactions.pick");

        UIElement right = new UIElement();
        right.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1)
                .heightPercent(100).paddingAll(PADDING));
        right.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        right.addChildren(pickNote, top, rule, lower);

        /* ------------------------------------------- money window ------------------------------------------ */

        boolean[] asking = {false};
        Label moneyTarget = new Label();
        Label moneySummary = new Label();
        TextField amount = VendorUIHelper.intField("", t -> {});
        Switch receive = new Switch();
        HubSection.Floating[] moneyWindow = new HubSection.Floating[1];
        moneyWindow[0] = HubSection.popup(Component.translatable("gui.roll_mod.hub.interactions.money.title"),
                MONEY_W, MONEY_H, content -> {
                    moneyTarget.layout(l -> l.widthPercent(100).height(ROW_H - 4));
                    moneyTarget.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

                    UIElement amountRow = row(ROW_H);
                    Label amountLabel = new Label();
                    amountLabel.setText(Component.translatable("gui.roll_mod.hub.interactions.money.amount"));
                    amountLabel.layout(l -> l.width(60).heightPercent(100).flexShrink(0));
                    amountLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
                    amount.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    amountRow.addChildren(amountLabel, amount);

                    UIElement receiveRow = row(ROW_H);
                    Label receiveLabel = new Label();
                    receiveLabel.setText(Component.translatable("gui.roll_mod.hub.interactions.money.receive"));
                    receiveLabel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    receiveLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
                    FriendsTab.hint(receiveLabel, "gui.roll_mod.hub.interactions.money.receive.tip");
                    receive.setOn(false);
                    receive.setOnSwitchChanged(on -> asking[0] = on);
                    receive.layout(l -> l.width(28).height(14).flexShrink(0));
                    receiveRow.addChildren(receiveLabel, receive);

                    moneySummary.layout(l -> l.widthPercent(100));
                    moneySummary.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

                    UIElement buttons = row(ROW_H);
                    Button confirm = new Button();
                    confirm.setText(Component.translatable("gui.roll_mod.hub.interactions.money.confirm"));
                    confirm.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    confirm.setOnClick(e -> {
                        long value = VendorUIHelper.parseLong(amount.getText(), 0);
                        if (selected[0] == null || value <= 0) return;
                        PacketDistributor.sendToServer(MoneyActionPacket.send(selected[0], value, asking[0]));
                        moneyWindow[0].close();
                    });
                    Button cancel = new Button();
                    cancel.setText(Component.translatable("gui.roll_mod.hub.interactions.money.cancel"));
                    cancel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    cancel.setOnClick(e -> moneyWindow[0].close());
                    buttons.addChildren(confirm, cancel);

                    content.addChildren(moneyTarget, amountRow, receiveRow, moneySummary, buttons);
                    content.layout(l -> l.gapRow(4));
                }).windowed().noCloseButton();
        /* ---------------------------------------- configure window ---------------------------------------- */

        Label configureTarget = new Label();
        List<InteractionSettings.Flag> flagOrder = List.of(InteractionSettings.Flag.AUTO_MONEY,
                InteractionSettings.Flag.IGNORE, InteractionSettings.Flag.AUTO_TPA, InteractionSettings.Flag.AUTO_TPAHERE);
        Map<InteractionSettings.Flag, Switch> switches = new java.util.EnumMap<>(InteractionSettings.Flag.class);
        Switch pinSwitch = new Switch();
        HubSection.Floating configureWindow = HubSection.popup(
                Component.translatable("gui.roll_mod.hub.interactions.configure.title"),
                CONFIGURE_W, CONFIGURE_H, content -> {
                    configureTarget.layout(l -> l.widthPercent(100).height(ROW_H - 4));
                    configureTarget.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
                    content.addChild(configureTarget);
                    // Pinning is the viewer's own list order, not a setting the server keeps.
                    UIElement pinLine = row(ROW_H);
                    Label pinCaption = new Label();
                    pinCaption.setText(Component.translatable("gui.roll_mod.hub.interactions.configure.pin"));
                    pinCaption.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    pinCaption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
                    FriendsTab.hint(pinCaption, "gui.roll_mod.hub.interactions.configure.pin.tip");
                    pinSwitch.setOn(false);
                    pinSwitch.layout(l -> l.width(28).height(14).flexShrink(0));
                    pinSwitch.setOnSwitchChanged(on -> {
                        if (selected[0] != null) HubPins.set(player, HubWindowState.PinKind.PLAYERS, selected[0], on);
                    });
                    pinLine.addChildren(pinCaption, pinSwitch);
                    content.addChild(pinLine);
                    for (InteractionSettings.Flag flag : flagOrder) {
                        String key = "gui.roll_mod.hub.interactions.configure." + flag.name().toLowerCase(Locale.ROOT);
                        UIElement line = row(ROW_H);
                        Label caption = new Label();
                        caption.setText(Component.translatable(key));
                        caption.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                        caption.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
                        FriendsTab.hint(caption, key + ".tip");
                        Switch toggle = new Switch();
                        toggle.setOn(false);
                        toggle.layout(l -> l.width(28).height(14).flexShrink(0));
                        toggle.setOnSwitchChanged(on -> {
                            if (selected[0] != null) {
                                PacketDistributor.sendToServer(new InteractionSettingPacket(selected[0], flag, on));
                            }
                        });
                        switches.put(flag, toggle);
                        line.addChildren(caption, toggle);
                        content.addChild(line);
                    }
                    content.layout(l -> l.gapRow(4));
                }).windowed();
        configure.setOnClick(e -> {
            if (selected[0] == null) return;
            configureWindow.open();
        });

        money.setOnClick(e -> {
            if (selected[0] == null) return;
            amount.setText("");
            moneyWindow[0].open();
        });

        // The divider between the list and the panel: drag to resize, double-click to reset.
        body.addChildren(left, HubSplit.handle("interactions", left, LIST_W, player.level().isClientSide()), right);
        root.addChildren(HubSection.header("interactions", section), body, section.overlay(), moneyWindow[0].root(),
                configureWindow.root());

        /* --------------------------------------------- tick -------------------------------------------- */

        String[] listSig = {null};
        String[] detailSig = {null};
        String[] pendingSig = {null};
        String[] friendshipShown = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            if (focusId != null) {
                UUID id = focusId;
                String name = focusName == null ? "?" : focusName;
                focusId = null;
                viewed[0] = new FriendEntry(id, name, false, -1L, List.of());
                pick.accept(id);
            }

            List<FriendEntry> online = online();
            List<FriendEntry> elsewhere = new ArrayList<>();
            for (FriendEntry friend : ClientFriendsCache.ENTRIES) {
                if (!ClientTpaCache.ONLINE.containsKey(friend.id())) elsewhere.add(friend);
            }
            elsewhere.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1)
                    : String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
            // The viewed player stops being "viewed" once they are in one of the real lists.
            if (viewed[0] != null && (find(online, viewed[0].id()) != null || find(elsewhere, viewed[0].id()) != null)) {
                viewed[0] = null;
            }

            FriendEntry current = find(online, selected[0]);
            if (current == null) current = find(elsewhere, selected[0]);
            if (current == null && viewed[0] != null && viewed[0].id().equals(selected[0])) current = viewed[0];
            if (current == null) current = requestEntry(selected[0]);
            if (current == null) selected[0] = null;

            long now = System.currentTimeMillis();
            String sig = ClientFriendsCache.VERSION + "|" + ClientMoneyCache.VERSION + "|" + ClientInteractionCache.VERSION
                    + "|" + HubPins.version(player)
                    + "|" + selected[0] + "|"
                    + filter[0] + "|" + ClientTpaCache.ONLINE.hashCode() + "|" + tpaSignature(now)
                    + "|" + (viewed[0] == null ? "" : viewed[0].id());
            if (!sig.equals(listSig[0])) {
                listSig[0] = sig;
                list.clearAllScrollViewChildren();
                list.clearLayoutCache();
                fillList(player, list, online, elsewhere, viewed[0], selected[0], filter[0], now, pick);
            }

            // A profile that never came back — a select dropped by the server's rate limit — is asked
            // for again rather than left loading.
            if (current != null && !ClientFriendsCache.DETAILS.containsKey(current.id())
                    && !ClientFriendsCache.FAILED.contains(current.id())
                    && now - selectSentAt[0] > RESELECT_MILLIS) {
                pick.accept(current.id());
            }

            boolean chosen = current != null;
            if (pickNote.isDisplayed() == chosen) pickNote.setDisplay(!chosen);
            if (top.isDisplayed() != chosen) top.setDisplay(chosen);
            if (rule.isDisplayed() != chosen) rule.setDisplay(chosen);
            if (lower.isDisplayed() != chosen) lower.setDisplay(chosen);

            boolean here = current != null && ClientTpaCache.ONLINE.containsKey(current.id());
            tpa.setActive(here);
            tpaHere.setActive(here);
            invite.setActive(here && !current.teammate());
            money.setActive(chosen);
            configure.setActive(chosen);

            if (configureWindow.isOpen()) {
                if (current == null) {
                    configureWindow.close();
                } else {
                    configureTarget.setText(Component.translatable("gui.roll_mod.hub.interactions.configure.with",
                            Component.literal(current.name()).withStyle(ChatFormatting.YELLOW)));
                    InteractionSettings.Flags flags = ClientInteractionCache.of(current.id());
                    // Quietly: these follow the server's copy, and must not echo it back as a change.
                    switches.forEach((flag, toggle) -> {
                        if (toggle.isOn() != flags.get(flag)) toggle.setOn(flags.get(flag), false);
                    });
                    boolean pinnedNow = HubPins.pinned(player, HubWindowState.PinKind.PLAYERS, current.id());
                    if (pinSwitch.isOn() != pinnedNow) pinSwitch.setOn(pinnedNow, false);
                }
            }

            boolean friend = current != null && isFriend(current.id());
            boolean armed = friend && now - removeArmedAt[0] < CONFIRM_MILLIS;
            String friendshipKey = !friend ? "gui.roll_mod.hub.friends.add"
                    : armed ? "gui.roll_mod.hub.friends.remove.confirm" : "gui.roll_mod.hub.friends.remove";
            if (!friendshipKey.equals(friendshipShown[0])) {
                friendshipShown[0] = friendshipKey;
                friendship.setText(Component.translatable(friendshipKey));
            }
            friendship.setActive(current != null
                    && (friend || ClientFriendsCache.STATUS != FriendsService.Status.UNAVAILABLE));

            String dSig = ClientFriendsCache.VERSION + "|" + selected[0] + "|" + here;
            if (!dSig.equals(detailSig[0])) {
                detailSig[0] = dSig;
                fillTop(top, current, now);
                info.clearAllScrollViewChildren();
                info.clearLayoutCache();
                if (current != null) FriendsTab.fillDetail(info, current);
            }

            String pSig = selected[0] + "|" + ClientMoneyCache.VERSION + "|" + ClientFriendsCache.VERSION + "|"
                    + tpaSignature(now);
            if (!pSig.equals(pendingSig[0])) {
                pendingSig[0] = pSig;
                pending.clearAllChildren();
                if (current != null) fillPending(pending, current.id(), now);
            }

            if (moneyWindow[0].isOpen()) {
                String name = current == null ? "?" : current.name();
                moneyTarget.setText(Component.translatable("gui.roll_mod.hub.interactions.money.with",
                        Component.literal(name).withStyle(ChatFormatting.YELLOW)));
                long value = VendorUIHelper.parseLong(amount.getText(), 0);
                Component coins = Component.literal(EnergyFormatUtils.formatEnergy(value)).withStyle(ChatFormatting.GOLD);
                long balance = ClientCurrencyHolder.get(CurrencyType.MAIN);
                MutableComponent summary = value <= 0
                        ? Component.translatable("gui.roll_mod.hub.interactions.money.enter").withStyle(ChatFormatting.GRAY)
                        : asking[0]
                        ? Component.translatable("gui.roll_mod.hub.interactions.money.summary.request", coins, name)
                        : Component.translatable("gui.roll_mod.hub.interactions.money.summary.send", coins, name);
                if (!asking[0]) {
                    summary.append("\n").append(Component.translatable("gui.roll_mod.hub.interactions.money.balance",
                            Component.literal(EnergyFormatUtils.formatEnergy(balance))
                                    .withStyle(value > balance ? ChatFormatting.RED : ChatFormatting.GOLD)));
                }
                moneySummary.setText(summary);
            }
        });

        return root;
    }

    /* --------------------------------------------- list --------------------------------------------- */

    private static void fillList(Player player, ScrollerView list, List<FriendEntry> online, List<FriendEntry> elsewhere,
                                 @Nullable FriendEntry viewed, @Nullable UUID selected, String filter, long now,
                                 java.util.function.Consumer<UUID> pick) {
        // What waits for the viewer's answer, pinned at the top whatever the filter says.
        List<UIElement> waiting = new ArrayList<>();
        for (TpaRequest request : ClientTpaCache.INCOMING) {
            if (request.remainingMillis(now) <= 0) continue;
            waiting.add(answerRow(Component.translatable(request.kind() == TpaRequest.Kind.TO
                            ? "gui.roll_mod.hub.interactions.wait.tpa" : "gui.roll_mod.hub.interactions.wait.tpahere",
                            request.fromName()), request.from(), request.from().equals(selected), pick,
                    () -> PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), true)),
                    () -> PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), false))));
        }
        for (FriendRequest request : ClientFriendsCache.INCOMING) {
            waiting.add(answerRow(Component.translatable("gui.roll_mod.hub.interactions.wait.friend", request.name()),
                    request.id(), request.id().equals(selected), pick,
                    () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), true)),
                    () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), false))));
        }
        for (CurrencyOffer offer : ClientMoneyCache.OFFERS_IN) {
            waiting.add(answerRow(Component.translatable("gui.roll_mod.hub.interactions.wait.offer",
                            offer.senderName(), coins(offer.amount())), offer.sender(), offer.sender().equals(selected), pick,
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.ACCEPT_OFFER, offer.id())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.DENY_OFFER, offer.id()))));
        }
        for (MoneyRequest request : ClientMoneyCache.REQUESTS_IN) {
            waiting.add(answerRow(Component.translatable("gui.roll_mod.hub.interactions.wait.request",
                            request.requesterName(), coins(request.amount())), request.requester(),
                    request.requester().equals(selected), pick,
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.PAY_REQUEST, request.id())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.REFUSE_REQUEST, request.id()))));
        }
        if (!waiting.isEmpty()) {
            list.addScrollViewChild(FriendsTab.heading(Component.translatable(
                    "gui.roll_mod.hub.interactions.waiting", waiting.size())));
            waiting.forEach(list::addScrollViewChild);
        }

        // The viewer's own pins, straight under what waits for an answer: requests expire, pins do
        // not, so the requests keep the very top. Taken out of the lists below so each shows once.
        List<FriendEntry> rest = new ArrayList<>(online);
        List<FriendEntry> pinned = new ArrayList<>(HubPins.takePinned(player, HubWindowState.PinKind.PLAYERS, rest, FriendEntry::id));
        online = rest;
        rest = new ArrayList<>(elsewhere);
        pinned.addAll(HubPins.takePinned(player, HubWindowState.PinKind.PLAYERS, rest, FriendEntry::id));
        elsewhere = rest;
        pinned.removeIf(entry -> !matches(entry, filter));
        if (!pinned.isEmpty()) {
            list.addScrollViewChild(FriendsTab.heading(Component.translatable("gui.roll_mod.hub.pinned")));
            for (FriendEntry entry : pinned) {
                list.addScrollViewChild(playerRow(entry, isFriend(entry.id()) && ClientTpaCache.ONLINE.containsKey(entry.id()),
                        entry.id().equals(selected), pick));
            }
        }

        if (viewed != null && matches(viewed, filter)) {
            list.addScrollViewChild(FriendsTab.heading(Component.translatable("gui.roll_mod.hub.interactions.viewed")));
            list.addScrollViewChild(playerRow(viewed, false, viewed.id().equals(selected), pick));
        }

        List<FriendEntry> shownOnline = online.stream().filter(entry -> matches(entry, filter)).toList();
        list.addScrollViewChild(FriendsTab.heading(Component.translatable(
                "gui.roll_mod.hub.interactions.online", shownOnline.size())));
        if (shownOnline.isEmpty()) list.addScrollViewChild(FriendsTab.note("gui.roll_mod.hub.interactions.online.empty"));
        for (FriendEntry entry : shownOnline) {
            list.addScrollViewChild(playerRow(entry, isFriend(entry.id()), entry.id().equals(selected), pick));
        }

        List<FriendEntry> shownElsewhere = elsewhere.stream().filter(entry -> matches(entry, filter)).toList();
        FriendsService.Status status = ClientFriendsCache.STATUS;
        list.addScrollViewChild(FriendsTab.heading(Component.translatable(
                "gui.roll_mod.hub.interactions.friends", shownElsewhere.size())));
        if (status != FriendsService.Status.OK) {
            list.addScrollViewChild(FriendsTab.note("gui.roll_mod.hub.friends.status." + status.name().toLowerCase(Locale.ROOT)));
        } else if (shownElsewhere.isEmpty()) {
            list.addScrollViewChild(FriendsTab.note("gui.roll_mod.hub.interactions.friends.empty"));
        }
        for (FriendEntry entry : shownElsewhere) {
            list.addScrollViewChild(playerRow(entry, false, entry.id().equals(selected), pick));
        }
    }

    /** {@code markFriend}: online players include strangers, so friends among them get a heart. */
    private static UIElement playerRow(FriendEntry entry, boolean markFriend, boolean isSelected,
                                       java.util.function.Consumer<UUID> pick) {
        UIElement row = listRow(isSelected);
        boolean here = ClientTpaCache.ONLINE.containsKey(entry.id());
        ChatFormatting color = here ? ChatFormatting.GREEN : entry.online() ? ChatFormatting.AQUA : ChatFormatting.GRAY;
        MutableComponent text = Component.literal(entry.name()).withStyle(color);
        if (entry.teammate()) text.append(Component.literal(" ★").withStyle(ChatFormatting.GOLD));
        if (markFriend) text.append(Component.literal(" ❤").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (ClientMoneyCache.involves(entry.id())) text.append(Component.literal(" ✦").withStyle(ChatFormatting.GOLD));
        boolean ignored = ClientInteractionCache.ignores(entry.id());
        if (ignored) text.append(Component.literal(" ⦸").withStyle(ChatFormatting.DARK_GRAY));

        PlayerHeadElement head = new PlayerHeadElement(entry.id(), entry.name(), entry.skin(), entry.skinSignature());
        head.layout(l -> l.width(ROW_H - 2).height(ROW_H - 2).flexShrink(0));
        row.addChildren(head, label(text));
        row.addEventListener(UIEvents.CLICK, e -> pick.accept(entry.id()));
        if (ignored) FriendsTab.hint(row, "gui.roll_mod.hub.interactions.ignored");
        return row;
    }

    /** Something waiting for the viewer's answer: what it is, ✔ and ✖. A click on the text selects the player. */
    private static UIElement answerRow(Component text, UUID who, boolean isSelected, java.util.function.Consumer<UUID> pick,
                                       Runnable yes, Runnable no) {
        UIElement row = listRow(isSelected);
        row.addChildren(label(text),
                FriendsTab.small("✔", ChatFormatting.GREEN, "gui.roll_mod.hub.interactions.accept", yes),
                FriendsTab.small("✖", ChatFormatting.RED, "gui.roll_mod.hub.interactions.decline", no));
        row.addEventListener(UIEvents.CLICK, e -> pick.accept(who));
        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(text), null, null, ItemStack.EMPTY));
        return row;
    }

    /* -------------------------------------------- header -------------------------------------------- */

    /** The header: face; name and level; group and status. */
    private static void fillTop(UIElement top, @Nullable FriendEntry entry, long now) {
        top.clearAllChildren();
        if (entry == null) return;
        FriendDetail profile = ClientFriendsCache.DETAILS.get(entry.id());

        PlayerHeadElement head = new PlayerHeadElement(entry.id(), entry.name(), entry.skin(), entry.skinSignature());
        head.layout(l -> l.width(HEAD).height(HEAD).flexShrink(0));

        // Name and level on one line, the level in Minestar's pink; hover it for the XP still missing.
        MutableComponent title = Component.literal(entry.name()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
                .append(Component.literal("  "))
                .append((profile == null ? Component.literal("…")
                        : Component.translatable("gui.roll_mod.hub.interactions.level", profile.level()))
                        .withColor(COLOR_LEVEL & 0xFFFFFF));
        Label nameLine = label(title);
        nameLine.layout(l -> l.widthPercent(100).height(ROW_H - 4).flexGrow(0).flexBasis(ROW_H - 4));
        if (profile != null) {
            nameLine.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    Component.translatable("gui.roll_mod.hub.friends.xp.next", profile.xpToNext(), profile.level() + 1),
                    Component.translatable("gui.roll_mod.hub.friends.xp.progress",
                            profile.levelXp(), profile.levelXpNeeded()).withStyle(ChatFormatting.GRAY)),
                    null, null, ItemStack.EMPTY));
        }

        MutableComponent second = Component.empty();
        if (profile != null && !profile.groupName().isEmpty()) {
            MutableComponent group = Component.literal(profile.groupName());
            if (profile.groupColor() >= 0) group = group.withColor(profile.groupColor());
            second.append(group).append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY));
        }
        boolean here = ClientTpaCache.ONLINE.containsKey(entry.id());
        second.append(here && !entry.here()
                ? Component.translatable("gui.roll_mod.hub.friends.online.here").withStyle(ChatFormatting.GREEN)
                : FriendsTab.status(entry, now));
        Label secondLine = label(second);
        secondLine.layout(l -> l.widthPercent(100).height(ROW_H - 4).flexGrow(0).flexBasis(ROW_H - 4));

        UIElement text = new UIElement();
        text.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).gapRow(2));
        text.addChildren(nameLine, secondLine);

        top.addChildren(head, text);
    }

    /* -------------------------------------------- pending ------------------------------------------- */

    /** Everything open between the viewer and {@code other}, each with its answer. */
    private static void fillPending(UIElement pending, UUID other, long now) {
        List<UIElement> rows = new ArrayList<>();
        for (TpaRequest request : ClientTpaCache.INCOMING) {
            if (!request.from().equals(other) || request.remainingMillis(now) <= 0) continue;
            rows.add(pendingRow(Component.translatable(request.kind() == TpaRequest.Kind.TO
                            ? "gui.roll_mod.hub.interactions.pending.tpa.in" : "gui.roll_mod.hub.interactions.pending.tpahere.in"),
                    () -> PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), true)),
                    () -> PacketDistributor.sendToServer(TpaActionPacket.answer(request.id(), false))));
        }
        for (TpaRequest request : ClientTpaCache.OUTGOING) {
            if (!request.to().equals(other) || request.remainingMillis(now) <= 0) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.tpa.out"), null,
                    () -> PacketDistributor.sendToServer(TpaActionPacket.cancel(request.id()))));
        }
        for (FriendRequest request : ClientFriendsCache.INCOMING) {
            if (!request.id().equals(other)) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.friend.in"),
                    () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), true)),
                    () -> PacketDistributor.sendToServer(FriendActionPacket.answer(request.id(), false))));
        }
        for (FriendRequest request : ClientFriendsCache.OUTGOING) {
            if (!request.id().equals(other)) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.friend.out"), null,
                    () -> PacketDistributor.sendToServer(FriendActionPacket.cancel(request.id()))));
        }
        for (CurrencyOffer offer : ClientMoneyCache.OFFERS_IN) {
            if (!offer.sender().equals(other)) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.offer.in", coins(offer.amount())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.ACCEPT_OFFER, offer.id())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.DENY_OFFER, offer.id()))));
        }
        for (CurrencyOffer offer : ClientMoneyCache.OFFERS_OUT) {
            if (!offer.recipient().equals(other)) continue;
            // An offer cannot be taken back — the recipient answers it, or it lapses and refunds.
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.offer.out", coins(offer.amount())),
                    null, null));
        }
        for (MoneyRequest request : ClientMoneyCache.REQUESTS_IN) {
            if (!request.requester().equals(other)) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.request.in", coins(request.amount())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.PAY_REQUEST, request.id())),
                    () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.REFUSE_REQUEST, request.id()))));
        }
        for (MoneyRequest request : ClientMoneyCache.REQUESTS_OUT) {
            if (!request.payer().equals(other)) continue;
            rows.add(pendingRow(Component.translatable("gui.roll_mod.hub.interactions.pending.request.out", coins(request.amount())),
                    null, () -> PacketDistributor.sendToServer(MoneyActionPacket.answer(MoneyActionPacket.Action.CANCEL_REQUEST, request.id()))));
        }
        if (rows.isEmpty()) return;
        pending.addChild(FriendsTab.heading(Component.translatable("gui.roll_mod.hub.interactions.pending")));
        rows.forEach(pending::addChild);
    }

    /** One open thing: what it is, then ✔ (when {@code yes}) and ✖ (when {@code no} — decline or cancel). */
    private static UIElement pendingRow(Component text, @Nullable Runnable yes, @Nullable Runnable no) {
        UIElement row = row(ROW_H);
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        row.layout(l -> l.paddingAll(1).paddingHorizontal(HubSection.ROW_INSET));
        Label label = label(text);
        row.addChild(label);
        label.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(text), null, null, ItemStack.EMPTY));
        if (yes != null) row.addChild(FriendsTab.small("✔", ChatFormatting.GREEN, "gui.roll_mod.hub.interactions.accept", yes));
        if (no != null) row.addChild(FriendsTab.small("✖", ChatFormatting.RED,
                yes != null ? "gui.roll_mod.hub.interactions.decline" : "gui.roll_mod.hub.interactions.cancel", no));
        return row;
    }

    /* -------------------------------------------- helpers ------------------------------------------- */

    /** Everyone online here (the viewer excluded): their friend entry when they are a friend, a bare one otherwise. */
    private static List<FriendEntry> online() {
        List<FriendEntry> players = new ArrayList<>();
        for (Map.Entry<UUID, String> player : ClientTpaCache.ONLINE.entrySet()) {
            FriendEntry friend = find(ClientFriendsCache.ENTRIES, player.getKey());
            players.add(friend != null ? friend
                    : new FriendEntry(player.getKey(), player.getValue(), true, -1L, List.of())
                            .withServerView(true, false, "", ""));
        }
        players.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return players;
    }

    /** Someone who is only there through a request — selected from the waiting block. */
    @Nullable
    private static FriendEntry requestEntry(@Nullable UUID id) {
        if (id == null) return null;
        String name = null;
        for (FriendRequest request : ClientFriendsCache.INCOMING) if (request.id().equals(id)) name = request.name();
        for (FriendRequest request : ClientFriendsCache.OUTGOING) if (request.id().equals(id)) name = request.name();
        for (CurrencyOffer offer : ClientMoneyCache.OFFERS_IN) if (offer.sender().equals(id)) name = offer.senderName();
        for (CurrencyOffer offer : ClientMoneyCache.OFFERS_OUT) if (offer.recipient().equals(id)) name = offer.recipientName();
        for (MoneyRequest request : ClientMoneyCache.REQUESTS_IN) if (request.requester().equals(id)) name = request.requesterName();
        for (MoneyRequest request : ClientMoneyCache.REQUESTS_OUT) if (request.payer().equals(id)) name = request.payerName();
        return name == null ? null : new FriendEntry(id, name, false, -1L, List.of());
    }

    /** Changes only when a teleport request comes, goes, or ticks over a second. */
    private static String tpaSignature(long now) {
        StringBuilder builder = new StringBuilder();
        for (TpaRequest request : ClientTpaCache.INCOMING) {
            builder.append('i').append(request.id()).append(request.remainingMillis(now) > 0);
        }
        for (TpaRequest request : ClientTpaCache.OUTGOING) {
            builder.append('o').append(request.id()).append(request.remainingMillis(now) > 0);
        }
        return builder.toString();
    }

    private static boolean matches(FriendEntry entry, String filter) {
        return filter.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(filter);
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

    private static Component coins(long amount) {
        return Component.literal(EnergyFormatUtils.formatEnergy(amount)).withStyle(ChatFormatting.GOLD);
    }

    private static UIElement listRow(boolean isSelected) {
        UIElement row = row(ROW_H);
        row.layout(l -> l.marginBottom(2).paddingAll(1).paddingHorizontal(HubSection.ROW_INSET));
        row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
        return row;
    }

    private static UIElement row(int height) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(height).flexShrink(0)
                .gapColumn(3).alignItems(AlignItems.CENTER));
        return row;
    }

    /** A one-line label that takes the rest of its row. */
    private static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        return label;
    }
}
