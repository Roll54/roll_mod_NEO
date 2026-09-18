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
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.hub.home.ClientHomeCache;
import com.roll_54.roll_mod.minestar.hub.home.HomeView;
import com.roll_54.roll_mod.minestar.hub.home.PlayerHome;
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
import java.util.HashSet;
import java.util.List;
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
    private static final int LIST_W = 160;

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
     * How far the buttons that change a home sit from the one that merely travels to it.
     *
     * <p>Teleport is the button reached for constantly and delete and leave are the two that cannot
     * be taken back, and until now they sat 3px apart — close enough that a list rebuild arriving
     * between two clicks could put one under a cursor aimed at the other. Two button-heights of
     * nothing is what makes that miss harmless.
     */
    private static final int EDIT_GAP = 32;

    /** The Invite button's width; the player picker takes whatever is left of the row. */
    private static final int INVITE_W = 68;

    /**
     * How many names the picker shows before it scrolls.
     *
     * <p>Bounded rather than growing with the roster: the list expands the panel in place, and a
     * server with hundreds of players would otherwise push everything below it off the screen.
     */
    private static final int PICKER_VISIBLE = 5;

    /**
     * The shortest the invite picker's list may be, in pixels. It is sized to its names, so with only
     * one player to offer it collapsed to a single 16px row.
     */
    private static final int PICKER_MIN_H = 32;

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
        // A column now, so the section title can sit above the list and detail panel. The old row
        // lives on as `body`, unchanged apart from taking its height from the leftover space.
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("homes");

        UIElement body = new UIElement();
        // flexBasis(0), not flexGrow alone: LdLib defaults flex-shrink to 0 and flex-basis to auto,
        // so the body's base size would be its own content — the whole list, however long — and
        // nothing would pull it back to the leftover space under the header. A zero basis leaves the
        // grow as the only thing deciding the height, which is what the list scrolls inside.
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1)
                .gapColumn(PADDING));

        // Per-viewer client state, so it lives in the tree rather than crossing the wire.
        UUID[] selected = {null};
        boolean[] creating = {false};
        // The half-typed nickname lives out here because the detail panel is torn down and rebuilt
        // whenever a sync arrives — and a sync arrives every time anyone answers an invite. Held in
        // the builder it would be wiped mid-word.
        String[] inviteDraft = {""};
        // Whether the invite picker is expanded. Held out here with the draft so it survives the
        // panel being rebuilt, and cleared alongside it.
        boolean[] pickerOpen = {false};

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
            pickerOpen[0] = false;
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
        body.addChildren(left, detail);
        // The panel last, so it covers the body when opened.
        root.addChildren(HubSection.header("homes", section), body, section.overlay());

        String[] listSignature = {null};
        String[] detailSignature = {null};

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
            String signature = shown.size() + ":" + rows + ":" + selected[0];
            if (!signature.equals(listSignature[0])) {
                listSignature[0] = signature;
                rebuildList(list, shown, selected, creating, inviteDraft);
            }

            // The roster is in the signature too, so the owner's "shared with" list repaints when
            // someone answers.
            HomeView current = find(homes, selected[0]);
            // The picker's state is part of the signature: opening it, or choosing a name, changes
            // what the panel shows and nothing else would trigger the rebuild.
            String mode = creating[0]
                    ? "create"
                    : selected[0] + ":" + fingerprint(current) + ":" + pickerOpen[0] + ":" + inviteDraft[0];
            if (!mode.equals(detailSignature[0])) {
                detailSignature[0] = mode;
                detail.clearAllChildren();
                if (creating[0]) {
                    buildCreateForm(detail, creating);
                } else {
                    buildDetail(player, detail, current, selected, creating, inviteDraft, pickerOpen);
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

    private static void rebuildList(ScrollerView list, List<HomeView> homes,
                                    UUID[] selected, boolean[] creating, String[] inviteDraft) {
        list.clearAllScrollViewChildren();

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

        Label header = new Label();
        header.setText(Component.translatable(headerKey).withStyle(ChatFormatting.DARK_GRAY));
        header.layout(l -> l.widthPercent(100).height(LINE_H).marginBottom(1));
        header.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        list.addScrollViewChild(header);

        for (HomeView home : section) {
            list.addScrollViewChild(switch (relation) {
                case INVITED -> inviteRow(home);
                case FTB -> legacyRow(home);
                default -> homeRow(home, selected, creating, inviteDraft);
            });
        }
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
                .marginBottom(2).paddingAll(ROW_PADDING).gapColumn(3));
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
                .marginBottom(2).paddingAll(ROW_PADDING).gapColumn(3));
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
                .marginBottom(2).paddingAll(2).gapColumn(3));
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

    private static void buildDetail(Player player, UIElement detail, @Nullable HomeView home,
                                    UUID[] selected, boolean[] creating, String[] inviteDraft,
                                    boolean[] pickerOpen) {
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

        if (mine) {
            Button delete = new Button();
            delete.setText(Component.translatable("gui.roll_mod.hub.homes.delete"));
            delete.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(EDIT_GAP));
            delete.setOnClick(e -> {
                PacketDistributor.sendToServer(HomeActionPacket.delete(home.id()));
                selected[0] = null;
            });
            detail.addChild(delete);
        } else {
            Button leave = new Button();
            leave.setText(Component.translatable("gui.roll_mod.hub.homes.leave"));
            leave.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(EDIT_GAP));
            leave.setOnClick(e -> {
                PacketDistributor.sendToServer(HomeActionPacket.leave(home.id()));
                selected[0] = null;
            });
            detail.addChild(leave);
        }

        // Below the destructive button and above Close, sharing Close's gap. Only the owner can
        // invite, so a guest's panel goes straight from Leave to Close.
        if (mine) detail.addChild(inviteBar(home, inviteDraft, pickerOpen));

        Button close = new Button();
        close.setText(Component.translatable("gui.roll_mod.hub.homes.close"));
        close.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
        close.setOnClick(e -> {
            selected[0] = null;
            creating[0] = false;
        });
        detail.addChild(close);
    }

    /** Who the owner has let in. Letting somebody else in is {@link #inviteBar}, down at the foot. */
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

    /**
     * The one control for letting somebody else in: a nickname to type and the button that sends it,
     * side by side on a single row.
     *
     * <p>Lives at the foot of the panel with the other buttons rather than under the roster it
     * belongs to, because it is an action and the roster above it is a fact. One row rather than two
     * for the same reason: typing a name and sending it is one thing to do, not two.
     *
     * <p>Returns the element instead of adding it, so {@link #buildDetail} can place it in the
     * button stack it builds — the roster is added while the panel is still filling from the top,
     * and this is added after the bottom stack has been pushed down.
     */
    private static UIElement inviteBar(HomeView home, String[] inviteDraft, boolean[] pickerOpen) {
        // The server refuses an invite over the cap anyway; replacing the field means the owner is
        // told before they type a name in rather than after. Countable here because the roster the
        // client already has carries the accepted flag — no extra field on the packet.
        long pending = home.roster().stream().filter(share -> !share.accepted()).count();
        if (pending >= PlayerHome.MAX_PENDING_INVITES) {
            Label full = new Label();
            full.setText(Component.translatable("gui.roll_mod.hub.homes.invite.limit",
                    PlayerHome.MAX_PENDING_INVITES).withStyle(ChatFormatting.DARK_GRAY));
            full.layout(l -> l.widthPercent(100).marginTop(3));
            full.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            return full;
        }

        List<String> candidates = invitable(home);

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).marginTop(3)
                .heightAuto());

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .gapColumn(3));

        Button choose = new Button();
        choose.setText(inviteDraft[0].isEmpty()
                ? Component.translatable("gui.roll_mod.hub.homes.invite.choose")
                        .withStyle(ChatFormatting.DARK_GRAY)
                : Component.literal(inviteDraft[0]));
        // flexBasis(0), not flexGrow on its own: LdLib defaults flex-shrink to 0 and flex-basis to
        // auto, so without it the element is measured by its own content and shoves the button off
        // the end of the row. HubInfoWindow's scroller needs the same for the same reason.
        choose.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H));
        // Nothing to choose from is not an error worth a dialog, but an opening-and-closing empty
        // list would look like one, so the control simply refuses to open.
        choose.setOnClick(e -> {
            if (!candidates.isEmpty()) pickerOpen[0] = !pickerOpen[0];
        });

        Button send = new Button();
        send.setText(Component.translatable("gui.roll_mod.hub.homes.invite"));
        send.layout(l -> l.width(INVITE_W).height(ROW_H));
        send.setOnClick(e -> {
            if (inviteDraft[0].isEmpty()) return;
            PacketDistributor.sendToServer(HomeActionPacket.invite(home.id(), inviteDraft[0]));
            inviteDraft[0] = "";
            pickerOpen[0] = false;
        });

        row.addChildren(choose, send);
        column.addChild(row);

        if (candidates.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.hub.homes.invite.nobody")
                    .withStyle(ChatFormatting.DARK_GRAY));
            none.layout(l -> l.widthPercent(100).marginTop(2));
            none.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            column.addChild(none);
        } else if (pickerOpen[0]) {
            // Expands in place rather than floating over the panel. A popup would have to be
            // positioned against the window and raised above everything else; this is laid out by
            // the same column as the rest of the panel and cannot end up detached from its button.
            ScrollerView names = new ScrollerView();
            names.layout(l -> l.widthPercent(100)
                    .height(Math.max(PICKER_MIN_H, Math.min(candidates.size(), PICKER_VISIBLE) * ROW_H))
                    .marginTop(2));
            names.scrollerStyle(sc -> sc.mode(ScrollerMode.VERTICAL)
                    .verticalScrollDisplay(ScrollDisplay.AUTO)
                    .horizontalScrollDisplay(ScrollDisplay.NEVER));
            names.viewContainer(c -> c.layout(l -> l.widthPercent(100)));
            names.style(sy -> sy.background(new ColorRectTexture(COLOR_PANEL)));

            for (String candidate : candidates) {
                Button option = new Button();
                option.setText(Component.literal(candidate));
                option.layout(l -> l.widthPercent(100).height(ROW_H));
                option.setOnClick(e -> {
                    inviteDraft[0] = candidate;
                    pickerOpen[0] = false;
                });
                names.addScrollViewChild(option);
            }
            column.addChild(names);
        }

        return column;
    }

    /**
     * Everyone this server knows, minus anyone already on this home.
     *
     * <p>Both halves come from data the client already holds: the roster arrives with the home, and
     * the names with the same packet. Filtering here rather than server-side keeps it correct for
     * <em>this</em> home without the server having to build a different roster per home.
     *
     * <p>Someone whose invite lapsed drops off the roster and reappears here, which is the point of
     * the invite having a lifetime at all.
     */
    private static List<String> invitable(HomeView home) {
        Set<String> taken = new HashSet<>();
        for (HomeView.ShareView share : home.roster()) {
            taken.add(share.name().toLowerCase());
        }
        List<String> candidates = new ArrayList<>();
        for (String name : ClientHomeCache.KNOWN_PLAYERS) {
            if (!taken.contains(name.toLowerCase())) candidates.add(name);
        }
        return candidates;
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
