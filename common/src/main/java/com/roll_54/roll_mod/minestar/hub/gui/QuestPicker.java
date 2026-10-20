package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;

/**
 * A pop-up listing every quest in the FTB Quests book, searchable by title, chapter or id — the
 * shape of FTB Quests' own "select quest" screen. Clicking a quest hands its id to the caller and
 * closes the window.
 *
 * <p>The list is read from the client's copy of the quest file, which FTB sends every player whole,
 * so no packet is needed to fill it. It is re-read on every {@link #open}, so a book edited while
 * the hub is up is picked up the next time the picker opens.
 */
final class QuestPicker {

    private static final int W = 260;
    private static final int H = 210;
    private static final int ROW_H = 20;
    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_HOVER = 0x50FFFFFF;
    /** More than this and the list is cut short; the search narrows it. */
    private static final int MAX_ROWS = 200;

    final HubSection.Floating popup;

    private final LongConsumer onPick;
    private final TextField search = new TextField();
    private final ScrollerView list = new ScrollerView();
    private List<QuestGate.Entry> quests = List.of();
    private String shown;

    QuestPicker(LongConsumer onPick) {
        this.onPick = onPick;
        this.popup = HubSection.popup(Component.translatable("gui.roll_mod.hub.questPicker.title"), W, H, this::fill);
    }

    void open() {
        quests = QuestGate.all(true);
        shown = null;
        search.setText("");
        popup.open();
    }

    private void fill(UIElement body) {
        body.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(3));

        search.setText("");
        search.layout(l -> l.widthPercent(100).height(16).flexShrink(0));
        search.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.questPicker.search")), null, null, ItemStack.EMPTY));

        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        list.viewPort(v -> v.layout(l -> l.paddingAll(0)));

        body.addChildren(search, list);

        // Only ticks while open: a closed popup is hidden.
        body.addEventListener(UIEvents.TICK, e -> {
            String filter = search.getText().strip().toLowerCase();
            if (filter.equals(shown)) return;
            shown = filter;
            rebuild(filter);
        });
    }

    private void rebuild(String filter) {
        list.clearAllScrollViewChildren();
        list.clearLayoutCache();
        if (!QuestGate.available() || quests.isEmpty()) {
            list.addScrollViewChild(note(QuestGate.available()
                    ? "gui.roll_mod.hub.questPicker.empty" : "gui.roll_mod.hub.questPicker.unavailable"));
            return;
        }
        List<QuestGate.Entry> matches = new ArrayList<>();
        for (QuestGate.Entry quest : quests) {
            if (filter.isEmpty() || quest.search().contains(filter)) matches.add(quest);
        }
        if (matches.isEmpty()) {
            list.addScrollViewChild(note("gui.roll_mod.hub.questPicker.none"));
            return;
        }
        for (int i = 0; i < Math.min(matches.size(), MAX_ROWS); i++) list.addScrollViewChild(row(matches.get(i)));
        if (matches.size() > MAX_ROWS) {
            list.addScrollViewChild(note("gui.roll_mod.hub.questPicker.more"));
        }
    }

    private UIElement row(QuestGate.Entry quest) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).height(ROW_H).marginBottom(2)
                .paddingHorizontal(HubSection.ROW_INSET));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        row.addEventListener(UIEvents.MOUSE_ENTER, e -> row.style(s -> s.background(new ColorRectTexture(COLOR_ROW_HOVER))));
        row.addEventListener(UIEvents.MOUSE_LEAVE, e -> row.style(s -> s.background(new ColorRectTexture(COLOR_ROW))));
        row.addEventListener(UIEvents.CLICK, e -> {
            onPick.accept(quest.id());
            popup.close();
        });

        Label title = new Label();
        title.setText(quest.title());
        title.layout(l -> l.widthPercent(100).height(10));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        Label chapter = new Label();
        chapter.setText(quest.chapter().copy().withStyle(ChatFormatting.GRAY)
                .append(Component.literal("  " + QuestGate.code(quest.id())).withStyle(ChatFormatting.DARK_GRAY)));
        chapter.layout(l -> l.widthPercent(100).height(9));
        chapter.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        row.addChildren(title, chapter);
        return row;
    }

    private static Label note(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }
}
