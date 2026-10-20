package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.deathlog.ClientDeathLogCache;
import com.roll_54.roll_mod.minestar.deathlog.DeathLogStore;
import com.roll_54.roll_mod.minestar.deathlog.DeathRecord;
import com.roll_54.roll_mod.network.packet.DeathLogPackets;
import com.roll_54.roll_mod.network.packet.DeathLogPackets.Summary;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * The moderation tab's "Deaths" mode: every player with a death on record on the left, their deaths
 * on top of the right, and the chosen death below — the inventory as it was, the curios and the
 * cyberware — with the two ways to give it back.
 *
 * <p>Fed by {@link ClientDeathLogCache}, on request only: a death carries a whole inventory, so
 * nothing is sent until a moderator picks it. The mode is offered only to those who may use it,
 * and the server checks again on every request.
 *
 * <p>Slots are {@link VendorUIHelper#phantomSlot()} plus {@code setItem}, never bound — see
 * {@link LetterRewardChips} for why that is the only safe kind inside a rebuild.
 */
final class DeathLogPanel {

    private static final int ROW_H = 16;
    private static final int LINE_H = 12;
    private static final int SLOT = LetterRewardChips.SLOT;
    private static final int GAP = 3;
    private static final int LIST_H = 4 * (ROW_H + 2);
    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;
    private static final int COLOR_SLOT = 0x30FFFFFF;

    /** Armour slots top to bottom, as the player's inventory screen shows them. */
    private static final int[] ARMOUR = {39, 38, 37, 36};
    private static final int OFFHAND = 40;

    /** The player whose deaths are shown, picked from the moderation tab's left list. */
    final UUID[] victim = {null};
    /** The death whose stacks are shown. */
    private final UUID[] death = {null};

    private final HubConfirm confirmer;
    /** The tab is built on both sides; only the client's copy asks the server for anything. */
    private final boolean client;
    private final Label title = new Label();
    private final ScrollerView deaths = scroller();
    private final ScrollerView detail = scroller();
    private final UIElement actions = new UIElement();

    private UUID requestedVictim;
    private UUID requestedDeath;
    private String listSig;
    private String detailSig;

    /** Fills {@code pane}, one of the moderation tab's stacked panes. */
    DeathLogPanel(UIElement pane, HubConfirm confirmer, boolean client) {
        this.confirmer = confirmer;
        this.client = client;

        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP)
                .flexShrink(0));
        title.layout(l -> l.flexGrow(1).height(ROW_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        Button refresh = new Button();
        refresh.setText(Component.literal("⟳"));
        refresh.layout(l -> l.width(ROW_H + 2).height(ROW_H).paddingAll(0).flexShrink(0));
        HubSection.centreCaption(refresh);
        refresh.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.moderation.deathlog.refresh")), null, null,
                ItemStack.EMPTY));
        refresh.setOnClick(e -> refresh());
        header.addChildren(title, refresh);

        deaths.layout(l -> l.height(LIST_H).flexBasis(LIST_H).flexGrow(0).flexShrink(0));
        deaths.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        // What the marks on a row mean.
        deaths.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                Component.literal("↺ ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable("gui.roll_mod.hub.moderation.deathlog.legend.restored")),
                Component.literal("⚑ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("gui.roll_mod.hub.moderation.deathlog.legend.kept"))),
                null, null, ItemStack.EMPTY));

        UIElement box = new UIElement();
        box.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1)
                .paddingAll(4));
        box.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        box.addChild(detail);

        actions.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP)
                .flexShrink(0));

        pane.addChildren(header, deaths, box, actions);
    }

    /** Back to nothing picked, and a fresh player list; called when the mode is entered. */
    void enter() {
        victim[0] = null;
        death[0] = null;
        requestedVictim = null;
        requestedDeath = null;
        listSig = null;
        detailSig = null;
        if (client) PacketDistributor.sendToServer(DeathLogPackets.Request.index());
    }

    private void refresh() {
        if (!client) return;
        PacketDistributor.sendToServer(DeathLogPackets.Request.index());
        if (victim[0] != null) PacketDistributor.sendToServer(DeathLogPackets.Request.list(victim[0]));
        if (victim[0] != null && death[0] != null) {
            PacketDistributor.sendToServer(DeathLogPackets.Request.detail(victim[0], death[0]));
        }
    }

    void tick() {
        if (!client) return;
        // Asks once per pick; the answer lands in the cache and the signatures below pick it up.
        if (!Objects.equals(victim[0], requestedVictim)) {
            requestedVictim = victim[0];
            death[0] = null;
            if (victim[0] != null) PacketDistributor.sendToServer(DeathLogPackets.Request.list(victim[0]));
        }
        if (!Objects.equals(death[0], requestedDeath)) {
            requestedDeath = death[0];
            if (victim[0] != null && death[0] != null) {
                PacketDistributor.sendToServer(DeathLogPackets.Request.detail(victim[0], death[0]));
            }
        }

        boolean ours = victim[0] != null && victim[0].equals(ClientDeathLogCache.DEATHS_OF);
        List<Summary> list = ours ? ClientDeathLogCache.DEATHS : List.of();

        String lSig = ClientDeathLogCache.VERSION + "|" + victim[0] + "|" + death[0];
        if (!lSig.equals(listSig)) {
            listSig = lSig;
            DeathLogStore.Victim who = findVictim(victim[0]);
            title.setText(who == null
                    ? Component.translatable("gui.roll_mod.hub.moderation.deathlog.title.none").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.roll_mod.hub.moderation.deathlog.title",
                            Component.literal(who.name()).withStyle(ChatFormatting.WHITE), who.deaths()));
            rebuildDeaths(list);
        }

        Summary current = find(list, death[0]);
        boolean loaded = current != null && current.id().equals(ClientDeathLogCache.DETAIL_ID);
        String dSig = ClientDeathLogCache.VERSION + "|" + death[0] + "|" + loaded;
        if (!dSig.equals(detailSig)) {
            detailSig = dSig;
            detail.clearAllScrollViewChildren();
            actions.clearAllChildren();
            buildDetail(current, loaded);
        }
    }

    /* ------------------------------------------- left list ------------------------------------------- */

    /** Every player with a death on file whose name matches the search, latest death first. */
    static void rebuildVictims(ScrollerView list, String filter, UUID[] selected) {
        list.clearAllScrollViewChildren();
        SimpleDateFormat when = new SimpleDateFormat("dd.MM HH:mm");
        for (DeathLogStore.Victim v : ClientDeathLogCache.VICTIMS) {
            if (!filter.isEmpty() && !v.name().toLowerCase().contains(filter)) continue;
            boolean isSelected = v.id().equals(selected[0]);

            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                    .marginBottom(2).paddingHorizontal(HubSection.ROW_INSET));
            row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            row.addEventListener(UIEvents.CLICK, e -> selected[0] = v.id().equals(selected[0]) ? null : v.id());
            row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable("gui.roll_mod.hub.moderation.deathlog.last",
                            when.format(new Date(v.lastDeath())))), null, null, ItemStack.EMPTY));

            Label name = new Label();
            name.setText(Component.literal(v.name()).append(
                    Component.literal(" ☠" + v.deaths()).withStyle(ChatFormatting.DARK_GRAY)));
            name.layout(l -> l.flexGrow(1).height(ROW_H));
            name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            name.setAllowHitTest(false);
            row.addChild(name);
            list.addScrollViewChild(row);
        }
    }

    /* ------------------------------------------ deaths list ------------------------------------------ */

    private void rebuildDeaths(List<Summary> list) {
        deaths.clearAllScrollViewChildren();
        SimpleDateFormat when = new SimpleDateFormat("dd.MM.yy HH:mm");
        for (Summary s : list) {
            boolean isSelected = s.id().equals(death[0]);
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                    .marginBottom(2).paddingHorizontal(HubSection.ROW_INSET).gapColumn(GAP));
            row.style(st -> st.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            row.addEventListener(UIEvents.CLICK, e -> death[0] = s.id());

            MutableComponent text = Component.literal(when.format(new Date(s.diedAt())))
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("  ")).append(s.reason().copy().withStyle(ChatFormatting.GRAY));
            if (!s.restores().isEmpty()) {
                // Who handed it out last, so the list answers "has anyone dealt with this?" at a glance.
                DeathRecord.Restore last = s.restores().getLast();
                text.append(Component.literal("  ↺" + s.restores().size() + " " + last.by()).withStyle(ChatFormatting.GREEN));
            }
            if (s.kept()) text.append(Component.literal("  ⚑").withStyle(ChatFormatting.GOLD));

            // The row is cut to one line; the tooltip has the whole reason and every hand-out.
            List<Component> tip = new java.util.ArrayList<>();
            tip.add(s.reason());
            for (DeathRecord.Restore r : s.restores()) tip.add(restoreLine(r, when));
            row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    tip, null, null, ItemStack.EMPTY));

            Label label = new Label();
            label.setText(text);
            label.layout(l -> l.flexGrow(1).height(ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
            label.setAllowHitTest(false);
            row.addChild(label);
            deaths.addScrollViewChild(row);
        }
        if (list.isEmpty() && victim[0] != null) {
            deaths.addScrollViewChild(hint("gui.roll_mod.hub.moderation.deathlog.loading"));
        }
    }

    /* --------------------------------------------- detail -------------------------------------------- */

    private void buildDetail(@Nullable Summary s, boolean loaded) {
        if (s == null) {
            detail.addScrollViewChild(hint(victim[0] == null
                    ? "gui.roll_mod.hub.moderation.deathlog.pickPlayer"
                    : "gui.roll_mod.hub.moderation.deathlog.pick"));
            return;
        }
        SimpleDateFormat when = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.moderation.deathlog.reason",
                s.reason().copy().withStyle(ChatFormatting.WHITE))));
        if (!s.damageType().isEmpty() || !s.killer().isEmpty()) {
            detail.addScrollViewChild(line(Component.translatable(s.killer().isEmpty()
                            ? "gui.roll_mod.hub.moderation.deathlog.damage"
                            : "gui.roll_mod.hub.moderation.deathlog.damage.by",
                    Component.literal(s.damageType()).withStyle(ChatFormatting.DARK_AQUA),
                    Component.literal(s.killer()).withStyle(ChatFormatting.RED))));
        }
        detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.moderation.deathlog.when",
                Component.literal(when.format(new Date(s.diedAt()))).withStyle(ChatFormatting.AQUA))));
        detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.moderation.deathlog.where",
                Component.literal(s.x() + " " + s.y() + " " + s.z()).withStyle(ChatFormatting.AQUA),
                Component.literal(s.dimension()).withStyle(ChatFormatting.AQUA))));
        if (s.kept()) {
            detail.addScrollViewChild(line(Component.translatable("gui.roll_mod.hub.moderation.deathlog.kept")
                    .withStyle(ChatFormatting.GOLD)));
        }
        for (DeathRecord.Restore r : s.restores()) {
            detail.addScrollViewChild(line(restoreLine(r, when)));
        }

        if (!loaded) {
            detail.addScrollViewChild(hint("gui.roll_mod.hub.moderation.deathlog.loading"));
            return;
        }

        detail.addScrollViewChild(caption("gui.roll_mod.hub.moderation.deathlog.inventory"));
        detail.addScrollViewChild(inventoryGrid(ClientDeathLogCache.INVENTORY));
        if (!ClientDeathLogCache.CURIOS.isEmpty()) {
            detail.addScrollViewChild(caption("gui.roll_mod.hub.moderation.deathlog.curios"));
            detail.addScrollViewChild(strip(ClientDeathLogCache.CURIOS));
        }
        if (!ClientDeathLogCache.CYBERWARE.isEmpty()) {
            detail.addScrollViewChild(caption("gui.roll_mod.hub.moderation.deathlog.cyberware"));
            detail.addScrollViewChild(strip(ClientDeathLogCache.CYBERWARE));
        }

        UUID who = victim[0];
        String name = nameOf(who);
        boolean repeat = !s.restores().isEmpty() || s.kept();
        actions.addChildren(
                action("gui.roll_mod.hub.moderation.deathlog.retrieve", () -> confirmer.ask(
                        question("retrieve", name, s, repeat),
                        Component.translatable("gui.roll_mod.hub.confirm.deathlog.retrieve.button"),
                        () -> PacketDistributor.sendToServer(new DeathLogPackets.Act(who, s.id(), DeathRecord.Mode.RETRIEVE)))),
                action("gui.roll_mod.hub.moderation.deathlog.return", () -> confirmer.ask(
                        question("return", name, s, repeat),
                        Component.translatable("gui.roll_mod.hub.confirm.deathlog.return.button"),
                        () -> PacketDistributor.sendToServer(new DeathLogPackets.Act(who, s.id(), DeathRecord.Mode.RETURN)))));
    }

    /** "↺ Steve took them for themselves, 12.10 14:05" / "↺ Steve returned them to Alex, …". */
    private static Component restoreLine(DeathRecord.Restore r, SimpleDateFormat when) {
        return Component.translatable("gui.roll_mod.hub.moderation.deathlog.restored." + r.mode(),
                Component.literal(r.by()).withStyle(ChatFormatting.WHITE),
                Component.literal(r.to()).withStyle(ChatFormatting.WHITE),
                when.format(new Date(r.at()))).withStyle(ChatFormatting.GREEN);
    }

    /** "Give X's things to you?", with the duplication warning when this death was handed out before. */
    private static Component question(String mode, String name, Summary s, boolean repeat) {
        MutableComponent q = Component.translatable("gui.roll_mod.hub.confirm.deathlog." + mode, name, s.items());
        if (repeat) {
            q.append("\n\n").append(Component.translatable(s.kept()
                    ? "gui.roll_mod.hub.confirm.deathlog.kept"
                    : "gui.roll_mod.hub.confirm.deathlog.again", s.restores().size()).withStyle(ChatFormatting.RED));
        }
        return q;
    }

    /**
     * The inventory as the player's own screen lays it out: armour down the left, the three main rows,
     * then the hotbar with the offhand after it.
     */
    private static UIElement inventoryGrid(Map<Integer, ItemStack> items) {
        UIElement grid = new UIElement();
        grid.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(1).marginTop(2));
        for (int row = 0; row < 4; row++) {
            UIElement line = new UIElement();
            line.layout(l -> l.flexDirection(FlexDirection.ROW).gapColumn(1).marginTop(0));
            line.addChild(slot(items.get(ARMOUR[row])));
            line.addChild(spacer());
            // Rows 1-3 are the main inventory (9-35); the last is the hotbar (0-8).
            int first = row < 3 ? 9 + row * 9 : 0;
            for (int i = first; i < first + 9; i++) line.addChild(slot(items.get(i)));
            if (row == 3) {
                line.addChild(spacer());
                line.addChild(slot(items.get(OFFHAND)));
            }
            grid.addChild(line);
        }
        return grid;
    }

    private static UIElement strip(List<ItemStack> stacks) {
        UIElement strip = new UIElement();
        strip.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100)
                .gapColumn(1).gapRow(1).marginTop(2));
        for (ItemStack stack : stacks) strip.addChild(slot(stack));
        return strip;
    }

    private static UIElement slot(@Nullable ItemStack stack) {
        ItemSlot slot = VendorUIHelper.phantomSlot();
        slot.setItem(stack == null ? ItemStack.EMPTY : stack.copy());
        slot.layout(l -> l.width(SLOT).height(SLOT).flexShrink(0));
        slot.style(s -> s.background(new ColorRectTexture(COLOR_SLOT)));
        return slot;
    }

    private static UIElement spacer() {
        UIElement gap = new UIElement();
        gap.layout(l -> l.width(4).height(SLOT).flexShrink(0));
        return gap;
    }

    /* -------------------------------------------- plumbing ------------------------------------------- */

    private static Label line(Component text) {
        Label line = new Label();
        line.setText(text.copy().withStyle(ChatFormatting.GRAY));
        line.layout(l -> l.widthPercent(100));
        line.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return line;
    }

    private static Label caption(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
        label.layout(l -> l.widthPercent(100).height(LINE_H).marginTop(4));
        return label;
    }

    private static Label hint(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100).marginTop(2));
        label.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

    private static Button action(String key, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexGrow(1).height(ROW_H));
        button.setOnClick(e -> onClick.run());
        return button;
    }

    private static ScrollerView scroller() {
        ScrollerView view = new ScrollerView();
        view.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        return view;
    }

    private static @Nullable DeathLogStore.Victim findVictim(@Nullable UUID id) {
        if (id == null) return null;
        for (DeathLogStore.Victim v : ClientDeathLogCache.VICTIMS) {
            if (v.id().equals(id)) return v;
        }
        return null;
    }

    private static String nameOf(@Nullable UUID id) {
        DeathLogStore.Victim v = findVictim(id);
        return v == null ? String.valueOf(id) : v.name();
    }

    private static @Nullable Summary find(List<Summary> list, @Nullable UUID id) {
        if (id == null) return null;
        for (Summary s : list) {
            if (s.id().equals(id)) return s;
        }
        return null;
    }
}
