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
import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.minestar.hub.warp.WarpService;
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
    private static final int LIST_W = 130;
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
     * <p>Deliberately small. The panel has no scroller and {@code HubUI.CONTENT_H} is fixed, so the
     * gaps come out of the same budget as a blocked warp's appeal paragraph and a full-length
     * description; at that extreme the teleport button's {@code marginTopAuto} has no slack left to
     * give and the buttons below it would be pushed off the bottom. Four also happens to be the
     * margin the owner line already carried, so the first group costs nothing new.
     */
    private static final int GROUP_GAP = 4;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private WarpTab() {}

    public static UIElement build(Player player) {
        // A column now, so the section title can sit above the list and detail panel. The old row
        // lives on as `body`, unchanged apart from taking its height from the leftover space.
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("warps");

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexGrow(1)
                .gapColumn(PADDING));

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
        list.layout(l -> l.widthPercent(100).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.widthPercent(100)));

        left.addChildren(create, search, list);
        body.addChildren(left, detail);
        // The panel last, so it covers the body when opened.
        root.addChildren(HubSection.header("warps", section), body, section.overlay());

        String[] listSignature = {null};
        String[] detailSignature = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            List<Warp> warps = ClientWarpCache.WARPS;

            // The server refuses a warp over the cap anyway; hiding the button means the player is
            // told before they fill the form in rather than after.
            long owned = warps.stream().filter(w -> w.owner().equals(player.getUUID())).count();
            create.setDisplay(owned < ClientWarpCache.LIMIT);

            List<Warp> shown = matching(warps, filter[0]);

            // Approval and the visitor count are in the signature because both are visible on a
            // row: approval as its dot, visitors as the order the server sorted the list into.
            // Keyed on ids alone, a moderator's verdict and a new arrival both went unnoticed.
            String signature = shown.size() + ":" + shown.stream()
                    .map(w -> w.id() + "/" + w.approval().id() + "/" + w.visitorsToday())
                    .reduce("", String::concat)
                    + ":" + selected[0];
            if (!signature.equals(listSignature[0])) {
                listSignature[0] = signature;
                rebuildList(list, shown, selected, creating);
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
                            + current.visitorsToday() + "/" + current.visitsTotal());
            if (!mode.equals(detailSignature[0])) {
                detailSignature[0] = mode;
                detail.clearAllChildren();
                if (creating[0]) {
                    buildCreateForm(player, detail, creating);
                } else {
                    buildDetail(player, detail, current, selected, creating);
                }
            }
        });

        return root;
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

    private static void rebuildList(ScrollerView list, List<Warp> warps,
                                    UUID[] selected, boolean[] creating) {
        list.clearAllScrollViewChildren();
        // Rendered in the order it arrived. Official first and then the busiest is decided by
        // WarpData.sorted() on the server, which is the side that owns the visit counts — sorting
        // again here would only be a second opinion that could disagree with the detail panel.
        for (Warp warp : warps) {
            boolean isSelected = warp.id().equals(selected[0]);

            // A row is a container rather than a bare Label so the status badge has somewhere to sit:
            // the badge opens the row at a fixed width and the name takes the slack, which is what
            // keeps every name starting in the same column.
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(LIST_ROW_H)
                    .marginBottom(2).paddingAll(2).gapColumn(3));
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
            badge.style(s -> s.background(warp.approval().icon()));
            badge.addEventListener(UIEvents.HOVER_TOOLTIPS,
                    e -> e.hoverTooltips = new HoverTooltips(
                            List.of(warp.approval().line()), null, null, ItemStack.EMPTY));
            row.addChild(badge);

            Label name = new Label();
            name.setText(LegacyText.display(warp.name()));
            name.layout(l -> l.flexGrow(1).height(MARK));
            name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            row.addChild(name);

            list.addScrollViewChild(row);
        }
    }

    /* -------------------------------------------- detail -------------------------------------------- */

    private static void buildDetail(Player player, UIElement detail, @Nullable Warp warp,
                                    UUID[] selected, boolean[] creating) {
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
        teleport.setOnClick(e -> PacketDistributor.sendToServer(
                WarpActionPacket.teleport(warp.id())));

        detail.addChildren(description, teleport);

        if (warp.owner().equals(player.getUUID())) {
            Button delete = new Button();
            delete.setText(Component.translatable("gui.roll_mod.hub.warp.delete"));
            delete.layout(l -> l.widthPercent(100).height(ROW_H).marginTop(3));
            delete.setOnClick(e -> PacketDistributor.sendToServer(
                    WarpActionPacket.delete(warp.id())));
            detail.addChild(delete);
        }

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
