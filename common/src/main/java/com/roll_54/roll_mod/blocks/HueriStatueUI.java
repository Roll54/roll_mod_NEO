package com.roll_54.roll_mod.blocks;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.math.Size;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import com.roll_54.roll_mod.minestar.hub.gui.LinkLabel;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.network.packet.HueriStatuePackets;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * The Hueri statue's page: a header, two tabs — the memorial's text and the event's participants,
 * both from {@link HueriStatueText} — one wrapping {@link LinkLabel} per paragraph so links and
 * {@code <item:…>} mentions show their hover. Under the participants list sits the button that
 * leaves flowers at the statue ({@link HueriStatueFlowers}), through a small popup.
 *
 * <p>Drawn in the player's theme ({@link RollUi}), centred, at most {@link #MAX_W}x{@link #MAX_H}.
 * Built on both sides like every LdLib2 UI here; the server's copy has no paragraphs, since the text
 * is a client asset and nothing in this screen syncs — which is also why its tabs can simply hide
 * the page they are not on (what keeps {@code HubUI} off {@code TabView} does not apply here). The donation is a
 * packet of its own.
 */
public final class HueriStatueUI {

    private static final int MAX_W = 360;
    private static final int MAX_H = 260;
    private static final int MARGIN = 16;
    private static final int PADDING = 10;
    private static final int TAB_H = 16;
    private static final int BUTTON_H = 18;
    private static final int POPUP_W = 240;
    private static final int POPUP_H = 120;

    /** The header's colour, #E5C48B — the default header's, and any part of a file's header that sets none. */
    private static final int TEXT_TITLE = 0xFFE5C48B;
    /** The donors' colour, here and over their flowers. */
    private static final int TEXT_DONOR = 0x76CCA8;

    private HueriStatueUI() {}

    public static ModularUI createUI(Player player, BlockPos pos) {
        UIElement root = new UIElement();
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(PADDING).gapRow(6));

        // The text is a client asset; the server's copy of the screen has none of it.
        boolean client = player.level().isClientSide();
        HueriStatueText.Page page = client ? HueriStatueText.load(HueriStatueText.FOLDER_TEXT) : null;
        HueriStatueText.Page participants = client ? HueriStatueText.load(HueriStatueText.FOLDER_PARTICIPANTS) : null;

        // The file's own header if it has one, styled as written; else the block's name.
        Label title = new Label();
        title.setText(page != null && page.title() != null ? page.title()
                : Component.translatable("block.roll_mod.hueri_statue")
                        .withStyle(ChatFormatting.BOLD).withColor(TEXT_TITLE & 0xFFFFFF));
        title.layout(l -> l.widthPercent(100));
        title.textStyle(t -> t.textColor(TEXT_TITLE).textAlignHorizontal(Horizontal.CENTER).fontSize(12));
        root.addChild(title);

        UIElement tabRow = new UIElement();
        tabRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(TAB_H).flexShrink(0).gapColumn(2));
        Tab memorialTab = tab(tabRow, "gui.roll_mod.hueri_statue.tab.memorial");
        Tab participantsTab = tab(tabRow, "gui.roll_mod.hueri_statue.tab.participants");
        root.addChild(tabRow);

        UIElement memorialPage = textPage(page);
        root.addChild(memorialPage);

        UIElement participantsPage = new UIElement();
        participantsPage.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .flexBasis(0).flexGrow(1).gapRow(6));
        ScrollerView participantsList = textPage(participants);
        participantsPage.addChild(participantsList);
        if (client) {
            // Donors follow the file's players, live: one who leaves flowers while the screen is up
            // shows at once, from the statue's synced copy.
            List<java.util.UUID> listed = HueriStatueText.ids(HueriStatueText.FOLDER_PARTICIPANTS);
            int[] shown = {0};
            Runnable refill = () -> {
                List<HueriStatueBlockEntity.Flower> donors = donors(player, pos, listed);
                if (donors.size() == shown[0]) return;
                shown[0] = donors.size();
                fill(participantsList, participants, donors);
            };
            refill.run();
            participantsPage.addEventListener(UIEvents.TICK, e -> refill.run());
        }

        DonatePopup popup = new DonatePopup(player, pos);
        Button donate = new Button();
        donate.setText(Component.translatable("gui.roll_mod.hueri_statue.flowers.button").withColor(TEXT_DONOR));
        donate.layout(l -> l.widthPercent(100).height(BUTTON_H).flexShrink(0));
        donate.setOnClick(e -> popup.open());
        participantsPage.addChild(donate);
        root.addChild(participantsPage);

        List<Tab> tabs = List.of(memorialTab, participantsTab);
        List<UIElement> pages = List.of(memorialPage, participantsPage);
        for (int i = 0; i < tabs.size(); i++) {
            int index = i;
            // Tab carries no click listener of its own; TabView normally supplies one.
            tabs.get(i).addEventListener(UIEvents.CLICK, e -> select(tabs, pages, index));
        }
        select(tabs, pages, 0);

        // Overlays go on the root, last.
        root.addChild(popup.root());

        return ModularUI.of(RollUi.of(player, root, screen -> Size.of(
                Math.min(MAX_W, screen.width - 2 * MARGIN), Math.min(MAX_H, screen.height - 2 * MARGIN))), player);
    }

    private static Tab tab(UIElement row, String key) {
        Tab tab = new Tab();
        tab.setText(Component.translatable(key));
        tab.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        row.addChild(tab);
        return tab;
    }

    private static void select(List<Tab> tabs, List<UIElement> pages, int selected) {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setDisplay(i == selected);
            tabs.get(i).setSelected(i == selected);
        }
    }

    /**
     * The players who left flowers at the statue, first donation first, one per player, leaving out
     * those the participants file already names. Client only: read from the statue's synced copy.
     */
    private static List<HueriStatueBlockEntity.Flower> donors(Player player, BlockPos pos, List<java.util.UUID> listed) {
        List<HueriStatueBlockEntity.Flower> out = new java.util.ArrayList<>();
        if (!(player.level().getBlockEntity(pos) instanceof HueriStatueBlockEntity statue)) return out;
        java.util.Set<java.util.UUID> seen = new java.util.HashSet<>(listed);
        for (HueriStatueBlockEntity.Flower flower : statue.flowers()) {
            if (seen.add(flower.donor())) out.add(flower);
        }
        return out;
    }

    /**
     * Refills {@code list} from scratch — {@code page}'s paragraphs, then a line per donor — the way
     * the skins picker rebuilds its list: cleared, layout cache dropped, so the scroller measures
     * the new content rather than keeping the first layout's.
     */
    private static void fill(ScrollerView list, @javax.annotation.Nullable HueriStatueText.Page page,
                             List<HueriStatueBlockEntity.Flower> donors) {
        list.clearAllScrollViewChildren();
        list.clearLayoutCache();
        if (page != null) {
            for (Component paragraph : page.body()) list.addScrollViewChild(paragraph(paragraph));
        }
        if (!donors.isEmpty()) {
            // Not at the event: they came later, through the button below.
            list.addScrollViewChild(paragraph(Component.literal("\n")
                    .append(Component.translatable("gui.roll_mod.hueri_statue.flowers.donors"))
                    .withStyle(ChatFormatting.GRAY)));
        }
        for (HueriStatueBlockEntity.Flower donor : donors) {
            // As a <player:UUID:rank> tag shows; the stored name until the server has described them.
            HueriStatuePlayers.Entry entry = ClientHueriStatue.PLAYERS.get(donor.donor());
            list.addScrollViewChild(paragraph(entry != null
                    ? HueriStatuePlayers.display(entry, true) : Component.literal(donor.donorName())));
        }
    }

    private static Label paragraph(Component text) {
        Label label = new LinkLabel();
        label.setText(text);
        label.layout(l -> l.widthPercent(100));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

    /** A scrolling column of {@code page}'s paragraphs; empty on the server. */
    private static ScrollerView textPage(HueriStatueText.Page page) {
        // flexBasis(0) for the reason DailyTasksUI gives: an auto basis sizes the scroller to its
        // content, and it never overflows.
        ScrollerView body = new ScrollerView();
        body.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        body.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        body.viewPort(v -> v.layout(l -> l.paddingAll(4).paddingRight(8)));
        body.addClass("preview_bg");
        body.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(4)));

        // Filled before the first layout, for the reason HubInfoWindow gives: lines added later
        // leave the scroller believing it has nothing to scroll.
        if (page != null) {
            for (Component paragraph : page.body()) body.addScrollViewChild(paragraph(paragraph));
        }
        return body;
    }

    /**
     * "Leave flowers": what it takes, how many the player has, Donate and Cancel. Refreshed on every
     * open from this client's inventory and its copy of the statue, and checked again by the server.
     */
    private static final class DonatePopup {

        private final Player player;
        private final BlockPos pos;
        private final HubSection.Floating floating;
        private final Label have = new Label();
        private final Button accept = new Button();

        DonatePopup(Player player, BlockPos pos) {
            this.player = player;
            this.pos = pos;
            floating = HubSection.popup(Component.translatable("gui.roll_mod.hueri_statue.flowers.title"),
                    POPUP_W, POPUP_H, body -> {
                        body.layout(l -> l.gapRow(4));
                        Label text = new Label();
                        text.setText(Component.translatable("gui.roll_mod.hueri_statue.flowers.text",
                                HueriStatueFlowers.COUNT));
                        text.layout(l -> l.widthPercent(100).flexGrow(1));
                        text.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
                        have.layout(l -> l.widthPercent(100));

                        UIElement buttons = new UIElement();
                        buttons.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100)
                                .height(BUTTON_H).flexShrink(0).gapColumn(4));
                        accept.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                        accept.setOnClick(e -> {
                            close();
                            PacketDistributor.sendToServer(new HueriStatuePackets.Donate(pos));
                        });
                        Button cancel = new Button();
                        cancel.setText(Component.translatable("gui.roll_mod.hub.confirm.cancel"));
                        cancel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                        cancel.setOnClick(e -> close());
                        buttons.addChildren(accept, cancel);
                        body.addChildren(text, have, buttons);
                    })
                    .noCloseButton();
        }

        UIElement root() {
            return floating.root();
        }

        void open() {
            int count = HueriStatueFlowers.count(player);
            boolean donated = player.level().getBlockEntity(pos) instanceof HueriStatueBlockEntity statue
                    && statue.hasDonated(player.getUUID());
            boolean enough = count >= HueriStatueFlowers.COUNT;
            have.setText(Component.translatable("gui.roll_mod.hueri_statue.flowers.have", count, HueriStatueFlowers.COUNT)
                    .withStyle(enough ? ChatFormatting.GREEN : ChatFormatting.RED));
            accept.setText(donated
                    ? Component.translatable("gui.roll_mod.hueri_statue.flowers.already").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("gui.roll_mod.hueri_statue.flowers.donate").withColor(TEXT_DONOR));
            accept.setActive(enough && !donated);
            floating.open();
        }

        private void close() {
            floating.close();
        }
    }
}
