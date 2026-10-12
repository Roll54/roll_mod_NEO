package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
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
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendEntry;
import com.roll_54.roll_mod.minestar.hub.home.ClientHomeCache;
import com.roll_54.roll_mod.minestar.hub.home.HomeView;
import com.roll_54.roll_mod.minestar.hub.home.PlayerHome;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.network.packet.HomeActionPacket;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * The hub's homes tab: the player's own homes and the ones shared with them down the left, the
 * selected one on the right.
 *
 * <p>Named {@code PlayerHomesTab} because {@link HomeTab} is already the hub's dashboard and has
 * nothing to do with player homes; {@code HomesTab} beside {@code HomeTab} would be misread forever.
 *
 * <p>Reads {@link ClientHomeCache}, which {@code SyncHomesPacket} keeps current — the same approach
 * as the warp tab, and for the same reason: a variable-length list does not fit LdLib2's positional
 * bindings. Unlike warps, the cache is already filtered to this player and each row carries what
 * they are to it, so nothing here has to work that out.
 *
 * <p>Panels are rebuilt only when what they show changes, guarded by a signature string, since
 * {@code UIEvents.TICK} fires every frame.
 */
public final class PlayerHomesTab {

    private static final int PADDING = 6;

    /** Wider than the warp list: an invite row carries two lines of text and two buttons. */
    /** The same as the warps list: the travel tab flips between the two, and a jump in width read badly. */
    private static final int LIST_W = TravelTab.LIST_W;

    private static final int ROW_H = 16;
    private static final int INVITE_ROW_H = 26;
    private static final int LINE_H = 12;
    private static final int MARK = 12;
    private static final int ACTION = 14;

    /** The inviter's head on a two-line invite row; 16px keeps the 8px face texture pixel-exact. */
    private static final int INVITE_HEAD = 16;

    /** Every row's inset. Named because the pearl has to cancel it — see {@link #quickTeleport}. */
    private static final int ROW_PADDING = 2;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    /** The orange of a demand for an answer, on the pending-invite badge. */
    private static final int COLOR_ALERT = 0xFFFF9A2E;

    /**
     * The red of the legacy badge. Louder than {@link #COLOR_ALERT} on purpose: an invite waits
     * patiently, whereas a home still living in FTB Essentials is unreachable by every command and
     * stays that way until it is brought across.
     */
    private static final int COLOR_LEGACY = 0xFFE04A3A;

    /**
     * How far delete and leave sit below the pin button in the Change window.
     *
     * <p>They are the two that cannot be taken back, so they are kept clear of the harmless toggle
     * above them — and behind a confirmation on top of that.
     */
    private static final int EDIT_GAP = 12;

    /** The Change window: pin, delete or leave, and the invite picker, which can expand in place. */
    private static final int CHANGE_W = 220;
    private static final int CHANGE_H = 250;

    /** The Invite button's width; the player picker takes whatever is left of the row. */
    private static final int INVITE_W = 68;

    /** The invite mode dropdown's width; the search field takes whatever is left of the row. */
    private static final int INVITE_MODE_W = 62;

    /**
     * How many names the picker shows before it scrolls. Fixed rather than sized to its names, so
     * the window does not jump as a search narrows the list.
     */
    private static final int PICKER_VISIBLE = 5;

    /**
     * Vanilla's own ender-pearl file, for the one-click teleport on the owner's rows.
     *
     * <p>A {@link SpriteTexture} over the file rather than an {@code ItemStackTexture} of the item,
     * for the reason {@link #inviteRow} records about its alert badge: an item texture draws outside
     * the UI's batch and always on top, so a row that had scrolled out of the list would keep
     * painting its pearl over the list's edge.
     */
    private static final ResourceLocation PEARL =
            ResourceLocation.withDefaultNamespace("textures/item/ender_pearl.png");

    /** The pearl's drawn size. Deliberately larger than the {@link #ROW_H} row it sits in. */
    private static final int PEARL_SIZE = 22;

    /**
     * Centres the pearl on the row's content box. Negative by however much {@link #PEARL_SIZE}
     * exceeds that box, so the pearl spills evenly past the row's edges into the margin between
     * rows rather than pushing the row out of shape.
     */
    private static final int PEARL_INSET = (ROW_H - 2 * ROW_PADDING - PEARL_SIZE) / 2;

    private PlayerHomesTab() {}

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

        HubSection.Info section = HubSection.info("homes");

        UIElement body = new UIElement();
        // flexBasis(0), not flexGrow alone: LdLib defaults flex-shrink to 0 and flex-basis to auto,
        // so the body's base size would be its own content — the whole list, however long — and
        // nothing would pull it back to the leftover space under the header. A zero basis leaves the
        // grow as the only thing deciding the height, which is what the list scrolls inside.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(1));

        // Per-viewer client state, so it lives in the tree rather than crossing the wire.
        UUID[] selected = {null};
        boolean[] creating = {false};
        // The half-typed nickname lives out here because the detail panel is torn down and rebuilt
        // whenever a sync arrives — and a sync arrives every time anyone answers an invite. Held in
        // the builder it would be wiped mid-word.
        String[] inviteDraft = {""};
        // The invite picker's mode and search text, held out here with the draft for the same reason.
        InviteSearch inviteSearch = new InviteSearch();

        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .paddingAll(PADDING));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement left = new UIElement();
        left.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(LIST_W).heightPercent(100)
                .gapRow(PADDING));

        Button create = new Button();
        create.setText(Component.translatable("gui.roll_mod.hub.homes.create"));
        create.layout(l -> l.widthPercent(100).height(ROW_H));
        create.setOnClick(e -> {
            creating[0] = true;
            selected[0] = null;
            inviteDraft[0] = "";
            inviteSearch.reset();
        });

        String[] filter = {""};
        TextField search = new TextField();
        search.setText("");
        search.layout(l -> l.widthPercent(100).height(ROW_H));
        search.setTextResponder(text -> filter[0] = text == null ? "" : text.strip().toLowerCase());
        hint(search, "gui.roll_mod.hub.homes.search.tip");

        ScrollerView list = new ScrollerView();
        // Same zero basis, for the same reason: measured by its content the scroller is as tall as
        // its rows, so it sees no overflow and never scrolls. See HubInfoWindow, where this bit.
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        left.addChildren(create, search, list);

        // Everything that changes the chosen home rather than travelling to it — pin, delete or
        // leave, invite — behind the detail panel's Change button, so the panel's one big target is
        // Teleport. Filled for the selected home by the tick below while it is open.
        HubConfirm confirmer = new HubConfirm();
        UIElement[] changeBody = {null};
        HubSection.Floating changeWindow = HubSection.popup(
                Component.translatable("gui.roll_mod.hub.change.title"), CHANGE_W, CHANGE_H, content -> {
                    content.layout(l -> l.flexDirection(FlexDirection.COLUMN));
                    changeBody[0] = content;
                }).windowed();
        // The divider between the list and the panel: drag to resize, double-click to reset.
        body.addChildren(left, HubSplit.handle(titled ? "homes" : TravelTab.SPLIT_KEY, left, LIST_W, player.level().isClientSide()), detail);
        // The panel last, so it covers the body when opened.
        if (titled) {
            root.addChildren(HubSection.header("homes", section), body, section.overlay());
        } else {
            root.addChildren(body);
        }
        root.addChildren(changeWindow.root(), confirmer.root());

        String[] listSignature = {null};
        String[] detailSignature = {null};
        String[] changeSignature = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            List<HomeView> homes = ClientHomeCache.HOMES;

            long owned = homes.stream()
                    .filter(h -> h.relation() == HomeView.Relation.OWNER).count();
            create.setDisplay(owned < ClientHomeCache.LIMIT);

            List<HomeView> shown = matching(homes, filter[0]);

            // The relation is part of the signature, not just the id: accepting an invite changes
            // only the relation, and without it the row would keep rendering as an invite.
            StringBuilder rows = new StringBuilder();
            for (HomeView home : shown) {
                rows.append(home.id()).append(home.relation().ordinal());
            }
            String signature = shown.size() + ":" + rows + ":" + selected[0] + ":" + HubPins.version(player);
            if (!signature.equals(listSignature[0])) {
                listSignature[0] = signature;
                rebuildList(player, list, shown, selected, creating, inviteDraft);
            }

            // The roster is in the signature too, so the owner's "shared with" list repaints when
            // someone answers.
            HomeView current = find(homes, selected[0]);
            String mode = creating[0] ? "create" : selected[0] + ":" + fingerprint(current);
            if (!mode.equals(detailSignature[0])) {
                detailSignature[0] = mode;
                detail.clearAllChildren();
                if (creating[0]) {
                    buildCreateForm(detail, creating);
                } else {
                    buildDetail(detail, current, selected, creating, () -> {
                        inviteDraft[0] = "";
                        inviteSearch.reset();
                        changeSignature[0] = null;
                        changeWindow.open();
                    });
                }
            }

            // The Change window follows the selection: it goes when the home does (deleted, left,
            // another one picked), and repaints when the roster or a pin changes. The invite picker
            // refills itself as the player types, so its state stays out of the signature — a
            // rebuild per keystroke would take the search field's focus away.
            if (changeWindow.isOpen()) {
                if (current == null || creating[0] || !changeable(current)) {
                    changeWindow.close();
                } else {
                    String change = current.id() + ":" + fingerprint(current) + ":" + HubPins.version(player);
                    if (!change.equals(changeSignature[0])) {
                        changeSignature[0] = change;
                        changeBody[0].clearAllChildren();
                        buildChange(player, changeBody[0], current, selected, inviteDraft, inviteSearch,
                                confirmer);
                    }
                }
            }
        });

        return root;
    }

    /** Matches on the home's name and on its owner, the way the warp search does. */
    private static List<HomeView> matching(List<HomeView> homes, String filter) {
        if (filter.isEmpty()) return homes;
        List<HomeView> matches = new ArrayList<>();
        for (HomeView home : homes) {
            if (LegacyText.plain(home.name()).toLowerCase().contains(filter)
                    || home.ownerName().toLowerCase().contains(filter)) {
                matches.add(home);
            }
        }
        return matches;
    }

    /** Roster state, so the detail panel repaints when a guest accepts, declines or leaves. */
    private static String fingerprint(@Nullable HomeView home) {
        if (home == null) return "none";
        StringBuilder out = new StringBuilder(home.name());
        for (HomeView.ShareView share : home.roster()) {
            out.append('|').append(share.name()).append(share.accepted());
        }
        return out.toString();
    }

    /* --------------------------------------------- list --------------------------------------------- */

    private static void rebuildList(Player player, ScrollerView list, List<HomeView> homes,
                                    UUID[] selected, boolean[] creating, String[] inviteDraft) {
        list.clearAllScrollViewChildren();

        // The viewer's pins above everything, invites included: the player put them there. Only
        // homes they can travel to can be pinned, and each shows once — here, not in its section.
        List<HomeView> rest = new ArrayList<>(homes);
        List<HomeView> pinned = HubPins.takePinned(player, HubWindowState.PinKind.HOMES, rest,
                home -> changeable(home) ? home.id() : null);
        if (!pinned.isEmpty()) {
            addHeader(list, "gui.roll_mod.hub.pinned");
            for (HomeView home : pinned) list.addScrollViewChild(homeRow(home, selected, creating, inviteDraft));
        }
        homes = rest;

        // Invites first: they are the only rows asking the player for something.
        addSection(list, homes, HomeView.Relation.INVITED, "gui.roll_mod.hub.homes.section.invites",
                selected, creating, inviteDraft);
        addSection(list, homes, HomeView.Relation.OWNER, "gui.roll_mod.hub.homes.section.mine",
                selected, creating, inviteDraft);
        addSection(list, homes, HomeView.Relation.GUEST, "gui.roll_mod.hub.homes.section.shared",
                selected, creating, inviteDraft);
        // Last: these are a chore to clear, not part of the list the player came here to use.
        addSection(list, homes, HomeView.Relation.FTB, "gui.roll_mod.hub.homes.section.legacy",
                selected, creating, inviteDraft);
    }

    private static void addSection(ScrollerView list, List<HomeView> homes,
                                   HomeView.Relation relation, String headerKey,
                                   UUID[] selected, boolean[] creating, String[] inviteDraft) {
        List<HomeView> section = new ArrayList<>();
        for (HomeView home : homes) {
            if (home.relation() == relation) section.add(home);
        }
        if (section.isEmpty()) return;

        addHeader(list, headerKey);

        for (HomeView home : section) {
            list.addScrollViewChild(switch (relation) {
                case INVITED -> inviteRow(home);
                case FTB -> legacyRow(home);
                default -> homeRow(home, selected, creating, inviteDraft);
            });
        }
    }

    private static void addHeader(ScrollerView list, String headerKey) {
        Label header = new Label();
        header.setText(Component.translatable(headerKey).withStyle(ChatFormatting.DARK_GRAY));
        header.layout(l -> l.widthPercent(100).height(LINE_H).marginBottom(1));
        header.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        list.addScrollViewChild(header);
    }

    /** Homes the Change window applies to: the viewer's own and the ones they are a guest of. */
    private static boolean changeable(HomeView home) {
        return home.relation() == HomeView.Relation.OWNER || home.relation() == HomeView.Relation.GUEST;
    }

    /**
     * An owned or shared home. Owned: name, then the teleport pearl. Shared: the owner's head, name,
     * then the pearl — the head is what says whose home it is, so it carries the owner tooltip.
     */
    private static UIElement homeRow(HomeView home, UUID[] selected, boolean[] creating,
                                     String[] inviteDraft) {
        boolean isSelected = home.id().equals(selected[0]);

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(ROW_PADDING).paddingHorizontal(HubSection.ROW_INSET).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(
                isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
        row.addEventListener(UIEvents.CLICK, e -> {
            if (!home.id().equals(selected[0])) inviteDraft[0] = "";
            selected[0] = home.id();
            creating[0] = false;
        });

        if (home.relation() == HomeView.Relation.GUEST) {
            row.addChild(ownerHead(home, MARK));
        }

        Label name = new Label();
        name.setText(LegacyText.display(home.name()));
        name.layout(l -> l.flexGrow(1).height(MARK));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        row.addChild(name);

        // Both relations here may travel: an accepted guest passes PlayerHome.canTeleport.
        row.addChild(quickTeleport(home));

        return row;
    }

    /** Head of an invited-into home's owner, sized to fit the row, naming them on hover. */
    private static UIElement ownerHead(HomeView home, int size) {
        PlayerHeadElement head = new PlayerHeadElement(home.owner(), home.ownerName());
        head.layout(l -> l.width(size).height(size).marginTopAuto().marginBottomAuto());
        head.addEventListener(UIEvents.HOVER_TOOLTIPS,
                e -> e.hoverTooltips = new HoverTooltips(
                        List.of(Component.translatable("gui.roll_mod.hub.homes.owner",
                                home.ownerName())), null, null, ItemStack.EMPTY));
        return head;
    }

    /**
     * The one-click way home, on owned and shared rows: teleports straight from the list, without
     * having to select the home and cross to the detail panel's teleport button first.
     *
     * <p>Owned homes and accepted shared homes — both pass {@code PlayerHome.canTeleport}. Never on a
     * pending invite: that grants no access yet, so the pearl would only answer "no access".
     *
     * <p>Still a {@link Button}, but a bare one: the pearl <em>is</em> the control, so all three of
     * the button's plate textures are blanked with {@link IGuiTexture#EMPTY} and the icon is drawn
     * at its full {@link #PEARL_SIZE} rather than shrunk to fit inside a frame. Kept as a Button
     * rather than a plain element with a click listener because that is what carries the press
     * handling and the narration; only its looks are being taken away.
     *
     * <p>The pearl is bigger than the row it sits in, and {@link #PEARL_INSET} is what lets it be:
     * a negative margin centres it on the row's 12px content box, so instead of pushing the row out
     * of shape it overhangs 2px above and below, into the {@code marginBottom} that already
     * separates one row from the next.
     *
     * <p>Overhanging is safe here only because this is a {@link SpriteTexture} — it draws inside the
     * UI's batch, so the scroller's clip contains it. The pearl on a row that is half-scrolled past
     * the top or bottom of the list is therefore cut off with its row, rather than painting over the
     * list's edge the way an item texture would. That is the same property {@link #PEARL} is chosen
     * for.
     */
    private static Button quickTeleport(HomeView home) {
        Button button = new Button();
        button.text.setDisplay(false);
        button.buttonStyle(s -> s.baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));
        button.layout(l -> l.width(PEARL_SIZE).height(PEARL_SIZE)
                .marginTop(PEARL_INSET).marginBottom(PEARL_INSET));
        button.setOnClick(e -> {
            PacketDistributor.sendToServer(HomeActionPacket.teleport(home.id()));
            // The row under it is the selector for the detail panel. Teleporting from a row must not
            // also select it: the hub is about to be left behind anyway, and re-selecting would
            // rebuild the panel for a home the player is no longer looking at.
            e.stopPropagation();
        });

        UIElement icon = new UIElement();
        icon.layout(l -> l.widthPercent(100).heightPercent(100));
        icon.style(s -> s.background(SpriteTexture.of(PEARL)));
        button.addChild(icon);

        hint(button, "gui.roll_mod.hub.homes.teleport.quick");
        return button;
    }

    /**
     * A home the player set with FTB Essentials and has not brought across yet.
     *
     * <p>Carries no click listener, for the same reason {@link #inviteRow} does not: there is no
     * {@code PlayerHome} behind it, so selecting it would open a detail panel with nothing it could
     * offer — no teleport, no roster, no delete. The one thing that can be done to it is done from
     * the row.
     *
     * <p>The red badge is the whole point of listing these. {@code HomeCommandRedirect} hands
     * {@code /home} to this mod, which leaves an FTB home stranded: it is still in FTB's player data
     * and no command reaches it any more. Nothing tells the player that, so the row does.
     */
    private static UIElement legacyRow(HomeView home) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(ROW_PADDING).paddingHorizontal(HubSection.ROW_INSET).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        Label name = new Label();
        name.setText(LegacyText.display(home.name()));
        name.layout(l -> l.flexGrow(1).height(MARK));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        row.addChild(name);

        // A SpriteTexture rather than an item, as everywhere else in this list — see PEARL.
        UIElement warning = new UIElement();
        warning.layout(l -> l.width(MARK).height(MARK).marginTopAuto().marginBottomAuto());
        warning.style(s -> s.background(Icons.ALERT.copy().setColor(COLOR_LEGACY)));
        warning.addEventListener(UIEvents.HOVER_TOOLTIPS,
                e -> e.hoverTooltips = new HoverTooltips(
                        List.of(Component.translatable("gui.roll_mod.hub.homes.legacy.warning")
                                        .withStyle(ChatFormatting.RED),
                                Component.translatable("gui.roll_mod.hub.homes.legacy.hint")
                                        .withStyle(ChatFormatting.GRAY)),
                        null, null, ItemStack.EMPTY));
        row.addChild(warning);

        row.addChild(migrateButton(home));
        return row;
    }

    /** Brings one FTB home across. Addressed by name: its row id is synthetic. */
    private static Button migrateButton(HomeView home) {
        Button button = new Button();
        button.text.setDisplay(false);
        button.layout(l -> l.width(ACTION).height(ACTION).marginTopAuto().marginBottomAuto()
                .paddingAll(1));
        // home.name() verbatim, not LegacyText.plain: the server matches this against FTB's own
        // map key, so anything that rewrites the string breaks the match.
        button.setOnClick(e -> PacketDistributor.sendToServer(
                HomeActionPacket.migrate(home.name())));

        UIElement icon = new UIElement();
        icon.layout(l -> l.widthPercent(100).heightPercent(100));
        icon.style(s -> s.background(Icons.IMPORT));
        button.addChild(icon);

        hint(button, "gui.roll_mod.hub.homes.legacy.migrate");
        return button;
    }

    /**
     * A pending invite: the inviter's head, the home and who it came from, then the alert badge and
     * the two answers.
     *
     * <p>Answering happens here rather than in the detail panel because an invite is an inbox item
     * with exactly two outcomes — and a selected invite would have nothing to show, since a pending
     * invite grants no access to teleport to or share. The row therefore carries no click listener
     * of its own: clicking it selects nothing.
     */
    private static UIElement inviteRow(HomeView home) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(INVITE_ROW_H)
                .marginBottom(2).paddingAll(2).paddingHorizontal(HubSection.ROW_INSET).gapColumn(3));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        row.addChild(ownerHead(home, INVITE_HEAD));

        UIElement text = new UIElement();
        text.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100));

        Label name = new Label();
        name.setText(LegacyText.display(home.name()));
        name.layout(l -> l.widthPercent(100).height(11));

        Label from = new Label();
        from.setText(Component.translatable("gui.roll_mod.hub.homes.invitedBy", home.ownerName())
                .withStyle(ChatFormatting.GRAY));
        from.layout(l -> l.widthPercent(100).height(9));

        text.addChildren(name, from);
        row.addChild(text);

        // A SpriteTexture, not an item: HubUI's HubTab.texture() javadoc records that an
        // ItemStackTexture draws outside the UI batch and always on top, so inside a scroller a
        // row that had scrolled away would keep painting its icon over the edge of the list.
        UIElement alert = new UIElement();
        alert.layout(l -> l.width(MARK).height(MARK).marginTopAuto().marginBottomAuto());
        alert.style(s -> s.background(Icons.ALERT.copy().setColor(COLOR_ALERT)));
        row.addChild(alert);

        row.addChild(answerButton(home, true));
        row.addChild(answerButton(home, false));
        return row;
    }

    private static Button answerButton(HomeView home, boolean accept) {
        Button button = new Button();
        button.text.setDisplay(false);
        button.layout(l -> l.width(ACTION).height(ACTION).marginTopAuto().marginBottomAuto()
                .paddingAll(1));
        button.setOnClick(e -> PacketDistributor.sendToServer(accept
                ? HomeActionPacket.accept(home.id())
                : HomeActionPacket.decline(home.id())));

        UIElement icon = new UIElement();
        icon.layout(l -> l.widthPercent(100).heightPercent(100));
        icon.style(s -> s.background(accept ? Icons.CHECK : Icons.CLOSE));
        button.addChild(icon);

        hint(button, accept ? "gui.roll_mod.hub.homes.accept" : "gui.roll_mod.hub.homes.decline");
        return button;
    }

    /* -------------------------------------------- detail -------------------------------------------- */

    private static void buildDetail(UIElement detail, @Nullable HomeView home,
                                    UUID[] selected, boolean[] creating, Runnable openChange) {
        if (home == null) {
            Label empty = new Label();
            empty.setText(Component.translatable("gui.roll_mod.hub.homes.none")
                    .withStyle(ChatFormatting.GRAY));
            empty.layout(l -> l.widthPercent(100).height(LINE_H));
            empty.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
            detail.addChild(empty);
            return;
        }

        Label title = new Label();
        title.setText(LegacyText.display(home.name()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
        detail.addChild(title);

        boolean mine = home.relation() == HomeView.Relation.OWNER;
        if (!mine) {
            Label owner = new Label();
            owner.setText(Component.translatable("gui.roll_mod.hub.homes.owner", home.ownerName())
                    .withStyle(ChatFormatting.GRAY));
            owner.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(4));
            detail.addChild(owner);
        }

        Label coordinates = new Label();
        coordinates.setText(Component.translatable("gui.roll_mod.hub.homes.coordinates",
                        (int) home.x(), (int) home.y(), (int) home.z())
                .withStyle(ChatFormatting.GRAY));
        coordinates.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(mine ? 4 : 0));

        Label world = new Label();
        world.setText(Component.translatable("gui.roll_mod.hub.homes.world",
                home.dimension().toString()).withStyle(ChatFormatting.GRAY));
        world.layout(l -> l.widthPercent(100).height(LINE_H));

        detail.addChildren(coordinates, world);

        if (mine) addRoster(detail, home);

        Button teleport = new Button();
        teleport.setText(Component.translatable("gui.roll_mod.hub.homes.teleport"));
        teleport.layout(l -> l.widthPercent(100).height(ROW_H).marginTopAuto());
        teleport.setOnClick(e -> PacketDistributor.sendToServer(
                HomeActionPacket.teleport(home.id())));
        detail.addChild(teleport);

        // Pin, delete or leave, invite: everything that changes the home rather than going there.
        if (changeable(home)) {
            Button change = new Button();
            change.setText(Component.translatable("gui.roll_mod.hub.change"));
            change.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
            change.setOnClick(e -> openChange.run());
            detail.addChild(change);
        }

        Button close = new Button();
        close.setText(Component.translatable("gui.roll_mod.hub.homes.close"));
        close.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        close.setOnClick(e -> {
            selected[0] = null;
            creating[0] = false;
        });
        detail.addChild(close);
    }

    /**
     * The Change window's body for {@code home}: the pin toggle, then delete (owner) or leave
     * (guest), both confirmed, then the owner's invite picker.
     */
    private static void buildChange(Player player, UIElement body, HomeView home, UUID[] selected,
                                    String[] inviteDraft, InviteSearch inviteSearch, HubConfirm confirmer) {
        Label title = new Label();
        title.setText(LegacyText.display(home.name()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));
        body.addChild(title);

        Button pin = HubPins.toggle(player, HubWindowState.PinKind.HOMES, home.id());
        pin.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        body.addChild(pin);

        boolean mine = home.relation() == HomeView.Relation.OWNER;
        Button drop = new Button();
        drop.setText(Component.translatable(mine ? "gui.roll_mod.hub.homes.delete" : "gui.roll_mod.hub.homes.leave"));
        drop.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(EDIT_GAP));
        Component name = LegacyText.display(home.name());
        drop.setOnClick(e -> confirmer.ask(
                Component.translatable(mine ? "gui.roll_mod.hub.confirm.homeDelete" : "gui.roll_mod.hub.confirm.homeLeave", name),
                Component.translatable(mine ? "gui.roll_mod.hub.confirm.homeDelete.button"
                        : "gui.roll_mod.hub.confirm.homeLeave.button"),
                () -> {
                    PacketDistributor.sendToServer(mine
                            ? HomeActionPacket.delete(home.id())
                            : HomeActionPacket.leave(home.id()));
                    if (home.id().equals(selected[0])) selected[0] = null;
                }));
        body.addChild(drop);

        // Only the owner can let somebody else in.
        if (mine) body.addChild(inviteBar(player, home, inviteDraft, inviteSearch));
    }

    /** Who the owner has let in. Letting somebody else in is {@link #inviteBar}, in the Change window. */
    private static void addRoster(UIElement detail, HomeView home) {
        Label header = new Label();
        header.setText(Component.translatable("gui.roll_mod.hub.homes.shared.header")
                .withStyle(ChatFormatting.GRAY));
        header.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(6));
        detail.addChild(header);

        if (home.roster().isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.hub.homes.shared.none")
                    .withStyle(ChatFormatting.DARK_GRAY));
            none.layout(l -> l.widthPercent(100).height(LINE_H));
            detail.addChild(none);
        } else {
            for (HomeView.ShareView share : home.roster()) {
                Label line = new Label();
                line.setText(share.accepted()
                        ? Component.literal(share.name()).withStyle(ChatFormatting.WHITE)
                        : Component.translatable("gui.roll_mod.hub.homes.shared.pending",
                                share.name()).withStyle(ChatFormatting.DARK_GRAY));
                line.layout(l -> l.widthPercent(100).height(LINE_H));
                detail.addChild(line);
            }
        }
    }

    /** Where the invite picker draws its names from — the mode dropdown beside the search field. */
    private enum InviteSource {
        /** Everyone who has ever joined this server. */
        ALL("gui.roll_mod.hub.homes.invite.mode.all"),
        /** Players on this server right now. */
        ONLINE("gui.roll_mod.hub.homes.invite.mode.online"),
        /** The viewer's Minestar friends, online ones first. */
        FRIENDS("gui.roll_mod.hub.homes.invite.mode.friends");

        final String key;

        InviteSource(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(key);
        }
    }

    /**
     * The picker's search state, held by the tab rather than the window: the Change window is torn
     * down and rebuilt whenever a sync arrives — and one arrives every time anyone answers an invite.
     * Held in the builder, a half-typed search would be wiped mid-word.
     */
    private static final class InviteSearch {
        InviteSource source = InviteSource.ALL;
        String query = "";

        void reset() {
            source = InviteSource.ALL;
            query = "";
        }
    }

    /**
     * Letting somebody else in: a mode dropdown and a search field, the names they match, then who
     * is chosen and the button that sends it.
     *
     * <p>Typing and switching modes refill the name list in place rather than rebuilding the window,
     * so the field keeps its focus. A name typed that matches nobody listed can still be sent — the
     * server resolves it ({@code PlayerLookup}), so a player who has never joined can be invited.
     *
     * <p>Lives in the Change window with the other edits rather than under the roster it belongs
     * to, because it is an action and the roster is a fact.
     */
    private static UIElement inviteBar(Player player, HomeView home, String[] inviteDraft, InviteSearch search) {
        // The server refuses an invite over the cap anyway; replacing the picker means the owner is
        // told before they search rather than after. Countable here because the roster the client
        // already has carries the accepted flag — no extra field on the packet.
        long pending = home.roster().stream().filter(share -> !share.accepted()).count();
        if (pending >= PlayerHome.MAX_PENDING_INVITES) {
            Label full = new Label();
            full.setText(Component.translatable("gui.roll_mod.hub.homes.invite.limit",
                    PlayerHome.MAX_PENDING_INVITES).withStyle(ChatFormatting.DARK_GRAY));
            full.layout(l -> l.widthPercent(100).marginTop(3));
            full.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            return full;
        }

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).marginTop(6)
                .heightAuto().gapRow(2));

        // Mode, then search. flexBasis(0) on the field, not flexGrow on its own: LdLib defaults
        // flex-shrink to 0 and flex-basis to auto, so it would be measured by its own content and
        // shove the dropdown off the row. HubInfoWindow's scroller needs the same for the same reason.
        UIElement searchRow = new UIElement();
        searchRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
        Selector<InviteSource> mode = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        mode.setCandidateUIProvider(UIElementProvider.text(m -> m == null ? Component.empty() : m.title()));
        mode.setCandidates(List.of(InviteSource.values()));
        mode.setSelected(search.source, false);
        mode.layout(l -> l.width(INVITE_MODE_W).height(ROW_H).flexShrink(0));
        hint(mode, "gui.roll_mod.hub.homes.invite.mode.tip");
        TextField field = new TextField();
        field.setText(search.query);
        field.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H));
        hint(field, "gui.roll_mod.hub.homes.invite.search.tip");
        searchRow.addChildren(mode, field);

        ScrollerView names = new ScrollerView();
        names.layout(l -> l.widthPercent(100).height(PICKER_VISIBLE * ROW_H));
        names.scrollerStyle(sc -> sc.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        names.viewContainer(c -> c.layout(l -> l.widthPercent(100)));
        names.style(sy -> sy.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement sendRow = new UIElement();
        sendRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
        Label chosen = new Label();
        chosen.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        chosen.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        Button send = new Button();
        send.setText(Component.translatable("gui.roll_mod.hub.homes.invite"));
        send.layout(l -> l.width(INVITE_W).height(ROW_H).flexShrink(0));
        sendRow.addChildren(chosen, send);

        // Who Invite sends to: the picked name, else whatever is typed.
        Runnable showChosen = () -> {
            String target = inviteTarget(inviteDraft, search);
            chosen.setText(target.isEmpty()
                    ? Component.translatable("gui.roll_mod.hub.homes.invite.choose").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(target).withStyle(ChatFormatting.YELLOW));
        };

        Runnable[] refill = {null};
        refill[0] = () -> {
            names.clearAllScrollViewChildren();
            names.clearLayoutCache();
            List<String> candidates = invitable(player, home, search);
            if (candidates.isEmpty()) {
                Label none = new Label();
                none.setText(Component.translatable(search.query.isEmpty() && search.source == InviteSource.ALL
                        ? "gui.roll_mod.hub.homes.invite.nobody" : "gui.roll_mod.hub.homes.invite.noMatch")
                        .withStyle(ChatFormatting.DARK_GRAY));
                none.layout(l -> l.widthPercent(100).paddingAll(2));
                none.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                names.addScrollViewChild(none);
            }
            for (String candidate : candidates) {
                Button option = new Button();
                boolean picked = candidate.equalsIgnoreCase(inviteDraft[0]);
                option.setText(picked
                        ? Component.literal(candidate).withStyle(ChatFormatting.YELLOW)
                        : Component.literal(candidate));
                option.layout(l -> l.widthPercent(100).height(ROW_H));
                option.setOnClick(e -> {
                    inviteDraft[0] = candidate;
                    showChosen.run();
                    refill[0].run();
                });
                names.addScrollViewChild(option);
            }
            showChosen.run();
        };

        field.setTextResponder(text -> {
            search.query = text == null ? "" : text.strip();
            // A new search drops a pick it no longer shows, so Invite never sends to someone hidden.
            if (!inviteDraft[0].isEmpty() && !inviteDraft[0].toLowerCase(Locale.ROOT)
                    .contains(search.query.toLowerCase(Locale.ROOT))) {
                inviteDraft[0] = "";
            }
            refill[0].run();
        });
        mode.setOnValueChanged(m -> {
            if (m == null) return;
            search.source = m;
            refill[0].run();
        });
        send.setOnClick(e -> {
            String target = inviteTarget(inviteDraft, search);
            if (target.isEmpty()) return;
            PacketDistributor.sendToServer(HomeActionPacket.invite(home.id(), target));
            inviteDraft[0] = "";
            search.query = "";
            field.setText("", false);
            refill[0].run();
        });
        refill[0].run();

        column.addChildren(searchRow, names, sendRow);
        return column;
    }

    /** The picked name, else the search text when it is a plausible nickname. */
    private static String inviteTarget(String[] inviteDraft, InviteSearch search) {
        if (!inviteDraft[0].isEmpty()) return inviteDraft[0];
        return search.query.matches("[A-Za-z0-9_]{3,16}") ? search.query : "";
    }

    /**
     * The names {@code search} offers for this home: its mode's source, minus the viewer and anyone
     * already on the roster, filtered by the search text — names that start with it first.
     *
     * <p>All of it comes from data the client already holds: the roster arrives with the home,
     * known names with the same packet, online players and friends with the interactions syncs.
     * Someone whose invite lapsed drops off the roster and reappears here, which is the point of
     * the invite having a lifetime at all.
     */
    private static List<String> invitable(Player player, HomeView home, InviteSearch search) {
        Set<String> taken = new HashSet<>();
        taken.add(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
        for (HomeView.ShareView share : home.roster()) {
            taken.add(share.name().toLowerCase(Locale.ROOT));
        }

        List<String> source = new ArrayList<>();
        switch (search.source) {
            case ALL -> source.addAll(ClientHomeCache.KNOWN_PLAYERS);
            case ONLINE -> {
                source.addAll(ClientTpaCache.ONLINE.values());
                source.sort(String.CASE_INSENSITIVE_ORDER);
            }
            case FRIENDS -> ClientFriendsCache.ENTRIES.stream()
                    .sorted(Comparator.comparing((FriendEntry f) -> !f.online())
                            .thenComparing(FriendEntry::name, String.CASE_INSENSITIVE_ORDER))
                    .forEach(f -> source.add(f.name()));
        }

        String query = search.query.toLowerCase(Locale.ROOT);
        List<String> starts = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String name : source) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (taken.contains(lower) || !seen.add(lower)) continue;
            if (lower.startsWith(query)) starts.add(name);
            else if (lower.contains(query)) contains.add(name);
        }
        starts.addAll(contains);
        return starts;
    }

    /* ------------------------------------------ create form ----------------------------------------- */

    private static void buildCreateForm(UIElement detail, boolean[] creating) {
        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.hub.homes.create"));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));

        TextField name = new TextField();
        name.setText("");
        name.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(4));
        hint(name, "gui.roll_mod.hub.homes.field.name");

        Label where = new Label();
        where.setText(Component.translatable("gui.roll_mod.hub.homes.createHint")
                .withStyle(ChatFormatting.DARK_GRAY));
        where.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(3));

        Button confirm = new Button();
        confirm.setText(Component.translatable("gui.roll_mod.hub.homes.confirm"));
        confirm.layout(l -> l.widthPercent(100).height(ROW_H).marginTopAuto());
        confirm.setOnClick(e -> {
            String typed = name.getText() == null ? "" : name.getText().strip();
            if (typed.isEmpty()) return;
            PacketDistributor.sendToServer(HomeActionPacket.create(typed));
            creating[0] = false;
        });

        Button cancel = new Button();
        cancel.setText(Component.translatable("gui.roll_mod.hub.homes.cancel"));
        cancel.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        cancel.setOnClick(e -> creating[0] = false);

        detail.addChildren(title, name, where, confirm, cancel);
    }

    /* --------------------------------------------- helpers ------------------------------------------- */

    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }

    @Nullable
    private static HomeView find(List<HomeView> homes, @Nullable UUID id) {
        if (id == null) return null;
        for (HomeView home : homes) {
            if (home.id().equals(id)) return home;
        }
        return null;
    }
}
