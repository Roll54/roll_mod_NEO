package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.economy.currency.ledger.HistoryFilter;
import com.roll_54.roll_mod.economy.currency.ledger.LedgerEntry;
import com.roll_54.roll_mod.network.packet.HistoryPackets;
import com.roll_54.roll_mod.util.Durations;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * The money history, opened from the clock button in the Home tab's title row: a filter drop-down
 * over a list of every movement of the player's main money — what, how much, with whom and when.
 * Vending-block and auction rows name the item and the other player.
 *
 * <p>A floating panel of {@link HubSection#panel}, so it shares the one-panel-at-a-time slot with
 * the section explanations. Its rows come from {@link ClientHistoryCache}; opening the panel or
 * changing the filter asks the server again, so the list is always the current one.
 */
final class HistoryPanel {

    private static final int WIDTH = 340;
    private static final int HEIGHT = 240;
    private static final int ROW_H = 20;
    private static final int AMOUNT_W = 64;
    private static final int COLOR_ROW = 0x30FFFFFF;

    private HistoryPanel() {}

    static HubSection.Info build() {
        HistoryFilter[] filter = {HistoryFilter.ALL};
        HubSection.Info info = HubSection.panel(
                Component.translatable("gui.roll_mod.hub.history.title"),
                IGuiTexture.dynamic(() -> Icons.HISTORY),
                Component.translatable("gui.roll_mod.hub.history.tip"),
                // A desktop window, like the interactions popups: a long history reads better large.
                WIDTH, HEIGHT, true,
                content -> fill(content, filter));
        // Opening it fetches afresh: the panel only toggles, and a history from the last opening is
        // stale by the next sale.
        info.button().addEventListener(UIEvents.CLICK,
                e -> PacketDistributor.sendToServer(new HistoryPackets.Request(filter[0])));
        return info;
    }

    private static void fill(UIElement content, HistoryFilter[] filter) {
        Selector<HistoryFilter> selector = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        selector.setCandidateUIProvider(UIElementProvider.text(f -> f == null ? Component.empty() : f.title()));
        selector.setCandidates(List.of(HistoryFilter.values()));
        selector.setSelected(HistoryFilter.ALL, false);
        selector.layout(l -> l.widthPercent(100).height(16).flexShrink(0).marginBottom(3));
        // Above the floating panel it sits in: the drop-down list hangs off the UI root.
        selector.dialog.style(s -> s.zIndex(30));
        selector.setOnValueChanged(f -> {
            if (f == null) return;
            filter[0] = f;
            PacketDistributor.sendToServer(new HistoryPackets.Request(f));
        });

        ScrollerView list = new ScrollerView();
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        list.viewPort(v -> v.layout(l -> l.paddingAll(0)));

        content.addChildren(selector, list);

        int[] shown = {-1};
        list.addEventListener(UIEvents.TICK, e -> {
            int version = ClientHistoryCache.VERSION;
            if (version == shown[0]) return;
            shown[0] = version;
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();
            List<LedgerEntry> entries = ClientHistoryCache.ENTRIES;
            if (!ClientHistoryCache.LOADED) {
                list.addScrollViewChild(note("gui.roll_mod.hub.history.loading"));
            } else if (entries.isEmpty()) {
                list.addScrollViewChild(note("gui.roll_mod.hub.history.empty"));
            } else {
                long now = System.currentTimeMillis();
                for (LedgerEntry entry : entries) list.addScrollViewChild(row(entry, now));
            }
        });
    }

    /** {@code +900}  Vending sale · Diamond ×3 · Bob · 5m ago */
    private static UIElement row(LedgerEntry entry, long now) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(2).paddingHorizontal(HubSection.ROW_INSET).gapColumn(6));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));

        Component amountText = Component.literal((entry.delta() > 0 ? "+" : "−") + money(Math.abs(entry.delta())))
                .withStyle(entry.delta() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
        Label amount = new Label();
        amount.setText(amountText);
        amount.layout(l -> l.width(AMOUNT_W).heightPercent(100).flexShrink(0));
        amount.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT).textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        MutableComponent what = entry.reason().title().copy();
        if (!entry.item().isEmpty()) {
            what.append(Component.literal(" · " + entry.item()
                    + (entry.count() > 1 ? " ×" + entry.count() : "")).withStyle(ChatFormatting.WHITE));
        }
        if (!entry.otherName().isEmpty()) {
            what.append(Component.literal(" · " + entry.otherName()).withStyle(ChatFormatting.AQUA));
        }
        what.append(Component.literal(" · ").append(Component.translatable("gui.roll_mod.hub.history.ago",
                Durations.format(Math.max(0, now - entry.at())))).withStyle(ChatFormatting.DARK_GRAY));

        Label text = new Label();
        text.setText(what.withStyle(ChatFormatting.GRAY));
        text.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        text.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        row.addChildren(amount, text);
        // The full line on hover, for the rows the panel is too narrow to show whole.
        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips =
                new com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips(List.of(amountText, what),
                        null, null, net.minecraft.world.item.ItemStack.EMPTY));
        return row;
    }

    private static Label note(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }

    private static String money(long amount) {
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(amount).replace(',', ' ');
    }
}
