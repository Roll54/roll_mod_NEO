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
import com.roll_54.roll_mod.minestar.letters.LetterView;
import com.roll_54.roll_mod.network.packet.LetterAcceptPacket;
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
 * decides whether the button is still there.
 */
public final class LettersTab {

    private static final int PADDING = 6;
    private static final int LIST_W = 130;
    private static final int ROW_H = 16;
    private static final int LINE_H = 12;
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
        list.viewPort(v -> v.layout(l -> l.paddingAll(2).paddingRight(6)));

        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .paddingAll(PADDING).gapRow(3));
        detail.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0)
                .flexGrow(1).gapColumn(PADDING));
        body.addChildren(list, detail);

        root.addChildren(HubSection.header("letters", section), body, section.overlay());

        int[] listVersion = {-1};
        UUID[] listSelected = {null};
        String[] detailSig = {null};

        root.addEventListener(UIEvents.TICK, e -> {
            // Staff are sent due and expired letters too, for the moderation tab; a reader only
            // ever sees what is out and in date.
            long now = System.currentTimeMillis();
            List<LetterView> letters = ClientLetterCache.LETTERS.stream()
                    .filter(l -> l.visible(now)).toList();
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
                    .marginBottom(2).paddingRight(TEXT_INSET).gapColumn(3));
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
            Label caption = new Label();
            caption.setText(Component.translatable("gui.roll_mod.letters.rewards").withStyle(ChatFormatting.GRAY));
            caption.layout(l -> l.widthPercent(100).height(LINE_H).paddingHorizontal(TEXT_INSET));
            detail.addChildren(caption, LetterRewardChips.strip(letter.rewards(), null, null));
        }

        Button accept = new Button();
        accept.layout(l -> l.widthPercent(100).height(ROW_H));
        if (letter.accepted()) {
            accept.setText(Component.translatable("gui.roll_mod.letters.accepted"));
            accept.setActive(false);
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
            Label label = new Label();
            label.setText(line.isEmpty() ? Component.literal(" ") : LegacyText.display(line));
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

    @Nullable
    private static LetterView find(List<LetterView> letters, @Nullable UUID id) {
        if (id == null) return null;
        for (LetterView letter : letters) {
            if (letter.id().equals(id)) return letter;
        }
        return null;
    }
}
