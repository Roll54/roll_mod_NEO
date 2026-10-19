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
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.letters.ClientLetterCache;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.LetterView;
import com.roll_54.roll_mod.minestar.letters.LetterText;
import com.roll_54.roll_mod.network.packet.LetterAcceptPacket;
import com.roll_54.roll_mod.network.packet.LetterDismissPacket;
import com.roll_54.roll_mod.util.Durations;
import com.roll_54.roll_mod.util.LegacyText;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Letters from the staff: the list on the left, the open one on the right.
 *
 * <p>No sync values at all — everything comes from {@link ClientLetterCache}, which
 * {@code SyncLettersPacket} keeps current — so this tab can be rebuilt freely.
 *
 * <p>A letter is shown in full whether or not it has been accepted, rewards included. Accepting only
 * decides whether the button is still there — and, once it is accepted, the reader may delete it
 * from their list.
 *
 * <p>Bodies may hold links, {@code [text](https://…)} or a bare address; see {@link LetterText}.
 */
public final class LettersTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 130;
    private static final int ROW_H = 16;
    private static final int LINE_H = 12;
    /** Space between reward chips, as {@code LetterRewardChips.strip} lays them. */
    private static final int CHIP_GAP = 3;
    /** The page arrows either side of the reward row. */
    private static final int ARROW_W = 14;
    /** Chips per page before the row has been laid out and can be measured — what the default hub fits. */
    private static final int DEFAULT_PER_PAGE = 12;
    /** Space between any text and the edge of the box it sits in. */
    private static final int TEXT_INSET = 4;

    private static final int COLOR_PANEL = 0x40000000;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_SELECTED = 0x60FFFFFF;

    private LettersTab() {}

    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("letters");

        UUID[] selected = {null};

        ScrollerView list = scroller();
        // Fixed width: scroller() grows, which in this row would mean sideways.
        list.layout(l -> l.width(LIST_W).flexGrow(0).flexBasis(LIST_W).heightPercent(100));
        list.viewPort(v -> v.layout(l -> l.paddingAll(2)));

        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .paddingAll(PADDING).gapRow(3));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0)
                .flexGrow(1).gapColumn(1));
        // The divider between the list and the panel: drag to resize, double-click to reset.
        body.addChildren(list, HubSplit.handle("letters", list, LIST_W, player.level().isClientSide()), detail);

        root.addChildren(HubSection.header("letters", section), body, section.overlay());

        int[] listVersion = {-1};
        UUID[] listSelected = {null};
        String[] detailSig = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            // Staff are sent due, expired and deleted letters too, for the moderation tab; a reader
            // only ever sees what is out, in date and still in their list.
            long now = System.currentTimeMillis();
            List<LetterView> letters = ClientLetterCache.LETTERS.stream()
                    .filter(l -> l.visible(now) && !l.dismissed()).toList();
            // A withdrawn or expired letter takes its selection with it.
            if (selected[0] != null && find(letters, selected[0]) == null) selected[0] = null;
            // Opening the tab lands on the newest unread letter, or the newest one.
            if (selected[0] == null && !letters.isEmpty()) {
                selected[0] = letters.stream().filter(l -> !l.accepted()).findFirst()
                        .orElse(letters.get(0)).id();
            }

            // Size too: a due letter going out, or one expiring, changes the list with no new packet.
            int version = ClientLetterCache.VERSION * 31 + letters.size();
            if (listVersion[0] != version || listSelected[0] != selected[0]) {
                listVersion[0] = version;
                listSelected[0] = selected[0];
                rebuildList(list, letters, selected);
            }

            LetterView current = find(letters, selected[0]);
            String sig = ClientLetterCache.VERSION + ":" + selected[0] + ":" + letters.size();
            if (!sig.equals(detailSig[0])) {
                detailSig[0] = sig;
                detail.clearAllChildren();
                buildDetail(detail, current);
            }
        });

        return root;
    }

    private static void rebuildList(ScrollerView list, List<LetterView> letters, UUID[] selected) {
        list.clearAllScrollViewChildren();
        if (letters.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.letters.none").withStyle(ChatFormatting.GRAY));
            none.layout(l -> l.widthPercent(100));
            none.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
            list.addScrollViewChild(none);
            return;
        }
        for (LetterView letter : letters) {
            boolean isSelected = letter.id().equals(selected[0]);
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                    .marginBottom(2).paddingLeft(HubSection.ROW_INSET).paddingRight(Math.max(TEXT_INSET, HubSection.ROW_INSET)).gapColumn(3));
            row.style(s -> s.background(new ColorRectTexture(isSelected ? COLOR_ROW_SELECTED : COLOR_ROW)));
            row.addEventListener(UIEvents.CLICK, e -> selected[0] = letter.id());
            // Unread shows on the icon itself: the marked envelope, or the badge on a custom icon.
            // A long title scrolls while the row is hovered, so the whole of it can be read.
            MarqueeLabel title = new MarqueeLabel(LegacyText.display(letter.title()),
                    row::isSelfOrChildHover, 0xFFFFFFFF);
            title.layout(l -> l.flexGrow(1).flexShrink(1).height(ROW_H));
            row.addChildren(LetterIcons.element(letter.icon(), !letter.accepted()), title);
            list.addScrollViewChild(row);
        }
    }

    private static void buildDetail(UIElement detail, @Nullable LetterView letter) {
        if (letter == null) {
            Label empty = new Label();
            empty.setText(Component.translatable("gui.roll_mod.letters.pick").withStyle(ChatFormatting.GRAY));
            empty.layout(l -> l.widthPercent(100).height(LINE_H));
            empty.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
            detail.addChild(empty);
            return;
        }

        Label title = new Label();
        title.setText(LegacyText.display(letter.title()));
        title.layout(l -> l.widthPercent(100).height(LINE_H + 2).paddingHorizontal(TEXT_INSET));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textShadow(true));

        Label meta = new Label();
        meta.setText(meta(letter).withStyle(ChatFormatting.DARK_GRAY));
        meta.layout(l -> l.widthPercent(100).height(LINE_H).paddingHorizontal(TEXT_INSET));
        meta.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));

        detail.addChildren(title, meta, bodyView(letter.body()));

        if (!letter.rewards().isEmpty()) {
            detail.addChild(rewardsView(letter.rewards()));
        }

        Button accept = new Button();
        accept.layout(l -> l.widthPercent(100).height(ROW_H));
        if (letter.accepted()) {
            accept.setText(Component.translatable("gui.roll_mod.letters.accepted"));
            accept.setActive(false);
            accept.layout(l -> l.widthPercent(50).flexGrow(1));
            UIElement buttons = new UIElement();
            buttons.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(3));
            buttons.addChildren(accept, deleteButton(letter));
            detail.addChild(buttons);
            return;
        } else {
            accept.setText(Component.translatable(letter.rewards().isEmpty()
                    ? "gui.roll_mod.letters.read" : "gui.roll_mod.letters.accept"));
            accept.setOnClick(e -> {
                PacketDistributor.sendToServer(new LetterAcceptPacket(letter.id()));
                // Greyed at once; the server's answer replaces the whole letter a moment later.
                accept.setActive(false);
            });
        }
        detail.addChild(accept);
    }

    /**
     * Deletes the letter from the reader's list. Two clicks: the first only arms it, so a stray
     * click cannot throw a letter away.
     */
    private static Button deleteButton(LetterView letter) {
        Button delete = new Button();
        delete.layout(l -> l.widthPercent(50).flexGrow(1).height(ROW_H));
        delete.setText(Component.translatable("gui.roll_mod.letters.delete"));
        boolean[] armed = {false};
        delete.setOnClick(e -> {
            if (!armed[0]) {
                armed[0] = true;
                delete.setText(Component.translatable("gui.roll_mod.letters.delete.confirm")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            PacketDistributor.sendToServer(new LetterDismissPacket(letter.id()));
            delete.setActive(false);
        });
        return delete;
    }

    /** "from Steve · 23.09 14:05 · expires in 2:03:00:00". */
    private static MutableComponent meta(LetterView letter) {
        MutableComponent meta = Component.translatable("gui.roll_mod.letters.from", letter.author(),
                new SimpleDateFormat("dd.MM HH:mm").format(new Date(letter.createdAt())));
        if (letter.expiresAt() > 0L) {
            meta.append(Component.translatable("gui.roll_mod.letters.expires",
                    Durations.format(Math.max(0L, letter.expiresAt() - System.currentTimeMillis()))));
        }
        return meta;
    }

    /**
     * The body as a scrolling column of wrapped lines, one {@link Label} per {@code \n}-separated
     * line so a blank line in the text is a blank line on screen.
     *
     * <p>{@code flexBasis(0)} and the COLUMN container are the two things that make it scroll at
     * all — see {@link #scroller()}.
     */
    static ScrollerView bodyView(String body) {
        ScrollerView view = scroller();
        // Inset on every side, with extra on the right for the scroll bar, so no line of the letter
        // runs into the edge of its box.
        view.viewPort(v -> v.layout(l -> l.paddingAll(TEXT_INSET).paddingRight(TEXT_INSET + 4)));
        view.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        for (String line : body.split("\n", -1)) {
            Label label = new LinkLabel();
            label.setText(line.isEmpty() ? Component.literal(" ") : LetterText.display(line));
            label.layout(l -> l.widthPercent(100).marginBottom(1));
            label.textStyle(t -> t.font(HubInfo.FONT).textWrap(TextWrap.WRAP).adaptiveHeight(true));
            view.addScrollViewChild(label);
        }
        return view;
    }

    static ScrollerView scroller() {
        ScrollerView view = new ScrollerView();
        view.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        return view;
    }

    /**
     * The attachments as one row of chips, paged: a shop letter can carry dozens of products, and
     * left to wrap they would squeeze the body out of the panel. The row holds as many chips as fit
     * its width — about {@link #DEFAULT_PER_PAGE} in a default-size hub, more as the hub is widened —
     * re-measured every tick, so resizing the hub reflows the pages on the spot.
     */
    private static UIElement rewardsView(List<LetterReward> rewards) {
        UIElement box = new UIElement();
        box.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexShrink(0));

        UIElement head = new UIElement();
        head.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(LINE_H)
                .paddingHorizontal(TEXT_INSET));
        Label caption = new Label();
        caption.setText(Component.translatable("gui.roll_mod.letters.rewards").withStyle(ChatFormatting.GRAY));
        caption.layout(l -> l.flexGrow(1).height(LINE_H));
        Label pageLabel = new Label();
        pageLabel.layout(l -> l.width(60).height(LINE_H));
        pageLabel.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
        head.addChildren(caption, pageLabel);

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(LetterRewardChips.SLOT)
                .gapColumn(CHIP_GAP));
        Button prev = pageArrow("‹");
        Button next = pageArrow("›");
        UIElement chips = new UIElement();
        chips.layout(l -> l.flexBasis(0).flexGrow(1).height(LetterRewardChips.SLOT));
        chips.setOverflowVisible(false);
        row.addChildren(prev, chips, next);
        box.addChildren(head, row);

        int[] page = {0};
        int[] shown = {-1, -1}; // per page, page
        prev.setOnClick(e -> page[0]--);
        next.setOnClick(e -> page[0]++);
        row.addEventListener(UIEvents.TICK, e -> {
            float width = row.getSizeWidth();
            int all = fit(width);
            boolean paged = rewards.size() > all;
            int perPage = paged ? fit(width - 2 * (ARROW_W + CHIP_GAP)) : Math.max(1, rewards.size());
            int pages = Math.max(1, (rewards.size() + perPage - 1) / perPage);
            page[0] = Math.floorMod(page[0], pages); // past the last wraps to the first, and back
            if (perPage == shown[0] && page[0] == shown[1]) return;
            shown[0] = perPage;
            shown[1] = page[0];

            prev.setDisplay(paged);
            next.setDisplay(paged);
            pageLabel.setText(paged ? Component.literal((page[0] + 1) + " / " + pages).withStyle(ChatFormatting.DARK_GRAY)
                    : Component.empty());
            int from = page[0] * perPage;
            chips.clearAllChildren();
            chips.addChild(LetterRewardChips.strip(rewards.subList(from, Math.min(rewards.size(), from + perPage)),
                    null, null));
        });
        return box;
    }

    /** How many chips a row of this width holds; the default before it has been laid out. */
    private static int fit(float width) {
        if (width <= 0f) return DEFAULT_PER_PAGE;
        return Math.max(1, (int) ((width + CHIP_GAP) / (LetterRewardChips.SLOT + CHIP_GAP)));
    }

    private static Button pageArrow(String text) {
        Button button = new Button();
        button.setText(Component.literal(text));
        button.layout(l -> l.width(ARROW_W).height(LetterRewardChips.SLOT).flexShrink(0).paddingAll(0));
        HubSection.centreCaption(button);
        return button;
    }

    @Nullable
    private static LetterView find(List<LetterView> letters, @Nullable UUID id) {
        if (id == null) return null;
        for (LetterView letter : letters) {
            if (letter.id().equals(id)) return letter;
        }
        return null;
    }
}
